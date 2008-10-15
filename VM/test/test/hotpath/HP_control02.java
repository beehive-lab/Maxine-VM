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
 * @Runs: 60 = 515; 100 = 555;
 */
public class HP_control02 {
    public static int test(int count) {
        int sum = 0;
        for (int i = 0; i < count; i++) {
            switch (i) {
                case 30: sum += 30; break;
                case 31: sum += 31; break;
                case 32: sum += 32; break;
                case 33: sum += 33; break;
                case 34: sum += 34; break;
                case 35: sum += 35; break;
                case 36: sum += 36; break;
                case 37: sum += 37; break;
                case 38: sum += 38; break;
                case 39: sum += 39; break;
                case 40: sum += 40; break;
                case 41: sum += 41; break;
                case 42: sum += 42; break;
                default: sum += 1; break;
            }
        }
        return sum;
    }
}
