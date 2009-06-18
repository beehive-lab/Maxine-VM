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
package com.sun.c1x.util;

import com.sun.c1x.ir.*;

/**
 * The <code>InstructionVisitor</code> implements one half of the visitor
 * pattern for instructions, allowing clients to implement functionality
 * depending on the type of an instruction without doing type tests.
 *
 * @author Ben L. Titzer
 */
public interface InstructionVisitor {
    void visitPhi(Phi i);
    void visitLocal(Local i);
    void visitConstant(Constant i);
    void visitLoadField(LoadField i);
    void visitStoreField(StoreField i);
    void visitArrayLength(ArrayLength i);
    void visitLoadIndexed(LoadIndexed i);
    void visitStoreIndexed(StoreIndexed i);
    void visitNegateOp(NegateOp i);
    void visitArithmeticOp(ArithmeticOp i);
    void visitShiftOp(ShiftOp i);
    void visitLogicOp(LogicOp i);
    void visitCompareOp(CompareOp i);
    void visitIfOp(IfOp i);
    void visitConvert(Convert i);
    void visitNullCheck(NullCheck i);
    void visitInvoke(Invoke i);
    void visitNewInstance(NewInstance i);
    void visitNewTypeArray(NewTypeArray i);
    void visitNewObjectArray(NewObjectArray i);
    void visitNewMultiArray(NewMultiArray i);
    void visitCheckCast(CheckCast i);
    void visitInstanceOf(InstanceOf i);
    void visitMonitorEnter(MonitorEnter i);
    void visitMonitorExit(MonitorExit i);
    void visitIntrinsic(Intrinsic i);
    void visitBlockBegin(BlockBegin i);
    void visitGoto(Goto i);
    void visitIf(If i);
    void visitIfInstanceOf(IfInstanceOf i);
    void visitTableSwitch(TableSwitch i);
    void visitLookupSwitch(LookupSwitch i);
    void visitReturn(Return i);
    void visitThrow(Throw i);
    void visitBase(Base i);
    void visitOsrEntry(OsrEntry i);
    void visitExceptionObject(ExceptionObject i);
    void visitRoundFP(RoundFP i);
    void visitUnsafeGetRaw(UnsafeGetRaw i);
    void visitUnsafePutRaw(UnsafePutRaw i);
    void visitUnsafeGetObject(UnsafeGetObject i);
    void visitUnsafePutObject(UnsafePutObject i);
    void visitUnsafePrefetchRead(UnsafePrefetchRead i);
    void visitUnsafePrefetchWrite(UnsafePrefetchWrite i);
    void visitProfileCall(ProfileCall i);
    void visitProfileCounter(ProfileCounter i);
}
