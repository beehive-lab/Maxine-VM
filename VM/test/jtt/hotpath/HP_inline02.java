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
// Checkstyle: stop
package jtt.hotpath;
/*
 * @Harness: java
 * @Runs: 20 = 196;
 */
public class HP_inline02 {
    public static int test(int count) {
        int sum = 0;
        for (int i = 0; i < count; i++) {
            sum += foo(i, sum);
        }
        return sum;
    }

    public static int foo(int x, int y) {
        if (x < 18) {
            return bar(x, x - y);
        }
        return bar(x, x + y);
    }

    public static int bar(int x, int y) {
        if (x < 15) {
            return car(x, x + y);
        }
        return x - 1;
    }

    public static int car(int x, int y) {
        if (x < 13) {
            return x + 1;
        }
        return x - 1;
    }
}
