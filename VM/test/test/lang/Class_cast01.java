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
/*VCSID=1484c93e-ac99-4a74-8a7b-5b6207d7ee90*/
/*
 * @Harness: java
 * @Runs: 1 = !java.lang.ClassCastException; 0 = 0;
 * @Runs: 3 = !java.lang.ClassCastException; 2 = 2;
 * @Runs: 4 = 4
 */
package test.lang;

public final class Class_cast01 {
    private Class_cast01() {
    }

    static final String _string = "";
    static final Object _object = new Object();
    static final Class_cast01 _this = new Class_cast01();

    public static int test(int i) {
        if (i == 0) {
            if (Object.class.cast(_string) == null) {
                return -1;
            }
        }
        if (i == 1) {
            if (String.class.cast(_object) == null) {
                return -1;
            }
        }
        if (i == 2) {
            if (Object.class.cast(_this) == null) {
                return -1;
            }
        }
        if (i == 3) {
            if (Class_cast01.class.cast(_object) == null) {
                return -1;
            }
        }
        return i;
    }
}
