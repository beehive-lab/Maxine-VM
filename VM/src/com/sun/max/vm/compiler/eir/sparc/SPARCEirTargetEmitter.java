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
package com.sun.max.vm.compiler.eir.sparc;

import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.sparc.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class SPARCEirTargetEmitter extends EirTargetEmitter<SPARCAssembler> {
    private Label _literalBaseLabel;

    public Label literalBaseLabel() {
        return _literalBaseLabel;
    }

    private GPR _stackPointer;

    public GPR stackPointer() {
        return _stackPointer;
    }

    private GPR _framePointer;

    public GPR framePointer() {
        return _framePointer;
    }

    /**
     * Returns the stack address corresponding to a EirStackSlot.
     * @see EirStackSlot
     * @see StackAddress
     * @param slot the Eir stack slot
     * @return a StackAddress instance
     */
    public StackAddress stackAddress(EirStackSlot slot) {
        if (slot.purpose() == EirStackSlot.Purpose.PARAMETER) {
            // Parameters are on the caller's stack, at positive offset from the callee's FP.
            final int offset = SPARCStackFrameLayout.STACK_BIAS + SPARCStackFrameLayout.SAVED_AREA + SPARCStackFrameLayout.ARGUMENT_SLOTS + slot.offset();
            return new StackAddress(offset, framePointer());
        }
        // Locals are addressed from the frame pointer too. But they are located below FP. Due to the bias, their offset may be positive as well.
        // This makes their offset independent of SP.
        // Note that slot.offset is an offset relative to the stack pointer. It needs to be converted into a frame pointer-relative offset.
        final int offset =  SPARCStackFrameLayout.localSlotOffsetFromFrame(frameSize(), slot.offset());
        return new StackAddress(offset, framePointer());
    }

    public static final class StackAddress {
        private final int _offset;
        private final GPR _base;

        StackAddress(int offset, GPR base) {
            _offset = offset;
            _base = base;
        }
        public int offset() {
            return _offset;
        }
        public GPR base() {
            return _base;
        }
    }

    private final boolean _is32Bit;

    public SPARCEirTargetEmitter(SPARCEirABI abi, int frameSize, Safepoint safepoint, AdapterFrameGenerator<SPARCAssembler> adapterFrameGenerator) {
        super(abi.createAssembler(), abi, frameSize, safepoint, abi.vmConfiguration().platform().processorKind().dataModel().wordWidth(), adapterFrameGenerator);
        _is32Bit = abi.vmConfiguration().platform().processorKind().dataModel().wordWidth() == WordWidth.BITS_32;
        _stackPointer = abi.targetABI().stackPointer();
        _framePointer = abi.targetABI().framePointer();
        _literalBaseLabel = new Label();
    }

    @Override
    protected void setStartAddress(Address address) {
        if (_is32Bit) {
            final SPARC32Assembler assembler32 = (SPARC32Assembler) assembler();
            assembler32.setStartAddress(address.toInt());
            //assembler32.fixLabel(_literalBaseLabel, address.toInt());
        } else {
            final SPARC64Assembler assembler64 = (SPARC64Assembler) assembler();
            assembler64.setStartAddress(address.toInt());
            //assembler64.fixLabel(_literalBaseLabel, address.toLong());
        }
    }

    @Override
    protected void fixLabel(Label label, Address address) {
        if (_is32Bit) {
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
