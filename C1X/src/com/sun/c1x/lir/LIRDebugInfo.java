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

import java.util.*;

import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;
import com.sun.c1x.ci.*;

/**
 * This class represents debugging and deoptimization information attached to a LIR instruction.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class LIRDebugInfo {

    public static abstract class ValueLocator {
        public abstract CiValue locate(Value value);
    }

    public final ValueStack stack;
    public final int bci;
    public final List<ExceptionHandler> exceptionHandlers;

    public IRScopeDebugInfo scopeDebugInfo;

    private CiCodePos codePos;
    private CiDebugInfo.Frame debugFrame;
    private byte[] registerRefMap;
    private byte[] stackRefMap;

    public LIRDebugInfo(ValueStack state, int bci, List<ExceptionHandler> exceptionHandlers) {
        this.bci = bci;
        this.scopeDebugInfo = null;
        this.stack = state;
        this.exceptionHandlers = exceptionHandlers;
    }

    private LIRDebugInfo(LIRDebugInfo info) {
        this.bci = info.bci;
        this.scopeDebugInfo = null;
        this.stack = info.stack;

        // deep copy of exception handlers
        if (info.exceptionHandlers != null) {
            this.exceptionHandlers = new ArrayList<ExceptionHandler>();
            for (ExceptionHandler h : info.exceptionHandlers) {
                this.exceptionHandlers.add(new ExceptionHandler(h));
            }
        } else {
            this.exceptionHandlers = null;
        }
    }

    public LIRDebugInfo copy() {
        return new LIRDebugInfo(this);
    }

    public void allocateRefMaps(int registerSize, int frameSize) {
        if (registerSize > 0) {
            registerRefMap = newRefMap(registerSize);
        }
        if (frameSize > 0) {
            stackRefMap = newRefMap(frameSize);
        }
    }

    public void setOop(CiLocation location, CiTarget target) {
        if (location.isStack()) {
            int offset = location.stackOffset();
            assert offset % target.arch.wordSize == 0 : "must be aligned";
            int stackMapIndex = offset / target.arch.wordSize;
            setBit(stackRefMap, stackMapIndex);
        } else {
            int index = target.allocatableRegs.referenceMapIndex[location.first().number];
            assert index >= 0 : "object cannot be in non-object register " + location.first();
            assert location.isSingleRegister() : "objects can only be in a single register";
            setBit(registerRefMap, index);
        }
    }

    public void buildDebugFrame(ValueLocator locator) {
        debugFrame = makeFrame(stack, bci, locator);
    }

    public byte[] registerRefMap() {
        return registerRefMap;
    }

    public byte[] stackRefMap() {
        return stackRefMap;
    }

    public CiDebugInfo.Frame debugFrame() {
        return debugFrame;
    }

    private CiDebugInfo.Frame makeFrame(ValueStack stack, int bci, ValueLocator locator) {
        // XXX: cache the debug information for each value stack if equivalent to previous
        return createFrame(stack, bci, locator);
    }

    private byte[] newRefMap(int slots) {
        return new byte[(slots + 7) >> 3];
    }

    private void setBit(byte[] array, int bit) {
        int index = bit >> 3;
        int offset = bit & 0x7;

        array[index] = (byte) (array[index] | (1 << offset));
    }

    private CiDebugInfo.Frame createFrame(ValueStack stack, int bci, ValueLocator locator) {
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
             parent = makeFrame(caller, stack.scope().callerBCI(), locator);
        }
        return new CiDebugInfo.Frame(parent, stack.scope().toCodeSite(bci), values, numLocals, numStack, numLocks);
    }

}
