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

import com.sun.max.annotate.*;

/**
 * The UnresolvedAtCompileTime class is used for generating a template that makes no assumption about the
 * initialization/loading state of a classes. The class is placed in a package that escapes the package loader when
 * building a VM prototype, so that when templates are generated, the UnresolvedAtCompileTime class is seen as not
 * loaded nor initialized by the prototype's optimizing compiler.
 *
 * @author Laurent Daynes
 */
@HOSTED_ONLY
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
