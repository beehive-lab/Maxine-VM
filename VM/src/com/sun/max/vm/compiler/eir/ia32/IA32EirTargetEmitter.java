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
/*VCSID=20b407b3-3cd7-47d3-871b-94d5aeb1136c*/
package com.sun.max.vm.compiler.eir.ia32;

import com.sun.max.asm.*;
import com.sun.max.asm.ia32.*;
import com.sun.max.asm.ia32.complete.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class IA32EirTargetEmitter extends EirTargetEmitter<IA32Assembler> {
    /**
     * Converts a logical stack slot offset to a {@linkplain StackAddress target address}.
     */
    public StackAddress stackAddress(EirStackSlot slot) {
        if (slot.purpose() == EirStackSlot.Purpose.PARAMETER) {
            // The offset is adjusted to account for the frame allocated for local variables as
            // well as the return address that is pushed to the stack by a call instruction.
            return new StackAddress(slot.offset() + frameSize() + abi().stackSlotSize(), _stackPointer.indirect());
        }
        // FIXME: A better way would be to have the first index to locals be set to the offsetToSpillArea, so that slot.offset()
        // is automatically correct. (this however would have to be done for all register allocator)
        return new StackAddress(slot.offset(),  _framePointer.indirect());
    }

    /**
     * Represents a stack slot as an indirect address composed of a {@linkplain #base() base register} and
     * an {@linkplain #isOffset8Bit() 8 or 32 bit} {@linkplain #offset32() offset}.
     */
    public static final class StackAddress {
        private final int _offset;
        private final IA32IndirectRegister32 _base;

        StackAddress(int offset, IA32IndirectRegister32 base) {
            _offset = offset;
            _base = base;
        }

        public int offset32() {
            assert !isOffset8Bit();
            return _offset;
        }

        public byte offset8() {
            assert isOffset8Bit();
            return (byte) _offset;
        }

        public int offset() {
            return _offset;
        }

        public IA32IndirectRegister32 base() {
            return _base;
        }

        public WordWidth offsetWidth() {
            return isOffset8Bit()  ? WordWidth.BITS_8 : WordWidth.BITS_32;
        }

        public boolean isOffset8Bit() {
            return WordWidth.signedEffective(_offset) == WordWidth.BITS_8;
        }

        public boolean isOffsetZero() {
            return _offset == 0;
        }
    }

    private IA32GeneralRegister32 _stackPointer;

    public IA32GeneralRegister32 stackPointer() {
        return _stackPointer;
    }

    private IA32GeneralRegister32 _framePointer;

    public IA32GeneralRegister32 framePointer() {
        return _framePointer;
    }

    public IA32EirTargetEmitter(IA32EirABI abi, int frameSize, Safepoint safepoint) {
        // FIXME: needs a adapter frame generator parameter.
        super(new IA32Assembler(), abi, frameSize, safepoint, WordWidth.BITS_32, null);
        _stackPointer = abi.targetABI().stackPointer();
        _framePointer = abi.targetABI().framePointer();
    }

    @Override
    protected void setStartAddress(Address address) {
        assembler().setStartAddress(address.toInt());
    }

    @Override
    protected void fixLabel(Label label, Address address) {
        assembler().fixLabel(label, address.toInt());
    }

    public IA32EirRegister loadIntoScratchRegister(Kind kind, EirLocation value) {
        final EirRegister destination = abi().getScratchRegister(kind);
        IA32EirAssignment.emit(this, kind, destination, value);
        return (IA32EirRegister) destination;
    }

    @Override
    protected boolean isCall(byte[] code, int offset) {
        return code[offset] == (byte) 0xE8;
    }
}
