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
/*
 * @Harness: java
 * @Runs: 0 = 0; 1 = 1
 */
package jtt.except;

public class Except_Synchronized05 {

    Object field;

    public static int test(int arg) {
        return new Except_Synchronized05().bar(arg) != null ? 1 : 0;
    }

    public synchronized Object bar(int arg) {
        try {
            String f = foo1(arg);
            if (f == null) {
                field = new Object();
            }
        } catch (NullPointerException e) {
            // do nothing
        }
        return field;
    }

    private String foo1(int arg) {
        if (arg == 0) {
            throw null;
        }
        return null;
    }
}