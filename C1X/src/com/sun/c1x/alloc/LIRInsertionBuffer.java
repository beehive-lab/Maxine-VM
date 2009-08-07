/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.alloc;

import java.util.*;

import com.sun.c1x.lir.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class LIRInsertionBuffer {

    LIRList lir; // the lir list where ops of this buffer should be inserted later (null when uninitialized)

    // list of insertion points. index and count are stored alternately:
    // indexAndCount[i * 2]: the index into lir list where "count" ops should be inserted
    // indexAndCount[i * 2 + 1]: the number of ops to be inserted at index
    List<Integer> indexAndCount;

    // the LIROps to be inserted
    List<LIRInstruction> ops;

    void appendNew(int index, int count) {
        indexAndCount.add(index);
        indexAndCount.add(count);
    }

    void setIndexAt(int i, int value) {
        indexAndCount.set((i << 1), value);
    }

    void setCountAt(int i, int value) {
        indexAndCount.set((i << 1) + 1, value);
    }

    LIRInsertionBuffer() {
        ops = new ArrayList<LIRInstruction>(8);
        indexAndCount = new ArrayList<Integer>(8);
    }

    // must be called before using the insertion buffer
    void init(LIRList lir) {
        assert !initialized() : "already initialized";
        this.lir = lir;
        indexAndCount.clear();
        ops.clear();
    }

    boolean initialized() {
        return lir != null;
    }

    // called automatically when the buffer is appended to the LIRList
    public void finish() {
        lir = null;
    }

    // accessors
    public LIRList lirList() {
        return lir;
    }

    public int numberOfInsertionPoints() {
        return indexAndCount.size() >> 1;
    }

    public int indexAt(int i) {
        return indexAndCount.get((i << 1));
    }

    public int countAt(int i) {
        return indexAndCount.get((i << 1) + 1);
    }

    public int numberOfOps() {
        return ops.size();
    }

    public LIRInstruction opAt(int i) {
        return ops.get(i);
    }

    void move(int index, LIROperand src, LIROperand dst, CodeEmitInfo info) {
        append(index, new LIROp1(LIROpcode.Move, src, dst, dst.type(), LIRPatchCode.PatchNone, info));
    }

    // Implementation of LIRInsertionBuffer

    void append(int index, LIRInstruction op) {
        assert indexAndCount.size() % 2 == 0 : "must have a count for each index";

        int i = numberOfInsertionPoints() - 1;
        if (i < 0 || indexAt(i) < index) {
            appendNew(index, 1);
        } else {
            assert indexAt(i) == index : "can append LIROps in ascending order only";
            assert countAt(i) > 0 : "check";
            setCountAt(i, countAt(i) + 1);
        }
        ops.add(op);

        assert verify();
    }

    boolean verify() {
        int sum = 0;
        int prevIdx = -1;

        for (int i = 0; i < numberOfInsertionPoints(); i++) {
            assert prevIdx < indexAt(i) : "index must be ordered ascending";
            sum += countAt(i);
        }
        assert sum == numberOfOps() : "wrong total sum";
        return true;
    }

    public void move(int insertIdx, LIROperand fromOpr, LIROperand toOpr) {
        move(insertIdx, fromOpr, toOpr, null);

    }
}
