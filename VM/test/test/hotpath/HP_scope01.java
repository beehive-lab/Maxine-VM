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
package test.hotpath;
/*
 * @Harness: java
 * @Runs: 40 = 59480;
 */
public class HP_scope01 {
    public static int test(int count) {
        int sum = 0;

        for (int k = 0; k < count; k++) {
            {
                int i  = 1;
                sum += i;
            }
            {
                float f = 3;
                sum += f;
            }
            {
                long l = 7;
                sum += l;
            }
            {
                double d = 11;
                sum += d;
            }
        }

        for (int k = 0; k < count; k++) {
            if (k < 20) {
                int i = 1;
                sum += i;
            } else {
                float f = 3;
                sum += f;
            }
        }

        for (int k = 0; k < count; k++) {
            int i = 3;
            for (int j = 0; j < count; j++) {
                float f = 7;
                sum += i + f;
            }
        }

        for (int k = 0; k < count; k++) {
            for (int j = 0; j < count; j++) {
                float f = 7;
                sum += j + f;
            }
            int i = 3;
            sum += i;
        }

        return sum;
    }
}
