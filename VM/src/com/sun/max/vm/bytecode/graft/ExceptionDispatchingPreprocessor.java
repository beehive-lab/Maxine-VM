/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.bytecode.graft;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

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

    private final SeekableByteArrayOutputStream codeStream;
    private final CodeAttribute _result;

    public ExceptionDispatchingPreprocessor(ConstantPoolEditor constantPoolEditor, CodeAttribute codeAttribute) {
        super(constantPoolEditor, codeAttribute.code().length, codeAttribute.maxStack(), codeAttribute.maxLocals());

        codeStream = new SeekableByteArrayOutputStream();

        final ExceptionDispatcher[] dispatcherMap = synthesizeExceptionDispatchers(codeAttribute.code(), codeAttribute.exceptionHandlerTable());
        final Sequence<ExceptionHandlerEntry> exceptionDispatcherTable = exceptionDispatcherTable(dispatcherMap);

        fixup();

        final byte[] originalCode = codeAttribute.code();
        final byte[] code = Arrays.copyOf(originalCode, originalCode.length + codeStream.size());
        codeStream.copyTo(0, code, originalCode.length, codeStream.size());
        _result = new CodeAttribute(codeAttribute.constantPool(),
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
        return _result;
    }

    private ExceptionDispatcher[] synthesizeExceptionDispatchers(byte[] code, Sequence<ExceptionHandlerEntry> exceptionHandlerEntries) {
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
    private Sequence<ExceptionHandlerEntry> exceptionDispatcherTable(ExceptionDispatcher[] dispatcherMap) {
        final AppendableSequence<ExceptionHandlerEntry> exceptionDispatcherTable = new ArrayListSequence<ExceptionHandlerEntry>();
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
                exceptionDispatcherTable.append(dispatcherInfo);
                i = endAddress;
            } else {
                ++i;
            }
        }
        return exceptionDispatcherTable;
    }
}
