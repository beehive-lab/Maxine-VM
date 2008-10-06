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
/*VCSID=fccd4b4b-2d41-49bf-be1c-40848726105f*/
package test.bytecode;

/*
 * @Harness: java
 * @Runs: -1 = 11; -2 = 22; -3 = 99; -4 = 99;  1 = 77; 2 = 99; 10 = 99
 */
public class BC_tableswitch3 {
    public static int test(int a) {
            switch (a) {
                case -2:
                    return 22;
                case -1:
                    return 11;
               case 0:
                    return 33;
                case 1:
                    return 77;
            }
            return 99;
        }
}
