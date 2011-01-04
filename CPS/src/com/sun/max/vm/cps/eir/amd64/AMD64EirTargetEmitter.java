/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.eir.amd64;

import static com.sun.max.vm.cps.eir.EirStackSlot.Purpose.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.eir.*;
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
        if (slot.purpose == PARAMETER) {
            // The offset is adjusted to account for the frame allocated for local variables as
            // well as the return address that is pushed to the stack by a call instruction.
            return new StackAddress(slot.offset + frameSize() + abi().stackSlotSize(), stackPointer.indirect());
        }
        if (slot.purpose == BLOCK) {
            return new StackAddress(currentEirBlock().method().frameSize() - slot.offset, framePointer.indirect());
        }
        return new StackAddress(slot.offset, framePointer.indirect());
    }

    /**
     * Represents a stack slot as an indirect address composed of a {@linkplain #base() base register} and
     * an {@linkplain #isOffset8Bit() 8 or 32 bit} {@linkplain #offset32() offset}.
     */
    public static final class StackAddress {
        public final int offset;
        public final AMD64IndirectRegister64 base;

        StackAddress(int offset, AMD64IndirectRegister64 base) {
            this.offset = offset;
            this.base = base;
        }

        public int offset32() {
            assert !isOffset8Bit();
            return offset;
        }

        public byte offset8() {
            assert isOffset8Bit();
            return (byte) offset;
        }

        public int offset() {
            return offset;
        }

        public AMD64IndirectRegister64 base() {
            return base;
        }

        public WordWidth offsetWidth() {
            return isOffset8Bit() ? WordWidth.BITS_8 : WordWidth.BITS_32;
        }

        public boolean isOffset8Bit() {
            return WordWidth.signedEffective(offset) == WordWidth.BITS_8;
        }

        public boolean isOffsetZero() {
            return offset == 0;
        }
    }

    private AMD64GeneralRegister64 stackPointer;

    public AMD64GeneralRegister64 stackPointer() {
        return stackPointer;
    }

    public AMD64GeneralRegister64 scratchRegister() {
        return ((AMD64EirRegister.General) abi().integerRegisterActingAs(Role.ABI_SCRATCH)).as64();
    }

    private AMD64GeneralRegister64 framePointer;

    public AMD64GeneralRegister64 framePointer() {
        return framePointer;
    }

    public AMD64EirTargetEmitter(AMD64EirABI abi, int frameSize, Safepoint safepoint, AdapterGenerator adapterGenerator) {
        super(new AMD64Assembler(), abi, frameSize, safepoint, WordWidth.BITS_64, adapterGenerator);
        stackPointer = abi.targetABI().stackPointer();
        framePointer = abi.targetABI().framePointer();
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

    /**
     * Indicates whether this emitter is for a template.
     * @return
     */
    @HOSTED_ONLY
    boolean templatesOnly() {
        return abi().templatesOnly();
    }
}
