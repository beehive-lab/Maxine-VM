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
/*VCSID=eacce52a-7cc9-4545-a457-f129c84b1e29*/
// Checkstyle: stop
package test.hotpath;
/*
 * @Harness: java
 * @Runs: 40 = 20; 22 = 20;
 */
public class HP_scope02 {
    public static int test(int count) {
        int sum = 0;
        // Although sum is not explicitly read in the tree below it is implicitly read
        // by the guard bail-out.
        for (int i = 0; i < count; i++) {
            if (i > 20) {
               break; // We need to write back either the original value of sum, or the previous iteration's value.
            }
            sum = i;
        }
        return sum;
    }
}
