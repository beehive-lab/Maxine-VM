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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.hosted.*;

/**
 * Memory access using wrapped Word types.
 */
@HOSTED_ONLY
public final class BoxedMemory {

    static {
        // Ensure the native code is loaded
        Prototype.loadHostedLibrary();
    }

    private BoxedMemory() {
    }

    private static native long nativeAllocate(long size);

    public static Pointer allocate(Size size) {
        final Boxed box = (Boxed) size;
        return BoxedPointer.from(nativeAllocate(box.value()));
    }

    private static native long nativeReallocate(long block, long size);

    public static Pointer reallocate(Pointer block, Size size) {
        final Boxed blockBox = (Boxed) block;
        final Boxed sizeBox = (Boxed) size;
        return BoxedPointer.from(nativeReallocate(blockBox.value(), sizeBox.value()));
    }

    private static native int nativeDeallocate(long pointer);

    public static int deallocate(Address block) {
        final Boxed box = (Boxed) block;
        return nativeDeallocate(box.value());
    }

    private static native void nativeWriteBytes(byte[] fromArray, int startIndex, int numberOfBytes, long toPointer);

    public static void writeBytes(byte[] fromArray, int startIndex, int numberOfBytes, Pointer toPointer) {
        nativeWriteBytes(fromArray, startIndex, numberOfBytes, toPointer.toLong());
    }

}
