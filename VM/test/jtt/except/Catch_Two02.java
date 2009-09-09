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
package jtt.except;

/*
 * @Harness: java
 * @Runs: 0="java.lang.NullPointerException"; 1="java.lang.ArithmeticException"; 3="none13"
 */
public class Catch_Two02 {

    public static String test(int arg) {
        try {
            throwSomething(arg + 10);
        } catch (NullPointerException e) {
            return e.getClass().getName();
        } catch (ArithmeticException e) {
            return e.getClass().getName();
        }
        return "none" + (arg + 10);
    }

    private static void throwSomething(int arg) {
        if (arg == 10) {
            throw new NullPointerException();
        }
        if (arg == 11) {
            throw new ArithmeticException();
        }
    }
}
