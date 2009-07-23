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
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.target.*;
import com.sun.c1x.value.*;

public class X86CodeStubVisitor implements CodeStubVisitor {

    private final X86LIRAssembler ce;
    private final X86MacroAssembler masm;
    private final C1XCompilation compilation;

    public X86CodeStubVisitor(X86LIRAssembler lirAssembler) {
        this.ce = lirAssembler;
        this.masm = lirAssembler.masm;
        this.compilation = lirAssembler.compilation;
    }

    @Override
    public void visitArrayCopyStub(ArrayCopyStub stub) {
        // ---------------slow case: call to native-----------------
        lir().bind(stub.entry);

        BasicType[] signature = new BasicType[] {BasicType.Object, BasicType.Int, BasicType.Object, BasicType.Int, BasicType.Int};
        CallingConvention cc = compilation.frameMap().javaCallingConvention(signature, true);

        // push parameters
        // (src, srcPos, dest, destPos, length)
        Register[] r = new Register[5];
        r[0] = stub.source().asRegister();
        r[1] = stub.sourcePos().asRegister();
        r[2] = stub.dest().asRegister();
        r[3] = stub.destPos().asRegister();
        r[4] = stub.length().asRegister();

        // next registers will get stored on the stack
        for (int i = 0; i < 5; i++) {
            LIROperand r1 = cc.args().get(i);
            if (r1.isStack()) {
                int stOff = r1.singleStackIx() * compilation.target.arch.wordSize;
                lir().movptr(new Address(X86Register.rsp, stOff), r[i]);
            } else {
                assert r[i] == r1.asRegister() : "Wrong register for arg ";
            }
        }

        ce.alignCall(LIROpcode.StaticCall);

        ce.emitStaticCallStub();
        lir().call(new RuntimeAddress(CiRuntimeCall.ResolveStaticCall));
        ce.addCallInfoHere(stub.info);
        lir().jmp(stub.continuation);
    }

    private X86MacroAssembler lir() {
        return masm;
    }

    @Override
    public void visitArrayStoreExceptionStub(ArrayStoreExceptionStub stub) {
        assert lir().rspOffset() == 0 : "frame size should be fixed";
        lir().bind(stub.entry);
        lir().call(new RuntimeAddress(CiRuntimeCall.ThrowArrayStoreException));
        ce.addCallInfoHere(stub.info);
        if (C1XOptions.GenerateAssertionCode) {
            lir().shouldNotReachHere();
        }
    }

    @Override
    public void visitConversionStub(ConversionStub stub) {
        lir().bind(stub.entry);
        assert stub.bytecode == Bytecodes.F2I || stub.bytecode == Bytecodes.D2I : "other conversions do not require stub";

        if (stub.input.isSingleXmm()) {
            lir().comiss(stub.input.asRegister(), new InternalAddress(masm.floatConstant(0.0f)));
        } else if (stub.input.isDoubleXmm()) {
            lir().comisd(stub.input.asRegister(), new InternalAddress(masm.doubleConstant(0.0d)));
        } else {
            assert !compilation.target.arch.is64bit();
            lir().push(X86Register.rax);
            lir().ftst();
            lir().fnstswAx();
            lir().sahf();
            lir().pop(X86Register.rax);
        }

        Label labelNaN = new Label();
        Label doReturn = new Label();
        lir().jccb(X86Assembler.Condition.parity, labelNaN);
        lir().jccb(X86Assembler.Condition.below, doReturn);

        // input is > 0 . return maxInt
        // result register already contains 0x80000000, so subtracting 1 gives 0x7fffffff
        lir().decrement(stub.result.asRegister(), 1);
        lir().jmpb(doReturn);

        // input is NaN . return 0
        lir().bind(labelNaN);
        lir().xorptr(stub.result.asRegister(), stub.result.asRegister());

        lir().bind(doReturn);
        lir().jmp(stub.continuation);

    }

    @Override
    public void visitDivByZeroStub(DivByZeroStub stub) {
        if (stub.offset != -1) {
            compilation.recordImplicitException(stub.offset, masm.offset());
        }

        masm.bind(stub.entry);
        masm.call(new RuntimeAddress(CiRuntimeCall.ThrowDiv0exception));
        ce.addCallInfoHere(stub.info);
        if (C1XOptions.GenerateAssertionCode) {
            masm.shouldNotReachHere();
        }
    }

    @Override
    public void visitImplicitNullCheckStub(ImplicitNullCheckStub stub) {
        ce.compilation.recordImplicitException(stub.offset, lir().offset());
        lir().bind(stub.entry);
        lir().call(new RuntimeAddress(CiRuntimeCall.ThrowNullPointerException));
        ce.addCallInfoHere(stub.info);
        if (C1XOptions.GenerateAssertionCode) {
            lir().shouldNotReachHere();
        }
    }

