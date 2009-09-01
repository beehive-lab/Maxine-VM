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
package jtt.optimize;

/*
 * Tests constant folding of integer operations.
 * @Harness: java
 * @Runs: 0=18; 1=18; 2=!java.lang.NullPointerException
 */
public class VN_Cast01 {
    static final Object object = new VN_Cast01();

    int field = 9;

    public static int test(int arg) {
        if (arg == 0) {
            return test1();
        }
        if (arg == 1) {
            return test2();
        }
        if (arg == 2) {
            return test3();
        }
        return 0;
    }

    private static int test1() {
        Object o = object;
        VN_Cast01 a = (VN_Cast01) o;
        VN_Cast01 b = (VN_Cast01) o;
        return a.field + b.field;
    }

    private static int test2() {
        Object obj = new VN_Cast01();
        VN_Cast01 a = (VN_Cast01) obj;
        VN_Cast01 b = (VN_Cast01) obj;
        return a.field + b.field;
    }

    private static int test3() {
        Object o = null;
        VN_Cast01 a = (VN_Cast01) o;
        VN_Cast01 b = (VN_Cast01) o;
        return a.field + b.field;
    }
}
