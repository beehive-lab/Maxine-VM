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
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved. Use is subject to license terms.
 */
/*
 * @Harness: java
 * @Runs: 0 = "XYZ"; 1 = "string"; 2 = "class java.lang.String"; 3 = !java.lang.NullPointerException
 */
package jtt.lang;

public class Object_toString02 {

    static final Object obj = new Object_toString02();

    public static String test(int i) {
        Object object = null;
        if (i == 0) {
            object = obj;
        } else if (i == 1) {
            object = "string";
        } else if (i == 2) {
            object = "string".getClass();
        }
        return object.toString();
    }

    @Override
    public String toString() {
        return "XYZ";
    }
}
