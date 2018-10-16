/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.*;

/*
 * @Harness: java
 * @Runs: 1 = true
 */
public class JNI_ManyParameters {

    private static void parametersAsString(StringBuilder sb, Object rdi, int rsi, long rdx, short rcx, char r8, Object r9, int sp24, long sp16, short sp8, char sp0) {
        sb.append("" + rdi + rsi + rdx + rcx + r8 + r9 + sp24 + sp16 + sp8 + sp0);
    }

    private static native void manyParameters(Method reflectedMethod, StringBuilder sb, Object rdi, int rsi, long rdx, short rcx, char r8, Object r9, int sp24, long sp16, short sp8, char sp0);

    public static boolean test(int arg) throws Exception {
        Method reflectedMethod = JNI_ManyParameters.class.getDeclaredMethod("parametersAsString", StringBuilder.class,
                        Object.class, int.class, long.class, short.class, char.class,
                        Object.class, int.class, long.class, short.class, char.class);
        StringBuilder sb = new StringBuilder();
        manyParameters(reflectedMethod, sb, "XXX", 1, 2L, (short) 3, '4', null, 5, 6L, (short) 7, '8');
        String result = sb.toString();
        sb.setLength(0);
        parametersAsString(sb, "XXX", 1, 2L, (short) 3, '4', null, 5, 6L, (short) 7, '8');
        return sb.toString().equals(result);
    }
}
