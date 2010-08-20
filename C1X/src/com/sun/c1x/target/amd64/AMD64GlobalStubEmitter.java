/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.target.amd64;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.target.amd64.AMD64Assembler.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;
import com.sun.cri.xir.CiXirAssembler.*;

public class AMD64GlobalStubEmitter implements GlobalStubEmitter {

    public static final int ARGUMENT_SIZE = 8;

    private static final long FloatSignFlip = 0x8000000080000000L;
    private static final long DoubleSignFlip = 0x8000000000000000L;
    private static final CiRegister convertArgument = AMD64.xmm0;
    private static final CiRegister convertResult = AMD64.rax;
    private static final CiRegister negateArgument = AMD64.xmm0;
    private static final CiRegister negateTemp = AMD64.xmm1;

    private AMD64MacroAssembler asm;
    private final CiTarget target;
    private int argsSize;
    private int[] argOffsets;
    private int resultOffset;
    private int saveSize;
    private int registerRestoreEpilogueOffset;

    private RiRuntime runtime;
    private C1XCompiler compiler;
    private CiRegister[] registersSaved;

    private List<CiRegister> allocatableRegisters = new ArrayList<CiRegister>();
    private CiRegister[] allRegisters;
    private boolean savedAllRegisters;

    public AMD64GlobalStubEmitter(C1XCompiler compiler) {
        this.compiler = compiler;
        this.target = compiler.target;
        this.runtime = compiler.runtime;
        allRegisters = AMD64.allRegisters;
    }

    private void reset(CiKind resultKind, CiKind[] argTypes) {
        asm = new AMD64MacroAssembler(compiler, compiler.target);
        saveSize = 0;
        argsSize = 0;
        argOffsets = new int[argTypes.length];
        resultOffset = 0;
        registerRestoreEpilogueOffset = -1;
        registersSaved = null;

        for (int i = 0; i < argTypes.length; i++) {
            argOffsets[i] = argsSize;
            argsSize += ARGUMENT_SIZE;
        }

        if (resultKind != CiKind.Void) {
            if (argsSize == 0) {
                argsSize = ARGUMENT_SIZE;
            }
            resultOffset = 0;
        }
    }

    public GlobalStub emit(CiRuntimeCall runtimeCall, RiRuntime runtime) {
        reset(runtimeCall.resultKind, runtimeCall.arguments);
        emitStandardForward(null, runtimeCall);
        String name = "stub-" + runtimeCall;
        CiTargetMethod targetMethod = asm.finishTargetMethod(name, runtime, registerRestoreEpilogueOffset);
        Object stubObject = runtime.registerGlobalStub(targetMethod, name);
        return new GlobalStub(null, runtimeCall.resultKind, stubObject, argsSize, argOffsets, resultOffset);
    }

    public GlobalStub emit(GlobalStub.Id stub, RiRuntime runtime) {
        reset(stub.resultKind, stub.arguments);

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
            case fneg:
                emitFNEG();
                break;
            case dneg:
                emitDNEG();
                break;
        }

