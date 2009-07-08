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
package com.sun.c1x.lir;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.value.*;


public class LIRList {

    public LIRList(C1XCompilation compilation, BlockBegin block) {
        // TODO Auto-generated constructor stub
    }

    public void move(LIROperand src, LIROperand dest) {
        // TODO Auto-generated method stub

    }

    public List<LIRInstruction> instructionsList() {
        // TODO implement
        return null;
    }

    public void branchDestination(Label label) {
        // TODO Auto-generated method stub

    }

    public void roundfp(LIROperand opr, LIROperand illegalOperand, LIROperand result) {
        // TODO Auto-generated method stub

    }

    public void unalignedMove(LIROperand result, LIRAddress addr) {
        // TODO Auto-generated method stub

    }

    public void move(LIROperand result, LIRAddress addr) {
        // TODO Auto-generated method stub

    }

    public void load(LIROperand counter, LIROperand result) {
        // TODO Auto-generated method stub

    }

    public void store(LIROperand result, LIROperand counter) {
        // TODO Auto-generated method stub

    }

    public void add(LIROperand result, LIROperand intConst, LIROperand result2) {
        // TODO Auto-generated method stub

    }

    public void mul(LIROperand leftOp, LIROperand rightOp, LIROperand resultOp) {
        // TODO Auto-generated method stub

    }

    public void mulStrictfp(LIROperand leftOp, LIROperand rightOp, LIROperand resultOp, LIROperand tmpOp) {
        // TODO Auto-generated method stub

    }

    public void shiftLeft(LIROperand leftOp, int exactLog2, LIROperand resultOp) {
        // TODO Auto-generated method stub

    }

    public void rem(LIROperand leftOp, LIROperand rightOp, LIROperand resultOp) {
        // TODO Auto-generated method stub

    }

    public void divStrictfp(LIROperand leftOp, LIROperand rightOp, LIROperand resultOp, LIROperand tmpOp) {
        // TODO Auto-generated method stub

    }

    public void div(LIROperand leftOp, LIROperand rightOp, LIROperand resultOp) {
        // TODO Auto-generated method stub

    }

    public void sub(LIROperand leftOp, LIROperand rightOp, LIROperand resultOp) {
        // TODO Auto-generated method stub

    }

    public void shiftLeft(LIROperand value, LIROperand count, LIROperand resultOp, LIROperand tmp) {
        // TODO Auto-generated method stub

    }

    public void shiftRight(LIROperand value, LIROperand count, LIROperand resultOp, LIROperand tmp) {
        // TODO Auto-generated method stub

    }

    public void unsignedShiftRight(LIROperand value, LIROperand count, LIROperand resultOp, LIROperand tmp) {
        // TODO Auto-generated method stub

    }

    public void logicalAnd(LIROperand leftOp, LIROperand rightOp, LIROperand resultOp) {
        // TODO Auto-generated method stub

    }

    public void logicalOr(LIROperand leftOp, LIROperand rightOp, LIROperand resultOp) {
        // TODO Auto-generated method stub

    }

    public void logicalXor(LIROperand leftOp, LIROperand rightOp, LIROperand resultOp) {
        // TODO Auto-generated method stub

    }

    public void cmove(LIRCondition lirCond, LIROperand intConst, LIROperand intConst2, LIROperand dataOffsetReg) {
        // TODO Auto-generated method stub

    }

    public void leal(LIROperand fakeIncrValue, LIROperand dataReg) {
        // TODO Auto-generated method stub

    }

    public void branch(LIRCondition belowequal, BasicType i, CodeStub stub) {
        // TODO Auto-generated method stub

    }

    public void loadStackAddressMonitor(int monitorNo, LIROperand lock) {
        // TODO Auto-generated method stub

    }

    public void lockObject(LIROperand hdr, LIROperand object, LIROperand lock, LIROperand scratch, CodeStub slowPath, CodeEmitInfo infoForException) {
        // TODO Auto-generated method stub

    }

    public void unlockObject(LIROperand hdr, LIROperand object, LIROperand lock, CodeStub slowPath) {
        // TODO Auto-generated method stub

    }

    public void returnOp(LIROperand illegalOperand) {
        // TODO Auto-generated method stub

    }

    public void move(LIROperand lirAddress, LIROperand result, CodeEmitInfo info) {
        // TODO Auto-generated method stub

    }

