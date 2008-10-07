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
/*VCSID=409c1b69-e2a7-48a3-9c71-9a10af3a52da*/
package com.sun.max.vm.object;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;

/**
 * Access to raw byte array data via pointers.
 *
 * Either the byte array will be pinned or it's contents
 * will be copied to a newly allocated buffer as quickly as possible.
 *
 * @author Bernd Mathiske
 */
@Hypothetical
public abstract class RawByteArrayAccess implements Procedure<byte[]> {

    @INLINE
    public static Pointer elementPointer(byte[] array, int offset) {
        return Reference.fromJava(array).toOrigin().plus(Layout.byteArrayLayout().getElementOffsetFromOrigin(offset));
    }

    private final Pointer _data;

    /**
     * Client code can access the selected byte array contents via this pointer.
     * Whether copying or direct object access will occur is decided by the implementation.
     *
     * @return a pointer to data that correspond to the contents of the byte array at the given offset
     */
    public final Pointer data() {
        return _data;
    }

    protected final byte[] _array;
    protected final int _offset;
    protected final int _length;

    protected RawByteArrayAccess(byte[] array, int offset, int length) {
        _array = array;
        _offset = offset;
        _length = length;
        if (Heap.pin(array)) {
            _data = elementPointer(array, offset);
        } else {
            _data = MemoryBuffer.allocate(Size.fromInt(length));
        }
    }

    @INLINE
    public final boolean isCopy() {
        return !Heap.isPinned(_array);
    }

    /**
     * Release the buffer or unpin the byte array.
     */
    public void release() {
        if (Heap.isPinned(_array)) {
            Heap.unpin(_array);
        } else {
            MemoryBuffer.deallocate(_data);
        }
    }

    protected boolean _writing;

    public void run(byte[] bytes) {
        if (_writing) {
            Memory.copyBytes(_data, elementPointer(_array, _offset), Size.fromInt(_length));
        } else {
            Memory.copyBytes(elementPointer(_array, _offset), _data, Size.fromInt(_length));
        }
    }

    protected void readBytes() {
        if (isCopy() && !Heap.flash(_array, this)) {
            for (int i = 0; i < _length; i++) {
                _data.setByte(i, ArrayAccess.getByte(_array, _offset + i));
            }
        }
    }

    /**
     * Provide read access to raw array data.
     */
    public static class Read extends RawByteArrayAccess {
        public Read(byte[] bytes, int offset, int length) {
            super(bytes, offset, length);
            readBytes();
        }

        public Read(byte[] bytes) {
            this(bytes, 0, bytes.length);
        }
    }

    /**
     * Provide write access to raw bytes.
     *
     * ATTENTION: bytes that are not explicitly set may end up with an arbitrary value.
     */
    public static class Write extends RawByteArrayAccess {
        public Write(byte[] bytes, int offset, int length) {
            super(bytes, offset, length);
        }

        public Write(byte[] bytes) {
            this(bytes, 0, bytes.length);
        }

        /**
         * If buffered, write buffer contents to the byte array.
         * Then release the buffer or unpin the byte array.
         */
        public void finish() {
            _writing = true;
            if (isCopy() && !Heap.flash(_array, this)) {
                for (int i = 0; i < _length; i++) {
                    ArrayAccess.setByte(_array, _offset + i, data().getByte(i));
                }
            }
            release();
        }
    }

    /**
     * Provide read and write access to raw bytes.
     */
    public static class Update extends Write {
        public Update(byte[] bytes, int offset, int length) {
            super(bytes, offset, length);
            readBytes();
        }

        public Update(byte[] bytes) {
            this(bytes, 0, bytes.length);
        }
    }
}