        String name = "stub-" + stub;
        CiTargetMethod targetMethod = asm.finishTargetMethod(name, runtime, registerRestoreEpilogueOffset);
        Object stubObject = runtime.registerGlobalStub(targetMethod, name);
        return new GlobalStub(stub, stub.resultKind, stubObject, argsSize, argOffsets, resultOffset);
    }

    private CiValue allocateParameterOperand(XirParameter param, int parameterIndex) {
        return new CiAddress(param.kind, AMD64.RSP, argumentIndexToStackOffset(parameterIndex));
    }

    private CiValue allocateResultOperand(XirOperand result) {
        return new CiAddress(result.kind, AMD64.RSP, argumentIndexToStackOffset(0));
    }

    private CiValue allocateOperand(XirTemp temp) {
        if (temp instanceof XirRegister) {
            XirRegister fixed = (XirRegister) temp;
            return fixed.register;
        }

        return newRegister(temp.kind);
    }

    private CiValue newRegister(CiKind kind) {
        assert kind != CiKind.Float && kind != CiKind.Double;
        assert allocatableRegisters.size() > 0;
        return allocatableRegisters.remove(allocatableRegisters.size() - 1).asValue(kind);
    }

    public GlobalStub emit(XirTemplate template, RiRuntime runtime) {
        reset(template.resultOperand.kind, getArgumentKinds(template));

        C1XCompilation compilation = new C1XCompilation(compiler, compiler.target, compiler.runtime, null);
        compilation.initFrameMap(0);
        compilation.frameMap().setFrameSize(frameSize());
        AMD64LIRAssembler assembler = new AMD64LIRAssembler(compilation);
        asm = assembler.masm;

        for (CiRegister reg : compiler.target.allocationSpec.allocatableRegisters) {
            if (reg.isCpu()) {
                allocatableRegisters.add(reg);
            }
        }

        for (XirTemp t : template.temps) {
            if (t instanceof XirRegister) {
                final XirRegister fixed = (XirRegister) t;
                if (fixed.register.isRegister()) {
                    allocatableRegisters.remove(fixed.register.asRegister());
                }
            }
        }

        completeSavePrologue();

        CiValue[] operands = new CiValue[template.variableCount];

        XirOperand resultOperand = template.resultOperand;

        if (template.allocateResultOperand) {
            CiValue outputOperand = CiValue.IllegalValue;
            // This snippet has a result that must be separately allocated
            // Otherwise it is assumed that the result is part of the inputs
            if (resultOperand.kind != CiKind.Void && resultOperand.kind != CiKind.Illegal) {
                outputOperand = allocateResultOperand(resultOperand);
                assert operands[resultOperand.index] == null;
            }
            operands[resultOperand.index] = outputOperand;
        }

        for (XirParameter param : template.parameters) {
            assert !(param instanceof XirConstantOperand) : "constant parameters not supported for stubs";
            CiValue op = allocateParameterOperand(param, param.parameterIndex);
            assert operands[param.index] == null;

            // Is the value destroyed?
            if (template.isParameterDestroyed(param.parameterIndex)) {
                CiValue newOp = newRegister(op.kind);
                assembler.moveOp(op, newOp, op.kind, null, false);
                operands[param.index] = newOp;
            } else {
                operands[param.index] = op;
            }
        }

        for (XirConstant c : template.constants) {
            assert operands[c.index] == null;
            operands[c.index] = c.value;
        }

        for (XirTemp t : template.temps) {
            CiValue op = allocateOperand(t);
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

        assert template.marks.length == 0 : "marks not supported in global stubs";
        assembler.emitXirInstructions(null, template.fastPath, labels, operands, null);
        epilogue();
        CiTargetMethod targetMethod = asm.finishTargetMethod(template.name, runtime, registerRestoreEpilogueOffset);
        Object stubObject = runtime.registerGlobalStub(targetMethod, template.name);
        return new GlobalStub(null, template.resultOperand.kind, stubObject, argsSize, argOffsets, resultOffset);
    }

    private CiKind[] getArgumentKinds(XirTemplate template) {
        CiXirAssembler.XirParameter[] params = template.parameters;
        CiKind[] result = new CiKind[params.length];
        for (int i = 0; i < params.length; i++) {
            result[i] = params[i].kind;
        }
        return result;
    }

    private void negatePrologue() {
        partialSavePrologue(negateArgument, negateTemp);
    }

    private void negateEpilogue() {
        storeArgument(0, negateArgument);
        epilogue();
    }

    private void emitDNEG() {
        negatePrologue();
        asm.movsd(negateTemp, asm.recordDataReferenceInCode(CiConstant.forLong(DoubleSignFlip)));
        asm.xorpd(negateArgument, negateTemp);
        negateEpilogue();
    }

    private void emitFNEG() {
        negatePrologue();
        asm.movsd(negateTemp, asm.recordDataReferenceInCode(CiConstant.forLong(FloatSignFlip)));
        asm.xorps(negateArgument, negateTemp);
        negateEpilogue();
    }

    private void convertPrologue() {
        partialSavePrologue(convertArgument, convertResult);
        loadArgument(0, convertArgument);
    }

    private void convertEpilogue() {
        storeArgument(0, convertResult);
        epilogue();
    }

    private void emitD2L() {
        emitCOMISSD(true, false);
    }

    private void emitD2I() {
        emitCOMISSD(true, true);
    }

    private void emitF2L() {
        emitCOMISSD(false, false);
    }

    private void emitF2I() {
        emitCOMISSD(false, true);
    }

    private void emitCOMISSD(boolean isDouble, boolean isInt) {
        convertPrologue();
        if (isDouble) {
            asm.ucomisd(convertArgument, asm.recordDataReferenceInCode(CiConstant.DOUBLE_0));
        } else {
            asm.ucomiss(convertArgument, asm.recordDataReferenceInCode(CiConstant.FLOAT_0));
        }
        Label nan = new Label();
        Label ret = new Label();
        asm.jccb(ConditionFlag.parity, nan);
        asm.jccb(ConditionFlag.below, ret);

        // input is > 0 -> return maxInt
        // result register already contains 0x80000000, so subtracting 1 gives 0x7fffffff
        asm.decrementl(convertResult, 1);
        asm.jmpb(ret);

        // input is NaN -> return 0
        asm.bind(nan);
        asm.xorptr(convertResult, convertResult);

        asm.bind(ret);
        convertEpilogue();
    }

    private void emitStandardForward(GlobalStub.Id stub, CiRuntimeCall call) {
        if (stub != null) {
            assert stub.resultKind == call.resultKind;
            assert stub.arguments.length == call.arguments.length;
            for (int i = 0; i < stub.arguments.length; i++) {
                assert stub.arguments[i] == call.arguments[i];
            }
        }

        completeSavePrologue();
        forwardRuntimeCall(call);
        epilogue();
    }

    private int argumentIndexToStackOffset(int index) {
        // <-- lower addresses
        // | stub frame              | caller frame   |
        // | locals,savearea,retaddr | args .....     |
        return frameSize() + (index + 1) * ARGUMENT_SIZE;
    }

    private void loadArgument(int index, CiRegister register) {
        asm.movq(register, new CiAddress(CiKind.Word, AMD64.RSP, argumentIndexToStackOffset(index)));
    }

    private void storeArgument(int index, CiRegister register) {
        asm.movq(new CiAddress(CiKind.Word, AMD64.RSP, argumentIndexToStackOffset(index)), register);
    }

    private void partialSavePrologue(CiRegister... registersToSave) {
        this.registersSaved = registersToSave;
        this.saveSize = registersToSave.length * target.wordSize;

        // align to code size
        int entryCodeOffset = runtime.codeOffset();
        if (entryCodeOffset != 0) {
            asm.nop(entryCodeOffset);
        }
        asm.subq(AMD64.rsp, frameSize());

        int index = 0;
        for (CiRegister r : registersToSave) {
            asm.movq(new CiAddress(CiKind.Word, AMD64.RSP, index * target.arch.wordSize), r);
            index++;
        }

        asm.setFrameSize(frameSize());
        this.savedAllRegisters = false;
    }

    private void completeSavePrologue() {
        this.saveSize = target.registerConfig.getMinimumCalleeSaveFrameSize();
        int entryCodeOffset = runtime.codeOffset();
        if (entryCodeOffset != 0) {
            // align to code size
            asm.nop(entryCodeOffset);
        }
        asm.subq(AMD64.rsp, frameSize());
        asm.setFrameSize(frameSize());
        int rspToRegisterState = frameSize() - saveSize;
        assert rspToRegisterState >= 0;
        // save all registers
        for (CiRegister r : allRegisters) {
            int offset = target.registerConfig.getCalleeSaveRegisterOffset(r);
            assert offset >= 0;
            if (r != AMD64.rsp && offset >= 0) {
                asm.movq(new CiAddress(CiKind.Word, AMD64.RSP, rspToRegisterState + offset), r);
            }
        }
        this.savedAllRegisters = true;
    }

    private void epilogue() {
        assert registerRestoreEpilogueOffset == -1;
        registerRestoreEpilogueOffset = asm.codeBuffer.position();

        if (savedAllRegisters) {
            // saved all registers, restore all registers
            int rspToRegisterState = frameSize() - saveSize;
            for (CiRegister r : allRegisters) {
                int offset = target.registerConfig.getCalleeSaveRegisterOffset(r);
                if (r != AMD64.rsp && offset >= 0) {
                    asm.movq(r, new CiAddress(CiKind.Word, AMD64.RSP, rspToRegisterState + offset));
                }
            }
        } else {
            // saved only select registers
            for (int index = 0; index < registersSaved.length; index++) {
                CiRegister r = registersSaved[index];
                asm.movq(r, new CiAddress(CiKind.Word, AMD64.RSP, index * target.wordSize));
            }
            registersSaved = null;
        }

        // Restore rsp
        asm.addq(AMD64.rsp, frameSize());
        asm.ret(0);
    }

    private int frameSize() {
        return target.alignFrameSize(saveSize);
    }

    private void forwardRuntimeCall(CiRuntimeCall call) {
        // Load arguments
        CiCallingConvention cc = target.registerConfig.getRuntimeCallingConvention(call.arguments, target);
        for (int i = 0; i < cc.locations.length; ++i) {
            CiValue location = cc.locations[i];
            loadArgument(i, location.asRegister());
        }

        // Call to the runtime
        asm.directCall(call, null);

        if (call.resultKind != CiKind.Void) {
            CiRegister returnRegister = target.registerConfig.getReturnRegister(call.resultKind);
            this.storeArgument(0, returnRegister);
        }
    }
}
