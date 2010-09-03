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
package com.sun.max.vm.bytecode;

import java.util.*;

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

    @Override
    public boolean equals(Object other) {
        if (other instanceof BytecodeLocation) {
            final BytecodeLocation bytecodeLocation = (BytecodeLocation) other;
            return classMethodActor.equals(bytecodeLocation.classMethodActor) && bytecodePosition == bytecodeLocation.bytecodePosition;
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
