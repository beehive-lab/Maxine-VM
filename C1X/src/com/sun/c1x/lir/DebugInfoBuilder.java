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
package com.sun.c1x.lir;

import com.sun.c1x.ci.*;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.ir.Value;

/**
 * This class is responsible for collecting the debug information while compilation in order
 * to produce a {@link com.sun.c1x.ci.CiDebugInfo} instance which can be passed back through
 * the compiler interface.
 *
 * @author Ben L. Titzer
 */
public class DebugInfoBuilder {

    private final ValueLocator locator;
    private final CiTarget target;
    private final int frameSize;

    private CiCodePos currentCodePos;
    private CiDebugInfo.Frame currentFrame;
    private byte[] currentRegisterRefMap;
    private byte[] currentStackRefMap;

    public DebugInfoBuilder(CiTarget target, ValueLocator locator, int frameSize) {
        this.target = target;
        this.locator = locator;
        this.frameSize = frameSize;
    }

    public void begin(CiCodePos codePos) {
        currentCodePos = codePos;
        currentFrame = null;
        currentRegisterRefMap = null;
        currentStackRefMap = null;
    }

    public void begin(ValueStack stack, int bci) {
        currentRegisterRefMap = null;
        currentStackRefMap = null;
        setFrame(stack, bci);
    }

    public CiDebugInfo finish() {
        assert currentCodePos != null : "not currently building debug info";
        return new CiDebugInfo(currentCodePos, currentFrame, currentRegisterRefMap, currentStackRefMap);
    }

    public void setFrame(ValueStack stack, int bci) {
        currentFrame = makeFrame(stack, bci);
        currentCodePos = currentFrame.codePos;
    }

    private CiDebugInfo.Frame makeFrame(ValueStack stack, int bci) {
        // XXX: cache the debug information for each value stack if equivalent to previous
        return createFrame(stack, bci);
    }

    private CiDebugInfo.Frame createFrame(ValueStack stack, int bci) {
        int stackBegin = stack.callerStackSize();
        int numStack = 0;
        int numLocals = 0;
        int numLocks;
        for (int i = 0; i < stack.localsSize(); i++) {
            if (stack.localAt(i) != null) {
                numLocals = 1 + i;
            }
        }
        for (int i = 0; i < stack.stackSize(); i++) {
            if (stack.stackAt(i) != null) {
                numStack = 1 + i;
            }
        }
        numLocks = stack.locksSize();

        CiValue[] values = new CiValue[numLocals + numStack + numLocks];
        int pos = 0;
        for (int i = 0; i < stack.localsSize(); i++, pos++) {
            Value v = stack.localAt(i);
            if (v != null) {
                values[pos] = locator.locate(v);
            }
        }
        for (int i = stackBegin; i < stack.stackSize(); i++, pos++) {
            Value v = stack.stackAt(i);
            if (v != null) {
                values[pos] = locator.locate(v);
            }
        }
        for (int i = 0; i < stack.locksSize(); i++, pos++) {
            Value v = stack.lockAt(i);
            assert v != null;
            values[pos] = locator.locate(v);
        }

        ValueStack caller = stack.scope().callerState();
        CiDebugInfo.Frame parent = null;
        if (caller != null) {
             parent = makeFrame(caller, stack.scope().callerBCI());
        }
        return new CiDebugInfo.Frame(parent, stack.scope().toCodeSite(bci), values, numLocals, numStack, numLocks);
    }

    public void setOop(CiLocation location) {
        if (location.isStack()) {
            int offset = location.stackOffset();
            assert offset % target.arch.wordSize == 0 : "must be aligned";
            int stackMapIndex = offset / target.arch.wordSize;
            if (currentStackRefMap == null) {
                currentStackRefMap = newRefMap(frameSize);
            }
            setBit(currentStackRefMap, stackMapIndex);
        } else {
            int index = target.allocatableRegs.referenceMapIndex[location.first().number];
            if (currentRegisterRefMap == null) {
                currentRegisterRefMap = newRefMap(target.allocatableRegs.registerRefMapSize);
            }
            assert index >= 0 : "object cannot be in non-object register " + location.first();
            assert location.isSingleRegister() : "objects can only be in a single register";
            setBit(currentRegisterRefMap, index);
        }
    }

    public abstract class ValueLocator {
        public abstract CiValue locate(Value value);
    }

    private byte[] newRefMap(int slots) {
        return new byte[(slots + 7) >> 3];
    }

    private void setBit(byte[] array, int bit) {
        int index = bit >> 3;
        int offset = bit & 0x7;

        array[index] = (byte) (array[index] | (1 << offset));
    }
}
