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
 * @Runs: 0=false; 1=false; 2=false; 3=true; 4=true; 5=false; 6=true; 7=false; 8=false
 */
package test.lang;

public final class Object_equals01 {
    private Object_equals01() {
    }

    public static Object_equals01 _field = new Object_equals01();

    public static boolean test(int i) {
        final Object obj1 = new Object();
        final Object obj2 = new Object();
        switch (i) {
            case 0:
                return obj1.equals(_field);
            case 1:
                return obj1.equals(obj2);
            case 2:
                return obj1.equals(null);
            case 3:
                return obj1.equals(obj1);
            case 4:
                return _field.equals(_field);
            case 5:
                return obj2.equals(_field);
            case 6:
                return obj2.equals(obj2);
            case 7:
                return obj2.equals(null);
            case 8:
                return obj2.equals(obj1);
        }
        return false;
    }
}
