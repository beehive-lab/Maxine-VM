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

import org.junit.*;

import com.oracle.max.asm.target.aarch64.*;
import com.sun.cri.bytecode.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.aarch64.*;

import static com.oracle.max.asm.target.aarch64.Aarch64.linkRegister;
import static com.sun.max.vm.compiler.target.aarch64.Aarch64AdapterGenerator.Baseline2Opt.PROLOGUE_SIZE;

public class TestOpt2Baseline_DD_D extends Opt2BaselineAarch64AdapterTest {

    public TestOpt2Baseline_DD_D() throws Exception {
        super("(DD)D");
    }

    @Override
    public byte[] createPrelude() {
        masm.codeBuffer.reset();
        masm.push(linkRegister);
        masm.fmov(64, Aarch64.d0, 2.5d);
        masm.fmov(64, Aarch64.d1, 3.0d);
        // Branch to optimised entry point +16 for the bl, pops and ret below.
        masm.bl(PROLOGUE_SIZE + 3 * 4);
        masm.pop(linkRegister);
        masm.ret(linkRegister);
        return masm.codeBuffer.close(true);
    }

    @Override
    public byte[] createMethod(Adapter adapter) {
        Aarch64MacroAssembler masm = t1xCompiler.getMacroAssembler();
        byte [] instructions = {Bytecodes.DLOAD_0, Bytecodes.DLOAD_2};
        // compile the 2 dloads
        t1xCompiler.offlineT1XCompileNoEpilogue(method(), codeAttribute(), instructions);
        // add them
        t1xCompiler.do_daddTests();
        // place the result in the return register
        t1xCompiler.peekDouble(Aarch64.d0, 0);
        t1xCompiler.decStack(2);
        // The test framework expects the return value to be in GP reg0.
        masm.fmovFpu2Cpu(64, Aarch64.r0, Aarch64.d0);
        t1xCompiler.emitEpilogueTests();
        byte [] code = masm.codeBuffer.close(true);
        int displacement = code.length - adapter.callOffsetInPrologue();
        Aarch64TargetMethodUtil.fixupCall28Site(code, adapter.callOffsetInPrologue(), displacement);
        return code;
    }

    @Test
    public void test_dd_d() throws Exception {
        testValues[0] = true;
        long [] values = generateAndTest(expectedValues, testValues, bitmasks);
        double res = Double.longBitsToDouble(values[0]);
        assert 5.5d == res : "Expected " + 5.5d + ", got " + res;
    }
}
