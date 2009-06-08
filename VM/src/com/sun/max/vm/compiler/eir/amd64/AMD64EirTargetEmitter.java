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
package com.sun.max.vm.compiler.eir.amd64;

import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64EirTargetEmitter extends EirTargetEmitter<AMD64Assembler> {
    /**
     * Converts a logical stack slot offset to a {@linkplain StackAddress target address}.
     */
    public StackAddress stackAddress(EirStackSlot slot) {
        if (slot.purpose() == EirStackSlot.Purpose.PARAMETER) {
            // The offset is adjusted to account for the frame allocated for local variables as
            // well as the return address that is pushed to the stack by a call instruction.
            return new StackAddress(slot.offset() + frameSize() + abi().stackSlotSize(), _stackPointer.indirect());
        }
        return new StackAddress(slot.offset(),  _framePointer.indirect());
    }

    /**
     * Represents a stack slot as an indirect address composed of a {@linkplain #base() base register} and
     * an {@linkplain #isOffset8Bit() 8 or 32 bit} {@linkplain #offset32() offset}.
     */
    public static final class StackAddress {
        private final int _offset;
        private final AMD64IndirectRegister64 _base;

        StackAddress(int offset, AMD64IndirectRegister64 base) {
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

        public AMD64IndirectRegister64 base() {
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

    private AMD64GeneralRegister64 _stackPointer;

    public AMD64GeneralRegister64 stackPointer() {
        return _stackPointer;
    }

    public AMD64GeneralRegister64 scratchRegister() {
        return ((AMD64EirRegister.General) abi().integerRegisterActingAs(Role.ABI_SCRATCH)).as64();
    }

    private AMD64GeneralRegister64 _framePointer;

    public AMD64GeneralRegister64 framePointer() {
        return _framePointer;
    }

    public AMD64EirTargetEmitter(AMD64EirABI abi, int frameSize, Safepoint safepoint, AdapterFrameGenerator<AMD64Assembler> adapterFrameGenerator) {
        super(new AMD64Assembler(), abi, frameSize, safepoint, WordWidth.BITS_64, adapterFrameGenerator);
        _stackPointer = abi.targetABI().stackPointer();
        _framePointer = abi.targetABI().framePointer();
    }

    @Override
    protected void setStartAddress(Address address) {
        assembler().setStartAddress(address.toLong());
    }

    @Override
    protected void fixLabel(Label label, Address address) {
        assembler().fixLabel(label, address.toLong());
    }

    public AMD64EirRegister loadIntoScratchRegister(Kind kind, EirLocation value) {
        final EirRegister destination = abi().getScratchRegister(kind);
        AMD64EirAssignment.emit(this, kind, destination, value);
        return (AMD64EirRegister) destination;
    }

    @Override
    protected boolean isCall(byte[] code, int offset) {
        return code[offset] == (byte) 0xE8;
    }

}
