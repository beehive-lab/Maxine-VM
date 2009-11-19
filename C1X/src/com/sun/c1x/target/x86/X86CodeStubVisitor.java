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
import com.sun.c1x.lir.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.*;

public final class X86CodeStubVisitor extends CodeStubVisitor {

    private final X86LIRAssembler ce;
    private final X86MacroAssembler masm;
    private final C1XCompilation compilation;
    private static final CiRegister[] NO_PARAMS = new CiRegister[0];

    public X86CodeStubVisitor(X86LIRAssembler lirAssembler) {
        this.ce = lirAssembler;
        this.masm = lirAssembler.masm;
        this.compilation = lirAssembler.compilation;
    }

    public void visitJITAdapterFrameStub(JITAdapterFrameStub stub) {
        masm.bind(stub.entry);

        // Computing how much space need to be allocated on the stack for the adapter. It's at least one slot for saving framePointer(), plus
        // space for out-of-band arguments.
        // The stack will look like this:
        //        <low address>  oarg(n), oarg(n-1), oarg(m), RBP, RIP, jarg(n), jarg(n -1), ... jarg0, ...  <high address>
        //                               |<--------------------------------------------->|
        //                                             adapterFrameSize
        // where "oarg" is an overflow argument and "jarg" is an argument from the caller's java stack.
        // save the caller's RBP

        final int wordSize =  compilation.target.arch.wordSize;

        // Receive calling convention (for the current method, but with outgoing==true, i.e. as if we were calling the current method)
        FrameMap map = compilation.frameMap();
        CallingConvention cc = map.javaCallingConvention(Util.signatureToKinds(compilation.method.signatureType(), !compilation.method.isStatic()), true, false);

        // Adapter frame includes space for save the jited-callee's frame pointer (RBP)
        final int adapterFrameSize = cc.overflowArgumentSize;

        // Allocate space on the stack (adapted parameters + caller's frame pointer)
        masm.push(X86.rbp);
        if (adapterFrameSize != 0) {
            masm.decrement(X86.rsp, adapterFrameSize);
        }

         // Prefix of a frame is RIP + saved RBP.
        final int framePrefixSize = 2 * wordSize;

        // On entry to the adapter, the top of the stack contains the RIP. The last argument on the stack is
        // immediately above the RIP.
        // We set an offset to that last argument (relative to the new stack pointer) and iterate over the arguments
        // in reverse order,
        // from last to first.
        // This avoids computing the size of the arguments to get the offset to the first argument.
        int jitCallerStackOffset = adapterFrameSize + framePrefixSize;

        final int jitSlotSize = compilation.runtime.getJITStackSlotSize();
        for (int i = cc.locations.length - 1; i >= 0;  i--) {
            CiLocation location = cc.locations[i];
            LIROperand src = LIROperandFactory.address(CiRegister.Stack, jitCallerStackOffset, location.kind);
            ce.moveOp(src, cc.operands[i], location.kind, null, false);
            jitCallerStackOffset += location.kind.size * jitSlotSize;
        }

        // jitCallerOffset is now set to the first location before the first parameter, i.e., the point where
        // the caller stack will retract to.
        masm.call(stub.continuation);
        final int jitCallArgumentSize = jitCallerStackOffset - (adapterFrameSize + framePrefixSize);
        if (adapterFrameSize != 0) {
            masm.increment(X86.rsp, adapterFrameSize);
        }
        masm.pop(X86.rbp);
        // Retract the stack pointer back to its position before the first argument on the caller's stack.
        masm.ret(Util.safeToShort(jitCallArgumentSize));
    }

    public void visitArrayStoreExceptionStub(ArrayStoreExceptionStub stub) {
        masm.bind(stub.entry);
        int infoPos = masm.callRuntimeCalleeSaved(CiRuntimeCall.ThrowArrayStoreException, stub.info, CiRegister.None, NO_PARAMS);
        compilation.addCallInfo(infoPos, stub.info);

        // Insert nop such that the IP is within the range of the target at the position after the call
        masm.nop();

        if (C1XOptions.GenAssertionCode) {
            masm.shouldNotReachHere();
        }
    }

