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
package com.sun.max.memory;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;

/**
 * A chunk of memory at a fixed address by virtue of being in the boot image.
 * This can be used even before having allocation via JNI enabled.
 * It is immediately present at boot time, right after mapping the boot image.
 *
 * The {@link #lock} object must be synchronized if {@link #bufferUseRequiresSynchronization()} is
 * true when using the {@linkplain #address() buffer}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class BootMemory {

    /**
     * Making this constructor {@linkplain PROTOTYPE_ONLY prototype-only} forces all {@code BootMemory}
     * objects to live in the boot heap.
     *
     * @param size the size of the buffer
     */
    @PROTOTYPE_ONLY
    public BootMemory(int size) {
        bufferBytes = new byte[size];
    }

    /**
     * Since this object lands in the boot image, it will have a fixed address.
     */
    private final byte[] bufferBytes;

    /**
     * The offset of the byte array data from the byte array object's origin.
     */
    private static final Offset dataOffset = Layout.byteArrayLayout().getElementOffsetFromOrigin(0);

    /**
     * The raw pointer to element 0 of {@link #bufferBytes}.
     */
    private Pointer buffer;

    /**
     * Gets a pointer to the buffer.
     */
    public Pointer address() {
        if (buffer.isZero()) {
            if (MaxineVM.isHosted()) {
                buffer = Memory.mustAllocate(bufferBytes.length);
            } else {
                buffer = Reference.fromJava(bufferBytes).toOrigin().plus(dataOffset);
            }
        }
        return buffer;
    }

    /**
     * Gets the size of the buffer.
     */
    public int size() {
        return bufferBytes.length;
    }
}
