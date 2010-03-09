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
package com.sun.c1x.ir;

/**
 * The <code>InstructionVisitor</code> implements one half of the visitor
 * pattern for instructions, allowing clients to implement functionality
 * depending on the type of an instruction without doing type tests.
 *
 * @author Ben L. Titzer
 */
public abstract class ValueVisitor {
    // Checkstyle: stop
    public void visitPhi(Phi i) {}
    public void visitLocal(Local i) {}
    public void visitConstant(Constant i) {}
    public void visitResolveClass(ResolveClass i) {}
    public void visitLoadPC(LoadPC i) {}
    public void visitLoadRegister(LoadRegister i) {}
    public void visitStoreRegister(StoreRegister i) {}
    public void visitLoadPointer(LoadPointer i) {}
    public void visitStorePointer(StorePointer i) {}
    public void visitLoadField(LoadField i) {}
    public void visitStoreField(StoreField i) {}
    public void visitArrayLength(ArrayLength i) {}
    public void visitLoadIndexed(LoadIndexed i) {}
    public void visitStoreIndexed(StoreIndexed i) {}
    public void visitNegateOp(NegateOp i) {}
    public void visitArithmeticOp(ArithmeticOp i) {}
    public void visitShiftOp(ShiftOp i) {}
    public void visitLogicOp(LogicOp i) {}
    public void visitCompareOp(CompareOp i) {}
    public void visitIfOp(IfOp i) {}
    public void visitConvert(Convert i) {}
    public void visitNullCheck(NullCheck i) {}
    public void visitInvoke(Invoke i) {}
    public void visitNewInstance(NewInstance i) {}
    public void visitNewTypeArray(NewTypeArray i) {}
    public void visitNewObjectArray(NewObjectArray i) {}
    public void visitNewMultiArray(NewMultiArray i) {}
    public void visitCheckCast(CheckCast i) {}
    public void visitInstanceOf(InstanceOf i) {}
    public void visitMonitorEnter(MonitorEnter i) {}
    public void visitMonitorExit(MonitorExit i) {}
    public void visitIntrinsic(Intrinsic i) {}
    public void visitBlockBegin(BlockBegin i) {}
    public void visitGoto(Goto i) {}
    public void visitIf(If i) {}
    public void visitIfInstanceOf(IfInstanceOf i) {}
    public void visitTableSwitch(TableSwitch i) {}
    public void visitLookupSwitch(LookupSwitch i) {}
    public void visitReturn(Return i) {}
    public void visitThrow(Throw i) {}
    public void visitBase(Base i) {}
    public void visitOsrEntry(OsrEntry i) {}
    public void visitExceptionObject(ExceptionObject i) {}
    public void visitRoundFP(RoundFP i) {}
    public void visitUnsafeGetRaw(UnsafeGetRaw i) {}
    public void visitUnsafePutRaw(UnsafePutRaw i) {}
    public void visitUnsafeGetObject(UnsafeGetObject i) {}
    public void visitUnsafePutObject(UnsafePutObject i) {}
    public void visitUnsafePrefetchRead(UnsafePrefetchRead i) {}
    public void visitUnsafePrefetchWrite(UnsafePrefetchWrite i) {}
    // Checkstyle: resume
}
