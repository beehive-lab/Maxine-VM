/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
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
package com.sun.c1x.target;

import static com.sun.cri.ci.CiCallingConvention.Type.JavaCallee;

import java.util.*;

import com.oracle.max.asm.*;
import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.stub.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.CiXirAssembler.*;
import com.sun.cri.xir.*;

/**
 * An object used to produce a single compiler stub.
 */
public abstract class CompilerStubEmitter {

    protected final long       floatSignFlip;
    protected final long       doubleSignFlip;
    protected final CiRegister convertArgument;
    protected final CiRegister convertResult;
    protected final CiRegister negateArgument;
    protected final CiRegister negateTemp;

    /**
     * The slots in which the stub finds its incoming arguments.
     * To get the arguments from the perspective of the stub's caller,
     * use {@link CiStackSlot#asOutArg()}.
     */
    protected final CiStackSlot[] inArgs;

    /**
     * The slot in which the stub places its return value (if any).
     * To get the value from the perspective of the stub's caller,
     * use {@link CiStackSlot#asOutArg()}.
     */
    protected final CiStackSlot outResult;

    /**
     * The offset of the stub code restoring the saved registers and returning to the caller.
     */
    protected int registerRestoreEpilogueOffset = -1;

    /**
     * The layout of the callee save area of the stub being emitted.
     */
    protected CiCalleeSaveLayout csl;

    /**
     * The compilation object for the stub being emitted.
     */
    protected final C1XCompilation comp;

    protected final TargetMethodAssembler tasm;
    private final   AbstractAssembler     asm;

    public CompilerStubEmitter(C1XCompilation compilation, AbstractAssembler asm, CiKind[] argTypes,
                               CiKind resultKind, long floatSignFlip, long doubleSignFlip, CiRegister convertArgument,
                               CiRegister convertResult, CiRegister negateArgument, CiRegister negateTemp) {
        compilation.initFrameMap(0);
        this.comp = compilation;
        final RiRegisterConfig registerConfig = compilation.compiler.compilerStubRegisterConfig;
        this.asm = asm;
        this.tasm = new TargetMethodAssembler(asm);

        inArgs = new CiStackSlot[argTypes.length];
        if (argTypes.length != 0) {
            final CiValue[] locations = registerConfig.getCallingConvention(JavaCallee, argTypes, compilation.target, true).locations;
            for (int i = 0; i < argTypes.length; i++) {
                inArgs[i] = (CiStackSlot) locations[i];
            }
        }

        if (resultKind != CiKind.Void) {
            final CiValue location = registerConfig.getCallingConvention(JavaCallee, new CiKind[] {resultKind}, compilation.target, true).locations[0];
            outResult = (CiStackSlot) location;
        } else {
            outResult = null;
        }
        this.floatSignFlip = floatSignFlip;
        this.doubleSignFlip = doubleSignFlip;
        this.convertArgument = convertArgument;
        this.convertResult = convertResult;
        this.negateArgument = negateArgument;
        this.negateTemp = negateTemp;
    }

    protected void prepareOperands(XirTemplate template, ArrayList<CiRegister> allocatableRegisters, CiValue[] operands, LIRAssembler lasm) {
        for (int i = 0; i < template.parameters.length; i++) {
            final XirParameter param = template.parameters[i];
            assert !(param instanceof XirConstantOperand) : "constant parameters not supported for stubs";

            CiValue op = inArgs[i];
            assert operands[param.index] == null;

            // Is the value destroyed?
            if (template.isParameterDestroyed(param.parameterIndex)) {
                CiValue newOp = newRegister(op.kind, allocatableRegisters);
                lasm.moveOp(op, newOp, op.kind, null, false);
                operands[param.index] = newOp;
            } else {
                operands[param.index] = op;
            }
        }
    }

    protected static void reserveRegistersForTemplate(XirTemplate template, ArrayList<CiRegister> allocatableRegisters) {
        for (XirTemp t : template.temps) {
            if (t instanceof XirRegister) {
                final XirRegister fixed = (XirRegister) t;
                if (fixed.register.isRegister()) {
                    allocatableRegisters.remove(fixed.register.asRegister());
                }
            }
        }
    }

    public CompilerStub emit(CiRuntimeCall runtimeCall) {
        emitStandardForward(null, runtimeCall);
        String name = "c1x-stub-" + runtimeCall;
        CiTargetMethod targetMethod = tasm.finishTargetMethod(name, comp.runtime, registerRestoreEpilogueOffset, true);
        Object stubObject = comp.runtime.registerCompilerStub(targetMethod, name);
        return new CompilerStub(null, runtimeCall.resultKind, stubObject, inArgs, outResult);
    }

    protected abstract void emit0(CompilerStub.Id stub);

    public CompilerStub emit(CompilerStub.Id stub) {
        emit0(stub);

        String name = "c1x-stub-" + stub;
        CiTargetMethod targetMethod = tasm.finishTargetMethod(name, comp.runtime, registerRestoreEpilogueOffset, true);
        Object stubObject = comp.runtime.registerCompilerStub(targetMethod, name);
        return new CompilerStub(stub, stub.resultKind, stubObject, inArgs, outResult);
    }

    protected CiValue allocateOperand(XirTemp temp, ArrayList<CiRegister> allocatableRegisters) {
        if (temp instanceof XirRegister) {
            XirRegister fixed = (XirRegister) temp;
            return fixed.register;
        }

        return newRegister(temp.kind, allocatableRegisters);
    }

    private CiValue newRegister(CiKind kind, ArrayList<CiRegister> allocatableRegisters) {
        assert kind != CiKind.Float && kind != CiKind.Double;
        assert allocatableRegisters.size() > 0;
        return allocatableRegisters.remove(allocatableRegisters.size() - 1).asValue(kind);
    }

    public abstract CompilerStub emit(XirTemplate template);

    protected abstract void convertPrologue();

    protected abstract void convertEpilogue();

    protected void emitD2L() {
        emitCOMISSD(true, false);
    }

    protected void emitD2I() {
        emitCOMISSD(true, true);
    }

    protected void emitF2L() {
        emitCOMISSD(false, false);
    }

    protected void emitF2I() {
        emitCOMISSD(false, true);
    }

    protected abstract void emitCOMISSD(boolean isDouble, boolean isInt);

    private void emitStandardForward(CompilerStub.Id stub, CiRuntimeCall call) {
        if (stub != null) {
            assert stub.resultKind == call.resultKind;
            assert stub.arguments.length == call.arguments.length;
            for (int i = 0; i < stub.arguments.length; i++) {
                assert stub.arguments[i] == call.arguments[i];
            }
        }

        prologue(comp.registerConfig.getCalleeSaveLayout());
        forwardRuntimeCall(call);
        epilogue();
    }

    protected abstract void epilogue();

    protected abstract void prologue(CiCalleeSaveLayout csl);

    protected int frameSize() {
        return comp.target.alignFrameSize(csl.size);
    }

    protected abstract void forwardRuntimeCall(CiRuntimeCall call);

    protected AbstractAssembler getAssembler() {
        return asm;
    }

}
