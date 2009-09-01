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
 *
 * @author Thomas Wuerthinger
 *
 */
public class FrameMap {

    public static final int SpillSlotSize = 4;

    private final C1XCompiler compilation;

    private final CallingConvention incomingArguments;
    private final int monitorCount;

    // Values set after register allocation is complete
    private int frameSize;
    private int spillSlotCount;

    // Area occupied by outgoing overflow arguments. This value is adjusted as calling conventions for outgoing calls are retrieved.
    private int reservedOutgoingArgumentsArea;

    public FrameMap(C1XCompiler compiler, RiMethod method, int monitors) {
        this.compilation = compiler;
        frameSize = -1;
        spillSlotCount = -1;

        assert monitors >= 0 : "not set";
        monitorCount = monitors;
        incomingArguments = javaCallingConvention(Util.signatureToBasicTypes(method.signatureType(), !method.isStatic()), false);
    }

    public CallingConvention runtimeCallingConvention(CiKind[] signature) {
        CiLocation[] locations = compilation.runtime.runtimeCallingConvention(signature);
        return createCallingConvention(locations, true);
    }

    public CallingConvention javaCallingConvention(CiKind[] signature, boolean outgoing) {
        CiLocation[] locations = compilation.runtime.javaCallingConvention(signature, outgoing);
        return createCallingConvention(locations, outgoing);
    }

    private CallingConvention createCallingConvention(CiLocation[] locations, boolean outgoing) {
        final CallingConvention result = new CallingConvention(locations);
        if (outgoing) {
            reservedOutgoingArgumentsArea = Math.max(reservedOutgoingArgumentsArea, result.overflowArgumentsSize());
        }
        return result;
    }

    public CallingConvention incomingArguments() {
        return this.incomingArguments;
    }

    public Address addressForSlot(int stackSlot) {
        return addressForSlot(stackSlot, 0);
    }

    public Address addressForSlot(int stackSlot, int offset) {
        return new Address(compilation.target.stackRegister, spOffsetForSlot(stackSlot) + offset);
    }

    int spOffsetForSlot(int index) {
        int offset = spOffsetForSpill(index);
        assert offset < frameSize() : "spill outside of frame";
        return offset;
    }

    int spOffsetForSpill(int index) {
        assert index >= 0 && index < spillSlotCount : "out of range";
        int offset = Util.roundTo(reservedOutgoingArgumentsArea + incomingArguments.overflowArgumentsSize(), Double.SIZE / Byte.SIZE) + index * SpillSlotSize;
        return offset;
    }

    int spOffsetForMonitorBase(int index) {
        int endOfSpills = Util.roundTo(reservedOutgoingArgumentsArea + incomingArguments.overflowArgumentsSize(), Double.SIZE / Byte.SIZE) + spillSlotCount * SpillSlotSize;
        int offset = Util.roundTo(endOfSpills, compilation.target.arch.wordSize) + index * compilation.runtime.basicObjectLockSize();
        return offset;
    }

    int spOffsetForMonitorLock(int index)  {
      checkMonitorIndex(index);
      return spOffsetForMonitorBase(index) + compilation.runtime.basicObjectLockOffsetInBytes();
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
        assert frameSize != -1 : "hasn't been calculated";
        return frameSize;
    }

    public void finalizeFrame(int spillSlotCount) {
        assert spillSlotCount >= 0 : "must be positive";
        assert this.spillSlotCount == -1 : "can only be set once";
        this.spillSlotCount = spillSlotCount;
        assert frameSize == -1 : "should only be calculated once";
        frameSize = Util.roundTo(spOffsetForMonitorBase(0) + monitorCount * compilation.runtime.sizeofBasicObjectLock() +
        compilation.target.arch.framePadding, compilation.target.stackAlignment);
    }

    public CiLocation regname(LIROperand opr) {
        if (opr.isStack()) {
            return new CiLocation(opr.type(), opr.stackIx() * SpillSlotSize, SpillSlotSize * opr.basicType.size, false);
        } else if (opr.isRegister()) {
            if (opr.isDoubleCpu() || opr.isDoubleXmm()) {
                return new CiLocation(opr.type(), opr.asRegisterLo(), opr.asRegisterHi());
            } else {
                return new CiLocation(opr.type(), opr.asRegister());
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    public boolean isCallerSaveRegister(LIROperand res) {
        return Util.nonFatalUnimplemented(false);
    }

    public int oopMapArgCount() {
        return Util.nonFatalUnimplemented(0);
    }

    public CiLocation objectSlotRegname(int i) {
        return new CiLocation(CiKind.Object, this.spOffsetForSpill(i), SpillSlotSize, false);
    }

    public CiLocation monitorObjectRegname(int i) {
        return Util.nonFatalUnimplemented(null);
    }

    public int stackRefMapSize() {
        return ((frameSize + 7) >> 3) << 3; // round up to next byte size
    }

    public CiLocation locationForMonitor(int monitorIndex) {
        return new CiLocation(CiKind.Object, spOffsetForMonitorObject(monitorIndex), SpillSlotSize, false);
    }
}
