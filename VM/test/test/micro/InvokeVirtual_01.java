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
 * @Runs: 0 = 0; 1 = 11; 2 = 22; 3 = 42
 */
package test.micro;

public class InvokeVirtual_01 {

    static class A {
        int plus(int a) {
            return a;
        }
    }

    static class B extends A {
        @Override
        int plus(int a) {
            return a + 10;
        }
    }

    static class C extends A {
        @Override
        int plus(int a) {
            return a + 20;
        }
    }

    static A _a = new A();
    static A _b = new B();
    static A _c = new C();

    public static int test(int a) {
        if (a == 0) {
            return _a.plus(a);
        }
        if (a == 1) {
            return _b.plus(a);
        }
        if (a == 2) {
            return _c.plus(a);
        }
        return 42;
    }
}
