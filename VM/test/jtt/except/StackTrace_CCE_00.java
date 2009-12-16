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
package jtt.except;

/*
 * @Harness: java
 * @Runs: 0 = 0; 2 = !java.lang.ClassCastException; 3 = !java.lang.ClassCastException; 4 = 4
 */
public class StackTrace_CCE_00 {
    static Object object2 = new Object();
    static Object object3 = "";
    static Object object4 = new StackTrace_CCE_00();

    public static int test(int arg) {
        Object obj = null;
        if (arg == 2) {
            obj = object2;
        }
        if (arg == 3) {
            obj = object3;
        }
        if (arg == 4) {
            obj = object4;
        }
        try {
            final StackTrace_CCE_00 bc = (StackTrace_CCE_00) obj;
            if (bc == null) {
                return arg;
            }
            return arg;
        } catch (ClassCastException npe) {
            for (StackTraceElement e : npe.getStackTrace()) {
                if (e.getClassName().equals(StackTrace_CCE_00.class.getName()) && e.getMethodName().equals("test")) {
                    return -100;
                }
            }
            return -200;
        }
    }
}