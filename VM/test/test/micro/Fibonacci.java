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
/*VCSID=b7849141-ec44-4ba6-9da5-e62f2f9b7cb4*/
package test.micro;

/*
 * @Harness: java
 * @Runs: 0 = 0; 1 = 1; 2 = 1; 3 = 2; 4 = 3; 5 = 5; 6 = 8; 7 = 13
 */
public class Fibonacci {
    public static int test(int num) {
        if (num <= 0) {
            return 0;
        }
        int n1 = 0;
        int n2 = 1;
        for (int i = 1; i < num; i++) {
            final int next = n2 + n1;
            n1 = n2;
            n2 = next;
        }
        return n2;
    }
}