    @Override
    public void visitMonitorEnterStub(MonitorEnterStub stub) {
        assert lir().rspOffset() == 0 : "frame size should be fixed";
        lir().bind(stub.entry);
        ce.storeParameter(stub.objReg.asRegister(), 1);
        ce.storeParameter(stub.lockReg.asRegister(), 0);
        CiRuntimeCall enterId;
        if (ce.compilation.hasFpuCode()) {
            enterId = CiRuntimeCall.Monitorenter;
        } else {
            enterId = CiRuntimeCall.MonitorenterNofpu;
        }
        lir().call(new RuntimeAddress(enterId));
        ce.addCallInfoHere(stub.info);
        ce.verifyOopMap(stub.info);
        lir().jmp(stub.continuation);
    }

    @Override
    public void visitMonitorExitStub(MonitorExitStub stub) {
        lir().bind(stub.entry);
        if (stub.computeLock) {
            // lockReg was destroyed by fast unlocking attempt => recompute it
            ce.monitorAddress(stub.monitorIx, stub.lockReg);
        }
        ce.storeParameter(stub.lockReg.asRegister(), 0);
        // note: non-blocking leaf routine => no call info needed
        CiRuntimeCall enterId;
        if (ce.compilation.hasFpuCode()) {
            enterId = CiRuntimeCall.Monitorexit;
        } else {
            enterId = CiRuntimeCall.MonitorexitNofpu;
        }
        lir().call(new RuntimeAddress(enterId));
        lir().jmp(stub.continuation);
    }

    @Override
    public void visitNewInstanceStub(NewInstanceStub stub) {
        assert lir().rspOffset() == 0 : "frame size should be fixed";
        lir().bind(stub.entry);
        lir().movptr(X86Register.rdx, stub.klassReg.asRegister());
        lir().call(new RuntimeAddress(stub.stubId));
        ce.addCallInfoHere(stub.info);
        ce.verifyOopMap(stub.info);
        assert stub.result.asRegister() == X86Register.rax : "result must in X86Register.rax : ";
        lir().jmp(stub.continuation);
    }

    @Override
    public void visitNewObjectArrayStub(NewObjectArrayStub stub) {
        assert lir().rspOffset() == 0 : "frame size should be fixed";
        lir().bind(stub.entry);
        assert stub.length.asRegister() == X86Register.rbx : "length must in X86Register.rbx : ";
        assert stub.klassReg.asRegister() == X86Register.rdx : "klassReg must in X86Register.rdx";
        lir().call(new RuntimeAddress(CiRuntimeCall.NewObjectArray));
        ce.addCallInfoHere(stub.info);
        ce.verifyOopMap(stub.info);
        assert stub.result.asRegister() == X86Register.rax : "result must in X86Register.rax : ";
        lir().jmp(stub.continuation);
    }

    @Override
    public void visitNewTypeArrayStub(NewTypeArrayStub stub) {
        assert lir().rspOffset() == 0 : "frame size should be fixed";
        lir().bind(stub.entry);
        assert stub.length.asRegister() == X86Register.rbx : "length must in X86Register.rbx : ";
        assert stub.klassReg.asRegister() == X86Register.rdx : "klassReg must in X86Register.rdx";
        lir().call(new RuntimeAddress(CiRuntimeCall.NewTypeArray));
        ce.addCallInfoHere(stub.info);
        ce.verifyOopMap(stub.info);
        assert stub.result.asRegister() == X86Register.rax : "result must in X86Register.rax : ";
        lir().jmp(stub.continuation);
    }

