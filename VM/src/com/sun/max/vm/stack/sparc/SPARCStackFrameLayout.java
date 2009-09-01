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
package com.sun.max.vm.stack.sparc;

import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.*;

/**
  * Utility class that provides functionality common to all SPARC stack frame layout.
 *
 * @author Laurent Daynes
 * @author Paul Caprioli
 */
public final class SPARCStackFrameLayout {

    /**
     * Stack frame alignment requirement. 16 bytes on Solaris SPARC 64-bit.
     */
    public static final int STACK_FRAME_ALIGNMENT = 16;

    /**
     * Stack bias defined by the Solaris SPARC 64-bits system V ABI.
     * The BIAS must be added to the %sp register to obtain the actual top of the stack frame.
     * The BIAS exists to distinguish 64-bit code from 32-bit code.
     * See /usr/include/sys/stack.h on a Solaris/SPARC system.
     */
    public static final int STACK_BIAS = 2047;

    /**
     * Every stack frame must have a 16-extended word save area for the in and local registers (in
     * case of window overflow or window flushing).
     * This save area always must exist at %sp plus a BIAS of 2047 (0x7ff).
     * @see StackBias
     */
    public static final int SAVE_AREA_SIZE = 16 * Word.size();

    /**
     * Local registers are spilled to the first half of the save area.
     */
    public static final int LOCAL_REGISTERS_SAVE_AREA_SIZE = 8 * Word.size();

    public static final int ARGUMENT_SLOTS_SIZE = 6 * Word.size();

    public static final int MIN_STACK_FRAME_SIZE = SAVE_AREA_SIZE + ARGUMENT_SLOTS_SIZE;

    /**
     * The offset relative to the stack pointer register of the first slot (i.e., the first slot following the frame's save and argument slot area).
     */
    public static final int OFFSET_FROM_SP_TO_FIRST_SLOT = STACK_BIAS + MIN_STACK_FRAME_SIZE;


    public static Pointer unbias(Pointer biasedPointer) {
        return biasedPointer.plus(STACK_BIAS);
    }

    public static Pointer bias(Pointer unbiasedPointer) {
        return unbiasedPointer.minus(STACK_BIAS);
    }

    /**
     * Computes the offset of a stack slot relative to the frame pointer register.
     * Slots are numbered from 0 to n, where slot 0 is at the top of the stack.
     *
     * @param frameSize size of the stack frame where the slot resides.
     * @param slotOffset offset of the stack slot.
     * @return
     */
    public static int slotOffsetFromFrame(int frameSize, int slotOffset) {
        return STACK_BIAS - frameSize + slotOffset;
    }

    /**
     * Computes the offset of a local stack slot relative to the frame pointer register (%fp).
     * The offset of the local slot is computed by the EIR from the top of the stack.
     * The Eir is oblivious to the details of the stack frame layout (stack bias, register window saving area, etc...).
     * These needs to be accounted for when computing a offset from the frame pointer register.
     * @see  offsetToFirstFreeSlotFromStackPointer
     *
     * @param frameSize size of the stack frame where the slot resides.
     * @param slotOffset offset of the stack slot.
     * @return an offset in bytes relative to the frame pointer register
     */
    public static int localSlotOffsetFromFrame(int frameSize,  int slotOffset) {
        return OFFSET_FROM_SP_TO_FIRST_SLOT - frameSize + slotOffset;
    }

    /**
     * Returns the offset in bytes to the location in the saved area of the stack frame where the specified general register is saved.
     * Only IN and LOCAL registers are saved in the window.
     * The offset is positive relative to the top of the stack frame associated with the window.
     * The offset can be used relative to unbiased SP (for the current frame) or to unbiased FP (for the caller's frame).
     *
     * @param register
     * @return a positive offset relative to the top of the stack frame.
     */
    public static int offset_in_saved_window(GPR register) {
        assert register.isLocal() || register.isIn();
        return (register.value() - GPR.L0.value()) * Word.size();
    }

    /**
     * Returns the instruction pointer of the caller of the current stack frame referenced by the stackFrameWalker.
     * @param stackFrameWalker
     * @return an instruction pointer.
     */
    public static Pointer getCallerPC(StackFrameWalker stackFrameWalker) {
        return getRegisterInSavedWindow(stackFrameWalker, GPR.I7).asPointer();
    }

    public static Word getRegisterInSavedWindow(StackFrameWalker stackFrameWalker, GPR register) {
        final Pointer unbiasedFramePointer = unbias(stackFrameWalker.stackPointer());
        return getRegisterInSavedWindow(stackFrameWalker, unbiasedFramePointer, register);
    }

    public static Word getRegisterInSavedWindow(StackFrameWalker stackFrameWalker, Pointer savedRegisterWindow, GPR register) {
        return stackFrameWalker.readWord(savedRegisterWindow, offset_in_saved_window(register));
    }

    public static Pointer getReturnAddress(StackFrameWalker stackFrameWalker) {
        return getCallerPC(stackFrameWalker).plus(InstructionSet.SPARC.offsetToReturnPC);
    }

    public static Pointer getCallerFramePointer(StackFrameWalker stackFrameWalker) {
        return getCallerFramePointer(stackFrameWalker, unbias(stackFrameWalker.framePointer()));
    }

    public static Pointer getCallerFramePointer(StackFrameWalker stackFrameWalker, Pointer registerWindow) {
        return stackFrameWalker.readWord(registerWindow, offset_in_saved_window(GPR.I6)).asPointer();
    }

    public static void setRegisterInSavedWindow(Pointer framePointer, GPR register, Word data) {
        unbias(framePointer).writeWord(offset_in_saved_window(register), data);
    }

    public static Word getRegisterInSavedWindow(Pointer framePointer, GPR register) {
        return unbias(framePointer).readWord(offset_in_saved_window(register));
    }

    public static void setCallerFramePointer(Pointer framePointer, Pointer callerFramePointer) {
        unbias(framePointer).writeWord(offset_in_saved_window(GPR.I6), callerFramePointer);
    }

    private SPARCStackFrameLayout() {
    }
}
