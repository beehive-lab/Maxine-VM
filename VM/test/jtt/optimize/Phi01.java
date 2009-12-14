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
 * @Runs: 0=8; 1=10; 2=12; 3=8; 4=10; 6=14
 */
public class Phi01 {
    int f;

    Phi01(int f) {
        this.f = f;
    }

    public static int test(int arg) {
        return test2(new Phi01(arg), arg);
    }

    @NEVER_INLINE
    private static int test2(Phi01 p, int arg) {
        if (arg > 2) {
            p.f += 1;
            arg += 1;
        } else {
            p.f += 2;
            arg += 2;
            if (arg > 3) {
                p.f += 1;
                arg += 1;
                if (arg > 4) {
                    p.f += 1;
                    arg += 1;
                } else {
                    p.f += 2;
                    arg += 2;
                }
            } else {
                p.f += 2;
                arg += 2;
            }
        }
        return arg + p.f;
    }

    @NEVER_INLINE
    private static void inc(Phi01 p, int inc) {
        p.f += inc;
    }
}
