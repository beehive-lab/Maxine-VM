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

/**
 * The {@code LirOpcode} enum represents the Operation code of each LIR instruction.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public enum LIROpcode {
    // Checkstyle: stop
    // @formatter:off
    BeginOp0,
        Label,
        StdEntry,
        OsrEntry,
        ReadPC,
        Alloca,
        Breakpoint,
        Pause,
        RuntimeCall,
        Membar,
        MembarAcquire,
        MembarRelease,
    EndOp0,
    BeginOp1,
        NullCheck,
        Return,
        Lea,
        Neg,
        Branch,
        CondFloatBranch,
        Move,
        Prefetchr,
        Prefetchw,
        Convert,
        Lsb,
        Msb,
        MonitorAddress,
    EndOp1,
    BeginOp2,
        Cmp,
        Cmpl2i,
        Ucmpfd2i,
        Cmpfd2i,
        Cmove,
        Add,
        Sub,
        Mul,
        Div,
        Rem,
        Sqrt,
        Abs,
        Sin,
        Cos,
        Tan,
        Log,
        Log10,
        LogicAnd,
        LogicOr,
        LogicXor,
        Shl,
        Shr,
        Ushr,
        Throw,
        Unwind,
        CompareTo,
    EndOp2,
    BeginOp3,
        Idiv,
        Irem,
        Ldiv,
        Lrem,
        Wdiv,
        Wdivi,
        Wrem,
        Wremi,
    EndOp3,
    NativeCall,
    DirectCall,
    IndirectCall,
    InstanceOf,
    CheckCast,
    StoreCheck,
    CasLong,
    CasWord,
    CasObj,
    CasInt,
    Xir,
    // @formatter:on
    // Checkstyle: resume
}
