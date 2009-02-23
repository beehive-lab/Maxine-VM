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


package test.except;

/*
 * @Harness: java
 * @Runs: 0 = 0; 1 = 1
 */

public class Catch_NPE_07 {
    public static class MyThrowable extends Throwable {
    }

    public static int foo(Throwable t) {
        try {
            throw t;
        } catch (Throwable t1) {
            if (t1 == null) {
                return -1;
            }
            if (t1 instanceof NullPointerException) {
                return 0;
            }
            if (t1 instanceof MyThrowable) {
                return 1;
            }
            return -2;
        }
    }

    public static int test(int i) {
        Throwable t = (i == 0) ? null : new MyThrowable();
        try {
            return foo(t);
        } catch (Throwable t1) {
            return -3;
        }
    }
}
