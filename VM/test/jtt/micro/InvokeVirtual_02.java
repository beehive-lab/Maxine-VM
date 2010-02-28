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
/*
 * @Harness: java
 * @Runs: 0L = 0L; 1L = 11L; 2L = 22L; 3L = 42L
 */
package jtt.micro;

public class InvokeVirtual_02 {

    static class A {
        long plus(long a) {
            return a;
        }
    }

    static class B extends A {
        @Override
        long plus(long a) {
            return a + 10;
        }
    }

    static class C extends A {
        @Override
        long plus(long a) {
            return a + 20;
        }
    }

    static A objectA = new A();
    static A objectB = new B();
    static A objectC = new C();

    public static long test(long a) {
        if (a == 0) {
            return objectA.plus(a);
        }
        if (a == 1) {
            return objectB.plus(a);
        }
        if (a == 2) {
            return objectC.plus(a);
        }
        return 42;
    }
}
