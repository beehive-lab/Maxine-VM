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
