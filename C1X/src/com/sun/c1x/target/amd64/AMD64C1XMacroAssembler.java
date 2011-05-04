package com.sun.c1x.target.amd64;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.lir.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;


public class AMD64C1XMacroAssembler extends AMD64C1XAssembler {
    public static class WithCompiler extends AMD64C1XMacroAssembler {

        private final C1XCompiler compiler;

        public WithCompiler(C1XCompiler compiler, RiRegisterConfig registerConfig) {
            super(compiler.target, registerConfig);
            this.compiler = compiler;
        }

        @Override
        public GlobalStub lookupGlobalStub(XirTemplate template) {
            return compiler.lookupGlobalStub(template);
        }
    }

    public AMD64C1XMacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
    }

    /**
     * Must be overridden if compiling code that makes calls to global stubs.
     */
    public GlobalStub lookupGlobalStub(XirTemplate template) {
        throw new IllegalArgumentException("This assembler does not support compiling calls to global stubs");
    }

    public final int callGlobalStub(XirTemplate stub, LIRDebugInfo info, CiRegister result, CiValue... args) {
        assert args.length == stub.parameters.length;
        return callGlobalStubHelper(lookupGlobalStub(stub), stub.resultOperand.kind, info, result, args);
    }

    public final int callGlobalStub(GlobalStub stub, LIRDebugInfo info, CiRegister result, CiValue... args) {
        assert args.length == stub.argOffsets.length;
        return callGlobalStubHelper(stub, stub.resultKind, info, result, args);
    }

    private int callGlobalStubHelper(GlobalStub stub, CiKind resultKind, LIRDebugInfo info, CiRegister result, CiValue... args) {
        for (int i = 0; i < args.length; i++) {
            storeParameter(args[i], stub.argOffsets[i]);
        }

        int pos = directCall(stub.stubObject, info);

        if (result != CiRegister.None) {
            loadResult(result, stub.resultOffset, resultKind);
        }

        // Clear out parameters
        if (AsmOptions.GenAssertionCode) {
            for (int i = 0; i < args.length; i++) {
                movptr(new CiAddress(CiKind.Word, AMD64.RSP, stub.argOffsets[i]), 0);
            }
        }
        return pos;
    }

    private void loadResult(CiRegister r, int offset, CiKind kind) {
        if (kind == CiKind.Int || kind == CiKind.Boolean) {
            movl(r, new CiAddress(CiKind.Int, AMD64.RSP, offset));
        } else if (kind == CiKind.Float) {
            movss(r, new CiAddress(CiKind.Float, AMD64.RSP, offset));
        } else if (kind == CiKind.Double) {
            movsd(r, new CiAddress(CiKind.Double, AMD64.RSP, offset));
        } else {
            movq(r, new CiAddress(CiKind.Word, AMD64.RSP, offset));
        }
    }

    private void storeParameter(CiValue registerOrConstant, int offset) {
        CiKind k = registerOrConstant.kind;
        if (registerOrConstant.isConstant()) {
            CiConstant c = (CiConstant) registerOrConstant;
            if (c.kind == CiKind.Object) {
                movoop(new CiAddress(CiKind.Word, AMD64.RSP, offset), c);
            } else {
                movptr(new CiAddress(CiKind.Word, AMD64.RSP, offset), c.asInt());
            }
        } else if (registerOrConstant.isRegister()) {
            if (k.isFloat()) {
                movss(new CiAddress(CiKind.Float, AMD64.RSP, offset), registerOrConstant.asRegister());
            } else if (k.isDouble()) {
                movsd(new CiAddress(CiKind.Double, AMD64.RSP, offset), registerOrConstant.asRegister());
            } else {
                movq(new CiAddress(CiKind.Word, AMD64.RSP, offset), registerOrConstant.asRegister());
            }
        } else {
            throw new InternalError("should not reach here");
        }
    }


    void movoop(CiRegister dst, CiConstant obj) {
        assert obj.kind == CiKind.Object;
        if (obj.isNull()) {
            xorq(dst, dst);
        } else {
            if (target.inlineObjects) {
                recordDataReferenceInCode(obj);
                movq(dst, 0xDEADDEADDEADDEADL);
            } else {
                movq(dst, recordDataReferenceInCode(obj));
            }
        }
    }

    void movoop(CiAddress dst, CiConstant obj) {
        movoop(rscratch1, obj);
        movq(dst, rscratch1);
    }

    int correctedIdivq(CiRegister reg) {
        // Full implementation of Java ldiv and lrem; checks for special
        // case as described in JVM spec. : p.243 & p.271. The function
        // returns the (pc) offset of the idivl instruction - may be needed
        // for implicit exceptions.
        //
        // normal case special case
        //
        // input : X86Register.rax: dividend minLong
        // reg: divisor (may not be eax/edx) -1
        //
        // output: X86Register.rax: quotient (= X86Register.rax idiv reg) minLong
        // X86Register.rdx: remainder (= X86Register.rax irem reg) 0
        assert reg != AMD64.rax && reg != AMD64.rdx : "reg cannot be X86Register.rax or X86Register.rdx register";
        final long minLong = 0x8000000000000000L;
        Label normalCase = new Label();
        Label specialCase = new Label();

        // check for special case
        cmpq(AMD64.rax, recordDataReferenceInCode(CiConstant.forLong(minLong)));
        jcc(AMD64Assembler.ConditionFlag.notEqual, normalCase);
        xorl(AMD64.rdx, AMD64.rdx); // prepare X86Register.rdx for possible special case (where
        // remainder = 0)
        cmpq(reg, -1);
        jcc(AMD64Assembler.ConditionFlag.equal, specialCase);

        // handle normal case
        bind(normalCase);
        cdqq();
        int idivqOffset = codeBuffer.position();
        idivq(reg);

        // normal and special case exit
        bind(specialCase);

        return idivqOffset;
    }

}