    public void visitDivByZeroStub(DivByZeroStub stub) {
        if (stub.offset != -1) {
            compilation.recordImplicitException(stub.offset, masm.codeBuffer.position());
        }

        masm.bind(stub.entry);
        int infoPos = masm.callRuntimeCalleeSaved(CiRuntimeCall.ThrowArithmeticException, stub.info, CiRegister.None, NO_PARAMS);
        compilation.addCallInfo(infoPos, stub.info);

        // Insert nop such that the IP is within the range of the target at the position after the call
        masm.nop();

        if (C1XOptions.GenAssertionCode) {
            masm.shouldNotReachHere();
        }
    }

    public void visitImplicitNullCheckStub(ImplicitNullCheckStub stub) {
        ce.compilation.recordImplicitException(stub.offset, masm.codeBuffer.position());
        masm.bind(stub.entry);
        int infoPos = masm.callRuntimeCalleeSaved(CiRuntimeCall.ThrowNullPointerException, stub.info, CiRegister.None, NO_PARAMS);
        compilation.addCallInfo(infoPos, stub.info);

        // Insert nop such that the IP is within the range of the target at the position after the call
        masm.nop();

        if (C1XOptions.GenAssertionCode) {
            masm.shouldNotReachHere();
        }
    }

    public void visitMonitorEnterStub(MonitorEnterStub stub) {
        masm.bind(stub.entry);
        int infoPos = masm.callRuntimeCalleeSaved(CiRuntimeCall.Monitorenter, stub.info, CiRegister.None, stub.objReg().asRegister(), stub.lockReg().asRegister());
        compilation.addCallInfo(infoPos, stub.info);
        ce.verifyOopMap(stub.info);
        masm.jmp(stub.continuation);
    }

    public void visitMonitorExitStub(MonitorExitStub stub) {
        masm.bind(stub.entry);
        if (stub.computeLock) {
            // lockReg was destroyed by fast unlocking attempt => recompute it
            ce.emitMonitorAddress(stub.monitorIx, stub.lockReg());
        }

        int infoPos = masm.callRuntimeCalleeSaved(CiRuntimeCall.Monitorexit, stub.info, CiRegister.None, stub.objReg().asRegister(), stub.lockReg().asRegister());
        compilation.addCallInfo(infoPos, stub.info);
        masm.jmp(stub.continuation);
    }

    public void visitNewTypeArrayStub(NewTypeArrayStub stub) {
        masm.bind(stub.entry);
        assert stub.length().asRegister() == X86.rbx : "length must in X86Register.rbx : ";
        assert stub.klassReg().asRegister() == X86.rdx : "klassReg must in X86Register.rdx";
        int infoPos = masm.callRuntimeCalleeSaved(CiRuntimeCall.NewArray, stub.info, X86.rax, X86.rdx, X86.rbx);
        compilation.addCallInfo(infoPos, stub.info);
        ce.verifyOopMap(stub.info);
        assert stub.result().asRegister() == X86.rax : "result must in X86Register.rax : ";
        masm.jmp(stub.continuation);
    }

    public void visitRangeCheckStub(RangeCheckStub stub) {
        masm.bind(stub.entry);

        LIROperand index = stub.index();
        RegisterOrConstant rindex;
        if (index.isRegister()) {
            rindex = new RegisterOrConstant(index.asRegister());
        } else {
            assert index.isConstant();
            LIRConstant constantIndex = (LIRConstant) index;
            rindex = new RegisterOrConstant(constantIndex.asInt());
        }
        int infoPos = masm.callRuntimeCalleeSaved(CiRuntimeCall.ThrowArrayIndexOutOfBoundsException, stub.info, CiRegister.None, rindex);
        compilation.addCallInfo(infoPos, stub.info);

        // Insert nop such that the IP is within the range of the target at the position after the call
        masm.nop();

        if (C1XOptions.GenAssertionCode) {
            masm.shouldNotReachHere();
        }
    }

    public void visitCheckCastStub(CheckCastStub stub) {
        masm.bind(stub.entry);
        int infoPos = masm.callRuntimeCalleeSaved(CiRuntimeCall.ThrowClassCastException, stub.info, CiRegister.None, stub.obj().asRegister());
        compilation.addCallInfo(infoPos, stub.info);

        // Insert nop such that the IP is within the range of the target at the position after the call
        masm.nop();

        if (C1XOptions.GenAssertionCode) {
            masm.shouldNotReachHere();
        }
    }
}
