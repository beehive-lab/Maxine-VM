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
 * A default implementation of {@link ValueVisitor} that simply
 * does nothing for each value visited. This convenience class
 * simplifies implementing a value visitor that is only interested
 * in a subset of the value types.
 *
 * @author Doug Simon
 */
public class DefaultValueVisitor extends ValueVisitor {

    /**
     * All the specific visitor methods in this class call this method by default.
     *
     * @param value the value being visited
     */
    protected  void visit(Value value) {
    }

    // Checkstyle: stop
    @Override public void visitArithmeticOp(ArithmeticOp i) { visit(i); }
    @Override public void visitArrayLength(ArrayLength i) { visit(i); }
    @Override public void visitBase(Base i) { visit(i); }
    @Override public void visitBlockBegin(BlockBegin i) { visit(i); }
    @Override public void visitCheckCast(CheckCast i) { visit(i); }
    @Override public void visitCompareOp(CompareOp i) { visit(i); }
    @Override public void visitConstant(Constant i) { visit(i); }
    @Override public void visitConvert(Convert i) { visit(i); }
    @Override public void visitExceptionObject(ExceptionObject i) { visit(i); }
    @Override public void visitGoto(Goto i) { visit(i); }
    @Override public void visitIf(If i) { visit(i); }
    @Override public void visitIfInstanceOf(IfInstanceOf i) { visit(i); }
    @Override public void visitIfOp(IfOp i) { visit(i); }
    @Override public void visitInstanceOf(InstanceOf i) { visit(i); }
    @Override public void visitIntrinsic(Intrinsic i) { visit(i); }
    @Override public void visitInvoke(Invoke i) { visit(i); }
    @Override public void visitLoadField(LoadField i) { visit(i); }
    @Override public void visitLoadIndexed(LoadIndexed i) { visit(i); }
    @Override public void visitLoadPC(LoadPC i) { visit(i); }
    @Override public void visitLoadPointer(LoadPointer i) { visit(i); }
    @Override public void visitLoadRegister(LoadRegister i) { visit(i); }
    @Override public void visitLocal(Local i) { visit(i); }
    @Override public void visitLogicOp(LogicOp i) { visit(i); }
    @Override public void visitLookupSwitch(LookupSwitch i) { visit(i); }
    @Override public void visitMonitorEnter(MonitorEnter i) { visit(i); }
    @Override public void visitMonitorExit(MonitorExit i) { visit(i); }
    @Override public void visitNativeCall(NativeCall i) { visit(i); }
    @Override public void visitNegateOp(NegateOp i) { visit(i); }
    @Override public void visitNewInstance(NewInstance i) { visit(i); }
    @Override public void visitNewMultiArray(NewMultiArray i) { visit(i); }
    @Override public void visitNewObjectArray(NewObjectArray i) { visit(i); }
    @Override public void visitNewTypeArray(NewTypeArray i) { visit(i); }
    @Override public void visitNullCheck(NullCheck i) { visit(i); }
    @Override public void visitOsrEntry(OsrEntry i) { visit(i); }
    @Override public void visitPhi(Phi i) { visit(i); }
    @Override public void visitResolveClass(ResolveClass i) { visit(i); }
    @Override public void visitReturn(Return i) { visit(i); }
    @Override public void visitRoundFP(RoundFP i) { visit(i); }
    @Override public void visitShiftOp(ShiftOp i) { visit(i); }
    @Override public void visitStackAllocate(StackAllocate i) { visit(i); }
    @Override public void visitStoreField(StoreField i) { visit(i); }
    @Override public void visitStoreIndexed(StoreIndexed i) { visit(i); }
    @Override public void visitStorePointer(StorePointer i) { visit(i); }
    @Override public void visitStoreRegister(StoreRegister i) { visit(i); }
    @Override public void visitTableSwitch(TableSwitch i) { visit(i); }
    @Override public void visitThrow(Throw i) { visit(i); }
    @Override public void visitUnsafeCast(UnsafeCast i) { visit(i); }
    @Override public void visitUnsafeGetObject(UnsafeGetObject i) { visit(i); }
    @Override public void visitUnsafeGetRaw(UnsafeGetRaw i) { visit(i); }
    @Override public void visitUnsafePrefetchRead(UnsafePrefetchRead i) { visit(i); }
    @Override public void visitUnsafePrefetchWrite(UnsafePrefetchWrite i) { visit(i); }
    @Override public void visitUnsafePutObject(UnsafePutObject i) { visit(i); }
    @Override public void visitUnsafePutRaw(UnsafePutRaw i) { visit(i); }
   // Checkstyle: resume
}
