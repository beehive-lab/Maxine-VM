/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.c1x.target.armv7;

import static com.sun.cri.ci.CiCallingConvention.Type.*;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.armv7.*;
import com.sun.c1x.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.target.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiRegister.*;
import com.sun.cri.xir.CiXirAssembler.*;
import com.sun.cri.xir.*;

/**
 * An object used to produce a single compiler stub.
 */
public class ARMV7CompilerStubEmitter extends CompilerStubEmitter {

    private final ARMV7MacroAssembler asm;

    public ARMV7CompilerStubEmitter(C1XCompilation compilation, CiKind[] argTypes, CiKind resultKind) {
        super(compilation, new ARMV7MacroAssembler(compilation.target, compilation.compiler.compilerStubRegisterConfig),
                argTypes, resultKind, 0x8000000080000000L, 0x8000000000000000L,
                ARMV7.s0, ARMV7.s0, ARMV7.s0, ARMV7.s30);
        this.asm = (ARMV7MacroAssembler) super.getAssembler();
    }

    @Override
    protected void emit0(CompilerStub.Id stub) {
        switch (stub) {
            case f2i:
                emitF2I();
                break;
            case f2l:
                emitF2L();
                break;
            case d2i:
                emitD2I();
                break;
            case d2l:
                emitD2L();
                break;
        }
    }

    @Override
    public CompilerStub emit(XirTemplate template) {
        ArrayList<CiRegister> allocatableRegisters = new ArrayList<>(Arrays.asList(comp.registerConfig.getCategorizedAllocatableRegisters().get(RegisterFlag.CPU)));
        reserveRegistersForTemplate(template, allocatableRegisters);

        prologue(comp.registerConfig.getCalleeSaveLayout());

        CiValue[] operands = new CiValue[template.variableCount];

        XirOperand resultOperand = template.resultOperand;

        if (template.allocateResultOperand) {
            CiValue outputOperand = CiValue.IllegalValue;
            // This snippet has a result that must be separately allocated
            // Otherwise it is assumed that the result is part of the inputs
            if (resultOperand.kind != CiKind.Void && resultOperand.kind != CiKind.Illegal) {
                outputOperand = outResult;
                assert operands[resultOperand.index] == null;
            }
            operands[resultOperand.index] = outputOperand;
        }

        ARMV7LIRAssembler lasm = new ARMV7LIRAssembler(comp, tasm);
        prepareOperands(template, allocatableRegisters, operands, lasm);

        for (XirConstant c : template.constants) {
            assert operands[c.index] == null;
            operands[c.index] = c.value();
        }

        for (XirTemp t : template.temps) {
            CiValue op = allocateOperand(t, allocatableRegisters);
            assert operands[t.index] == null;
            operands[t.index] = op;
        }

        for (CiValue operand : operands) {
            assert operand != null;
        }

        Label[] labels = new Label[template.labels.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
        }

        assert template.marks.length == 0 : "marks not supported in compiler stubs";
        lasm.emitXirInstructions(null, template.fastPath, labels, operands, null);
        epilogue();
        String stubName = "c1x-" + template.name;
        CiTargetMethod targetMethod = tasm.finishTargetMethod(stubName, comp.runtime, registerRestoreEpilogueOffset, true);
        Object stubObject = comp.runtime.registerCompilerStub(targetMethod, stubName);
        return new CompilerStub(null, template.resultOperand.kind, stubObject, inArgs, outResult);
    }

    @Override
    protected void convertPrologue() {
        prologue(new CiCalleeSaveLayout(0, -1, comp.target.wordSize, convertArgument, convertResult));
        asm.setUpScratch(comp.frameMap().toStackAddress(inArgs[0]));
        asm.vldr(ARMV7Assembler.ConditionFlag.Always, convertArgument, ARMV7.r12, 0, CiKind.Int, CiKind.Int);
    }

    @Override
    protected void convertEpilogue() {
        asm.setUpScratch(comp.frameMap().toStackAddress(outResult));
        asm.vstr(ARMV7Assembler.ConditionFlag.Always, convertResult, ARMV7.r12, 0, CiKind.Int, CiKind.Int);
        epilogue();
    }

    @Override
    protected void emitCOMISSD(boolean isDouble, boolean isInt) {
        convertPrologue();
        Label nan = new Label();
        Label ret = new Label();

        if (isInt) {
            // input is > 0 -> return maxInt
            // result register already contains 0x80000000, so subtracting 1 gives 0x7fffffff
            asm.decrementl(convertResult, 1);
        } else {
            // input is > 0 -> return maxLong
            // result register already contains 0x8000000000000000, so subtracting 1 gives 0x7fffffffffffffff
            asm.decrementq(convertResult, 1);
        }

        // input is NaN -> return 0
        asm.bind(nan);
        asm.xorptr(convertResult, convertResult);
        asm.bind(ret);
        convertEpilogue();
    }

    @Override
    protected void prologue(CiCalleeSaveLayout csl) {
        assert this.csl == null;
        assert csl != null : "stub should define a callee save area";
        this.csl = csl;
        int entryCodeOffset = comp.runtime.codeOffset();
        if (entryCodeOffset != 0) {
            // pad to normal code entry point
            asm.nop(entryCodeOffset);
        }
        asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 14, true);
        final int frameSize = frameSize();
        asm.subq(ARMV7.r13, frameSize);
        tasm.setFrameSize(frameSize);
        comp.frameMap().setFrameSize(frameSize);
        asm.save(csl, csl.frameOffsetToCSA);
    }

    @Override
    protected void epilogue() {
        assert registerRestoreEpilogueOffset == -1;
        registerRestoreEpilogueOffset = asm.codeBuffer.position();

        // Restore registers
        int frameToCSA = csl.frameOffsetToCSA;
        asm.restore(csl, frameToCSA);

        // Restore rsp
        asm.addq(ARMV7.rsp, frameSize());
        asm.ret(0);
    }

    @Override
    protected void forwardRuntimeCall(CiRuntimeCall call) {
        // Load arguments
        CiCallingConvention cc = comp.registerConfig.getCallingConvention(RuntimeCall, call.arguments, comp.target, false);
        for (int i = 0; i < cc.locations.length; ++i) {
            CiValue location = cc.locations[i];
            asm.setUpScratch(comp.frameMap().toStackAddress(inArgs[i]));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, location.asRegister(), ARMV7.r12, 0);
        }

        if (C1XOptions.AlignDirectCallsForPatching) {
            asm.alignForPatchableDirectCall();
        }
        // Call to the runtime
        int before = asm.codeBuffer.position();
        asm.call();
        int after = asm.codeBuffer.position();
        if (C1XOptions.EmitNopAfterCall) {
            asm.nop();
        }
        tasm.recordDirectCall(before, after - before, comp.runtime.asCallTarget(call), null);

        if (call.resultKind != CiKind.Void) {
            CiRegister returnRegister = comp.registerConfig.getReturnRegister(call.resultKind);
            asm.setUpScratch(comp.frameMap().toStackAddress(outResult));
            if (returnRegister.number <= 15) {
                asm.str(ARMV7Assembler.ConditionFlag.Always, returnRegister, ARMV7.r12, 0);
            } else {
                asm.vstr(ARMV7Assembler.ConditionFlag.Always, returnRegister, ARMV7.r12, 0, CiKind.Float, CiKind.Int);
            }
        }
    }
}
