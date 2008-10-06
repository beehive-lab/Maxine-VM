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
/*VCSID=b20f1bf8-7145-4956-96b9-22e946679bf0*/
package test.micro;


/*
 * @Harness: java
 * @Runs: ("0", "1", "2", "3", "4", "5", "6", "7", "8", "9") = "0123456789"
 */
public class BigObjectParams02 {
    public static String test(String p0, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9) {
        return p0 + p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;
    }
}
