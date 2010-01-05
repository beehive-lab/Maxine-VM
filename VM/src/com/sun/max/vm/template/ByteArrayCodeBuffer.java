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

import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.vm.code.*;

/**
 * A simple code buffer implementation, using a byte array for backing storage.
 * Upon overflow, the buffer doubles its size.
 *
 * @author Laurent Daynes
 */
public class ByteArrayCodeBuffer extends CodeBuffer {
    private byte[] buffer;

    public ByteArrayCodeBuffer(int capacityInBytes) {
        super();
        if (capacityInBytes < 0) {
            throw new IllegalArgumentException("Negative buffer size: " + capacityInBytes);
        }
        buffer = new byte[capacityInBytes];
    }

    public ByteArrayCodeBuffer(byte[] prefix, int capacityInBytes) {
        super();
        if (capacityInBytes < 0) {
            throw new IllegalArgumentException("Negative buffer size: " + capacityInBytes);
        }
        if (capacityInBytes < prefix.length) {
            throw new IllegalArgumentException("Insufficient capacity for specified prefix: " + capacityInBytes);
        }
        buffer = new byte[capacityInBytes];
        emit(prefix);
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

    @Override
    public void copyTo(byte[] toArray) {
        final int length = toArray.length;
        assert length <= currentPosition;  // trusted client
        Bytes.copy(buffer, toArray, length);
    }

    @Override
    public void emit(byte b) {
        final int nextCurrentOffset = currentPosition + 1;
        if (nextCurrentOffset > buffer.length) {
            expand(nextCurrentOffset);
        }
        buffer[currentPosition] = b;
        currentPosition = nextCurrentOffset;
    }

    @Override
    public void emit(byte[] bytes) {
        final int len = bytes.length;
        final int nextCurrentOffset = currentPosition + len;
        if (nextCurrentOffset > buffer.length) {
            expand(nextCurrentOffset);
        }
        Bytes.copy(bytes, 0, buffer, currentPosition, len);
        currentPosition = nextCurrentOffset;
    }

    @Override
    public void reserve(int numBytes) {
        final int nextCurrentOffset = currentPosition + numBytes;
        if (nextCurrentOffset > buffer.length) {
            expand(nextCurrentOffset);
        }
        currentPosition = nextCurrentOffset;
    }

    @Override
    public void fix(int startPosition, BranchTargetModifier modifier, int disp32)  throws AssemblyException {
        modifier.fix(buffer, startPosition, disp32);
    }

    @Override
    public void fix(int startPosition, byte[] code, int position, int size) throws AssemblyException {
        if (startPosition + size > buffer.length) {
            throw new AssemblyException("CodeBuffer overflow. Incorrect fix specification");
        }
        Bytes.copy(code, position, buffer, startPosition, size);
    }

    @Override
    public void fix(int position, byte b) throws AssemblyException {
        if (position >= buffer.length) {
            throw new AssemblyException("CodeBuffer overflow. Incorrect fix specification");
        }
        buffer[position] = b;
    }
}

