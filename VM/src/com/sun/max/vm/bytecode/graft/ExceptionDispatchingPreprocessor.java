/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.bytecode.graft;

import java.util.*;

import com.sun.max.io.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;

/**
 * Implements exception dispatching as synthesized bytecode.
 * <p>
 * The following example shows how exception dispatching is expressed as synthesized code. The numbered lines
 * represent the original Java source code for a method. The other lines express a) the generated dispatchers as
 * psuedo Java code and b) the map from source lines covered by one or more exception handlers to the dispatcher for
 * the handler(s). The synthesized dispatchers are compiled along with the rest of the original code. The dispatcher
 * map is attached to the method's IR as it is transformed by the compiler and ends up as metadata of the machine
 * code emitted by the compiler backend.
 * <p>
 *
 * <pre>
 *  1:   void f() {
 *  2:       a();
 *  3:       try {
 *  4:           b();
 *  5:           try {
 *  6:               c();
 *  7:               d();
 *  8:           } catch (NullPointerException npe) {
 *  9:               npe.printStackTrace();
 * 10:           } catch (IOException ioe) {
 * 11:               ioe.printStackTrace();
 * 12:               e();
 * 13:           }
 * 14:           f();
 * 15:       } catch(RuntimeException rte) {
 * 16:           rte.printStackTrace();
 * 17:       } catch(InternalError err) {
 * 18:           err.printStrackTrace();
 * 19:       } catch(Throwable t) {
 * 20:           t.printStrackTrace();
 * 21:       }
 * 22:       g();
 *           // Dispatcher1:
 *                if (e instanceof NullPointerException) { npe = (NullPointerException) e; goto 9; }
 *           else if (e instanceof IOException)          { ioe = (IOException) e; goto 11; }
 *           // Dispatcher2:
 *                if (e instanceof RuntimeException) { rte = (RuntimeException) e; goto 16; }
 *           else if (e instanceof InternalError)    { err = (InternalError) e; goto 18; }
 *           else                                    { t = e; goto 20; }
 *           // DispatcherMap:
 *           //  [4] - { Dispatcher2 }
 *           //  [6] - { Dispatcher1, Dispatcher2 }
 *           //  [7] - { Dispatcher1, Dispatcher2 }
 *           //  [9] - { Dispatcher2 }
 *           // [11] - { Dispatcher2 }
 *           // [12] - { Dispatcher2 }
 *           // [14] - { Dispatcher2 }
 * 23:   }
 * </pre>
 *
 * If a dispatcher does not match the exception object to a handler and it is the last dispatcher in a dispatcher
 * chain, then it simply rethrows the exception.
 * <p>
 * Note that the dispatcher for the outer try/catch block unconditionally goes to the handler for Throwable. This is
 * an optimization reflecting the fact that all exceptions subclass Throwable.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public final class ExceptionDispatchingPreprocessor extends BytecodeAssembler {

    /**
     * Specifies whether the current runtime requires this preprocessing.
     */
    public static boolean REQUIRED = CPSCompiler.Static.compiler() != null;

    private final SeekableByteArrayOutputStream codeStream;
    private final CodeAttribute result;

    public ExceptionDispatchingPreprocessor(ConstantPoolEditor constantPoolEditor, CodeAttribute codeAttribute) {
        super(constantPoolEditor, codeAttribute.code().length, codeAttribute.maxStack, codeAttribute.maxLocals);

        codeStream = new SeekableByteArrayOutputStream();

        final ExceptionDispatcher[] dispatcherMap = synthesizeExceptionDispatchers(codeAttribute.code(), codeAttribute.exceptionHandlerTable());
        final ExceptionHandlerEntry[] exceptionDispatcherTable = exceptionDispatcherTable(dispatcherMap);

        fixup();

        final byte[] originalCode = codeAttribute.code();
        final byte[] code = Arrays.copyOf(originalCode, originalCode.length + codeStream.size());
        codeStream.copyTo(0, code, originalCode.length, codeStream.size());
        result = new CodeAttribute(codeAttribute.constantPool,
                                    code,
                                    (char) maxStack(),
                                    (char) maxLocals(),
                                    exceptionDispatcherTable,
                                    codeAttribute.lineNumberTable(),
                                    codeAttribute.localVariableTable(),
                                    codeAttribute.stackMapTable());
    }

    @Override
    protected void setWritePosition(int position) {
        codeStream.seek(position);
    }

    @Override
    protected void writeByte(byte b) {
        codeStream.write(b);
    }

    public CodeAttribute codeAttribute() {
        return result;
    }

    private ExceptionDispatcher[] synthesizeExceptionDispatchers(byte[] code, ExceptionHandlerEntry[] exceptionHandlerEntries) {
        final ExceptionDispatcher[] dispatcherMap = new ExceptionDispatcher[code.length];
        final ExceptionHandler[] handlerMap = ExceptionHandler.createHandlerMap(code.length, exceptionHandlerEntries);
        final Map<ExceptionHandler, ExceptionDispatcher> handlerToDispatcherMap = new IdentityHashMap<ExceptionHandler, ExceptionDispatcher>();
        for (int i = 0; i < handlerMap.length; i++) {
            final ExceptionHandler handler = handlerMap[i];
            if (handler != null) {
                ExceptionDispatcher dispatcher = handlerToDispatcherMap.get(handler);
                if (dispatcher == null) {
                    dispatcher = new ExceptionDispatcher(this, handler);
                    handlerToDispatcherMap.put(handler, dispatcher);
                }
                dispatcherMap[i] = dispatcher;
            }
        }
        return dispatcherMap;
    }

    @Override
    public byte[] code() {
        return codeStream.toByteArray();
    }

    /**
     * Gets a table of {@linkplain ExceptionHandlerEntry exception handlers}s that map ranges of bytecode to exception dispatchers.
     * Each dispatcher covers a disjoint range of code and not all code ranges are necessarily covered.
     */
    private ExceptionHandlerEntry[] exceptionDispatcherTable(ExceptionDispatcher[] dispatcherMap) {
        ArrayList<ExceptionHandlerEntry> table = new ArrayList<ExceptionHandlerEntry>();
        int i = 0;
        while (i < dispatcherMap.length) {
            final ExceptionDispatcher dispatcher = dispatcherMap[i];
            if (dispatcher != null) {
                final int startAddress = i;
                int endAddress = i + 1;
                while (endAddress < dispatcherMap.length && dispatcherMap[endAddress] == dispatcher) {
                    ++endAddress;
                }
                final ExceptionHandlerEntry dispatcherInfo = new ExceptionHandlerEntry(startAddress, endAddress, dispatcher.position(), 0);
                table.add(dispatcherInfo);
                i = endAddress;
            } else {
                ++i;
            }
        }
        return table.toArray(new ExceptionHandlerEntry[table.size()]);
    }
}
