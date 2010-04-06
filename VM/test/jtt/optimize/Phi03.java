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

import com.sun.max.annotate.NEVER_INLINE;

/*
 * @Harness: java
 * @Runs: 0=4; 1=5; 2=6; 3=4; 4=5; 6=7
 */
public class Phi03 {
    int f;

    Phi03(int f) {
        this.f = f;
    }

    static boolean ternary(int a, int b) {
        boolean result = a < b;
        return result;
    }

    public static int test(int arg) {
        return test2(new Phi03(arg), arg);
    }

    @NEVER_INLINE
    private static int test2(Phi03 p, int arg) {
        if (arg > 2) {
            inc(p, 1);
            arg += 1;
        } else {
            inc(p, 2);
            arg += 2;
            if (arg > 3) {
                inc(p, 1);
                arg += 1;
                if (arg > 4) {
                    inc(p, 1);
                    arg += 1;
                } else {
                    inc(p, 2);
                    arg += 2;
                }
            } else {
                inc(p, 2);
                arg += 2;
            }
        }
        return p.f;
    }

    @NEVER_INLINE
    private static void inc(Phi03 p, int inc) {
        p.f += inc;
    }
}
