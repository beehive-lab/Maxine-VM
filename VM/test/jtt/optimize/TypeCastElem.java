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
package jtt.optimize;

/*
 * @Harness: java
 * @Runs: (10, 13, 25) = 27183
 */
public class TypeCastElem {

    interface Int1 {

        int do1();
    }

    interface Int2 {

        int do2();
    }

    interface Int3 extends Int1 {

        int do3();
    }

    public static class ClassA implements Int1 {

        private int a;

        public ClassA(int a) {
            this.a = a;
        }

        public int do1() {
            return a;
        }
    }

    public static class ClassB extends ClassA implements Int2 {

        int b;

        public ClassB(int a, int b) {
            super(a);
            this.b = b;
        }

        public int do2() {
            return b;
        }
    }

    public static class ClassC implements Int3 {

        private int a;
        private int b;

        public ClassC(int a, int b) {
            this.a = a;
            this.b = b;
        }

        public int do3() {
            return b;
        }

        public int do1() {
            return a;
        }

    }

    public static int test1(Object o) {
        if (o instanceof ClassB) {
            ClassB b = (ClassB) o;
            if (o instanceof Int1) {
                return b.b - b.b + 1;
            }
            return 7;
        }
        return 3;
    }

    public static int test2(Object o) {
        Object b = o;
        if (o instanceof ClassB) {
            ClassA a = (ClassA) o;
            if (b instanceof Int1) {
                return ((Int1) a).do1();
            }
            return 7;
        }
        return 3;
    }

    public static int test3(Object o) {
        Object b = o;
        boolean t = o instanceof Int3;
        if (t) {
            Int1 a = (Int1) b;
            return a.do1();
        }
        return 3;
    }

    public static int test(int a, int b, int c) {
        ClassA ca = new ClassA(a);
        ClassB cb = new ClassB(a, b);
        ClassC cc = new ClassC(c, c);
        int sum1 = test1(ca) + test1(cb) * 10 + test1(cc) * 100;
        int sum2 = test2(ca) + test2(cb) * 10 + test2(cc) * 100;
        int sum3 = test3(ca) + test3(cb) * 10 + test3(cc) * 100;
        int result = sum1 * 5 + sum2 * 7 + sum3 * 9;
        return result;
    }

    public static void main(String[] args) {
        System.out.println(test(10, 13, 25));
    }
}
