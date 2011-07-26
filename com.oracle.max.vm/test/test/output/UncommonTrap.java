/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.output;

import java.util.*;

import com.sun.cri.bytecode.Bytecodes.Infopoints;

/**
 * Simple test for uncommon trap support.
 * On Maxine, the -XX:+TraceDeopt flag will force a GC during deopt which should
 * force the saved object registers to be updated.
 */
public class UncommonTrap {
    public static void main(String[] args) {
        System.out.println("result1: " + Arrays.toString(values(false)));
        for (int i = 0; i < 100000; i++) {
            values(true);
        }
        // By now 'values()' should have been recompiled.
        System.out.println("result2: " + Arrays.toString(values(false)));
    }

    private static Object[] values(boolean warmup) {
        if (warmup) {
            return null;
        }
        Object o1 = new String("obj1");
        Object o2 = new String("obj2");
        Object o3 = new String("obj3");
        Object o4 = new String("obj4");
        Object o5 = new String("obj5");
        Object o6 = new String("obj6");
        Object o7 = new String("obj7");
        Object o8 = new String("obj8");
        Object o9 = new String("obj9");

        Infopoints.uncommonTrap();

        return new Object[] {o1, o2, o3, o4, o5, o6, o7, o8, o9};
    }
}
