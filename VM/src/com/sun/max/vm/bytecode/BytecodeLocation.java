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
package com.sun.max.vm.bytecode;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * A bytecode position and method pair describing a VM bytecode instruction location for a loaded class.
 *
 * @author Doug Simon
 */
public class BytecodeLocation implements Iterable<BytecodeLocation> {

    public final ClassMethodActor classMethodActor;
    public final int bytecodePosition;

    public BytecodeLocation(ClassMethodActor classMethodActor, int bytecodePosition) {
        assert classMethodActor != null : "cannot create bytecode location for null ClassMethodActor";
        this.classMethodActor = classMethodActor;
        this.bytecodePosition = bytecodePosition;
    }

    /**
     * This test includes the {@linkplain BytecodeLocation#parent() parent} chain of this and {@code obj}.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BytecodeLocation) {
            final BytecodeLocation other = (BytecodeLocation) obj;
            if (classMethodActor.equals(other.classMethodActor) && bytecodePosition == other.bytecodePosition) {
                if (parent() == null) {
                    return other.parent() == null;
                }
                return parent().equals(other.parent());
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return classMethodActor.hashCode() ^ bytecodePosition;
    }

    @Override
    public String toString() {
        return classMethodActor.qualifiedName() + "@" + bytecodePosition;
    }

    /**
     * Gets the source line number corresponding to this bytecode location.
     *
     * @return -1 if a source line number is not available
     */
    public int sourceLineNumber() {
        return classMethodActor.sourceLineNumber(bytecodePosition);
    }

    /**
     * Converts this location object to a {@link CiCodePos} object.
     *
     * @param dict a map used to canonicalize the {@link CiCodePos} objects produced when translating more than one
     *            {@link BytecodeLocation}. This can be {@code null} if no canonicalization is to be performed.
     */
    public CiCodePos toCodePos(Map<BytecodeLocation, CiCodePos> dict) {
        BytecodeLocation parent = parent();
        if (dict != null) {
            CiCodePos pos = dict.get(this);
            if (pos != null) {
                return pos;
            }
        }
        CiCodePos caller = null;
        if (parent != null) {
            caller = parent.toCodePos(dict);
        }
        CiCodePos pos = new CiCodePos(caller, classMethodActor, bytecodePosition);
        if (dict != null) {
            dict.put(this, pos);
        }
        return pos;
    }

    /**
     * Gets the source file name corresponding to this bytecode location.
     *
     * @return {@code null} if a source file name is not available
     */
    public String sourceFileName() {
        return classMethodActor.holder().sourceFileName;
    }

    /**
     * Gets the opcode of the instruction at the bytecode position denoted by this
     * frame descriptor.
     */
    public int getBytecode() {
        final byte[] code = classMethodActor.codeAttribute().code();
        return code[bytecodePosition] & 0xff;
    }

    /**
     * Gets the bytecode location of the logical frame that called this frame where the call has been inlined.
     *
     * @return {@code null} to indicate this is top level frame of an inlining tree
     */
    public BytecodeLocation parent() {
        return null;
    }

    public Iterator<BytecodeLocation> iterator() {
        return new Iterator<BytecodeLocation>() {
            BytecodeLocation current = BytecodeLocation.this;
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public BytecodeLocation next() {
                if (current == null) {
                    throw new NoSuchElementException();
                }
                BytecodeLocation cur = current;
                current = current.parent();
                return cur;
            }

            @Override
            public boolean hasNext() {
                return current != null;
            }
        };
    }

    /**
     * Gets a {@link StackTraceElement} object derived from this frame descriptor describing the corresponding source code location.
     */
    public StackTraceElement toStackTraceElement() {
        return classMethodActor.toStackTraceElement(bytecodePosition);
    }

    class MethodRefFinder extends BytecodeAdapter {
        final ConstantPool constantPool = classMethodActor.holder().constantPool();
        int methodRefIndex = -1;

        @Override
        protected void invokestatic(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokespecial(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokevirtual(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokeinterface(int index, int count) {
            methodRefIndex = index;
        }

        public MethodRefConstant methodRef() {
            if (methodRefIndex != -1) {
                return constantPool.methodAt(methodRefIndex);
            }
            return null;
        }
        public MethodActor methodActor() {
            if (methodRefIndex != -1) {
                final MethodRefConstant methodRef = constantPool.methodAt(methodRefIndex);
                return methodRef.resolve(constantPool, methodRefIndex);
            }
            return null;
        }
    }

    public MethodActor getCalleeMethodActor() {
        final MethodRefFinder methodRefFinder = new MethodRefFinder();
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(methodRefFinder);
        bytecodeScanner.scanInstruction(classMethodActor.codeAttribute().code(), bytecodePosition);
        return methodRefFinder.methodActor();
    }

    public MethodRefConstant getCalleeMethodRef() {
        final MethodRefFinder methodRefFinder = new MethodRefFinder();
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(methodRefFinder);
        bytecodeScanner.scanInstruction(classMethodActor.codeAttribute().code(), bytecodePosition);
        return methodRefFinder.methodRef();
    }
}
