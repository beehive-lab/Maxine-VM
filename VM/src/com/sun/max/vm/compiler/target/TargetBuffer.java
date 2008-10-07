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
/*VCSID=dc5ed5a2-103b-4f09-8b7c-e2dd0fb8a6d0*/
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

    private Pointer _codeStart;
    private int _codeSize;

    private byte[] _codeBuffer;
    private int[] _safepointOffsets;
    private int _safepointCursor;
    private int[] _directCallOffsets;
    private int _directCallCursor;
    private int[] _dynamicCallOffsets;
    private int _dynamicCallCursor;

    public TargetBuffer() {
        _codeBuffer = new byte[INITIAL_CODEBUF_SIZE];
        _safepointOffsets = new int[INITIAL_OFFSET_SIZE];
        _directCallOffsets = new int[INITIAL_OFFSET_SIZE];
        _dynamicCallOffsets = new int[INITIAL_OFFSET_SIZE];
    }

    public void addSafePoint(int offset) {
        final int cursor = _safepointCursor++;
        _safepointOffsets = grow(_safepointOffsets, _safepointCursor);
        _safepointOffsets[cursor] = offset;
    }

    public void addDirectCall(int offset) {
        final int cursor = _directCallCursor++;
        _directCallOffsets = grow(_directCallOffsets, _directCallCursor);
        _directCallOffsets[cursor] = offset;
    }

    public void addDynamicCall(int offset) {
        final int cursor = _dynamicCallCursor++;
        _dynamicCallOffsets = grow(_dynamicCallOffsets, _dynamicCallCursor);
        _dynamicCallOffsets[cursor] = offset;
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
        if (_codeStart != Pointer.zero()) {
            // TODO: implement write to code buffer
        }
    }

    public Pointer getCodeStart() {
        assert _codeStart != Pointer.zero();
        return _codeStart;
    }

    public int getCodeSize() {
        return _codeSize;
    }
}
