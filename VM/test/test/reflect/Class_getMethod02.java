/*VCSID=edb6692f-ee74-4543-a97b-5f202ae32d3c*/
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
package test.reflect;



/*
 * @Harness: java
 * @Runs: 0=!java.lang.NoSuchMethodException; 1="test"; 2=!java.lang.NoSuchMethodException; 3="main";
 * @Runs: 4=!java.lang.NoSuchMethodException; 5=!java.lang.NoSuchMethodException; 6=null
 */
public class Class_getMethod02 {

    static String field;

    public static String test(int arg) throws NoSuchMethodException, IllegalAccessException {
        if (arg == 0) {
            return Class_getMethod02.class.getMethod("test").getName();
        } else if (arg == 1) {
            return Class_getMethod02.class.getMethod("test", int.class).getName();
        } else if (arg == 2) {
            return Class_getMethod02.class.getMethod("main").getName();
        } else if (arg == 3) {
            return Class_getMethod02.class.getMethod("main", String[].class).getName();
        } else if (arg == 4) {
            return Class_getMethod02.class.getMethod("<init>").getName();
        } else if (arg == 5) {
            return Class_getMethod02.class.getMethod("<clinit>").getName();
        }
        return null;
    }

    public static void main(String[] args) {
        field = args[0];
    }

    private void test() {

    }
}
