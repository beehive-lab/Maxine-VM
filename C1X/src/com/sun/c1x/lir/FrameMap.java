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

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;

/**
 * This class is used to build the stack frame layout for a compiled method.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public final class FrameMap {

    public static final int SPILL_SLOT_SIZE = 4;

    private final C1XCompiler compilation;
    private final CallingConvention incomingArguments;
    private final int monitorCount;

    // Values set after register allocation is complete
    private int frameSize;
    private int spillSlotCount;

    // Area occupied by outgoing overflow arguments. This value is adjusted as calling conventions for outgoing calls are retrieved.
    private int outgoingSize;

    public FrameMap(C1XCompiler compiler, RiMethod method, int monitors) {
        this.compilation = compiler;
        this.frameSize = -1;
        this.spillSlotCount = -1;

        assert monitors >= 0 : "not set";
        monitorCount = monitors;
        if (method == null) {
            incomingArguments = new CallingConvention(new CiLocation[0]);
        } else {
            incomingArguments = javaCallingConvention(Util.signatureToKinds(method.signatureType(), !method.isStatic()), false, false);
        }
    }

    public CallingConvention runtimeCallingConvention(CiKind[] signature) {
        CiLocation[] locations = compilation.target.config.getRuntimeParameterLocations(signature);
        return createCallingConvention(locations, false);
    }

    public CallingConvention javaCallingConvention(CiKind[] signature, boolean outgoing, boolean reserveOutgoingArgumentsArea) {
        CiLocation[] locations = compilation.target.config.getJavaParameterLocations(signature, outgoing);
        return createCallingConvention(locations, reserveOutgoingArgumentsArea);
    }

    private CallingConvention createCallingConvention(CiLocation[] locations, boolean reserveOutgoingArgumentsArea) {
        final CallingConvention result = new CallingConvention(locations);
        if (reserveOutgoingArgumentsArea) {
            assert frameSize == -1 : "frame size must not yet be fixed!";
            outgoingSize = Math.max(outgoingSize, result.overflowArgumentSize);
        }
        return result;
    }

    public CallingConvention incomingArguments() {
        return incomingArguments;
    }

    public Address addressForSlot(int stackSlot) {
        return addressForSlot(stackSlot, 0);
    }

    public Address addressForSlot(int stackSlot, int offset) {
        return new Address(compilation.target.stackPointerRegister, spOffsetForSlot(stackSlot) + offset);
    }

    int spOffsetForSlot(int index) {
        int offset = spOffsetForSpill(index);
        assert offset < frameSize() : "spill outside of frame";
        return offset;
    }

    int spOffsetForSpill(int index) {
        assert index >= 0 && index < spillSlotCount : "out of range";
        return Util.roundUp(outgoingSize + incomingArguments.overflowArgumentSize, Double.SIZE / Byte.SIZE) + index * SPILL_SLOT_SIZE;
    }

    int spOffsetForMonitorBase(int index) {
        int endOfSpills = Util.roundUp(outgoingSize + incomingArguments.overflowArgumentSize, Double.SIZE / Byte.SIZE) + spillSlotCount * SPILL_SLOT_SIZE;
        return Util.roundUp(endOfSpills, compilation.target.arch.wordSize) + index * compilation.runtime.sizeofBasicObjectLock();
    }

    int spOffsetForMonitorObject(int index)  {
      checkMonitorIndex(index);
      return spOffsetForMonitorBase(index) + compilation.runtime.basicObjectLockOffsetInBytes();
    }

    void checkMonitorIndex(int monitorIndex) {
        assert monitorIndex >= 0 && monitorIndex < monitorCount : "bad index";
    }

    public int addressForMonitorLock(int monitorNo) {
        return Util.nonFatalUnimplemented(0);
    }

    public Address addressForMonitorObject(int i) {
        return Util.nonFatalUnimplemented(null);
    }

    public int frameSize() {
        return frameSize;
    }

    public void setFrameSize(int frameSize) {
        assert this.frameSize == -1 : "should only be calculated once";
        this.frameSize = frameSize;
    }

    public void finalizeFrame(int spillSlotCount) {
        assert this.spillSlotCount == -1 : "can only be set once";
        assert this.frameSize == -1 : "should only be calculated once";
        assert spillSlotCount >= 0 : "must be positive";

        this.spillSlotCount = spillSlotCount;
        this.frameSize = compilation.target.alignFrameSize(spOffsetForMonitorBase(0) + monitorCount * compilation.runtime.sizeofBasicObjectLock());
    }

    public CiLocation regname(LIROperand opr) {
        if (opr.isStack()) {
            return new CiLocation(opr.kind, opr.stackIndex() * SPILL_SLOT_SIZE, compilation.target.sizeInBytes(opr.kind), false);
        } else if (opr.isRegister()) {
            if (opr.isDoubleCpu() || opr.isDoubleXmm()) {
                return new CiLocation(opr.kind, opr.asRegisterLow(), opr.asRegisterHigh());
            } else {
                return new CiLocation(opr.kind, opr.asRegister());
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    public CiLocation objectSlotRegname(int i) {
        return new CiLocation(CiKind.Object, this.spOffsetForSpill(i), SPILL_SLOT_SIZE, false);
    }

    public CiLocation locationForMonitor(int monitorIndex) {
        return new CiLocation(CiKind.Object, spOffsetForMonitorObject(monitorIndex), SPILL_SLOT_SIZE, false);
    }
}
