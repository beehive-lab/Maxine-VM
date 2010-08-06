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
package jtt.micro;

/*
 * @Harness: java
 * @Runs: (true, "0", "1", "2", "3", "4", "5", "6", "7", "8", "9") = "A0123456789";
 * @Runs: (false, "0", "1", "2", "3", "4", "5", "6", "7", "8", "9") = "B0123456789"
 */
public class BigVirtualParams01 {
    public static String test(boolean b, String p0, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9) {
        I i = b ? new A() : new B();
        return i.test(p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    abstract static class I {
        abstract String test(String p0, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9);
    }

    static class A extends I {
        @Override
        public String test(String p0, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9) {
            return "A" + p0 + p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;
        }
    }

    static class B extends I {
        @Override
        public String test(String p0, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9) {
            return "B" + p0 + p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;
        }
    }
}
