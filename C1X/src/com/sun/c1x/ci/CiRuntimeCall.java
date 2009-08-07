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

import static com.sun.c1x.value.BasicType.*;

import com.sun.c1x.value.*;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public enum CiRuntimeCall {

    UnwindException,
    ThrowRangeCheckFailed,
    ThrowIndexException,
    ThrowDiv0Exception,
    ThrowNullPointerException,
    RegisterFinalizer,
    NewInstance(Object, Object),
    NewArray(Object, Object, Int),
    NewMultiArray(Object, Object, Object),
    HandleException,
    ThrowArrayStoreException,
    ThrowClassCastException,
    ThrowIncompatibleClassChangeError,
    SlowSubtypeCheck(Boolean, Object, Object),
    Monitorenter,
    Monitorexit,
    TraceBlockEntry,
    OSRMigrationEnd,
    JavaTimeMillis,
    JavaTimeNanos,
    OopArrayCopy,
    PrimitiveArrayCopy,
    ArrayCopy,
    ResolveOptVirtualCall,
    ResolveStaticCall,
    ResolveVirtualCall,
    Debug,
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
    ArithmeticSin(Double, Double);

    public final BasicType resultType;
    public final BasicType[] arguments;

    private CiRuntimeCall() {
        resultType = Void;
        arguments = new BasicType[0];
    }

    private CiRuntimeCall(BasicType resultType, BasicType... args) {
        this.resultType = resultType;
        this.arguments = args;
    }
}
