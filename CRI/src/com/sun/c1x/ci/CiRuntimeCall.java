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
package com.sun.c1x.ci;

import static com.sun.c1x.ci.CiKind.Boolean;
import static com.sun.c1x.ci.CiKind.Double;
import static com.sun.c1x.ci.CiKind.Float;
import static com.sun.c1x.ci.CiKind.Int;
import static com.sun.c1x.ci.CiKind.Long;
import static com.sun.c1x.ci.CiKind.Object;
import static com.sun.c1x.ci.CiKind.Void;
import static com.sun.c1x.ci.CiKind.Word;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public enum CiRuntimeCall {

    UnwindException(Void, Object),
    ThrowRangeCheckFailed(Void, Int),
    ThrowIndexException(Void, Int),
    ThrowDiv0Exception(Void),
    ThrowNullPointerException(Void),
    ThrowArrayStoreException(Void),
    ThrowClassCastException(Void, Object),
    ThrowIncompatibleClassChangeError,
    RegisterFinalizer(Void),
    NewInstance(Object, Object),
    NewArray(Object, Object, Int),
    NewMultiArray(Object, Object, Object),
    HandleException(Void, Object),
    SlowSubtypeCheck(Boolean, Object, Object),
    Monitorenter(Void, Object, Int),
    Monitorexit(Void, Object, Int),
    TraceBlockEntry(Void),
    OSRMigrationEnd(Void),
    JavaTimeMillis(Long),
    JavaTimeNanos(Long),
    OopArrayCopy(Void),
    PrimitiveArrayCopy(Void),
    ArrayCopy(Void),
    ResolveOptVirtualCall(Word, Int, Object),
    ResolveStaticCall(Word, Int, Object),
    Debug(Void),
 ResolveInterfaceIndex(
            Int, Object, Int, Object),
    RetrieveInterfaceIndex(Int, Object, Int),
    ArithmethicLrem(Long, Long, Long),
    ArithmeticLdiv(Long, Long, Long),
    ArithmeticLmul(Long, Long, Long),
    ArithmeticFrem(Float, Float),
    ArithmeticDrem(Double, Double),
    ArithmeticCos(Double, Double),
    ArithmeticTan(Double, Double),
    ArithmeticLog(Double, Double),
    ArithmeticLog10(Double, Double),
    ArithmeticSin(Double, Double),
    ResolveClass(Object, Int, Object),
    ResolveArrayClass(Object, Int, Object),
    ResolveStaticFields(Object, Int, Object),
    ResolveJavaClass(Object, Int, Object),
    ResolveFieldOffset(Int, Int, Object),
    ResolveVTableIndex(Int, Int, Object);

    public final CiKind resultType;
    public final CiKind[] arguments;

    private CiRuntimeCall() {
        resultType = Void;
        arguments = new CiKind[0];
    }

    private CiRuntimeCall(CiKind resultType, CiKind... args) {
        this.resultType = resultType;
        this.arguments = args;
    }
}
