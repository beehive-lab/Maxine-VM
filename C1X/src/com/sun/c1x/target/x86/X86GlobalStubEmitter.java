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

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.target.x86.X86Assembler.*;
import com.sun.c1x.util.*;

public class X86GlobalStubEmitter implements GlobalStubEmitter {

    private X86MacroAssembler asm;
    private final CiTarget target;
    private int frameSize;
    private int registerRestoreEpilogueOffset;
    private RiRuntime runtime;
    private C1XCompiler compiler;
    private CiRegister[] registersSaved;
    private static final int ReservedArgumentSlots = 4;

    private final CiRegister convertArgument = X86.xmm0;
    private final CiRegister convertResult = X86.rax;
    private final CiRegister negateArgument = X86.xmm0;
    private final CiRegister negateTemp = X86.xmm1;

    private static final long FloatSignFlip = 0x8000000080000000L;
    private static final long DoubleSignFlip = 0x8000000000000000L;

    public X86GlobalStubEmitter(C1XCompiler compiler) {
        this.compiler = compiler;
        this.target = compiler.target;
        this.runtime = compiler.runtime;
    }

    public CiTargetMethod emitRuntimeStub(CiRuntimeCall runtimeCall) {
        return emitHelper(null, runtimeCall);
    }

    public CiTargetMethod emit(GlobalStub stub) {
        return emitHelper(stub, null);
    }

    public CiTargetMethod emitHelper(GlobalStub stub, CiRuntimeCall runtimeCall) {
        asm = new X86MacroAssembler(compiler, compiler.target, -1);
        this.frameSize = 0;
        this.registerRestoreEpilogueOffset = -1;

        if (stub == null) {
            emitStandardForward(null, runtimeCall);
        } else {
            switch (stub) {

                case NewInstance:
                    emitStandardForward(stub, CiRuntimeCall.NewInstance);
                    break;

                case ThrowRangeCheckFailed:
                    emitStandardForward(stub, CiRuntimeCall.ThrowRangeCheckFailed);
                    break;

                case ThrowIndexException:
                    emitStandardForward(stub, CiRuntimeCall.ThrowIndexException);
                    break;

                case ThrowDiv0Exception:
                    emitStandardForward(stub, CiRuntimeCall.ThrowDiv0Exception);
                    break;

                case ThrowNullPointerException:
                    emitStandardForward(stub, CiRuntimeCall.ThrowNullPointerException);
                    break;

                case ThrowArrayStoreException:
                    emitStandardForward(stub, CiRuntimeCall.ThrowArrayStoreException);
                    break;

                case ThrowClassCastException:
                    emitStandardForward(stub, CiRuntimeCall.ThrowClassCastException);
                    break;

                case ThrowIncompatibleClassChangeError:
                    emitStandardForward(stub, CiRuntimeCall.ThrowIncompatibleClassChangeError);
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
            }
        }

        return asm.finishTargetMethod(runtime, frameSize, null, registerRestoreEpilogueOffset);
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
        convertPrologue();
        asm.mov64(convertResult, Long.MIN_VALUE);
        asm.ucomiss(convertArgument, asm.recordDataReferenceInCode(CiConstant.forDouble(Double.NaN)));
        asm.cmovq(Condition.equal, convertResult, asm.recordDataReferenceInCode(CiConstant.forLong(0L)));
        convertEpilogue();
    }

    private void emitD2I() {
        convertPrologue();
        asm.mov64(convertResult, Long.MIN_VALUE);
        asm.ucomiss(convertArgument, asm.recordDataReferenceInCode(CiConstant.forDouble(Double.NaN)));
        asm.cmovl(Condition.equal, convertResult, asm.recordDataReferenceInCode(CiConstant.forInt(0)));
        convertEpilogue();
    }

    private void emitF2L() {
        convertPrologue();
        asm.movl(convertResult, Integer.MIN_VALUE);
        asm.ucomiss(convertArgument, asm.recordDataReferenceInCode(CiConstant.forFloat(Float.NaN)));
        asm.cmovq(Condition.equal, convertResult, asm.recordDataReferenceInCode(CiConstant.forLong(0L)));
        convertEpilogue();
    }

