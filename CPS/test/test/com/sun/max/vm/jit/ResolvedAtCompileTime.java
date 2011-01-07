/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.jit;

import com.sun.max.unsafe.*;

/**
 * @author Laurent Daynes
 */
public class ResolvedAtCompileTime implements ResolvedAtCompileTimeInterface {

    /*
     * The following static variables are used solely for generating template for the ldc bytecode.
     * A constant int value larger than what can be encoded in a short must be used to make sure
     * access to it compiles to a ldc bytecode. Otherwise, a compiler may use a bipush or sipush instead.
     */
    public static final int CONSTANT_INT = 333333333;
    public static final float CONSTANT_FLOAT = 33.33F;
    public static final long CONSTANT_LONG = 333333333333333333L;
    public static final double CONSTANT_DOUBLE = 33333333333333.3333D;

    /*
     * The following static variables are used solely for generating template for get/put static bytecodes
     */
    public static byte initializedMutableByteVar = 11;
    public static short initializedMutableShortVar = 22;
    public static char initializedMutableCharVar = '1';
    public static int initializedMutableIntVar = 33;
    public static long initializedMutableLongVar = 44;
    public static float initializedMutableFloatVar = 55;
    public static double initializedMutableDoubleVar = 66;
    public static boolean initializedMutableBooleanVar = true;
    public static  Object initializedMutableObjectVar = null;

    /*
     * The following instance variables are used solely for generating template for get/put field bytecodes
     */
    public int intField;
    public byte byteField;
    public char charField;
    public short shortField;
    public long longField;
    public float floatField;
    public boolean booleanField;
    public double doubleField;
    public Object objField;
    public Word wordField;

    public void parameterlessMethod() {
        // Do enough stuff here to avoid being inlined.
        for (int i = 0; i < 1000; i++) {
            intField += i;
        }
    }

    public static void parameterlessResolvedStaticMethod() {
        // Do enough stuff here to avoid being inlined.
        for (int i = 0; i < 1000; i++) {
            initializedMutableIntVar += i;
        }
    }
    public static int parameterlessResolvedStaticMethodReturningInt() {
        int a = 1;
        for (int i = 0; i < 1000; i++) {
            a += i;
        }
        return a;
    }

    public static long parameterlessResolvedStaticMethodReturningLong() {
        long a = 1;
        for (int i = 0; i < 1000; i++) {
            a += i;
        }
        return a;
    }

    public static Object parameterlessResolvedStaticMethodReturningReference() {
        return new Object();
    }

    public void parameterlessResolvedInterfaceMethod() {
        // Do enough stuff here to avoid being inlined.
        for (int i = 0; i < 1000; i++) {
            initializedMutableIntVar += i;
        }
    }
}
