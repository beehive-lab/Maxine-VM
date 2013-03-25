/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.memory;

import com.oracle.graal.replacements.Snippet.Fold;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;

/**
 * A chunk of memory at a fixed address by virtue of being in the boot image.
 * This can be used even before having allocation via JNI enabled.
 * It is immediately present at boot time, right after mapping the boot image.
 */
public final class BootMemory {

    /**
     * Making this constructor {@link HOSTED_ONLY} forces all {@code BootMemory}
     * objects to live in the boot heap.
     *
     * @param size the size of the buffer
     */
    @HOSTED_ONLY
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
    @Fold
    private static Offset dataOffset() {
        return Layout.byteArrayLayout().getElementOffsetFromOrigin(0);
    }

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
                buffer = Reference.fromJava(bufferBytes).toOrigin().plus(dataOffset());
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
