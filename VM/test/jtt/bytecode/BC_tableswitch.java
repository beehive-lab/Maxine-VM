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
 * @Runs: -1 = 42; 0 = 10; 1 = 20; 2 = 30; 3 = 42; 4 = 40; 5 = 50; 6 = 42
 */
public class BC_tableswitch {
    public static int test(int a) {
        switch (a) {
            case 0:
                return 10;
            case 1:
                return 20;
            case 2:
                return 30;
            case 4:
                return 40;
            case 5:
                return 50;
        }
        return 42;
    }
}
