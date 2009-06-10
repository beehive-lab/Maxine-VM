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
package test.optimize;

/*
 * Tests constant folding of float operations.
 * @Harness: java
 * @Runs: 0f=10f; 1f=11f; 2f=12f; 3f=13f; 4f=14f
 */
public class Fold_Float01 {
    public static float test(float arg) {
        if (arg == 0) {
            return add();
        }
        if (arg == 1) {
            return sub();
        }
        if (arg == 2) {
            return mul();
        }
        if (arg == 3) {
            return div();
        }
        if (arg == 4) {
            return mod();
        }
        return 0;
    }
    public static float add() {
        float x = 3;
        return x + 7;
    }
    public static float sub() {
        float x = 15;
        return x - 4;
    }
    public static float mul() {
        float x = 6;
        return x * 2;
    }
    public static float div() {
        float x = 26;
        return x / 2;
    }
    public static float mod() {
        float x = 29;
        return x % 15;
    }
}