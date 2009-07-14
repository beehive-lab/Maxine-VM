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
package com.sun.max.vm.template.generate;

import com.sun.max.annotate.*;
import com.sun.max.vm.template.*;

/**
 * The UnresolvedAtCompileTime class is used for generating a template that makes no assumption about the
 * initialization/loading state of a classes. The class is placed in a package that escapes the package loader when
 * building a VM prototype, so that when templates are generated, the UnresolvedAtCompileTime class is seen as not
 * loaded nor initialized by the prototype's optimizing compiler.
 * 
 * @author Laurent Daynes
 */
@PROTOTYPE_ONLY
public class UnresolvedAtCompileTime extends ResolvedAtCompileTime implements UnresolvedAtCompileTimeInterface {
    public static byte uninitializedMutableByteVar = 11;
    public static short uninitializedMutableShortVar = 22;
    public static char uninitializedMutableCharVar = '1';
    public static int uninitializedMutableIntVar = 33;
    public static long uninitializedMutableLongVar = 44;
    public static float uninitializedMutableFloatVar = 55;
    public static double uninitializedMutableDoubleVar = 66;
    public static boolean uninitializedMutableBooleanVar = true;
    public static  Object uninitializedMutableObjectVar = null;

    public static void parameterlessUnresolvedStaticMethod() {
        // Do enough stuff here to avoid being inlined.
        for (int i = 0; i < 1000; i++) {
            uninitializedMutableIntVar += i;
        }
    }

    public static int parameterlessUnresolvedStaticMethodReturningInt() {
        int a = 1;
        for (int i = 0; i < 1000; i++) {
            a += i;
        }
        return a;
    }

    public static long parameterlessUnresolvedStaticMethodReturningLong() {
        long a = 1;
        for (int i = 0; i < 1000; i++) {
            a += i;
        }
        return a;
    }

    public static Object parameterlessUnresolvedStaticMethodReturningReference() {
        return new Object();
    }

    public void parameterlessUnresolvedInterfaceMethod() {
        // Do enough stuff here to avoid being inlined.
        for (int i = 0; i < 1000; i++) {
            intField += i;
        }
    }
}
