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
/*VCSID=60568ab6-9f01-4942-9cea-639c817ac831*/
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
        final BitSet _locals;

        /**
         * The reference map for the operand stack in this frame.
         */
        final BitSet _stack;
        final int _sp;

        Frame(int maxLocals, int maxStack, int sp) {
            _locals = new BitSet(maxLocals);
            _stack = new BitSet(maxStack);
            _sp = sp;
        }

        Frame(Frame other, int sp) {
            _locals = (BitSet) other._locals.clone();
            _stack = (BitSet) other._stack.clone();
            final int stackDepth = _stack.length();
            if (stackDepth >= sp) {
                _stack.clear(sp, stackDepth);
            }
            _sp = sp;
        }

        void resetFrom(Frame other) {
            _locals.clear();
            _stack.clear();
            _locals.or(other._locals);
            _stack.or(other._stack);
        }

        private static boolean merge(BitSet dst, BitSet src) {
            final int dstCardinality = dst.cardinality();
            dst.and(src);
            return dstCardinality != dst.cardinality();
        }

        boolean mergeFrom(Frame other, boolean ignoreStack) {
            final boolean localsChanged = merge(_locals, other._locals);
            if (ignoreStack) {
                return localsChanged;
            }
            return merge(_stack, other._stack) || localsChanged;
        }

        @Override
        public String toString() {
            return "locals = " + _locals + ", stack = " + _stack;
        }
    }

    private Frame[] _frames;
    private Frame _currentFrame;

    @Override
    protected Object frames() {
        return _frames;
    }

    @Override
    protected boolean mergeInto(int targetBlockIndex, int sp) {
        final boolean targetIsExceptionHandler = sp == -1;
        if (_frames[targetBlockIndex] == null) {
            final Frame targetFrame;
            if (targetIsExceptionHandler) {
                targetFrame = new Frame(_currentFrame, 1);
                // Leave the exception object on the stack
                targetFrame._stack.clear();
                targetFrame._stack.set(0);
            } else {
                targetFrame = new Frame(_currentFrame, sp);
            }
            _frames[targetBlockIndex] = targetFrame;
            return true;
        }

        final Frame targetFrame = _frames[targetBlockIndex];
        if (targetFrame.mergeFrom(_currentFrame, targetIsExceptionHandler)) {
            if (targetIsExceptionHandler) {
                // Leave the exception object on the stack
                assert targetFrame._sp == 1;
                assert targetFrame._stack.get(0);
            } else {
                assert targetFrame._sp == sp;
            }
            return true;
        }
        return false;
    }

    @Override
    boolean isFrameInitialized(int blockIndex) {
        return _frames[blockIndex] != null;
    }

    @Override
    protected void resetInterpreter(ReferenceMapInterpreterContext context) {
        super.resetInterpreter(context);
        if (context.blockFrames() == null) {
            _frames = new Frame[context.numberOfBlocks()];
            _currentFrame = new Frame(maxLocals(), maxStack(), 0);
        } else {
            _frames = (Frame[]) context.blockFrames();
            _currentFrame = new Frame(_frames[0], 0);
        }
    }

    @Override
    public boolean performsAllocation() {
        return true;
    }

    @Override
    int resetAtBlock(int blockIndex) {
        final Frame blockFrame = _frames[blockIndex];
        _currentFrame.resetFrom(blockFrame);
        return blockFrame._sp;
    }

    @Override
    boolean isLocalRef(int index) {
        return _currentFrame._locals.get(index);
    }

    @Override
    boolean isStackRef(int index) {
        return _currentFrame._stack.get(index);
    }

    @Override
    void updateLocal(int index, boolean isRef) {
        _currentFrame._locals.set(index, isRef);
    }

    @Override
    void updateStack(int index, boolean isRef) {
        _currentFrame._stack.set(index, isRef);
    }
}
