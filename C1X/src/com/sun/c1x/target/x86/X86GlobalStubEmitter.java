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
package com.sun.c1x.target.x86;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.target.*;
import com.sun.c1x.target.x86.X86Assembler.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

public class X86GlobalStubEmitter implements GlobalStubEmitter {

    private X86MacroAssembler asm;
    private final Target target;
    private int frameSize;
    private RiRuntime runtime;
    private C1XCompiler compiler;
    private Register[] registersSaved;

    private final Register convertArgument = X86.xmm0;
    private final Register convertResult = X86.rax;
    private final Register negateArgument = X86.xmm0;
    private final Register negateTemp = X86.xmm1;

    private static final long FloatSignFlip = 0x8000000080000000L;
    private static final long DoubleSignFlip = 0x8000000000000000L;

    public X86GlobalStubEmitter(C1XCompiler compiler) {
        this.compiler = compiler;
        this.target = compiler.target;
        this.runtime = compiler.runtime;
    }

    public CiTargetMethod emit(GlobalStub stub) {
        asm = new X86MacroAssembler(compiler, compiler.target);
        this.frameSize = 0;

        switch (stub) {
            case SlowSubtypeCheck:
                emitStandardForward(stub, CiRuntimeCall.SlowSubtypeCheck);
                break;

            case NewObjectArray:
                emitStandardForward(stub, CiRuntimeCall.NewArray);
                break;

            case NewTypeArray:
                emitStandardForward(stub, CiRuntimeCall.NewArray);
                break;

            case NewInstance:
                emitStandardForward(stub, CiRuntimeCall.NewInstance);
                break;

            case ArithmethicLrem:
                emitStandardForward(stub, CiRuntimeCall.ArithmethicLrem);
                break;

            case ArithmeticDrem:
                emitStandardForward(stub, CiRuntimeCall.ArithmeticDrem);
                break;

            case ArithmeticFrem:
                emitStandardForward(stub, CiRuntimeCall.ArithmeticFrem);
                break;

            case ArithmeticLdiv:
                emitStandardForward(stub, CiRuntimeCall.ArithmeticLdiv);
                break;

            case ArithmeticLmul:
                emitStandardForward(stub, CiRuntimeCall.ArithmeticLmul);
                break;

            case ResolveClass:
                emitStandardForward(stub, CiRuntimeCall.ResolveClass);
                break;

            case ResolveVTableIndex:
                emitStandardForward(stub, CiRuntimeCall.ResolveVTableIndex);
                break;

            case RetrieveInterfaceIndex:
                emitStandardForward(stub, CiRuntimeCall.RetrieveInterfaceIndex);
                break;

            case ResolveOptVirtualCall:
                emitStandardForward(stub, CiRuntimeCall.ResolveOptVirtualCall);
                break;

            case ResolveStaticCall:
                emitStandardForward(stub, CiRuntimeCall.ResolveStaticCall);
                break;

            case MonitorEnter:
                emitStandardForward(stub, CiRuntimeCall.Monitorenter);
                break;

            case MonitorExit:
                emitStandardForward(stub, CiRuntimeCall.Monitorexit);
                break;

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

            default:
                throw Util.shouldNotReachHere();
        }

        return asm.finishTargetMethod(runtime, frameSize, null);
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
        asm.movsd(negateTemp, asm.longConstant(DoubleSignFlip));
        asm.xorpd(negateArgument, negateTemp);
        negateEpilogue();
    }

    private void emitFNEG() {
        negatePrologue();
        asm.movsd(negateTemp, asm.longConstant(FloatSignFlip));
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
        convertPrologue();
        asm.mov64(convertResult, Long.MIN_VALUE);
        asm.ucomiss(convertArgument, asm.doubleConstant(Double.NaN));
        asm.cmovq(Condition.equal, convertResult, asm.longConstant(0L));
        convertEpilogue();
    }

    private void emitD2I() {
        convertPrologue();
        asm.mov64(convertResult, Long.MIN_VALUE);
        asm.ucomiss(convertArgument, asm.doubleConstant(Double.NaN));
        asm.cmovl(Condition.equal, convertResult, asm.intConstant(0));
        convertEpilogue();
    }

