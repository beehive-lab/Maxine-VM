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
 * The {@link ValueVisitor} implements one half of the visitor
 * pattern for {@linkplain Value IR values}, allowing clients to implement functionality
 * depending on the type of an value without doing type tests.
 *
 * @author Ben L. Titzer
 */
public abstract class ValueVisitor {
    // Checkstyle: stop
    public abstract void visitArithmeticOp(ArithmeticOp i);
    public abstract void visitArrayLength(ArrayLength i);
    public abstract void visitBase(Base i);
    public abstract void visitBlockBegin(BlockBegin i);
    public abstract void visitBreakpointTrap(BreakpointTrap i);
    public abstract void visitCheckCast(CheckCast i);
    public abstract void visitCompareOp(CompareOp i);
    public abstract void visitCompareAndSwap(CompareAndSwap i);
    public abstract void visitConstant(Constant i);
    public abstract void visitConvert(Convert i);
    public abstract void visitExceptionObject(ExceptionObject i);
    public abstract void visitGoto(Goto i);
    public abstract void visitIf(If i);
    public abstract void visitIfInstanceOf(IfInstanceOf i);
    public abstract void visitIfOp(IfOp i);
    public abstract void visitInstanceOf(InstanceOf i);
    public abstract void visitIntrinsic(Intrinsic i);
    public abstract void visitInvoke(Invoke i);
    public abstract void visitLoadField(LoadField i);
    public abstract void visitLoadIndexed(LoadIndexed i);
    public abstract void visitLoadPC(LoadPC i);
    public abstract void visitLoadPointer(LoadPointer i);
    public abstract void visitLoadStackAddress(AllocateStackVariable i);
    public abstract void visitLoadRegister(LoadRegister i);
    public abstract void visitLocal(Local i);
    public abstract void visitLogicOp(LogicOp i);
    public abstract void visitLookupSwitch(LookupSwitch i);
    public abstract void visitMemoryBarrier(MemoryBarrier memoryBarrier);
    public abstract void visitMonitorAddress(MonitorAddress monitorAddress);
    public abstract void visitMonitorEnter(MonitorEnter i);
    public abstract void visitMonitorExit(MonitorExit i);
    public abstract void visitNativeCall(NativeCall i);
    public abstract void visitNegateOp(NegateOp i);
    public abstract void visitNewInstance(NewInstance i);
    public abstract void visitNewMultiArray(NewMultiArray i);
    public abstract void visitNewObjectArray(NewObjectArray i);
    public abstract void visitNewTypeArray(NewTypeArray i);
    public abstract void visitNullCheck(NullCheck i);
    public abstract void visitOsrEntry(OsrEntry i);
    public abstract void visitPause(Pause i);
    public abstract void visitPhi(Phi i);
    public abstract void visitResolveClass(ResolveClass i);
    public abstract void visitReturn(Return i);
    public abstract void visitShiftOp(ShiftOp i);
    public abstract void visitSignificantBit(SignificantBitOp i);
    public abstract void visitStackAllocate(StackAllocate i);
    public abstract void visitStoreField(StoreField i);
    public abstract void visitStoreIndexed(StoreIndexed i);
    public abstract void visitStorePointer(StorePointer i);
    public abstract void visitStoreRegister(StoreRegister i);
    public abstract void visitTableSwitch(TableSwitch i);
    public abstract void visitThrow(Throw i);
    public abstract void visitUnsafeCast(UnsafeCast i);
    public abstract void visitUnsafeGetObject(UnsafeGetObject i);
    public abstract void visitUnsafeGetRaw(UnsafeGetRaw i);
    public abstract void visitUnsafePrefetchRead(UnsafePrefetchRead i);
    public abstract void visitUnsafePrefetchWrite(UnsafePrefetchWrite i);
    public abstract void visitUnsafePutObject(UnsafePutObject i);
    public abstract void visitUnsafePutRaw(UnsafePutRaw i);
    public abstract void visitUnsignedCompareOp(UnsignedCompareOp i);
}