    @Override
    public void visitPatchingStub(PatchingStub stub) {
        assert compilation.target.arch.nativeMoveConstInstructionSize <= stub.bytesToCopy && stub.bytesToCopy <= 0xFF :  "not enough room for call";

        // TODO: Implement this properly!
//
//        Label callPatch = new Label();
//
//        // static field accesses have special semantics while the class
//        // initializer is being run so we emit a test which can be used to
//        // check that this code is being executed by the initializing
//        // thread.
//        Pointer beingInitializedEntry = lir(). pc();
//        if (C1XOptions.CommentedAssembly) {
//          lir(). blockComment(" patch template");
//        }
//        if (stub.id() == PatchID.LoadKlassId) {
//          // produce a copy of the load klass instruction for use by the being initialized case
//          Pointer start = lir(). pc();
//          Object o = null;
//          lir(). movoop(stub.obj, o);
//
//          if (C1XOptions.DetailedAsserts) {
//              for (int i = 0; i < stub.bytesToCopy; i++) {
//                Pointer ptr = (Pointer)(stub.pcStart + i);
//                int aByte = (ptr) & 0xFF;
//                assert aByte == *start++ :  "should be the same code";
//              }
//          }
//        } else {
//          // make a copy the code which is going to be patched.
//          for ( int i = 0; i < bytesToCopy; i++) {
//            Pointer ptr = (Pointer)(pcStart + i);
//            int aByte = (ptr) & 0xFF;
//            lir(). aByte (aByte);
//            *ptr = 0x90; // make the site look like a nop
//          }
//        }
//
//        Pointer endOfPatch = lir(). pc();
//        int bytesToSkip = 0;
//        if (stub.id() == PatchID.LoadKlassId) {
//          int offset = lir(). offset();
//          if (C1XOptions.CommentedAssembly) {
//            lir(). blockComment(" beingInitialized check");
//          }
//          assert stub.obj != Register.noreg :  "must be a valid register";
//          Register tmp = X86Register.rax;
//          if (stub.obj == tmp) tmp = X86Register.rbx;
//          lir(). push(tmp);
//          lir(). getThread(tmp);
//          lir(). cmpptr(tmp, new Address(stub.obj, compilation.runtime.initThreadOffsetInBytes() + compilation.runtime.sizeofKlassOopDesc()));
//          lir(). pop(tmp);
//          lir(). jcc(X86Assembler.Condition.notEqual, callPatch);
//
//          // accessField patches may execute the patched code before it's
//          // copied back into place so we need to jump back into the main
//          // code of the nmethod to continue execution.
//          lir(). jmp(stub.patchSiteContinuation);
//
//          // make sure this extra code gets skipped
//          bytesToSkip += lir(). offset() - offset;
//        }
//        if (C1XOptions.CommentedAssembly) {
//          lir(). blockComment("patch data encoded as movl");
//        }
//        // Now emit the patch record telling the runtime how to find the
//        // pieces of the patch.  We only need 3 bytes but for readability of
//        // the disassembly we make the data look like a movl reg, imm32,
//        // which requires 5 bytes
//        int sizeofPatchRecord = 5;
//        bytesToSkip += sizeofPatchRecord;
//
//        // emit the offsets needed to find the code to patch
//        int beingInitializedEntryOffset = lir(). pc() - beingInitializedEntry + sizeofPatchRecord;
//
//        lir(). aByte(0xB8);
//        lir(). aByte(0);
//        lir(). aByte(beingInitializedEntryOffset);
//        lir(). aByte(bytesToSkip);
//        lir(). aByte(stub.bytesToCopy);
//        Pointer patchInfoPc = lir(). pc();
//        assert patchInfoPc - endOfPatch == bytesToSkip :  "incorrect patch info";
//
//        Pointer entry = lir(). pc();
//        NativeGeneralJump.insertUnconditional((Pointer)pcStart, entry);
//        CiRuntimeCall target = null;
//        switch (stub.id()) {
//          case AccessFieldId:  target = CiRuntimeCall.AccessFieldPatching; break;
//          case LoadKlassId:    target = CiRuntimeCall.LoadKlassPatching; break;
//          default: throw Util.shouldNotReachHere();
//        }
//        lir(). bind(callPatch);
//
//        if (C1XOptions.CommentedAssembly) {
//          lir(). blockComment("patch entry point");
//        }
//        lir(). call(new RuntimeAddress(target));
//        assert patchInfoOffset == (patchInfoPc - lir(). pc()) :  "must not change";
//        ce.addCallInfoHere(info);
//        int jmpOff = lir(). offset();
//        lir(). jmp(stub.patchSiteEntry);
//        // Add enough nops so deoptimization can overwrite the jmp above with a call
//        // and not destroy the world.
//        for (int j = lir(). offset() ; j < jmpOff + 5 ; j++ ) {
//          lir(). nop();
//        }
//        if (stub.id() == PatchID.LoadKlassId) {
//          CodeSection cs = lir(). codeSection();
//          RelocIterator iter(cs, (Pointer)pcStart, (Pointer)(pcStart + 1));
//          RelocInfo.Type.changeRelocInfoForAddress(&iter, (Pointer) pcStart, RelocInfo.Type.oopType, RelocInfo.Type.none);
//        }
    }

    @Override
    public void visitRangeCheckStub(RangeCheckStub stub) {
        lir().bind(stub.entry);
        // pass the array index on stack because all registers must be preserved
        if (stub.index.isCpuRegister()) {
            ce.storeParameter(stub.index.asRegister(), 0);
        } else {
            ce.storeParameter(stub.index.asInt(), 0);
        }
        CiRuntimeCall stubId;
        if (stub.throwIndexOutOfBoundsException) {
            stubId = CiRuntimeCall.ThrowIndexException;
        } else {
            stubId = CiRuntimeCall.ThrowRangeCheckFailed;
        }
        lir().call(new RuntimeAddress(stubId));
        ce.addCallInfoHere(stub.info);
        if (C1XOptions.GenerateAssertionCode) {
            lir().shouldNotReachHere();
        }
    }

    @Override
    public void visitSimpleExceptionStub(SimpleExceptionStub stub) {
        assert lir().rspOffset() == 0 : "frame size should be fixed";

        lir().bind(stub.entry);
        // pass the object on stack because all registers must be preserved
        if (stub.obj.isCpuRegister()) {
            ce.storeParameter(stub.obj.asRegister(), 0);
        }
        lir().call(new RuntimeAddress(stub.stub));
        ce.addCallInfoHere(stub.info);
        if (C1XOptions.GenerateAssertionCode) {
            lir().shouldNotReachHere();
        }
    }
}
