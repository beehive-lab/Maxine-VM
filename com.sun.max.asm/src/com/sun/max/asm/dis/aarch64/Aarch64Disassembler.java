/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 */

package com.sun.max.asm.dis.aarch64;

import com.sun.max.asm.Argument;
import com.sun.max.asm.Assembler;
import com.sun.max.asm.InlineDataDecoder;
import com.sun.max.asm.arm.complete.ARMAssembler;
import com.sun.max.asm.dis.DisassembledInstruction;
import com.sun.max.asm.dis.risc.RiscDisassembler;
import com.sun.max.asm.gen.Immediate64Argument;
import com.sun.max.asm.gen.risc.RiscTemplate;
import com.sun.max.asm.gen.risc.arm.ARMAssembly;
import com.sun.max.lang.Endianness;

import java.util.List;

/**
 */
public class Aarch64Disassembler extends RiscDisassembler{

    public Aarch64Disassembler(long startAddress, InlineDataDecoder inlineDataDecoder) {
        super(new Immediate64Argument(startAddress), ARMAssembly.ASSEMBLY, Endianness.LITTLE, inlineDataDecoder);
    }

    @Override
    protected Assembler createAssembler(int position) {
        return new ARMAssembler((int) startAddress().asLong() + position);
    }

    @Override
    protected DisassembledInstruction createDisassembledInstruction(int position, byte[] bytes, RiscTemplate template, List<Argument> arguments) {
        return new DisassembledInstruction(this, position, bytes, template, arguments);
    }
}
