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
package com.sun.c1x.globalstub;

import static com.sun.c1x.ci.CiKind.*;

import com.sun.c1x.ci.*;

/**
 * A global stub is a shared routine that performs an operation on behalf of compiled code.
 * Typically the routine is too large to inline or requires runtime support.
 *
 * @author Thomas Wuerthinger
 */
public enum GlobalStub {
    ThrowArrayIndexOutOfBoundsException(Void, Int),
    ThrowArithmeticException(Void),
    ThrowNullPointerException(Void),
    ThrowArrayStoreException(Void),
    ThrowClassCastException(Void, Object),
    ThrowIncompatibleClassChangeError,
    NewInstance(Object, Object),
    MonitorEnter(Void, Object, Int),
    MonitorExit(Void, Object, Int),
    ArithmethicLrem(Long, Long, Long),
    ArithmeticLdiv(Long, Long, Long),
    ArithmeticLmul(Long, Long, Long),
    ArithmeticFrem(Float, Float),
    ArithmeticDrem(Double, Double),

    f2i(Int, Float),
    fneg(Float, Float),
    dneg(Double, Double),
    f2l(Long, Float),
    d2i(Int, Double),
    d2l(Long, Double);

    public final CiKind resultType;
    public final CiKind[] arguments;

    private GlobalStub() {
        resultType = Void;
        arguments = new CiKind[0];
    }

    private GlobalStub(CiKind resultType, CiKind... args) {
        this.resultType = resultType;
        this.arguments = args;
    }
}
