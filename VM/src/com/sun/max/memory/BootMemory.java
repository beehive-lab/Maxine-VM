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
/*VCSID=93e23516-91e3-45fa-86f4-fdfb7e702dd1*/
package com.sun.max.memory;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;

/**
 * A single, globally available chunk of memory at a fixed address.
 * This can be used even before having allocation via JNI enabled.
 * It is immediately present at boot time, right after mapping the boot image.
 *
 * @author Bernd Mathiske
 */
public final class BootMemory {

    private BootMemory() {
    }

    private static final int _SIZE = Ints.K;

    /**
     * Since this object lands in the boot image, it will have a fixed address.
     *
     * ATTENTION: To prevent the code in method 'buffer()' below from folding,
     * we must NOT declare this variable 'final':
     */
    private static byte[] _bufferBytes = new byte[_SIZE];

    /**
     * The offset of the byte array data from the byte array object's origin.
     */
    private static final Offset _dataOffset = VMConfiguration.target().layoutScheme().byteArrayLayout().getElementOffsetFromOrigin(0);

    /**
     * A single byte buffer with a fixed address.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static Pointer _buffer = Pointer.zero();

    public static Pointer buffer() {
        if (_buffer.isZero()) {
            if (MaxineVM.isPrototyping()) {
                _buffer = Memory.mustAllocate(_SIZE);
            } else {
                _buffer = Reference.fromJava(_bufferBytes).toOrigin().plus(_dataOffset);
                _bufferBytes = null; // This prevents optimizations from discovering that 'bytes' is de facto 'final'
            }
        }
        return _buffer;
    }
    public static int bufferSize() {
        return _SIZE;
    }

}
