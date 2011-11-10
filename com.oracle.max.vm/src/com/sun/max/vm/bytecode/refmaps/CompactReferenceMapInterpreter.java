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
package com.sun.max.vm.bytecode.refmaps;

/**
 * An interpreter that encodes the details for a frame in an {@code int}. The format of the encoding is:
 * <p>
 * <pre>
 *   0                                                   26 27     30  31
 *  +---------------------+-----------------------+--------+---------+---+
 *  |   locals ref map    | operand stack ref map | unused |   sp    | I |
 *  +---------------------+-----------------------+--------+---------+---+
 *
 *  |<---- maxLocals ---->|<----- maxStack ------>
 *
 *  I: the frame is initialized if I == 1;
 *  sp: stack depth (encoded on 4 bits, max depth being 15).
 */
public class CompactReferenceMapInterpreter extends ReferenceMapInterpreter {

    private static final int INITIALIZED_FRAME_BIT = 0x80000000;
    private static final int SP_SHIFT = 27;
    private static final int SP_MASK = 0xF;
    public static final int MAX_SLOTS = 27;
    public static final int MAX_STACK = 15;

    private int[] frames;
    private int currentFrame;

    /**
     * Decodes the part of a frame that encodes the reference map for the locals and operand stack.
     *
     * @param frame a frame
     * @param sp the number of reference map bits to include for the operand stack (i.e the current stack depth)
     * @return the reference maps for the locals and operand stack encoded in {@code frame}
     */
    private int decodeReferenceMap(int frame, int sp) {
        final int mask = (1 << maxLocals() + sp) - 1;
        return frame & mask;
    }

    /**
     * Decodes the stack depth from a frame.
     *
     * @param frame a frame
     * @return the stack depth encoded in {@code frame}
     */
    private static int decodeSp(int frame) {
        return (frame >> SP_SHIFT) & SP_MASK;
    }

    /**
     * Encodes a stack depth into a frame.
     *
     * @param frame a frame
     * @param sp a stack depth
     * @return the updated value of {@code frame} that now encodes {@code sp}
     */
    private static int encodeSp(int frame, int sp) {
        assert sp <= MAX_STACK;
        return frame | (sp << SP_SHIFT);
    }

    @Override
    protected boolean mergeInto(int targetBlockIndex, int sp) {
        final boolean targetIsExceptionHandler = sp == -1;
        final int spForMerge = targetIsExceptionHandler ? 0 : sp;
        if ((frames[targetBlockIndex] & INITIALIZED_FRAME_BIT) == 0) {
            final int currentMap = decodeReferenceMap(currentFrame, spForMerge);
            final int targetFrame;
            if (targetIsExceptionHandler) {
                // Leave the exception object on the stack
                targetFrame = encodeSp(updateStack(currentMap, 0, true), 1);
            } else {
                assert sp <= MAX_STACK;
                targetFrame = encodeSp(currentMap, sp);
            }
            frames[targetBlockIndex] = targetFrame | INITIALIZED_FRAME_BIT;
            return true;
        }

        final int targetMap = decodeReferenceMap(frames[targetBlockIndex], spForMerge);
        final int currentMap = decodeReferenceMap(currentFrame, spForMerge);

        final int mergedMap = targetMap & currentMap;
        if (mergedMap != targetMap) {
            final int mergedFrame;
            if (targetIsExceptionHandler) {
                assert decodeSp(frames[targetBlockIndex]) == 1;
                // Leave the exception object on the stack
                mergedFrame = encodeSp(updateStack(mergedMap, 0, true), 1);
                assert decodeSp(mergedFrame) == 1;
            } else {
                mergedFrame = encodeSp(mergedMap, sp);
            }
            frames[targetBlockIndex] = mergedFrame | INITIALIZED_FRAME_BIT;
            return true;
        }
        return false;
    }

    @Override
    public boolean isFrameInitialized(int blockIndex) {
        return (frames[blockIndex] & INITIALIZED_FRAME_BIT) != 0;
    }

    @Override
    protected void resetInterpreter(ReferenceMapInterpreterContext context) {
        super.resetInterpreter(context);
        currentFrame = 0;
        if (context.blockFrames() == null) {
            assert maxStack() <= MAX_STACK && (maxStack() + maxLocals()) < MAX_SLOTS;
            frames = new int[context.numberOfBlocks()];
        } else {
            frames = (int[]) context.blockFrames();
        }
    }

    @Override
    protected Object frames() {
        return frames;
    }

    @Override
    public boolean performsAllocation() {
        return false;
    }

    @Override
    int resetAtBlock(int blockIndex) {
        currentFrame = frames[blockIndex];
        return decodeSp(currentFrame);
    }

    @Override
    boolean isLocalRef(int index) {
        return testBit(currentFrame, index);
    }

    @Override
    boolean isStackRef(int index) {
        return testBit(currentFrame, index + maxLocals());
    }

    @Override
    void updateLocal(int index, boolean isRef) {
        currentFrame = updateLocal(currentFrame, index, isRef);
    }

    private static int updateLocal(int frame, int index, boolean isRef) {
        return updateBit(frame, index, isRef);
    }

    @Override
    void updateStack(int index, boolean isRef) {
        currentFrame = updateStack(currentFrame, maxLocals(), index, isRef);
    }

    private int updateStack(int frame, int index, boolean isRef) {
        return updateStack(frame, maxLocals(), index, isRef);
    }

    private static int updateStack(int frame, int maxLocals, int index, boolean isRef) {
        return updateBit(frame, index + maxLocals, isRef);
    }

    private static boolean testBit(int map, int index) {
        return (map & (1 << index)) != 0;
    }

    private static int updateBit(int map, int index, boolean set) {
        if (set) {
            return map | (1 << index);
        }
        return map & ~(1 << index);
    }
}
