/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package jtt.jni;

import java.util.*;

import com.sun.max.vm.value.*;

/*
 * @Harness: java
 * @Runs: 1 = true
 */
public class JNI_ManyObjectParameters {

    public static boolean test(int arg) {
        final int numberOfParameters = 56;
        final Object[] arguments = new Object[numberOfParameters + 1];
        final Object[] array = new Object[numberOfParameters];
        arguments[0] = ReferenceValue.from(array);
        for (int i = 0; i != numberOfParameters; ++i) {
            arguments[i + 1] = ReferenceValue.from("object" + i);
        }

        Object[] result = manyObjectParameters(arguments,
            arguments[0],
            arguments[1],
            arguments[2],
            arguments[3],
            arguments[4],
            arguments[5],
            arguments[6],
            arguments[7],
            arguments[8],
            arguments[9],
            arguments[10],
            arguments[11],
            arguments[12],
            arguments[13],
            arguments[14],
            arguments[15],
            arguments[16],
            arguments[17],
            arguments[18],
            arguments[19],
            arguments[20],
            arguments[21],
            arguments[22],
            arguments[23],
            arguments[24],
            arguments[25],
            arguments[26],
            arguments[27],
            arguments[28],
            arguments[29],
            arguments[30],
            arguments[31],
            arguments[32],
            arguments[33],
            arguments[34],
            arguments[35],
            arguments[36],
            arguments[37],
            arguments[38],
            arguments[39],
            arguments[40],
            arguments[41],
            arguments[42],
            arguments[43],
            arguments[44],
            arguments[45],
            arguments[46],
            arguments[47],
            arguments[48],
            arguments[49],
            arguments[50],
            arguments[51],
            arguments[52],
            arguments[53],
            arguments[54],
            arguments[55]);

        return Arrays.equals(result, arguments);
    }

    public static native Object[] manyObjectParameters(
                    Object[] array,
                    Object object0,
                    Object object1,
                    Object object2,
                    Object object3,
                    Object object4,
                    Object object5,
                    Object object6,
                    Object object7,
                    Object object8,
                    Object object9,
                    Object object10,
                    Object object11,
                    Object object12,
                    Object object13,
                    Object object14,
                    Object object15,
                    Object object16,
                    Object object17,
                    Object object18,
                    Object object19,
                    Object object20,
                    Object object21,
                    Object object22,
                    Object object23,
                    Object object24,
                    Object object25,
                    Object object26,
                    Object object27,
                    Object object28,
                    Object object29,
                    Object object30,
                    Object object31,
                    Object object32,
                    Object object33,
                    Object object34,
                    Object object35,
                    Object object36,
                    Object object37,
                    Object object38,
                    Object object39,
                    Object object40,
                    Object object41,
                    Object object42,
                    Object object43,
                    Object object44,
                    Object object45,
                    Object object46,
                    Object object47,
                    Object object48,
                    Object object49,
                    Object object50,
                    Object object51,
                    Object object52,
                    Object object53,
                    Object object54,
                    Object object55);
}
