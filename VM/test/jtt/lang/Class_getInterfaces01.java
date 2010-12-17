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
package jtt.lang;

/*
 * @Harness: java
 * @Runs: 0 = "";
 * @Runs: 1 = "jtt.lang.Class_getInterfaces01$I1";
 * @Runs: 2 = "jtt.lang.Class_getInterfaces01$I1";
 * @Runs: 3 = "jtt.lang.Class_getInterfaces01$I2";
 * @Runs: 4 = "jtt.lang.Class_getInterfaces01$I1 jtt.lang.Class_getInterfaces01$I2";
*/

public final class Class_getInterfaces01 {
    private Class_getInterfaces01() {
    }

    public static String test(int i)  {
        switch (i) {
            case 0:
                return toString(I1.class);
            case 1:
                return toString(I2.class);
            case 2:
                return toString(C1.class);
            case 3:
                return toString(C2.class);
            case 4:
                return toString(C12.class);
            default:
                return null;
        }
    }

    private static String toString(Class<?> klass) {
        final Class<?>[] classes = klass.getInterfaces();
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Class<?> c : classes) {
            if (!first) {
                sb.append(' ');
            } else {
                first = false;
            }
            sb.append(c.getName());
        }
        return sb.toString();
    }

    static interface I1 {

    }

    static interface I2 extends I1 {

    }

    static class C1 implements I1 {

    }

    static class C2 implements I2 {

    }

    static class C12 implements I1, I2 {

    }

}
