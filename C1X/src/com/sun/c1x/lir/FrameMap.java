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

import static com.sun.cri.ci.CiKind.*;
import static java.lang.reflect.Modifier.*;

import com.sun.c1x.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.util.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

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
 *          +--------------------------------+    ---            |
 *          | spill slot n                   |     ^           frame
 *          :     ...                        :     |           size
 *          | spill slot 0                   |  shared           |
 *          +- - - - - - - - - - - - - - - - +   slot            |
 *          | outgoing overflow argument n   |  indexes          |
 *          |     ...                        |     |             |
 *    %sp   | outgoing overflow argument 0   |     v             v
 *   -------+--------------------------------+----------------  ---
 *
 * </pre>
 * Note that the size {@link Bytecodes#ALLOCA ALLOCA} blocks and {@code monitor}s in the frame may be greater
 * than the size of a {@linkplain CiTarget#spillSlotSize spill slot}.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public final class FrameMap {

    private final C1XCompiler compiler;
    private final CiCallingConvention incomingArguments;

    /**
     * Number of monitors used in this frame.
     */
    private final int monitorCount;

    /**
     * The final frame size.
     * Value is only set after register allocation is complete.
     */
    private int frameSize;

    /**
     * The number of spill slots allocated by the register allocator.
     * The value {@code -2} means that the size of outgoing argument stack slots
     * is not yet fixed. The value {@code -1} means that the register
     * allocator has started allocating spill slots and so the size of
     * outgoing stack slots cannot change as outgoing stack slots and
     * spill slots share the same slot index address space.
     */
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
        this.spillSlotCount = -2;

        assert monitors >= 0 : "not set";
        monitorCount = monitors;
        if (method == null) {
            incomingArguments = new CiCallingConvention(new CiValue[0], 0);
        } else {
            CiKind receiver = !isStatic(method.accessFlags()) ? method.holder().kind() : null;
            incomingArguments = javaCallingConvention(Util.signatureToKinds(method.signature(), receiver), false);
        }
    }

    /**
     * Gets the calling convention for calling runtime methods with the specified signature.
     * @param signature the signature of the arguments
     * @return a {@link CiCallingConvention} instance describing the location of parameters and the return value
     */
    public CiCallingConvention runtimeCallingConvention(CiKind[] signature) {
        CiCallingConvention cc = compiler.target.registerConfig.getRuntimeCallingConvention(signature, compiler.target);
        assert cc.stackSize == 0 : "runtime call should not have stack arguments";
        return cc;
    }

    /**
     * Gets the calling convention for calling Java methods with the specified signature.
     *
     * @param signature the signature of the arguments
     * @param outgoing if {@code true}, the reserved space on the stack for outgoing stack parameters is adjusted if necessary
     * @return a {@link CiCallingConvention} instance describing the location of parameters and the return value
     */
    public CiCallingConvention javaCallingConvention(CiKind[] signature, boolean outgoing) {
        CiCallingConvention cc = compiler.target.registerConfig.getJavaCallingConvention(signature, outgoing, compiler.target);
        if (outgoing) {
            assert frameSize == -1 : "frame size must not yet be fixed!";
            reserveOutgoing(cc.stackSize);
        }
        return cc;
    }

    /**
     * Gets the calling convention for calling native code with the specified signature.
     *
     * @param signature the signature of the arguments
     * @param outgoing if {@code true}, the reserved space on the stack for outgoing stack parameters is adjusted if necessary
     * @return a {@link CiCallingConvention} instance describing the location of parameters and the return value
     */
    public CiCallingConvention nativeCallingConvention(CiKind[] signature, boolean outgoing) {
        CiCallingConvention cc = compiler.target.registerConfig.getNativeCallingConvention(signature, outgoing, compiler.target);
        if (outgoing) {
            assert frameSize == -1 : "frame size must not yet be fixed!";
            reserveOutgoing(cc.stackSize);
        }
        return cc;
    }

    /**
     * Gets the calling convention for the incoming arguments to the compiled method.
     * @return the calling convention for incoming arguments
     */
    public CiCallingConvention incomingArguments() {
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
     * Converts a stack slot into a stack address.
     *
     * @param slot a stack slot
     * @return a stack address
     */
    public CiAddress toStackAddress(CiStackSlot slot) {
        int size = compiler.target.sizeInBytes(slot.kind);
        if (slot.inCallerFrame()) {
            int offset = slot.index() * compiler.target.spillSlotSize;
            return new CiAddress(slot.kind, CiRegister.CallerFrame.asValue(), offset);
        } else {
            int offset = spOffsetForOutgoingOrSpillSlot(slot.index(), size);
            return new CiAddress(slot.kind, CiRegister.Frame.asValue(), offset);
        }
    }

    /**
     * Gets the stack address within this frame for a given reserved stack block.
     *
     * @param stackBlock the value returned from {@link #reserveStackBlock(int)} identifying the stack block
     * @return a representation of the stack location
     */
    public CiAddress toStackAddress(StackBlock stackBlock) {
        return new CiAddress(CiKind.Word, compiler.target.stackPointerRegister.asValue(Word), spOffsetForStackBlock(stackBlock));
    }

    /**
     * Converts the monitor index into a stack address.
     * @param monitorIndex the monitor index
     * @return a representation of the stack address
     */
    public CiAddress toMonitorStackAddress(int monitorIndex) {
        return new CiAddress(CiKind.Object, CiRegister.Frame.asValue(), spOffsetForMonitorObject(monitorIndex));
    }

    /**
     * Reserve space for stack-based outgoing arguments.
     *
     * @param argsSize the amount of space to reserve for stack-based outgoing arguments
     */
    private void reserveOutgoing(int argsSize) {
        assert spillSlotCount == -2 : "cannot reserve outgoing stack slot space once register allocation has started";
        if (argsSize > outgoingSize) {
            outgoingSize = Util.roundUp(argsSize, compiler.target.spillSlotSize);
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
        assert (size % wordSize) == 0;
        StackBlock block = new StackBlock(size, stackBlocksSize);
        stackBlocksSize += size;
        return block;
    }

    private int spOffsetForStackBlock(StackBlock stackBlock) {
        assert stackBlock.offset >= 0 && stackBlock.offset + stackBlock.size <= stackBlocksSize : "invalid stack block";
        int offset = stackBlocksStart() + stackBlock.offset;
        assert offset <= (frameSize() - stackBlock.size) : "spill outside of frame";
        return offset;
    }

    /**
     * Gets the stack pointer offset for a outgoing stack argument or compiler spill slot.
     *
     * @param slotIndex the index of the stack slot within the slot index space reserved for
     * @param size
     * @return
     */
    private int spOffsetForOutgoingOrSpillSlot(int slotIndex, int size) {
        assert slotIndex >= 0 && slotIndex < (initialSpillSlot() + spillSlotCount) : "invalid spill slot";
        int offset = slotIndex * compiler.target.spillSlotSize;
        assert offset <= (frameSize() - size) : "slot outside of frame";
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

    /**
     * Gets the index of the first available spill slot relative to the base of the frame.
     * After this call, no further outgoing stack slots can be {@linkplain #reserveOutgoing(int) reserved}.
     *
     * @return the index of the first available spill slot
     */
    public int initialSpillSlot() {
        if (spillSlotCount == -2) {
            spillSlotCount = -1;
        }
        return outgoingSize / compiler.target.spillSlotSize;
    }

}
