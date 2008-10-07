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
/*VCSID=87891bc3-a67a-400b-850c-45d830fb2e33*/
package com.sun.max.vm.bytecode;

import com.sun.max.program.*;
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

    private class DeclaredCalleeFinder extends BytecodeAdapter {
        private final ConstantPool _constantPool = _classMethodActor.holder().constantPool();
        MethodActor _declaredCallee;

        @Override
        protected void invokestatic(int index) {
            _declaredCallee = _constantPool.classMethodAt(index).resolveStatic(_constantPool, index);
        }

        @Override
        protected void invokespecial(int index) {
            _declaredCallee = _constantPool.classMethodAt(index).resolveVirtual(_constantPool, index);
        }

        @Override
        protected void invokevirtual(int index) {
            _declaredCallee = _constantPool.classMethodAt(index).resolveVirtual(_constantPool, index);
        }

        @Override
        protected void invokeinterface(int index, int count) {
            _declaredCallee = _constantPool.interfaceMethodAt(index).resolve(_constantPool, index);
        }
    }

    public MethodActor getDeclaredCallee() {
        final DeclaredCalleeFinder declaredCalleeFinder = new DeclaredCalleeFinder();
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(declaredCalleeFinder);
        try {
            bytecodeScanner.scanInstruction(classMethodActor().codeAttribute().code(), position());
        } catch (Throwable throwable) {
            ProgramError.unexpected("could not scan byte code", throwable);
        }
        return declaredCalleeFinder._declaredCallee;
    }

}
