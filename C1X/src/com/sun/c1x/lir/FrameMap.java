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

import static com.sun.c1x.ci.CiKind.*;

import com.sun.c1x.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;

/**
 * This class is used to build the stack frame layout for a compiled method.
 *
 * This is the format of a stack frame on an x86 (i.e. IA32 or X64) platform:
 * <pre>
 *   Base       Contents
 *
 *          :                                :
 *          | incoming overflow argument n   |
 *          |     ...                        |
 *          | incoming overflow argument 0   |
 *          | return address                 | Caller frame
 *   -------+--------------------------------+----------------  ---
 *          | ALLOCA block n                 |                   ^
 *          :     ...                        :                   |
 *          | ALLOCA block 0                 |                   |
 *          +--------------------------------+                   |
 *          | monitor n                      |                   |
 *          :     ...                        :                   |
 *          | monitor 0                      | Current frame     |
 *          +--------------------------------+                   |
 *          | spill slot n                   |                 frame
 *          :     ...                        :                 size
 *          | spill slot 0                   |                   |
 *          +--------------------------------+                   |
 *          | outgoing overflow argument n   |                   |
 *          |     ...                        |                   |
 *    %sp   | outgoing overflow argument 0   |                   v
 *   -------+--------------------------------+----------------  ---
 *
 * </pre>
 * Note that size {@code ALLOCA block}s and {@code monitor}s in the frame may be greater
 * than the size of a {@linkplain CiTarget#spillSlotSize spill slot}.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public final class FrameMap {

    private static final int DOUBLE_SIZE = 8;

    private final C1XCompiler compiler;
    private final CallingConvention incomingArguments;

    /**
     * Number of monitors used in this frame.
     */
    private final int monitorCount;

    /**
     * The final frame size.
     * Value is only set after register allocation is complete.
     */
    private int frameSize;

    private int spillSlotCount;

    /**
     * The amount of memory allocated within the frame for uses of {@link Bytecodes#ALLOCA}.
     */
    private int stackBlocksSize;

    /**
     * Area occupied by outgoing overflow arguments.
     * This value is adjusted as calling conventions for outgoing calls are retrieved.
     */
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
            incomingArguments = new CallingConvention(new CiLocation[0], compiler.target);
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
        CiLocation[] locations = compiler.target.registerConfig.getRuntimeParameterLocations(signature, compiler.target);
        return createCallingConvention(locations, false);
    }

    /**
     * Gets the calling convention for calling Java methods with the specified signature.
     * @param signature the signature of the arguments and return value
     * @return a {@link CallingConvention} instance describing the location of parameters and the return value
     */
    public CallingConvention javaCallingConvention(CiKind[] signature, boolean outgoing, boolean reserveOutgoingArgumentsArea) {
        CiLocation[] locations = compiler.target.registerConfig.getJavaParameterLocations(signature, outgoing, compiler.target);
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
        this.frameSize = compiler.target.alignFrameSize(stackBlocksEnd());
    }

    /**
     * Informs the frame map that the compiled code uses a particular global stub, which
     * may need stack space for outgoing arguments.
     *
     * @param stub the global stub
     */
    public void usesGlobalStub(GlobalStub stub) {
        reserveOutgoing(stub.argsSize);
    }

    /**
     * Converts a {@link CiValue} into a {@code CiLocation} within this frame.
     * @param opr the operand
     * @return a location
     */
    public CiLocation toLocation(CiValue opr) {
        if (opr.isStackSlot()) {
            CiStackSlot stkOpr = (CiStackSlot) opr;
            int size = compiler.target.sizeInBytes(opr.kind);
            int offset = spOffsetForSlot(stkOpr.index, size);
            return new CiAddress(opr.kind, CiRegister.Frame.asLocation(), offset);
        } else if (opr.isRegister()) {
            return (CiRegisterLocation) opr;
        }
        throw new CiBailout("cannot convert " + opr + "to location");
    }

    /**
     * Converts a LIR stack operand into a stack address within this frame.
     *
     * @param opr the operand to convert
     * @param offset an offset within the spill slot allocated for {@code opr}
     * @return the address of the operand + offset
     */
    public CiAddress toStackAddress(CiValue opr, int offset) {
        assert opr.isStackSlot();
        int spillIndex = ((CiStackSlot) opr).index;
        assert spillIndex >= 0;
        int size = compiler.target.sizeInBytes(opr.kind);
        return new CiAddress(opr.kind, compiler.target.stackPointerRegister.asLocation(Word), spOffsetForSlot(spillIndex, size) + offset);
    }

    /**
     * Converts a spill index into a stack location.
     *
     * @param kind the type of the spill slot
     * @param spillIndex the index into the spill slots
     * @return a representation of the stack location
     */
    public CiLocation toStackLocation(CiKind kind, int spillIndex) {
        int size = compiler.target.sizeInBytes(kind);
        return new CiAddress(kind, CiRegister.Frame.asLocation(), spOffsetForSlot(spillIndex, size));
    }

    /**
     * Gets the stack address within this frame for a given reserved stack block.
     *
     * @param stackBlock the value returned from {@link #reserveStackBlock(int)} identifying the stack block
     * @return a representation of the stack location
     */
    public CiAddress toStackAddress(StackBlock stackBlock) {
        return new CiAddress(CiKind.Word, compiler.target.stackPointerRegister.asLocation(Word), spOffsetForStackBlock(stackBlock));
    }

    /**
     * Converts the monitor index into a stack location.
     * @param monitorIndex the monitor index
     * @return a representation of the stack location
     */
    public CiLocation toMonitorLocation(int monitorIndex) {
        return new CiAddress(CiKind.Object, CiRegister.Frame.asLocation(), spOffsetForMonitorObject(monitorIndex));
    }

    /**
     * Reserve space for stack-based outgoing arguments.
     *
     * @param argsSize the amount of space to reserve for stack-based outgoing arguments
     */
    private void reserveOutgoing(int argsSize) {
        if (argsSize > outgoingSize) {
            outgoingSize = Util.roundUp(argsSize, DOUBLE_SIZE);
        }
    }

    /**
     * Encapsulates the details of a stack block reserved by a call to {@link FrameMap#reserveStackBlock(int)}.
     */
    public static final class StackBlock {
        /**
         * The size of this stack block.
         */
        public final int size;

        /**
         * The offset of this stack block within the frame space reserved for stack blocks.
         */
        public final int offset;

        public StackBlock(int size, int offset) {
            this.size = size;
            this.offset = offset;
        }
    }

    /**
     * Reserves a block of memory in the frame of the method being compiled.
     *
     * @param size the number of bytes to reserve
     * @return a descriptor of the reserved block that can be used with {@link #toStackAddress(StackBlock)} once register
     *         allocation is complete and the size of the frame has been {@linkplain #finalizeFrame(int) finalized}.
     */
    public StackBlock reserveStackBlock(int size) {
        int wordSize = compiler.target.sizeInBytes(CiKind.Word);
        assert (size & ~(wordSize - 1)) == 0;
        StackBlock block = new StackBlock(size, stackBlocksSize);
        stackBlocksSize += size;
        return block;
    }

    private CallingConvention createCallingConvention(CiLocation[] locations, boolean reserveOutgoingArgumentsArea) {
        final CallingConvention result = new CallingConvention(locations, compiler.target);
        if (reserveOutgoingArgumentsArea) {
            assert frameSize == -1 : "frame size must not yet be fixed!";
            reserveOutgoing(result.overflowArgumentSize);
        }
        return result;
    }

    private int spOffsetForStackBlock(StackBlock stackBlock) {
        assert stackBlock.offset >= 0 && stackBlock.offset + stackBlock.size < stackBlocksSize : "invalid stack block";
        int offset = stackBlocksStart() + stackBlock.offset;
        assert offset <= (frameSize() - stackBlock.size) : "spill outside of frame";
        return offset;
    }

    private int spOffsetForSlot(int spillIndex, int size) {
        assert spillIndex >= 0 && spillIndex < spillSlotCount : "invalid spill slot";
        int offset = spillStart() + spillIndex * compiler.target.spillSlotSize;
        assert offset <= (frameSize() - size) : "spill outside of frame";
        return offset;
    }

    private int spOffsetForMonitorBase(int index) {
        assert index >= 0 && index < monitorCount : "invalid monitor index";
        int size = compiler.runtime.sizeofBasicObjectLock();
        int offset = monitorsStart() + index * size;
        assert offset <= (frameSize() - size) : "monitor outside of frame";
        return offset;
    }

    private int spillStart() {
        return outgoingSize;
    }

    private int spillEnd() {
        return spillStart() + spillSlotCount * compiler.target.spillSlotSize;
    }

    private int monitorsStart() {
        return spillEnd();
    }

    private int monitorsEnd() {
        return monitorsStart() + (monitorCount * compiler.runtime.sizeofBasicObjectLock());
    }

    private int stackBlocksStart() {
        return monitorsEnd();
    }

    private int stackBlocksEnd() {
        return stackBlocksStart() + stackBlocksSize;
    }

    private int spOffsetForMonitorObject(int index)  {
        return spOffsetForMonitorBase(index) + compiler.runtime.basicObjectLockOffsetInBytes();
    }

}
