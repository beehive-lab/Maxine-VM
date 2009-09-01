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
import com.sun.c1x.lir.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.*;

public class X86CodeStubVisitor implements CodeStubVisitor {

    private final X86LIRAssembler ce;
    private final X86MacroAssembler masm;
    private final C1XCompilation compilation;
    private static final CiKind[] ARRAY_COPY_SIGNATURE = {CiKind.Object, CiKind.Int, CiKind.Object, CiKind.Int, CiKind.Int};

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


        final int wordSize =  masm.target.arch.wordSize;

        // Receive calling convention (for the current method, but with outgoing==true, i.e. as if we were calling the current method)
        FrameMap map = compilation.frameMap();
        CallingConvention cc = map.javaCallingConvention(Util.signatureToBasicTypes(compilation.method.signatureType(), !compilation.method.isStatic()), true);


        // Adapter frame includes space for save the jited-callee's frame pointer (RBP)
        final int adapterFrameSize = cc.overflowArgumentsSize();

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
        for (int i = cc.locations().length - 1; i >= 0;  i--) {
            CiLocation location = cc.locations()[i];
            CiKind t = location.kind;
            LIROperand src = LIROperandFactory.address(CiRegister.Stack, jitCallerStackOffset, t);
            ce.moveOp(src, cc.at(i), t, LIRPatchCode.PatchNone, null, false);
            jitCallerStackOffset += t.size * jitSlotSize;
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

    public void visitArrayCopyStub(ArrayCopyStub stub) {
        // ---------------slow case: call to native-----------------
        masm.bind(stub.entry);

        CallingConvention cc = compilation.frameMap().javaCallingConvention(ARRAY_COPY_SIGNATURE, true);

        // push parameters
        // (src, srcPos, dest, destPos, length)
        CiRegister[] r = new CiRegister[5];
        r[0] = stub.source().asRegister();
        r[1] = stub.sourcePos().asRegister();
        r[2] = stub.dest().asRegister();
        r[3] = stub.destPos().asRegister();
        r[4] = stub.length().asRegister();

        // next registers will get stored on the stack
        for (int i = 0; i < 5; i++) {
            LIROperand r1 = cc.arguments().get(i);
            if (r1.isStack()) {
                int stOff = r1.singleStackIx() * compilation.target.arch.wordSize;
                masm.movptr(new Address(X86.rsp, stOff), r[i]);
            } else {
                assert r[i] == r1.asRegister() : "Wrong register for arg ";
            }
        }

        ce.alignCall(LIROpcode.StaticCall);

        ce.emitStaticCallStub();

        // TODO: What is meant to be called here?
        //masm.callGlobalStub(GlobalStub.ResolveStaticCall);
        ce.addCallInfoHere(stub.info);
        masm.jmp(stub.continuation);
    }

    public void visitArrayStoreExceptionStub(ArrayStoreExceptionStub stub) {
        masm.bind(stub.entry);
        masm.callGlobalStub(GlobalStub.ThrowArrayStoreException, stub.info);
        ce.addCallInfoHere(stub.info);

        // Insert nop such that the IP is within the range of the target at the position after the call
        masm.nop();

        if (C1XOptions.GenerateAssertionCode) {
            masm.shouldNotReachHere();
        }
    }

    public void visitDivByZeroStub(DivByZeroStub stub) {
        if (stub.offset != -1) {
            compilation.recordImplicitException(stub.offset, masm.codeBuffer.position());
        }

        masm.bind(stub.entry);
        masm.callGlobalStub(GlobalStub.ThrowDiv0Exception, stub.info);
        ce.addCallInfoHere(stub.info);

        // Insert nop such that the IP is within the range of the target at the position after the call
        masm.nop();

        if (C1XOptions.GenerateAssertionCode) {
            masm.shouldNotReachHere();
        }
    }

    public void visitImplicitNullCheckStub(ImplicitNullCheckStub stub) {
        ce.compilation.recordImplicitException(stub.offset, masm.codeBuffer.position());
        masm.bind(stub.entry);
        masm.callGlobalStub(GlobalStub.ThrowNullPointerException, stub.info);
        ce.addCallInfoHere(stub.info);

        // Insert nop such that the IP is within the range of the target at the position after the call
        masm.nop();

        if (C1XOptions.GenerateAssertionCode) {
            masm.shouldNotReachHere();
        }
    }

    public void visitMonitorEnterStub(MonitorEnterStub stub) {
        masm.bind(stub.entry);
        masm.callGlobalStub(GlobalStub.MonitorEnter, stub.info, CiRegister.None, stub.objReg().asRegister(), stub.lockReg().asRegister());
        ce.addCallInfoHere(stub.info);
        ce.verifyOopMap(stub.info);
        masm.jmp(stub.continuation);
    }

    public void visitMonitorExitStub(MonitorExitStub stub) {
        masm.bind(stub.entry);
        if (stub.computeLock) {
            // lockReg was destroyed by fast unlocking attempt => recompute it
            ce.monitorAddress(stub.monitorIx, stub.lockReg());
        }


        masm.callGlobalStub(GlobalStub.MonitorExit, stub.info, CiRegister.None, stub.objReg().asRegister(), stub.lockReg().asRegister());
        masm.jmp(stub.continuation);
    }

    public void visitNewInstanceStub(NewInstanceStub stub) {
        masm.bind(stub.entry);
        ce.addCallInfoHere(stub.info);
        ce.verifyOopMap(stub.info);
        masm.callGlobalStub(stub.stubId, stub.info, stub.result().asRegister(), stub.klassReg().asRegister());
        masm.jmp(stub.continuation);
    }

    public void visitNewObjectArrayStub(NewObjectArrayStub stub) {
        masm.bind(stub.entry);
        assert stub.length().asRegister() == X86.rbx : "length must in X86Register.rbx : ";
        assert stub.klassReg().asRegister() == X86.rdx : "klassReg must in X86Register.rdx";
        masm.callRuntimeCalleeSaved(CiRuntimeCall.NewArray, stub.info, X86.rax, X86.rdx, X86.rbx);
        ce.addCallInfoHere(stub.info);
        ce.verifyOopMap(stub.info);
        assert stub.result().asRegister() == X86.rax : "result must in X86Register.rax : ";
        masm.jmp(stub.continuation);
    }

    public void visitNewTypeArrayStub(NewTypeArrayStub stub) {
        masm.bind(stub.entry);
        assert stub.length().asRegister() == X86.rbx : "length must in X86Register.rbx : ";
        assert stub.klassReg().asRegister() == X86.rdx : "klassReg must in X86Register.rdx";
        masm.callRuntimeCalleeSaved(CiRuntimeCall.NewArray, stub.info, X86.rax, X86.rdx, X86.rbx);
        ce.addCallInfoHere(stub.info);
        ce.verifyOopMap(stub.info);
        assert stub.result().asRegister() == X86.rax : "result must in X86Register.rax : ";
        masm.jmp(stub.continuation);
    }

    public void visitPatchingStub(PatchingStub stub) {
        assert compilation.target.arch.nativeMoveConstInstructionSize <= stub.bytesToCopy && stub.bytesToCopy <= 0xFF : "not enough room for call";

        // TODO: Implement this properly!
//
// Label callPatch = new Label();
//
// // static field accesses have special semantics while the class
// // initializer is being run so we emit a test which can be used to
// // check that this code is being executed by the initializing
// // thread.
// Pointer beingInitializedEntry = lir(). pc();
// if (C1XOptions.CommentedAssembly) {
// lir(). blockComment(" patch template");
// }
// if (stub.id() == PatchID.LoadKlassId) {
// // produce a copy of the load klass instruction for use by the being initialized case
// Pointer start = lir(). pc();
// Object o = null;
// lir(). movoop(stub.obj, o);
//
// if (C1XOptions.DetailedAsserts) {
// for (int i = 0; i < stub.bytesToCopy; i++) {
// Pointer ptr = (Pointer)(stub.pcStart + i);
// int aByte = (ptr) & 0xFF;
// assert aByte == *start++ : "should be the same code";
// }
// }
// } else {
// // make a copy the code which is going to be patched.
// for ( int i = 0; i < bytesToCopy; i++) {
// Pointer ptr = (Pointer)(pcStart + i);
// int aByte = (ptr) & 0xFF;
// lir(). aByte (aByte);
// *ptr = 0x90; // make the site look like a nop
// }
// }
//
// Pointer endOfPatch = lir(). pc();
// int bytesToSkip = 0;
// if (stub.id() == PatchID.LoadKlassId) {
// int offset = lir(). offset();
// if (C1XOptions.CommentedAssembly) {
// lir(). blockComment(" beingInitialized check");
// }
// assert stub.obj != Register.noreg : "must be a valid register";
// Register tmp = X86Register.rax;
// if (stub.obj == tmp) tmp = X86Register.rbx;
// lir(). push(tmp);
// lir(). getThread(tmp);
// lir(). cmpptr(tmp, new Address(stub.obj, compilation.runtime.initThreadOffsetInBytes() +
        // compilation.runtime.sizeofKlassOopDesc()));
// lir(). pop(tmp);
// lir(). jcc(X86Assembler.Condition.notEqual, callPatch);
//
// // accessField patches may execute the patched code before it's
// // copied back into place so we need to jump back into the main
// // code of the nmethod to continue execution.
// lir(). jmp(stub.patchSiteContinuation);
//
// // make sure this extra code gets skipped
// bytesToSkip += lir(). offset() - offset;
// }
// if (C1XOptions.CommentedAssembly) {
// lir(). blockComment("patch data encoded as movl");
// }
// // Now emit the patch record telling the runtime how to find the
// // pieces of the patch. We only need 3 bytes but for readability of
// // the disassembly we make the data look like a movl reg, imm32,
// // which requires 5 bytes
// int sizeofPatchRecord = 5;
// bytesToSkip += sizeofPatchRecord;
//
// // emit the offsets needed to find the code to patch
// int beingInitializedEntryOffset = lir(). pc() - beingInitializedEntry + sizeofPatchRecord;
//
// lir(). aByte(0xB8);
// lir(). aByte(0);
// lir(). aByte(beingInitializedEntryOffset);
// lir(). aByte(bytesToSkip);
// lir(). aByte(stub.bytesToCopy);
// Pointer patchInfoPc = lir(). pc();
// assert patchInfoPc - endOfPatch == bytesToSkip : "incorrect patch info";
//
// Pointer entry = lir(). pc();
// NativeGeneralJump.insertUnconditional((Pointer)pcStart, entry);
// CiRuntimeCall target = null;
// switch (stub.id()) {
// case AccessFieldId: target = CiRuntimeCall.AccessFieldPatching; break;
// case LoadKlassId: target = CiRuntimeCall.LoadKlassPatching; break;
// default: throw Util.shouldNotReachHere();
// }
// lir(). bind(callPatch);
//
// if (C1XOptions.CommentedAssembly) {
// lir(). blockComment("patch entry point");
// }
// lir(). call(new RuntimeAddress(target));
// assert patchInfoOffset == (patchInfoPc - lir(). pc()) : "must not change";
// ce.addCallInfoHere(info);
// int jmpOff = lir(). offset();
// lir(). jmp(stub.patchSiteEntry);
// // Add enough nops so deoptimization can overwrite the jmp above with a call
// // and not destroy the world.
// for (int j = lir(). offset() ; j < jmpOff + 5 ; j++ ) {
// lir(). nop();
// }
// if (stub.id() == PatchID.LoadKlassId) {
// CodeSection cs = lir(). codeSection();
// RelocIterator iter(cs, (Pointer)pcStart, (Pointer)(pcStart + 1));
// RelocInfo.Type.changeRelocInfoForAddress(&iter, (Pointer) pcStart, RelocInfo.Type.oopType, RelocInfo.Type.none);
// }
    }

    public void visitRangeCheckStub(RangeCheckStub stub) {
        masm.bind(stub.entry);
        GlobalStub stubId;
        if (stub.throwIndexOutOfBoundsException) {
            stubId = GlobalStub.ThrowIndexException;
        } else {
            stubId = GlobalStub.ThrowRangeCheckFailed;
        }

        LIROperand index = stub.index();
        if (index.isCpuRegister()) {
            masm.callGlobalStub(stubId, stub.info, CiRegister.None, index.asRegister());
        } else {
            assert index.isConstant();
            LIRConstant constantIndex = (LIRConstant) index;
            masm.callGlobalStub(stubId, stub.info, CiRegister.None, new RegisterOrConstant(constantIndex.asInt()));
        }

        ce.addCallInfoHere(stub.info);

        // Insert nop such that the IP is within the range of the target at the position after the call
        masm.nop();

        if (C1XOptions.GenerateAssertionCode) {
            masm.shouldNotReachHere();
        }
    }

    public void visitSimpleExceptionStub(SimpleExceptionStub stub) {
        masm.bind(stub.entry);
        if (stub.obj().isIllegal()) {
            masm.callGlobalStub(stub.stub, stub.info);
        } else {
            masm.callGlobalStub(stub.stub, stub.info, CiRegister.None, stub.obj().asRegister());
        }
        ce.addCallInfoHere(stub.info);

        // Insert nop such that the IP is within the range of the target at the position after the call
        masm.nop();

        if (C1XOptions.GenerateAssertionCode) {
            masm.shouldNotReachHere();
        }
    }
}
