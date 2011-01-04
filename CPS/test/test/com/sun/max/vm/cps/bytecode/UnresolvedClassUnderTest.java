/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.cps.bytecode;

import com.sun.max.unsafe.*;

/**
 * This is a class used to test the case where the compiled method
 * includes code that has symbolic references to members of a class
 * that is unresolved at compile time. The way to use it is to
 * include use of a member of the class in the method being compiled.
 * You should be careful to avoid loading the UnresolvedClassUnderTest
 * from within the prototype under test.
 * We leave the instance variables intentionally "package visible" so the
 * method being compiled can access without method invocation for
 * testing purposes.
 * 
 * @see BytecodeTest_getfield
 * @author Laurent Daynes
 */
public class UnresolvedClassUnderTest {
    byte byteField = 111;
    boolean booleanField = true;
    short shortField = 333;
    char charField = 444;
    int intField = 55;
    long longField = 77L;
    float floatField = 6.6F;
    double doubleField = 8.8;
    Word wordField;
    Object referenceField = this;

    public int getIntField() {
        return intField;
    }

    public static void unresolvedStaticMethod() {
        // Do enough stuff here to avoid being inlined.
        int j = 1;
        for (int i = 0; i < 1000; i++) {
            j = j * 2 + 1;
        }
    }
    public static void unresolvedStaticMethod(int k) {
        // Do enough stuff here to avoid being inlined.
        int j = 1;
        for (int i = 0; i < k; i++) {
            j = j * 2 + (k - i);
        }
    }
}
