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
package test.lang;

/*
 * @Harness: java
 * @Runs: 0 = true; 1 = true; 2 = true; 3 = true; 4 = true; 5 = true; 6 = false;
 */
public class JDK_ClassLoaders01 {
    public static boolean test(int i) {
        if (i == 0) {
            return Object.class.getClassLoader() == null;
        }
        if (i == 1) {
            return Class.class.getClassLoader() == null;
        }
        if (i == 2) {
            return String.class.getClassLoader() == null;
        }
        if (i == 3) {
            return Thread.class.getClassLoader() == null;
        }
        if (i == 4) {
            return System.class.getClassLoader() == null;
        }
        if (i == 5) {
            return ClassLoader.class.getClassLoader() == null;
        }
        return false;
    }
}
