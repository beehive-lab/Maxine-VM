/*
 * Copyright (c) 2009, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
    public static int idiv(int dividend, int divisor) {
        return Address.fromUnsignedInt(dividend).dividedBy(divisor).toInt();
    }

    /**
     * Performs unsigned long division.
     */
    @INLINE
    public static long ldiv(long dividend, long divisor) {
        return Address.fromLong(dividend).dividedBy(Address.fromLong(divisor)).toLong();
    }

    /**
     * Performs unsigned integer modulus.
     */
    @INLINE
    public static int irem(int dividend, int divisor) {
        return Address.fromUnsignedInt(dividend).remainder(divisor);
    }

    /**
     * Performs unsigned long modulus.
     */
    @INLINE
    public static long lrem(long dividend, long divisor) {
        return Address.fromLong(dividend).remainder(Address.fromLong(divisor)).toLong();
    }

}
