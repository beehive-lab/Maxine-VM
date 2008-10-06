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
/*VCSID=5026712d-3430-4acb-8163-e1770edcd4ad*/
package test.bytecode;

/*
 * @Harness: java
 * @Runs: (1,-2)=3; (0,1)=-1; (33,-67)=100; (1, 1)=0;
 * @Runs: (-2147483648,-1)=-2147483647; (2147483647,-1)=-2147483648;
 * @Runs: (-2147483647,2)=2147483647
 */
public class BC_isub {
    public static int test(int a, int b) {
        return a - b;
    }
}
