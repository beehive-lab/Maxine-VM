/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.lang;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * A collection of static methods for doing unsigned arithmetic on Java primitive types
 * where the semantics of the arithmetics operations differ from the signed version.
 * In addition to providing unsigned arithmetic semantics for the programmer,
 * these methods also expose different optimization possibilities to the compiler as
 * well as allowing for them to be implemented as compiler builtins.
 *
 * @author Doug Simon
 */
public class Unsigned {

    /**
     * Performs unsigned integer division.
     */
    @INLINE
    @UNSAFE
    public static int idiv(int dividend, int divisor) {
        return Address.fromUnsignedInt(dividend).dividedBy(divisor).toInt();
    }

    /**
     * Performs unsigned long division.
     */
    @INLINE
    @UNSAFE
    public static long ldiv(long dividend, long divisor) {
        return Address.fromLong(dividend).dividedBy(Address.fromLong(divisor)).toLong();
    }

    /**
     * Performs unsigned integer modulus.
     */
    @INLINE
    @UNSAFE
    public static int irem(int dividend, int divisor) {
        return Address.fromUnsignedInt(dividend).remainder(divisor);
    }

    /**
     * Performs unsigned long modulus.
     */
    @INLINE
    @UNSAFE
    public static long lrem(long dividend, long divisor) {
        return Address.fromLong(dividend).remainder(Address.fromLong(divisor)).toLong();
    }

}