    public void nullCheck(LIROperand result, CodeEmitInfo codeEmitInfo) {
        // TODO Auto-generated method stub

    }

    public void membarRelease() {
        // TODO Auto-generated method stub

    }

    public void store(LIROperand result, LIROperand address, CodeEmitInfo info, LIRPatchCode patchCode) {
        // TODO Auto-generated method stub

    }

    public void membar() {
        // TODO Auto-generated method stub

    }

    public void membarAcquire() {
        // TODO Auto-generated method stub

    }

    public void load(LIROperand address, LIROperand reg, CodeEmitInfo info, LIRPatchCode patchCode) {
        // TODO Auto-generated method stub

    }

    public void oop2reg(Object object, LIROperand meth) {
        // TODO Auto-generated method stub

    }

    public void unwindException(LIROperand illegalOperand, LIROperand exceptionOopOpr, CodeEmitInfo info) {
        // TODO Auto-generated method stub

    }

    public void convert(int bytecode, LIROperand result, LIROperand baseOp) {
        // TODO Auto-generated method stub

    }

    public void unalignedMove(LIROperand addr, LIROperand reg) {
        // TODO Auto-generated method stub

    }

    public void prefetch(LIROperand addr, boolean isStore) {
        // TODO Auto-generated method stub

    }

    public void cmp(LIRCondition equal, LIROperand value, int lowKey) {
        // TODO Auto-generated method stub

    }

    public void branch(LIRCondition equal, BasicType i, BlockBegin dest) {
        // TODO Auto-generated method stub

    }

    public void jump(BlockBegin defaultSux) {
        // TODO Auto-generated method stub

    }

    public void safepoint(LIROperand safepointPollRegister, CodeEmitInfo stateFor) {
        // TODO Auto-generated method stub

    }

    public void stdEntry(LIROperand illegalOperand) {
        // TODO Auto-generated method stub

    }

    public void callRuntime(Address entry, LIROperand threadTemp, LIROperand physReg, List<LIROperand> args, CodeEmitInfo info) {
        // TODO Auto-generated method stub

    }

    public void callRuntimeLeaf(Address entry, LIROperand threadTemp, LIROperand physReg, List<LIROperand> args) {
        // TODO Auto-generated method stub

    }

    public void allocateObject(LIROperand dst, LIROperand scratch1, LIROperand scratch2, LIROperand scratch3, LIROperand scratch4, int headerSize, int instanceSize, LIROperand klassReg, boolean b,
                    CodeStub slowPath) {
        // TODO Auto-generated method stub

    }

    public void oop2regPatch(Object object, LIROperand reg, CodeEmitInfo info) {
        // TODO Auto-generated method stub

    }

    public void cmp(LIRCondition belowequal, LIROperand result, LIROperand result2) {
        // TODO Auto-generated method stub

    }

    public void throwException(LIROperand exceptionPcOpr, LIROperand exceptionOopOpr, CodeEmitInfo info) {
        // TODO Auto-generated method stub

    }

    public void profileCall(CiMethod method, int bciOfInvoke, LIROperand mdo, LIROperand recv, LIROperand tmp, CiType knownHolder) {
        // TODO Auto-generated method stub

    }

    public void branch(LIRCondition less, Label l) {
        // TODO Auto-generated method stub

    }

    public void osrEntry(LIROperand osrBufferPointer) {
        // TODO Auto-generated method stub

    }

    public void callVirtual(CiMethod target, LIROperand receiver, LIROperand resultRegister, int vtableOffset, List<LIROperand> argList, CodeEmitInfo info) {
        // TODO Auto-generated method stub

    }

    public void callStatic(CiMethod target, LIROperand resultRegister, CodeStub resolveStaticCallStub, List<LIROperand> argList, CodeEmitInfo info) {
        // TODO Auto-generated method stub

    }

    public void callOptVirtual(CiMethod target, LIROperand receiver, LIROperand resultRegister, CodeStub resolveOptVirtualCallStub, List<LIROperand> argList, CodeEmitInfo info) {
        // TODO Auto-generated method stub

    }

    public void callIcvirtual(CiMethod target, LIROperand receiver, LIROperand resultRegister, CodeStub resolveVirtualCallStub, List<LIROperand> argList, CodeEmitInfo info) {
        // TODO Auto-generated method stub

    }


}
