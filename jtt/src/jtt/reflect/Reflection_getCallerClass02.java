/*
 * Copyright (c) 2017, 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 */
package jtt.reflect;

import sun.reflect.*;

/*
 * @Harness: java
 * @Runs: 0 = "jtt.reflect.Reflection_getCallerClass02$Caller2"; 1 ="jtt.reflect.Reflection_getCallerClass02"
 */
public final class Reflection_getCallerClass02 {
    private Reflection_getCallerClass02() {
    }

    public static final class Caller1 {
        private Caller1() {
        }

        @CallerSensitive
        static String caller1() {
            return Reflection.getCallerClass().getName();
        }
    }

    public static final class Caller2 {
        private Caller2() {
        }

        static String caller2() {
            return Caller1.caller1();
        }
    }

    public static String test(int argument) {
        if (argument == 0) {
            return Caller2.caller2();
        } else {
            return Caller1.caller1();
        }
    }
}
