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
package com.sun.max.io;

import java.io.*;

/**
 * A {@link ByteArrayOutputStream} that can have its write position {@linkplain #seek(int) updated}.
 * 
 * @author Doug Simon
 */
public class SeekableByteArrayOutputStream extends ByteArrayOutputStream {

    private int highestCount;

    /**
     * @see ByteArrayOutputStream#ByteArrayOutputStream()
     */
    public SeekableByteArrayOutputStream() {
    }

    /**
     * @see ByteArrayOutputStream#ByteArrayOutputStream(int)
     */
    public SeekableByteArrayOutputStream(int size) {
        super(size);
    }

    /**
     * Updates the write position of this stream. The stream can only be repositioned between 0 and the
     * {@linkplain #endOfStream() end of the stream}.
     * 
     * @param index
     *            the index to which the write position of this stream will be set
     * @throws IllegalArgumentException
     *             if {@code index > highestSeekIndex()}
     */
    public void seek(int index) throws IllegalArgumentException {
        if (endOfStream() < index) {
            throw new IllegalArgumentException();
        }
        count = index;
    }

    /**
     * Gets the index one past the highest index that has been written to in this stream.
     */
    public int endOfStream() {
        if (highestCount < count) {
            highestCount = count;
        }
        return highestCount;
    }

    @Override
    public void reset() {
        super.reset();
        highestCount = 0;
    }

    /**
     * Copies the {@code length} bytes of this byte array output stream starting at {@code offset} to {@code buf}
     * starting at {@code bufOffset}.
     */
    public void copyTo(int offset, byte[] toBuffer, int toOffset, int length) {
        System.arraycopy(this.buf, offset, toBuffer, toOffset, length);
    }
}
