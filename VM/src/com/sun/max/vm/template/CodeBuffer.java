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
package com.sun.max.vm.template;

import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.jit.*;


/**
 * Code buffer abstraction used by template-based code generator. It provides the illusion of an append-only, linear byte buffer
 * to template-based code generators. A code buffer is assumed to be private to a code generator and does not synchronize any
 * operations. Code generators can only emit templates, patch arbitrary positions in the emitted code and obtain the current
 * end of the buffer.
 *
 * @author Laurent Daynes
 */
public abstract class CodeBuffer {
    protected int _currentPosition;

    public CodeBuffer() {
        _currentPosition = 0;
    }

    /**
     * Returns the offset to the current end of the buffer.
     * @return
     */
    @INLINE
    public final int currentPosition() {
        return _currentPosition;
    }

    /**
     * Emits a template at the current position in the buffer and increases the {@linkplain #currentPosition() current position} with
     * the size of the emitted template. Modifies the emitted template so that a constant displacement is replaced
     * with a new constant displacement value.
     */
    public void emit(CompiledBytecodeTemplate template, DisplacementModifier modifier, int disp32) throws AssemblyException {
        final int startOfTemplate = currentPosition();
        emit(template.targetMethod().code());
        fix(startOfTemplate, modifier, disp32);
    }

    /**
     * Emits a template at the current position in the buffer and increases the {@linkplain #currentPosition() current position} with
     * the size of the emitted template. Modifies the emitted template so that a constant operand is replaced
     * with a new constant operand value.
     */
    public void emit(CompiledBytecodeTemplate template, ImmediateConstantModifier modifier, byte imm8) throws AssemblyException {
        final int startOfTemplate = currentPosition();
        emit(template.targetMethod().code());
        fix(startOfTemplate, modifier, imm8);
    }

    /**
     * Emits a template at the current position in the buffer and increases the {@linkplain #currentPosition() current position} with
     * the size of the emitted template. Modifies the emitted template so that a constant operand is replaced
     * with a new constant operand value.
     */
    public void emit(CompiledBytecodeTemplate template, ImmediateConstantModifier modifier, int imm32) throws AssemblyException {
        final int startOfTemplate = currentPosition();
        emit(template.targetMethod().code());
        fix(startOfTemplate, modifier, imm32);
    }

    /**
     * Emits a template at the current position in the buffer and increases the {@linkplain #currentPosition() current position} with
     * the size of the emitted template. Modifies the emitted template so that a constant operand is replaced
     * with a new constant operand value.
     */
    public void emit(CompiledBytecodeTemplate template, ImmediateConstantModifier modifier, long imm64) throws AssemblyException {
        final int startOfTemplate = currentPosition();
        emit(template.targetMethod().code());
        fix(startOfTemplate, modifier, imm64);
    }

    /**
     * Emits a template at the current position in the buffer and increase the current position with
     * the size of the emitted template.
     *
     * @param template to emit in the code buffer
     */
    public void emit(CompiledBytecodeTemplate template) {
        emit(template.targetMethod().code());
    }

    /**
     * Copy the bytes emitted in the code buffer to the byte array provided.
     *
     * @param toArray
     */
    public abstract void copyTo(byte[] toArray);

    private OutputStream _outputStream;

    public OutputStream outputStream() {
        if (_outputStream == null) {
            _outputStream = new OutputStream() {
                @Override
                public void write(byte[] b) throws IOException {
                    emit(b);
                }
                @Override
                public void write(int b) throws IOException {
                    emit((byte) b);
                }
            };
        }
        return _outputStream;
    }

    /**
     * Appends the object code assembled by a given assembler to this code buffer.
     */
    public void emitCodeFrom(Assembler assembler) {
        try {
            assembler.output(outputStream(), null);
        } catch (Exception exception) {
            throw new TranslationException(exception);
        }
    }

    public abstract void emit(byte b);
    public abstract void emit(byte[] bytes);
    public abstract void reserve(int numBytes);

    public abstract void fix(int startPosition, DisplacementModifier modifier, int disp32) throws AssemblyException;
    public abstract void fix(int startPosition, ImmediateConstantModifier modifier, byte imm8) throws AssemblyException;
    public abstract void fix(int startPosition, ImmediateConstantModifier modifier, int imm32) throws AssemblyException;
    public abstract void fix(int startPosition, ImmediateConstantModifier modifier, long imm64) throws AssemblyException;
    public abstract void fix(int startPosition, LiteralModifier modifier, int disp32) throws AssemblyException;

    public abstract void fix(int startPosition, BranchTargetModifier modifier, int disp32) throws AssemblyException;

    /**
     * Replaces code at a specified position.
     *
     * @param startPosition the position in this buffer of the code to be replaced
     * @param code the target code array containing the replacement code
     * @param position the position in {@code code} of the replacement code
     * @param size the size of the replacement code
     * @throws AssemblyException
     */
    public abstract void fix(int startPosition, byte[] code, int position, int size) throws AssemblyException;

    public abstract void fix(int position, byte b) throws AssemblyException;

    /**
     * Replaces code at a specified position.
     *
     * @param startPosition the position in this buffer of the code to be replaced
     * @param assembler the assembler whose {@linkplain Assembler#output(OutputStream, InlineDataRecorder) output} is to
     *            be used as the replacement code
     */
    public void fix(int startPosition, Assembler assembler) throws AssemblyException {
        final int savedPosition = _currentPosition;
        _currentPosition = startPosition;
        emitCodeFrom(assembler);
        _currentPosition = savedPosition;
    }
}
