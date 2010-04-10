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

import com.sun.c1x.ci.*;
import com.sun.c1x.ci.CiAddress.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;

/**
 * This class represents debugging and deoptimization information attached to a LIR instruction.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class LIRDebugInfo {

    public abstract static class ValueLocator {
        public abstract CiValue getLocation(Value value);
    }

    public final FrameState state;
    public final int bci;
    public final List<ExceptionHandler> exceptionHandlers;

    public IRScopeDebugInfo scopeDebugInfo;

    private CiDebugInfo debugInfo;
    private CiDebugInfo.Frame debugFrame;

    public LIRDebugInfo(FrameState state, int bci, List<ExceptionHandler> exceptionHandlers) {
        this.bci = bci;
        this.scopeDebugInfo = null;
        this.state = state;
        this.exceptionHandlers = exceptionHandlers;
    }

    private LIRDebugInfo(LIRDebugInfo info) {
        this.bci = info.bci;
        this.scopeDebugInfo = null;
        this.state = info.state;

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

    public void allocateDebugInfo(int registerSize, int frameSize, CiTarget target) {
        byte[] registerRefMap = registerSize > 0 ? newRefMap(registerSize) : null;
        byte[] stackRefMap = frameSize > 0 ? newRefMap(frameSize / target.spillSlotSize) : null;
        debugInfo = new CiDebugInfo(state.scope().toCodeSite(bci), null, registerRefMap, stackRefMap);
    }

    public void setOop(CiLocation location, CiTarget target) {
        assert debugInfo != null : "debug info not allocated yet";
        if (location.isAddress()) {
            CiAddress stackLocation = (CiAddress) location;
            assert stackLocation.format == Format.BASE_DISP;
            if (stackLocation.base == CiRegister.Frame.asLocation()) {
                int offset = stackLocation.displacement;
                assert offset % target.arch.wordSize == 0 : "must be aligned";
                int stackMapIndex = offset / target.arch.wordSize;
                setBit(debugInfo.frameRefMap, stackMapIndex);
            }
        } else {
            assert location.isRegister() : "objects can only be in a register";
            CiRegisterLocation registerLocation = (CiRegisterLocation) location;
            int index = target.allocationSpec.refMapIndexMap[registerLocation.register.number];
            assert index >= 0 : "object cannot be in non-object register " + registerLocation.register;
            setBit(debugInfo.registerRefMap, index);
        }
    }

    public void buildDebugFrame(ValueLocator locator) {
        debugFrame = makeFrame(state, bci, locator);
    }

    public byte[] registerRefMap() {
        return debugInfo.registerRefMap;
    }

    public byte[] stackRefMap() {
        return debugInfo.frameRefMap;
    }

    public CiDebugInfo debugInfo() {
        assert debugInfo != null : "debug info not allocated yet";
        return debugInfo;
    }

    public boolean hasDebugInfo() {
        return debugInfo != null;
    }

    private CiDebugInfo.Frame makeFrame(FrameState state, int bci, ValueLocator locator) {
        // XXX: cache the debug information for each value stack if equivalent to previous
        return createFrame(state, bci, locator);
    }

    private byte[] newRefMap(int slots) {
        return new byte[(slots + 7) >> 3];
    }

    private void setBit(byte[] array, int bit) {
        int index = bit >> 3;
        int offset = bit & 0x7;

        array[index] = (byte) (array[index] | (1 << offset));
    }

    private CiDebugInfo.Frame createFrame(FrameState state, int bci, ValueLocator locator) {
        int stackBegin = state.callerStackSize();
        int numStack = 0;
        int numLocals = 0;
        int numLocks;
        for (int i = 0; i < state.localsSize(); i++) {
            if (state.localAt(i) != null) {
                numLocals = 1 + i;
            }
        }
        for (int i = 0; i < state.stackSize(); i++) {
            if (state.stackAt(i) != null) {
                numStack = 1 + i;
            }
        }
        numLocks = state.locksSize();

        CiValue[] values = new CiValue[numLocals + numStack + numLocks];
        int pos = 0;
        for (int i = 0; i < state.localsSize(); i++, pos++) {
            Value v = state.localAt(i);
            if (v != null) {
                values[pos] = locator.getLocation(v);
            }
        }
        for (int i = stackBegin; i < state.stackSize(); i++, pos++) {
            Value v = state.stackAt(i);
            if (v != null) {
                values[pos] = locator.getLocation(v);
            }
        }
        for (int i = 0; i < state.locksSize(); i++, pos++) {
            Value v = state.lockAt(i);
            assert v != null;
            values[pos] = locator.getLocation(v);
        }

        FrameState caller = state.scope().callerState();
        CiDebugInfo.Frame parent = null;
        if (caller != null) {
             parent = makeFrame(caller, state.scope().callerBCI(), locator);
        }
        return new CiDebugInfo.Frame(parent, state.scope().toCodeSite(bci), values, numLocals, numStack, numLocks);
    }

}
