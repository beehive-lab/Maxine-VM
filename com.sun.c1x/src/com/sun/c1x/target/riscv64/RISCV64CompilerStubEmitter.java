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
package com.sun.c1x.target.riscv64;

import com.oracle.max.asm.Label;
import com.oracle.max.asm.target.riscv64.RISCV64;
import com.oracle.max.asm.target.riscv64.RISCV64Address;
import com.oracle.max.asm.target.riscv64.RISCV64MacroAssembler;
import com.sun.c1x.C1XCompilation;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.stub.CompilerStub;
import com.sun.c1x.target.CompilerStubEmitter;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiRegister.RegisterFlag;
import com.sun.cri.xir.CiXirAssembler.XirConstant;
import com.sun.cri.xir.CiXirAssembler.XirOperand;
import com.sun.cri.xir.CiXirAssembler.XirTemp;
import com.sun.cri.xir.XirTemplate;

import java.util.ArrayList;
import java.util.Arrays;

import static com.sun.cri.ci.CiCallingConvention.Type.RuntimeCall;

/**
 * An object used to produce a single compiler stub.
 */
public class RISCV64CompilerStubEmitter extends CompilerStubEmitter {

    private final RISCV64MacroAssembler asm;

    public RISCV64CompilerStubEmitter(C1XCompilation compilation, CiKind[] argTypes, CiKind resultKind) {
        super(compilation, new RISCV64MacroAssembler(compilation.target, compilation.compiler.compilerStubRegisterConfig),
                argTypes, resultKind, 0x8000000080000000L, 0x8000000000000000L,
                RISCV64.fa0, RISCV64.x28, RISCV64.fa0, RISCV64.f31);
        this.asm = (RISCV64MacroAssembler) super.getAssembler();
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

        RISCV64LIRAssembler lasm = new RISCV64LIRAssembler(comp, tasm);
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
        // Unused in RISCV64
    }

    @Override
    protected void convertEpilogue() {
        // Unused in RISCV64
    }

    @Override
    protected void emitCOMISSD(boolean isDouble, boolean isInt) {
        prologue(new CiCalleeSaveLayout(0, -1, comp.target.wordSize, convertArgument, convertResult));
        asm.load(convertArgument, comp.frameMap().toStackAddress(inArgs[0]), isDouble ? CiKind.Double : CiKind.Float);
        if (isInt) {
            asm.fcvtws(convertResult, convertArgument);
        } else {
            asm.fcvtls(convertResult, convertArgument);
        }
        asm.store(convertArgument, comp.frameMap().toStackAddress(inArgs[0]), isInt ? CiKind.Int : CiKind.Long);
        epilogue();
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
        asm.push(64, RISCV64.ra);
        final int frameSize = frameSize();
        asm.sub(64, RISCV64.sp, RISCV64.sp, frameSize);
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
        asm.add(64, RISCV64.sp, RISCV64.sp, frameSize());
        asm.ret();
    }

    @Override
    protected void forwardRuntimeCall(CiRuntimeCall call) {
        // Load arguments
        CiRegister scratchRegister = comp.registerConfig.getScratchRegister();
        CiCallingConvention cc = comp.registerConfig.getCallingConvention(RuntimeCall, call.arguments, comp.target, false);
        for (int i = 0; i < cc.locations.length; ++i) {
            CiValue location = cc.locations[i];
            asm.setUpScratch(comp.frameMap().toStackAddress(inArgs[i]));
            asm.load(location.asRegister(), RISCV64Address.createBaseRegisterOnlyAddress(scratchRegister), inArgs[i].kind);
        }

        if (C1XOptions.AlignDirectCallsForPatching) {
            asm.alignForPatchableDirectCall(0);
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
            asm.store(returnRegister, RISCV64Address.createBaseRegisterOnlyAddress(scratchRegister), call.resultKind);
        }
    }
}
