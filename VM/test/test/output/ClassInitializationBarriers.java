/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
package test.output;

/**
 * Tests that class initialization is executed at the right time.
 *
 * @author Doug Simon
 */
public class ClassInitializationBarriers {

    static int a;
    static int b;
    static int c;
    static int d;

    public static void main(String[] args) {
        INVOKESTATIC.a();
        INVOKESTATIC.a();
//        new NEW();
//        System.out.println(GETSTATIC.field + PUTSTATIC.field);
        System.out.println("az = " + INVOKESTATIC.az);
        System.out.println("bz = " + INVOKESTATIC.bz);
//        System.out.println("b = " + b);
//        System.out.println("c = " + c);
//        System.out.println("d = " + d);
    }

    /** Tests class initialization barrier for INVOKESTATIC. */
    static class INVOKESTATIC {
        static int az;
        static int bz;
        static void a() {
        }
        static {
            a = 42;
        }
    }

    /** Tests class initialization barrier for NEW. */
    static class NEW {
        static {
            b = 42;
        }
    }

    /** Tests class initialization barrier for GETSTATIC. */
    static class GETSTATIC {
        static int field;
        static {
            c = 42;
        }
    }

    /** Tests class initialization barrier for PUTSTATIC. */
    static class PUTSTATIC {
        static int field;
        static {
            d = 42;
        }
    }
}
