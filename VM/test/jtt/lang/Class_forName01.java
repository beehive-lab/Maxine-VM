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
 * @Runs: 0 = "class java.lang.Object"; 1 = "class java.lang.String";
 * @Runs: 2 = "class test.lang.Class_forName01"; 3 = !java.lang.ClassNotFoundException
 * @Runs: 4 = null
 */
package jtt.lang;


public final class Class_forName01 {
    private Class_forName01() {
    }

    public static String test(int i) throws ClassNotFoundException {
        if (i == 0) {
            return Class.forName("java.lang.Object").toString();
        }
        if (i == 1) {
            return Class.forName("java.lang.String").toString();
        }
        if (i == 2) {
            return Class.forName("test.lang.Class_forName01").toString();
        }
        if (i == 3) {
            return Class.forName("xyxzz.xyzyzyz.XXYYY").toString();
        }
        return null;
    }
}
