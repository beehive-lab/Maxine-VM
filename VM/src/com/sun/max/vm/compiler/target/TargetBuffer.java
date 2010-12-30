/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.target;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;

/**
 * The {@code TargetBuffer} class represents a buffer into which code can be written
 * before being transfer to a {@code CodeRegion} instance. These instances are
 * temporary and collect information about the target code being generated that
 * are needed to create such things as exception tables, GC safepoints, etc in
 * the code region.
 *
 * Created Nov 19, 2007
 *
 * @author Ben L. Titzer
 */
public class TargetBuffer {

    private static final int INITIAL_CODEBUF_SIZE = Ints.K;
    private static final int INITIAL_OFFSET_SIZE = 16;

    private Pointer codeStart;
    private int codeSize;

    private byte[] codeBuffer;
    private int[] safepointOffsets;
    private int safepointCursor;
    private int[] directCallOffsets;
    private int directCallCursor;
    private int[] dynamicCallOffsets;
    private int dynamicCallCursor;

    public TargetBuffer() {
        codeBuffer = new byte[INITIAL_CODEBUF_SIZE];
        safepointOffsets = new int[INITIAL_OFFSET_SIZE];
        directCallOffsets = new int[INITIAL_OFFSET_SIZE];
        dynamicCallOffsets = new int[INITIAL_OFFSET_SIZE];
    }

    public void addSafePoint(int offset) {
        final int cursor = safepointCursor++;
        safepointOffsets = grow(safepointOffsets, safepointCursor);
        safepointOffsets[cursor] = offset;
    }

    public void addDirectCall(int offset) {
        final int cursor = directCallCursor++;
        directCallOffsets = grow(directCallOffsets, directCallCursor);
        directCallOffsets[cursor] = offset;
    }

    public void addDynamicCall(int offset) {
        final int cursor = dynamicCallCursor++;
        dynamicCallOffsets = grow(dynamicCallOffsets, dynamicCallCursor);
        dynamicCallOffsets[cursor] = offset;
    }

    private int[] grow(int[] array, int size) {
        if (size > array.length) {
            final int[] tmp = new int[array.length * 2];
            System.arraycopy(array, 0, tmp, 0, array.length);
            return tmp;
        }
        return array;
    }

    public void write(CodeRegion region) {
        if (codeStart != Pointer.zero()) {
            // TODO: implement write to code buffer
        }
    }

    public Pointer getCodeStart() {
        assert codeStart != Pointer.zero();
        return codeStart;
    }

    public int getCodeSize() {
        return codeSize;
    }
}
