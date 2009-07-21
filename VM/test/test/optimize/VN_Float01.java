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
 * Tests optimization of float operations.
 * @Harness: java
 * @Runs: 0f=22f; 1f=0f; 2f=144f; 3f=1f
 */
public class VN_Float01 {
    public static float test(float arg) {
        if (arg == 0) {
            return add(arg + 10);
        }
        if (arg == 1) {
            return sub(arg + 10);
        }
        if (arg == 2) {
            return mul(arg + 10);
        }
        if (arg == 3) {
            return div(arg + 10);
        }
        return 0;
    }
    public static float add(float x) {
        float c = 1;
        float t = x + c;
        float u = x + c;
        return t + u;
    }
    public static float sub(float x) {
        float c = 1;
        float t = x - c;
        float u = x - c;
        return t - u;
    }
    public static float mul(float x) {
        float c = 1;
        float t = x * c;
        float u = x * c;
        return t * u;
    }
    public static float div(float x) {
        float c = 1;
        float t = x / c;
        float u = x / c;
        return t / u;
    }
}