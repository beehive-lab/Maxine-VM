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
 * The purpose of this class is solely for testing cases JIT-compilation of bytecode with operands to class that aren't resolved
 * at compile-time. We need to use a class different than UnresolvedAtCompileTime to make sure the patching of literal references to
 * the class by the template-based JIT compiler are effective.
 *
 * @author Laurent Daynes
 */
@HOSTED_ONLY
public class UnresolvedAtTestTime extends UnresolvedAtCompileTime {
    public static byte staticByteField;
    public static boolean staticBooleanField;
    public static char staticCharField;
    public static short staticShortField;
    public static int staticIntField;
    public static float staticFloatField;
    public static long staticLongField;
    public static double staticDoubleField;
    public static Object staticObjectField;

    public int intField2;
    public byte byteField2;
    public char charField2;
    public short shortField2;
    public long longField2;
    public float floatField2;
    public boolean booleanField2;
    public double doubleField2;
    public Object objField2;

    public int getInt() {
        return intField2;
    }

    public void updateInt(int i1, int i2) {
        intField2 = i1 << i2;
    }

    public static int staticGetInt() {
        return staticIntField;
    }
}
