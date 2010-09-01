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
package jtt.bytecode;

/*
 * @Harness: java
 * @Runs: 0 = 42; 1 = 42;
 * @Runs: 66 = 42; 67 = 0; 68 = 42;
 * @Runs: 96 = 42; 97 = 1; 98 = 42;
 * @Runs: 106 = 42; 107 = 2; 108 = 42;
 * @Runs: 132 = 42; 133 = 3; 134 = 42;
 * @Runs: 211 = 42; 212 = 4; 213 = 42;
 * @Runs: -121 = 42; -122 = 5; -123 = 42
 */
public class BC_lookupswitch03 {
    public static int test(int a) {
        final int b = a + 10;
        switch (b) {
            case 77:
                return 0;
            case 107:
                return 1;
            case 117:
                return 2;
            case 143:
                return 3;
            case 222:
                return 4;
            case -112:
                return 5;
        }
        return 42;
    }
}
