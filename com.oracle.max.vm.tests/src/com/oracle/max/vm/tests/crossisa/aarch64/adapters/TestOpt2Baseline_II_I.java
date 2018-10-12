/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
package com.oracle.max.vm.tests.crossisa.aarch64.adapters;

import com.oracle.max.asm.target.aarch64.*;
import com.sun.cri.bytecode.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.aarch64.*;

import static com.oracle.max.asm.target.aarch64.Aarch64.linkRegister;
import static com.sun.max.vm.compiler.target.aarch64.Aarch64AdapterGenerator.Baseline2Opt.PROLOGUE_SIZE;

/**
 * This class tests a call from optimised to baseline for a method
 * that takes 2 int parameters and returns an int.
 */
public class TestOpt2Baseline_II_I extends Opt2BaselineAarch64AdapterTest {

    public TestOpt2Baseline_II_I() throws Exception {
        super("(II)I");
    }


    @Override
    public byte [] createPrelude()  {
        masm.codeBuffer.reset();
        masm.push(linkRegister);
        masm.mov(Aarch64.r0, 1);        // r0 := 1
        masm.mov(Aarch64.r1, 2);        // r1 := 2
        // Branch to optimised entry point +16 for the bl, pops and ret below.
        masm.bl(PROLOGUE_SIZE + 3 * 4);
        masm.pop(linkRegister);
        masm.ret(linkRegister);
        byte [] code = masm.codeBuffer.close(true);
        return code;
    }

    @Override
    public byte [] createMethod(Adapter adapter)  {
        Aarch64MacroAssembler masm = t1xCompiler.getMacroAssembler();
        byte [] instructions = {Bytecodes.ILOAD_0, Bytecodes.ILOAD_1};
        // compile the 2 iloads
        t1xCompiler.offlineT1XCompileNoEpilogue(method(), codeAttribute(), instructions);
        // add them
        t1xCompiler.do_iaddTests();
        // place the result in the return register
        masm.pop(32, Aarch64.r0);
        t1xCompiler.decStack(1);
        t1xCompiler.emitEpilogueTests();
        byte [] code = masm.codeBuffer.close(true);
        int displacement = code.length - adapter.callOffsetInPrologue();
        Aarch64TargetMethodUtil.fixupCall28Site(code, adapter.callOffsetInPrologue(), displacement);
        return code;
    }

    /**
     * Test a method taking 2 ints returning an int.
     * @throws Exception
     */
    public void test_ii_i() throws Exception {
        testValues[0] = true;
        long [] values = generateAndTest(expectedValues, testValues, bitmasks);
        assert 3 == values[0] : "Expected 3, got " + values[0];
    }

}

