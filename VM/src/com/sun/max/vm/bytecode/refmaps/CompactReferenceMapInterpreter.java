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
/*VCSID=74bb2622-d223-4056-96e8-8c0ff2caad36*/
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
 *
 * @author Doug Simon
 */
public class CompactReferenceMapInterpreter extends ReferenceMapInterpreter {

    private static final int INITIALIZED_FRAME_BIT = 0x80000000;
    private static final int SP_SHIFT = 27;
    private static final int SP_MASK = 0xF;
    public static final int MAX_SLOTS = 27;
    public static final int MAX_STACK = 15;

    private int[] _frames;
    private int _currentFrame;

    /**
     * Decodes the part of a frame that encodes the reference map for the locals and operand stack.
     * 
     * @param frame
     *                a frame
     * @param sp
     *                the number of reference map bits to include for the operand stack (i.e the current stack depth)
     * @return the reference maps for the locals and operand stack encoded in {@code frame}
     */
    private int decodeReferenceMap(int frame, int sp) {
        final int mask = (1 << maxLocals() + sp) - 1;
        return frame & mask;
    }

    /**
     * Decodes the stack depth from a frame.
     * 
     * @param frame
     *                a frame
     * @return the stack depth encoded in {@code frame}
     */
    private static int decodeSp(int frame) {
        return (frame >> SP_SHIFT) & SP_MASK;
    }

    /**
     * Encodes a stack depth into a frame.
     * 
     * @param frame
     *                a frame
     * @param sp
     *                a stack depth
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
        if ((_frames[targetBlockIndex] & INITIALIZED_FRAME_BIT) == 0) {
            final int currentMap = decodeReferenceMap(_currentFrame, spForMerge);
            final int targetFrame;
            if (targetIsExceptionHandler) {
                // Leave the exception object on the stack
                targetFrame = encodeSp(updateStack(currentMap, 0, true), 1);
            } else {
                assert sp <= MAX_STACK;
                targetFrame = encodeSp(currentMap, sp);
            }
            _frames[targetBlockIndex] = targetFrame | INITIALIZED_FRAME_BIT;
            return true;
        }

        final int targetMap = decodeReferenceMap(_frames[targetBlockIndex], spForMerge);
        final int currentMap = decodeReferenceMap(_currentFrame, spForMerge);

        final int mergedMap = targetMap & currentMap;
        if (mergedMap != targetMap) {
            final int mergedFrame;
            if (targetIsExceptionHandler) {
                assert decodeSp(_frames[targetBlockIndex]) == 1;
                // Leave the exception object on the stack
                mergedFrame = encodeSp(updateStack(mergedMap, 0, true), 1);
                assert decodeSp(mergedFrame) == 1;
            } else {
                mergedFrame = encodeSp(mergedMap, sp);
            }
            _frames[targetBlockIndex] = mergedFrame | INITIALIZED_FRAME_BIT;
            return true;
        }
        return false;
    }

    @Override
    boolean isFrameInitialized(int blockIndex) {
        return (_frames[blockIndex] & INITIALIZED_FRAME_BIT) != 0;
    }

    @Override
    protected void resetInterpreter(ReferenceMapInterpreterContext context) {
        super.resetInterpreter(context);
        _currentFrame = 0;
        if (context.blockFrames() == null) {
            assert maxStack() <= MAX_STACK && (maxStack() + maxLocals()) < MAX_SLOTS;
            _frames = new int[context.numberOfBlocks()];
        } else {
            _frames = (int[]) context.blockFrames();
        }
    }

    @Override
    protected Object frames() {
        return _frames;
    }

    @Override
    public boolean performsAllocation() {
        return false;
    }

    @Override
    int resetAtBlock(int blockIndex) {
        _currentFrame = _frames[blockIndex];
        return decodeSp(_currentFrame);
    }

    @Override
    boolean isLocalRef(int index) {
        return testBit(_currentFrame, index);
    }

    @Override
    boolean isStackRef(int index) {
        return testBit(_currentFrame, index + maxLocals());
    }

    @Override
    void updateLocal(int index, boolean isRef) {
        _currentFrame = updateLocal(_currentFrame, index, isRef);
    }

    private static int updateLocal(int frame, int index, boolean isRef) {
        return updateBit(frame, index, isRef);
    }

    @Override
    void updateStack(int index, boolean isRef) {
        _currentFrame = updateStack(_currentFrame, maxLocals(), index, isRef);
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
