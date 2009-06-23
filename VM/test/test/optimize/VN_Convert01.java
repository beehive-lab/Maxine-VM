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
 * Tests optimization integer conversions.
 * @Harness: java
 * @Runs: 0=20; 1=22; 2=24
 */
public class VN_Convert01 {
    public static int test(int arg) {
        if (arg == 0) {
            return i2b(arg + 10);
        }
        if (arg == 1) {
            return i2s(arg + 10);
        }
        if (arg == 2) {
            return i2c(arg + 10);
        }
        return 0;
    }

    public static int i2b(int arg) {
        int x = (byte) arg;
        int y = (byte) arg;
        return x + y;
    }

    public static int i2s(int arg) {
        int x = (short) arg;
        int y = (short) arg;
        return x + y;
    }

    public static int i2c(int arg) {
        int x = (char) arg;
        int y = (char) arg;
        return x + y;
    }

}