    private void emitF2I() {
        convertPrologue();
        asm.movl(convertResult, Integer.MIN_VALUE);
        asm.ucomiss(convertArgument, asm.recordDataReferenceInCode(CiConstant.forFloat(Float.NaN)));
        asm.cmovl(Condition.equal, convertResult, asm.recordDataReferenceInCode(CiConstant.forInt(0)));
        convertEpilogue();
    }

    private void emitStandardForward(GlobalStub stub, CiRuntimeCall call) {
        if (stub != null) {
            assert stub.resultType == call.resultType;
            assert stub.arguments.length == call.arguments.length;
            for (int i = 0; i < stub.arguments.length; i++) {
                assert stub.arguments[i] == call.arguments[i];
            }
        }

        prologue(true);
        forwardRuntimeCall(call);
        epilogue();
    }

    private int argumentIndexToStackOffset(int index) {
        assert index < ReservedArgumentSlots;
        return frameSize - (index + 1) * target.arch.wordSize;
    }

    private void loadArgument(int index, CiRegister register) {
        asm.movptr(register, new Address(X86.rsp, argumentIndexToStackOffset(index)));
    }

    private void storeArgument(int index, CiRegister register) {
        asm.movptr(new Address(X86.rsp, argumentIndexToStackOffset(index)), register);
    }

    private int savedRegistersSize() {

        CiRegister[] registers = X86.allRegisters;
        if (target.arch.is64bit()) {
            registers = X86.allRegisters64;
        }

        return registers.length * target.arch.wordSize;
    }

    private void saveRegisters() {

        CiRegister[] registers = X86.allRegisters;
        if (target.arch.is64bit()) {
            registers = X86.allRegisters64;
        }

        List<CiRegister> savedRegistersList = new ArrayList<CiRegister>();
        int index = 0;
        for (CiRegister r : registers) {
            if (r != X86.rsp) {
                savedRegistersList.add(r);
                asm.movq(new Address(X86.rsp, index * target.arch.wordSize), r);
                index++;
            }
        }
        this.registersSaved = savedRegistersList.toArray(new CiRegister[savedRegistersList.size()]);

        int frameSize = (index + ReservedArgumentSlots) * target.arch.wordSize;
        assert this.frameSize >= frameSize;
    }

    private void partialSavePrologue(CiRegister... registersToSave) {

        this.registersSaved = registersToSave;

        this.frameSize = (target.arch.wordSize + ReservedArgumentSlots) * registersToSave.length;

        asm.makeOffset(runtime.codeOffset());

        // Modify rsp
        asm.subq(X86.rsp, this.frameSize);

        int index = 0;
        for (CiRegister r : registersToSave) {
            asm.movq(new Address(X86.rsp, index * target.arch.wordSize), r);
            index++;
        }

        asm.setFrameSize(this.frameSize);
    }

    private void prologue(boolean savesRegisters) {
        if (savesRegisters) {
            this.frameSize = savedRegistersSize();
        } else {
            this.frameSize = 0;
        }

        this.frameSize += ReservedArgumentSlots * target.arch.wordSize;

        asm.makeOffset(runtime.codeOffset());

        // Modify rsp
        asm.subq(X86.rsp, this.frameSize);

        asm.setFrameSize(this.frameSize);
    }

    private void epilogue() {

        assert registerRestoreEpilogueOffset == -1;
        registerRestoreEpilogueOffset = asm.codeBuffer.position();

        if (registersSaved != null) {
            int index = 0;
            for (CiRegister r : registersSaved) {
                asm.movq(r, new Address(X86.rsp, index * target.arch.wordSize));
                index++;
            }
            registersSaved = null;
        }

        // Restore rsp
        asm.addq(X86.rsp, this.frameSize);

        asm.ret(0);
    }

    private void forwardRuntimeCall(CiRuntimeCall call) {

        // Save registers
        saveRegisters();

        // Load arguments
        CiLocation[] result = runtime.runtimeCallingConvention(call.arguments);
        for (int i = 0; i < call.arguments.length; i++) {
            loadArgument(i, result[i].first);
        }

        // Call to the runtime
        asm.callRuntime(call);

        if (call.resultType != CiKind.Void) {
            this.storeArgument(0, runtime.returnRegister(call.resultType));
        }
    }
}
