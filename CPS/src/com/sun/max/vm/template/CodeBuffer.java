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
import com.sun.max.lang.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Code buffer used by template-based code generator. It provides the illusion of an append-only, linear byte buffer
 * to template-based code generators. A code buffer is assumed to be private to a code generator and does not synchronize any
 * operations. Code generators can only emit templates, patch arbitrary positions in the emitted code and obtain the current
 * end of the buffer.
 *
 * @author Laurent Daynes
 */
public final class CodeBuffer {
    protected int currentPosition;
    private byte[] buffer;

    public CodeBuffer(int capacityInBytes) {
        if (capacityInBytes < 0) {
            throw new IllegalArgumentException("Negative buffer size: " + capacityInBytes);
        }
        buffer = new byte[capacityInBytes];
    }

    /**
     * Returns the offset to the current end of the buffer.
     * @return the current position
     */
    @INLINE
    public int currentPosition() {
        return currentPosition;
    }

    /**
     * Emits a template at the current position in the buffer and increase the current position with
     * the size of the emitted template.
     *
     * @param template to emit in the code buffer
     */
    public void emit(TargetMethod template) {
        emit(template.code());
    }

    private OutputStream outputStream;

    public OutputStream outputStream() {
        if (outputStream == null) {
            outputStream = new OutputStream() {
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
        return outputStream;
    }

    /**
     * Appends the object code assembled by a given assembler to this code buffer.
     */
    public void emitCodeFrom(Assembler assembler) {
        try {
            assembler.output(outputStream(), null);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void expand(int minSize) {
        int newBufferCapacity = buffer.length << 1;
        if (newBufferCapacity < minSize) {
            newBufferCapacity += minSize;
        }
        final byte[] newBuffer = new byte[newBufferCapacity];
        Bytes.copy(buffer, newBuffer, currentPosition);
        buffer = newBuffer;
    }

    /**
     * Copy the bytes emitted in the code buffer to the byte array provided.
     *
     * @param toArray the array into which to copy the bytes
     */
    public void copyTo(byte[] toArray) {
        final int length = toArray.length;
        assert length <= currentPosition;  // trusted client
        Bytes.copy(buffer, toArray, length);
    }

    public byte[] buffer() {
        return buffer;
    }

    public void emit(byte b) {
        final int nextCurrentOffset = currentPosition + 1;
        if (nextCurrentOffset > buffer.length) {
            expand(nextCurrentOffset);
        }
        buffer[currentPosition] = b;
        currentPosition = nextCurrentOffset;
    }

    public void emit(byte[] bytes) {
        final int len = bytes.length;
        final int nextCurrentOffset = currentPosition + len;
        if (nextCurrentOffset > buffer.length) {
            expand(nextCurrentOffset);
        }
        Bytes.copy(bytes, 0, buffer, currentPosition, len);
        currentPosition = nextCurrentOffset;
    }

    public void reserve(int numBytes) {
        final int nextCurrentOffset = currentPosition + numBytes;
        if (nextCurrentOffset > buffer.length) {
            expand(nextCurrentOffset);
        }
        currentPosition = nextCurrentOffset;
    }

    public void fix(int startPosition, BranchTargetModifier modifier, int disp32)  throws AssemblyException {
        modifier.fix(buffer, startPosition, disp32);
    }

    /**
     * Replaces code at a specified position.
     *
     * @param startPosition the position in this buffer of the code to be replaced
     * @param code the target code array containing the replacement code
     * @param position the position in {@code code} of the replacement code
     * @param size the size of the replacement code
     * @throws AssemblyException
     */
    public void fix(int startPosition, byte[] code, int position, int size) throws AssemblyException {
        if (startPosition + size > buffer.length) {
            throw new AssemblyException("CodeBuffer overflow. Incorrect fix specification");
        }
        Bytes.copy(code, position, buffer, startPosition, size);
    }

    public void fix(int position, byte b) throws AssemblyException {
        if (position >= buffer.length) {
            throw new AssemblyException("CodeBuffer overflow. Incorrect fix specification");
        }
        buffer[position] = b;
    }


    /**
     * Replaces code at a specified position.
     *
     * @param startPosition the position in this buffer of the code to be replaced
     * @param assembler the assembler whose {@linkplain Assembler#output(OutputStream, InlineDataRecorder) output} is to
     *            be used as the replacement code
     */
    public void fix(int startPosition, Assembler assembler) throws AssemblyException {
        final int savedPosition = currentPosition;
        currentPosition = startPosition;
        emitCodeFrom(assembler);
        currentPosition = savedPosition;
    }
}
