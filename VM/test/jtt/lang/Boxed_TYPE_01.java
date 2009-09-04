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
package jtt.lang;

/*
 * @Harness: java
 * @Runs: 0 = "boolean"; 1 = "byte"; 2 = "char"; 3 = "double"; 4 = "float"; 5 = "int"; 6 = "long"; 7 = "short"; 8 = "void";
 */
public class Boxed_TYPE_01 {
    public static String test(int i) {
        if (i == 0) {
            return Boolean.TYPE.getName();
        }
        if (i == 1) {
            return Byte.TYPE.getName();
        }
        if (i == 2) {
            return Character.TYPE.getName();
        }
        if (i == 3) {
            return Double.TYPE.getName();
        }
        if (i == 4) {
            return Float.TYPE.getName();
        }
        if (i == 5) {
            return Integer.TYPE.getName();
        }
        if (i == 6) {
            return Long.TYPE.getName();
        }
        if (i == 7) {
            return Short.TYPE.getName();
        }
        if (i == 8) {
            return Void.TYPE.getName();
        }
        return null;
    }
}
