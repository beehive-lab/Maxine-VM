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

import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.bir.*;

/**
 * A bytecode position and method pair describing a VM bytecode instruction location for a loaded class.
 *
 * @author Doug Simon
 */
public final class BytecodeLocation {

    private final ClassMethodActor _classMethodActor;
    private final int _position;

    public BytecodeLocation(ClassMethodActor method, int position) {
        _classMethodActor = method;
        _position = position;
    }

    public ClassMethodActor classMethodActor() {
        return _classMethodActor;
    }

    public int position() {
        return _position;
    }

    public BytecodeBlock getBytecodeBlock() {
        final byte[] bytecode = _classMethodActor.codeAttribute().code();
        if (bytecode != null && _position < bytecode.length) {
            return new BytecodeBlock(bytecode, _position, _position);
        }
        final BirGeneratorScheme birCompilerScheme = (BirGeneratorScheme) VMConfiguration.target().compilerScheme();
        final BirMethod birMethod = birCompilerScheme.birGenerator().makeIrMethod(_classMethodActor);
        return new BytecodeBlock(birMethod.code(), _position, _position);
    }

    public Bytecode getBytecode() {
        final byte[] code = _classMethodActor.codeAttribute().code();
        return Bytecode.from(code[_position]);
    }

    public boolean isNativeCall() {
        return getBytecode() == Bytecode.CALLNATIVE;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof BytecodeLocation) {
            final BytecodeLocation bytecodeLocation = (BytecodeLocation) other;
            return _classMethodActor.equals(bytecodeLocation._classMethodActor) && _position == bytecodeLocation._position;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _classMethodActor.hashCode() ^ _position;
    }

    @Override
    public String toString() {
        return _classMethodActor.qualifiedName() + "@" + _position;
    }

    public StackTraceElement toStackTraceElement() {
        final ClassActor holder = _classMethodActor.holder();
        return new StackTraceElement(holder.name().string(), _classMethodActor.name().string(), holder.sourceFileName(), sourceLineNumber());
    }

    /**
     * Gets a source position string for this bytecode location if the necessary
     * {@linkplain ClassActor#sourceFileName() source file} and
     * {@linkplain LineNumberTable#findLineNumber(int) line number} information are available.
     *
     * @return a string in the format of a {@linkplain Throwable#printStackTrace() standard stack trace} line. If the
     *         information required for such a source description is missing, then {@code null} is returned.
     */
    public String toSourcePositionString() {
        final int sourceLineNumber = sourceLineNumber();
        if (sourceLineNumber == -1) {
            return null;
        }
        final String sourceFileName = _classMethodActor.holder().sourceFileName();
        if (sourceFileName == null) {
            return null;
        }
        return _classMethodActor.qualifiedName() + "(" + sourceFileName + ":" + sourceLineNumber + ")";
    }

    /**
     * Gets the source line number corresponding to this bytecode location.
     *
     * @return -1 if a source line number is not available
     */
    public int sourceLineNumber() {
        return _classMethodActor.codeAttribute().lineNumberTable().findLineNumber(_position);
    }

    /**
     * Gets the source file name corresponding to this bytecode location.
     *
     * @return -1 if a source file name is not available
     */
    public String sourceFileName() {
        return _classMethodActor.holder().sourceFileName();
    }

    class MethodRefFinder extends BytecodeAdapter {
        final ConstantPool _constantPool = _classMethodActor.holder().constantPool();
        int _methodRefIndex = -1;

        @Override
        protected void invokestatic(int index) {
            _methodRefIndex = index;
        }

        @Override
        protected void invokespecial(int index) {
            _methodRefIndex = index;
        }

        @Override
        protected void invokevirtual(int index) {
            _methodRefIndex = index;
        }

        @Override
        protected void invokeinterface(int index, int count) {
            _methodRefIndex = index;
        }

        public MethodRefConstant methodRef() {
            if (_methodRefIndex != -1) {
                return _constantPool.methodAt(_methodRefIndex);
            }
            return null;
        }
        public MethodActor methodActor() {
            if (_methodRefIndex != -1) {
                final MethodRefConstant methodRef = _constantPool.methodAt(_methodRefIndex);
                return methodRef.resolve(_constantPool, _methodRefIndex);
            }
            return null;
        }
    }

    public MethodActor getCalleeMethodActor() {
        final MethodRefFinder methodRefFinder = new MethodRefFinder();
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(methodRefFinder);
        bytecodeScanner.scanInstruction(classMethodActor().codeAttribute().code(), position());
        return methodRefFinder.methodActor();
    }

    public MethodRefConstant getCalleeMethodRef() {
        final MethodRefFinder methodRefFinder = new MethodRefFinder();
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(methodRefFinder);
        bytecodeScanner.scanInstruction(classMethodActor().codeAttribute().code(), position());
        return methodRefFinder.methodRef();
    }
}
