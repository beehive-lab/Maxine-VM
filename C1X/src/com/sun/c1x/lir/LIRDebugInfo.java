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

import com.sun.c1x.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiDebugInfo.Frame;

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
    public final List<ExceptionHandler> exceptionHandlers;

    public IRScopeDebugInfo scopeDebugInfo;

    private CiDebugInfo debugInfo;
    private CiDebugInfo.Frame debugFrame;

    public LIRDebugInfo(FrameState state, int bci, List<ExceptionHandler> exceptionHandlers) {
        assert state != null;
        this.scopeDebugInfo = null;
        this.state = state;
        this.exceptionHandlers = exceptionHandlers;
    }

    private LIRDebugInfo(LIRDebugInfo info) {
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

    public void allocateDebugInfo(int frameSize, CiTarget target) {
        int registerSlots = target.arch.registerReferenceMapBitCount;
        byte[] registerRefMap = registerSlots > 0 ? newRefMap(registerSlots) : null;
        byte[] stackRefMap = frameSize > 0 ? newRefMap(frameSize / target.spillSlotSize) : null;
        CiCodePos codePos = scopeDebugInfo == null ? state.scope().toCodeSite(state.bci) : makeFrame(scopeDebugInfo);
        debugInfo = new CiDebugInfo(codePos, registerRefMap, stackRefMap);
    }

    private static Frame makeFrame(IRScopeDebugInfo scope) {
        Frame caller = null;
        if (scope.caller != null) {
            caller = makeFrame(scope.caller);
        }
        int numLocals = 0;
        int numStack = 0;
        int numLocks = 0;
        ArrayList<CiValue> values = new ArrayList<CiValue>();

        if (scope.locals != null) {
            for (CiValue v : scope.locals) {
                if (v != null) {
                    numLocals++;
                    values.add(v);
                }
            }
        }
        if (scope.stack != null) {
            for (CiValue v : scope.stack) {
                if (v != null) {
                    numStack++;
                    values.add(v);
                }
            }
        }
        if (scope.locks != null) {
            for (CiValue v : scope.locks) {
                if (v != null) {
                    numLocks++;
                    assert v instanceof CiAddress;
                    CiAddress adr = (CiAddress) v;
                    assert adr.base.isRegister() && ((CiRegisterValue) adr.base).reg == CiRegister.Frame;
                    assert adr.index == CiValue.IllegalValue;

                    // TODO this is a hack ... and not portable, etc.
                    CiValue value = CiStackSlot.get(CiKind.Object, adr.displacement / 8);

                    values.add(value);
                }
            }
        }
        return new Frame(caller, scope.scope.method, scope.bci, values.toArray(new CiValue[values.size()]), numLocals, numStack, numLocks);
    }

    public void setOop(CiValue location, C1XCompilation compilation) {
        CiTarget target = compilation.target;
        assert debugInfo != null : "debug info not allocated yet";
        if (location.isAddress()) {
            CiAddress stackLocation = (CiAddress) location;
            assert stackLocation.index.isIllegal();
            if (stackLocation.base == CiRegister.Frame.asValue()) {
                int offset = stackLocation.displacement;
                assert offset % target.wordSize == 0 : "must be aligned";
                int stackMapIndex = offset / target.wordSize;
                setBit(debugInfo.frameRefMap, stackMapIndex);
            }
        } else {
            assert location.isRegister() : "objects can only be in a register";
            CiRegisterValue registerLocation = (CiRegisterValue) location;
            int encoding = registerLocation.reg.encoding;
            assert encoding >= 0 : "object cannot be in non-object register " + registerLocation.reg;
            assert encoding < target.arch.registerReferenceMapBitCount;
            setBit(debugInfo.registerRefMap, encoding);
        }
    }

    public void buildDebugFrame(ValueLocator locator) {
        debugFrame = makeFrame(state, state.bci, locator);
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
        // XXX: cache the debug information for each frame state if equivalent to previous
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

        FrameState caller = state.callerState();
        CiDebugInfo.Frame parent = null;
        IRScope scope = state.scope();
        if (caller != null) {
             parent = makeFrame(caller, scope.callerBCI(), locator);
        }
        return new CiDebugInfo.Frame(parent, scope.method, bci, values, numLocals, numStack, numLocks);
    }

}
