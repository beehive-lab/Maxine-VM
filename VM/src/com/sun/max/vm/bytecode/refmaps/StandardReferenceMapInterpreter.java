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
package com.sun.max.vm.bytecode.refmaps;

import java.util.*;

/**
 * An interpreter that encodes the details for a frame in a {@link Frame} object.
 *
 * @author Doug Simon
 */
public class StandardReferenceMapInterpreter extends ReferenceMapInterpreter {

    public static class Frame {
        /**
         * The reference map for the locals in this frame.
         */
        final BitSet locals;

        /**
         * The reference map for the operand stack in this frame.
         */
        final BitSet stack;
        final int sp;

        Frame(int maxLocals, int maxStack, int sp) {
            this.locals = new BitSet(maxLocals);
            this.stack = new BitSet(maxStack);
            this.sp = sp;
        }

        Frame(Frame other, int sp) {
            this.locals = (BitSet) other.locals.clone();
            this.stack = (BitSet) other.stack.clone();
            final int stackDepth = stack.length();
            if (stackDepth >= sp) {
                stack.clear(sp, stackDepth);
            }
            this.sp = sp;
        }

        void resetFrom(Frame other) {
            locals.clear();
            stack.clear();
            locals.or(other.locals);
            stack.or(other.stack);
        }

        private static boolean merge(BitSet dst, BitSet src) {
            final int dstCardinality = dst.cardinality();
            dst.and(src);
            return dstCardinality != dst.cardinality();
        }

        boolean mergeFrom(Frame other, boolean ignoreStack) {
            final boolean localsChanged = merge(locals, other.locals);
            if (ignoreStack) {
                return localsChanged;
            }
            return merge(stack, other.stack) || localsChanged;
        }

        @Override
        public String toString() {
            return "locals = " + locals + ", stack = " + stack;
        }
    }

    private Frame[] frames;
    private Frame currentFrame;

    @Override
    protected Object frames() {
        return frames;
    }

    @Override
    protected boolean mergeInto(int targetBlockIndex, int sp) {
        final boolean targetIsExceptionHandler = sp == -1;
        if (frames[targetBlockIndex] == null) {
            final Frame targetFrame;
            if (targetIsExceptionHandler) {
                targetFrame = new Frame(currentFrame, 1);
                // Leave the exception object on the stack
                targetFrame.stack.clear();
                targetFrame.stack.set(0);
            } else {
                targetFrame = new Frame(currentFrame, sp);
            }
            frames[targetBlockIndex] = targetFrame;
            return true;
        }

        final Frame targetFrame = frames[targetBlockIndex];
        if (targetFrame.mergeFrom(currentFrame, targetIsExceptionHandler)) {
            if (targetIsExceptionHandler) {
                // Leave the exception object on the stack
                assert targetFrame.sp == 1;
                assert targetFrame.stack.get(0);
            } else {
                assert targetFrame.sp == sp;
            }
            return true;
        }
        return false;
    }

    @Override
    boolean isFrameInitialized(int blockIndex) {
        return frames[blockIndex] != null;
    }

    @Override
    protected void resetInterpreter(ReferenceMapInterpreterContext context) {
        super.resetInterpreter(context);
        if (context.blockFrames() == null) {
            frames = new Frame[context.numberOfBlocks()];
            currentFrame = new Frame(maxLocals(), maxStack(), 0);
        } else {
            frames = (Frame[]) context.blockFrames();
            currentFrame = new Frame(frames[0], 0);
        }
    }

    @Override
    public boolean performsAllocation() {
        return true;
    }

    @Override
    int resetAtBlock(int blockIndex) {
        final Frame blockFrame = frames[blockIndex];
        currentFrame.resetFrom(blockFrame);
        return blockFrame.sp;
    }

    @Override
    boolean isLocalRef(int index) {
        return currentFrame.locals.get(index);
    }

    @Override
    boolean isStackRef(int index) {
        return currentFrame.stack.get(index);
    }

    @Override
    void updateLocal(int index, boolean isRef) {
        currentFrame.locals.set(index, isRef);
    }

    @Override
    void updateStack(int index, boolean isRef) {
        currentFrame.stack.set(index, isRef);
    }
}
