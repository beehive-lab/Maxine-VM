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
/*VCSID=01abac33-b8cd-4486-a40d-b9e8340055aa*/
package com.sun.max.vm.template;

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
    public static byte _initializedMutableByteVar = 11;
    public static short _initializedMutableShortVar = 22;
    public static char _initializedMutableCharVar = '1';
    public static int _initializedMutableIntVar = 33;
    public static long _initializedMutableLongVar = 44;
    public static float _initializedMutableFloatVar = 55;
    public static double _initializedMutableDoubleVar = 66;
    public static boolean _initializedMutableBooleanVar = true;
    public static  Object _initializedMutableObjectVar = null;

    /*
     * The following instance variables are used solely for generating template for get/put field bytecodes
     */
    public int _intField;
    public byte _byteField;
    public char _charField;
    public short _shortField;
    public long _longField;
    public float _floatField;
    public boolean _booleanField;
    public double _doubleField;
    public Object _objField;
    public Word _wordField;

    public void parameterlessMethod() {
        // Do enough stuff here to avoid being inlined.
        for (int i = 0; i < 1000; i++) {
            _intField += i;
        }
    }

    public static void parameterlessResolvedStaticMethod() {
        // Do enough stuff here to avoid being inlined.
        for (int i = 0; i < 1000; i++) {
            _initializedMutableIntVar += i;
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
            _initializedMutableIntVar += i;
        }
    }
}