    private void emitF2L() {
        convertPrologue();
        asm.movl(convertResult, Integer.MIN_VALUE);
        asm.ucomiss(convertArgument, asm.floatConstant(Float.NaN));
        asm.cmovq(Condition.equal, convertResult, asm.longConstant(0L));
        convertEpilogue();
    }

    private void emitF2I() {
        convertPrologue();
        asm.movl(convertResult, Integer.MIN_VALUE);
        asm.ucomiss(convertArgument, asm.floatConstant(Float.NaN));
        asm.cmovl(Condition.equal, convertResult, asm.intConstant(0));
        convertEpilogue();
    }

    private void emitStandardForward(GlobalStub stub, CiRuntimeCall call) {
        assert stub.resultType == call.resultType;
        assert stub.arguments.length == call.arguments.length;
        for (int i = 0; i < stub.arguments.length; i++) {
            assert stub.arguments[i] == call.arguments[i];
        }

        prologue(true);
        forwardRuntimeCall(call);
        epilogue();
    }

    private void loadArgument(int index, Register register) {
        asm.movptr(register, new Address(X86.rsp, (index + 1) * target.arch.wordSize + frameSize));
    }

    private void storeArgument(int index, Register register) {
        asm.movptr(new Address(X86.rsp, (index + 1) * target.arch.wordSize + frameSize), register);
    }

    private int savedRegistersSize() {

        Register[] registers = X86.allRegisters;
        if (target.arch.is64bit()) {
            registers = X86.allRegisters64;
        }

        return registers.length * target.arch.wordSize;
    }

    private void saveRegisters() {

        Register[] registers = X86.allRegisters;
        if (target.arch.is64bit()) {
            registers = X86.allRegisters64;
        }

        int index = 0;
        for (Register r : registers) {
            if (r != X86.rsp) {
                asm.movq(new Address(X86.rsp, index * target.arch.wordSize), r);
                index++;
            }
        }

        int frameSize = index * target.arch.wordSize;
        assert this.frameSize >= frameSize;
    }

    private void restoreRegisters() {

        Register[] registers = X86.allRegisters;
        if (target.arch.is64bit()) {
            registers = X86.allRegisters64;
        }

        int index = 0;
        for (Register r : registers) {

            if (r == X86.rsp) {
                continue;
            }

            asm.movq(r, new Address(X86.rsp, index * target.arch.wordSize));

            index++;
        }
    }

    private void partialSavePrologue(Register... registersToSave) {

        this.registersSaved = registersToSave;

        this.frameSize = target.arch.wordSize * registersToSave.length;

        asm.makeOffset(runtime.codeOffset());

        // Modify rsp
        asm.subq(X86.rsp, this.frameSize);

        int index = 0;
        for (Register r : registersToSave) {
            asm.movq(new Address(X86.rsp, index * target.arch.wordSize), r);
            index++;
        }
    }

    private void prologue(boolean savesRegisters) {
        if (savesRegisters) {
            this.frameSize = savedRegistersSize();
        } else {
            this.frameSize = 0;
        }

        asm.makeOffset(runtime.codeOffset());

        // Modify rsp
        asm.subq(X86.rsp, this.frameSize);
    }

    private void epilogue() {

        if (registersSaved != null) {
            int index = 0;
            for (Register r : registersSaved) {
                asm.movq(r, new Address(X86.rsp, index * target.arch.wordSize));
                index++;
            }
        }

        // Restore rsp
        asm.addq(X86.rsp, this.frameSize);

        asm.ret(0);
    }

    private void forwardRuntimeCall(CiRuntimeCall call) {

        // Save registers
        saveRegisters();

        // Load arguments
        CiLocation[] result = new CiLocation[call.arguments.length];
        runtime.runtimeCallingConvention(call.arguments, result);
        for (int i = 0; i < call.arguments.length; i++) {
            loadArgument(i, result[i].first);
        }

        // Call to the runtime
        asm.callRuntime(call);

        if (call.resultType != BasicType.Void) {
            this.storeArgument(0, runtime.returnRegister(call.resultType));
        }

        // Restore registers including rsp
        restoreRegisters();
    }

}
