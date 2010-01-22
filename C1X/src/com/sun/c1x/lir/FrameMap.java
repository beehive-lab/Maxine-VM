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
import com.sun.c1x.globalstub.GlobalStub;
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

    private static final int DOUBLE_SIZE = 8;

    private final C1XCompiler compiler;
    private final CallingConvention incomingArguments;
    private final int monitorCount;

    // Values set after register allocation is complete
    private int frameSize;
    private int spillSlotCount;

    // Area occupied by outgoing overflow arguments.
    // This value is adjusted as calling conventions for outgoing calls are retrieved.
    private int outgoingSize;

    /**
     * Creates a new frame map for the specified method.
     * @param compiler the compiler, which includes the runtime and target
     * @param method the outermost method being compiled
     * @param monitors the number of monitors allocated on the stack for this method
     */
    public FrameMap(C1XCompiler compiler, RiMethod method, int monitors) {
        this.compiler = compiler;
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

    /**
     * Gets the calling convention for calling runtime methods with the specified signature.
     * @param signature the signature of the arguments and return value
     * @return a {@link CallingConvention} instance describing the location of parameters and the return value
     */
    public CallingConvention runtimeCallingConvention(CiKind[] signature) {
        CiLocation[] locations = compiler.target.config.getRuntimeParameterLocations(signature);
        return createCallingConvention(locations, false);
    }

    /**
     * Gets the calling convention for calling Java methods with the specified signature.
     * @param signature the signature of the arguments and return value
     * @return a {@link CallingConvention} instance describing the location of parameters and the return value
     */
    public CallingConvention javaCallingConvention(CiKind[] signature, boolean outgoing, boolean reserveOutgoingArgumentsArea) {
        CiLocation[] locations = compiler.target.config.getJavaParameterLocations(signature, outgoing);
        return createCallingConvention(locations, reserveOutgoingArgumentsArea);
    }

    /**
     * Gets the calling convention for the incoming arguments to the compiled method.
     * @return the calling convention for incoming arguments
     */
    public CallingConvention incomingArguments() {
        return incomingArguments;
    }

    /**
     * Gets the frame size of the compiled frame.
     * @return the size in bytes of the frame
     */
    public int frameSize() {
        assert this.frameSize != -1 : "frame size not computed yet";
        return frameSize;
    }

    /**
     * Sets the frame size for this frame.
     * @param frameSize the frame size in bytes
     */
    public void setFrameSize(int frameSize) {
        assert this.frameSize == -1 : "should only be calculated once";
        this.frameSize = frameSize;
    }

    /**
     * Computes the frame size for this frame, given the number of spill slots.
     * @param spillSlotCount the number of spill slots
     */
    public void finalizeFrame(int spillSlotCount) {
        assert this.spillSlotCount == -1 : "can only be set once";
        assert this.frameSize == -1 : "should only be calculated once";
        assert spillSlotCount >= 0 : "must be positive";

        this.spillSlotCount = spillSlotCount;
        this.frameSize = compiler.target.alignFrameSize(spillEnd() + monitorCount * compiler.runtime.sizeofBasicObjectLock());
    }

    /**
     * Informs the frame map that the compiled code uses a particular global stub, which
     * may need stack space for outgoing arguments.
     * @param stub the global stub
     */
    public void usingGlobalStub(GlobalStub stub) {
        increaseOutgoing(stub.argsSize);
    }

    /**
     * Converts a {@link LIROperand} into a {@code CiLocation} within this frame.
     * @param opr the operand
     * @return a location
     */
    public CiLocation toLocation(LIROperand opr) {
        if (opr.isStack()) {
            // create a stack location
            int size = compiler.target.sizeInBytes(opr.kind);
            int offset = spOffsetForSlot(opr.stackIndex(), size);
            return new CiStackLocation(opr.kind, offset, size, false);
        } else if (opr.isSingleCpu() || opr.isSingleXmm()) {
            // create a single register location
            return new CiRegisterLocation(opr.kind, opr.asRegister());
        } else if (opr.isDoubleCpu() || opr.isDoubleXmm()) {
            // create a double register location
            return new CiRegisterLocation(opr.kind, opr.asRegisterLow(), opr.asRegisterHigh());
        }
        throw new CiBailout("cannot convert " + opr + "to location");
    }

    public Address toAddress(LIROperand opr, int offset) {
        assert opr.isStack();
        int index = opr.stackIndex();
        int size = compiler.target.sizeInBytes(opr.kind);
        return new Address(compiler.target.stackPointerRegister, spOffsetForSlot(index, size) + offset);

    }

    /**
     * Converts a spill index into a stack location.
     * @param kind the type of the spill slot
     * @param index the index into the spill slots
     * @return a representation of the stack location
     */
    public CiLocation toStackLocation(CiKind kind, int index) {
        int size = compiler.target.sizeInBytes(kind);
        return new CiStackLocation(kind, this.spOffsetForSlot(index, size), size, false);
    }

    /**
     * Converts the monitor index into a stack location.
     * @param monitorIndex the monitor index
     * @return a representation of the stack location
     */
    public CiLocation toMonitorLocation(int monitorIndex) {
        return new CiStackLocation(CiKind.Object, spOffsetForMonitorObject(monitorIndex), compiler.target.spillSlotSize, false);
    }

    private void increaseOutgoing(int argsSize) {
        if (argsSize > outgoingSize) {
            outgoingSize = Util.roundUp(argsSize, DOUBLE_SIZE);
        }
    }

    private CallingConvention createCallingConvention(CiLocation[] locations, boolean reserveOutgoingArgumentsArea) {
        final CallingConvention result = new CallingConvention(locations);
        if (reserveOutgoingArgumentsArea) {
            assert frameSize == -1 : "frame size must not yet be fixed!";
            increaseOutgoing(result.overflowArgumentSize);
        }
        return result;
    }

    private int spOffsetForSlot(int index, int size) {
        assert index >= 0 && index < spillSlotCount : "invalid spill slot";
        int offset = spillStart() + index * compiler.target.spillSlotSize;
        assert offset <= (frameSize() - size) : "spill outside of frame";
        return offset;
    }

    private int spOffsetForMonitorBase(int index) {
        assert index >= 0 && index < monitorCount : "invalid monitor index";
        int size = compiler.runtime.sizeofBasicObjectLock();
        int offset = spillEnd() + index * size;
        assert offset <= (frameSize() - size) : "monitor outside of frame";
        return offset;
    }

    private int spillStart() {
        return outgoingSize;
    }

    private int spillEnd() {
        return spillStart() + spillSlotCount * compiler.target.spillSlotSize;
    }

    private int spOffsetForMonitorObject(int index)  {
        return spOffsetForMonitorBase(index) + compiler.runtime.basicObjectLockOffsetInBytes();
    }

}
