/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
