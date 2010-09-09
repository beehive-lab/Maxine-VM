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
package com.sun.max.vm.cps.eir.sparc;

import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.EirStackSlot.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.sparc.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class SPARCEirTargetEmitter extends EirTargetEmitter<SPARCAssembler> {
    private Label literalBaseLabel;

    public Label literalBaseLabel() {
        return literalBaseLabel;
    }

    private GPR stackPointer;

    public GPR stackPointer() {
        return stackPointer;
    }

    private GPR framePointer;

    public GPR framePointer() {
        return framePointer;
    }

    /**
     * Returns the stack address corresponding to a EirStackSlot.
     * @see EirStackSlot
     * @see StackAddress
     * @param slot the Eir stack slot
     * @return a StackAddress instance
     */
    public StackAddress stackAddress(EirStackSlot slot) {
        if (slot.purpose == Purpose.PARAMETER) {
            // Parameters are on the caller's stack, at positive offset from the callee's FP.
            final int offset = SPARCStackFrameLayout.STACK_BIAS + SPARCStackFrameLayout.SAVE_AREA_SIZE + SPARCStackFrameLayout.ARGUMENT_SLOTS_SIZE + slot.offset;
            return new StackAddress(offset, framePointer());
        }

        if (slot.purpose == Purpose.BLOCK) {
            return new StackAddress(SPARCStackFrameLayout.STACK_BIAS - slot.offset, framePointer());
        }

        // Locals are addressed from the frame pointer too. But they are located below FP. Due to the bias, their offset may be positive as well.
        // This makes their offset independent of SP.
        // Note that slot.offset is an offset relative to the stack pointer. It needs to be converted into a frame pointer-relative offset.

        final int offset = SPARCStackFrameLayout.localSlotOffsetFromFrame(frameSize(), slot.offset);
        return new StackAddress(offset, framePointer());
    }

    public static final class StackAddress {
        public final int offset;
        public final GPR base;

        StackAddress(int offset, GPR base) {
            this.offset = offset;
            this.base = base;
        }
    }

    private final boolean is32Bit;

    public SPARCEirTargetEmitter(SPARCEirABI abi, int frameSize, Safepoint safepoint, AdapterGenerator adapterGenerator) {
        super(SPARCAssembler.createAssembler(Word.widthValue()), abi, frameSize, safepoint, abi.vmConfiguration().platform.wordWidth(), adapterGenerator);
        is32Bit = abi.vmConfiguration().platform.wordWidth() == WordWidth.BITS_32;
        stackPointer = abi.targetABI().stackPointer();
        framePointer = abi.targetABI().framePointer();
        literalBaseLabel = new Label();
    }

    @Override
    protected void setStartAddress(Address address) {
        if (is32Bit) {
            final SPARC32Assembler assembler32 = (SPARC32Assembler) assembler();
            assembler32.setStartAddress(address.toInt());
        } else {
            final SPARC64Assembler assembler64 = (SPARC64Assembler) assembler();
            assembler64.setStartAddress(address.toInt());
        }
    }

    @Override
    protected void fixLabel(Label label, Address address) {
        if (is32Bit) {
            final SPARC32Assembler assembler32 = (SPARC32Assembler) assembler();
            assembler32.fixLabel(label, address.toInt());
        } else {
            final SPARC64Assembler assembler64 = (SPARC64Assembler) assembler();
            assembler64.fixLabel(label, address.toLong());
        }
    }

    @Override
    protected boolean isCall(byte[] code, int offset) {
        return (code[offset] >> 6) == 1;
    }

}
