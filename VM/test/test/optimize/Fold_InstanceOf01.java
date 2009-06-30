/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package test.optimize;

/*
 * Tests constant folding of integer operations.
 * @Harness: java
 * @Runs: 0=true; 1=true; 2=false
 */
public class Fold_InstanceOf01 {
    static final Object object = new Fold_InstanceOf01();

    public static boolean test(int arg) {
        if (arg == 0) {
            return object instanceof Fold_InstanceOf01;
        }
        if (arg == 1) {
            Object obj = new Fold_InstanceOf01();
            return obj instanceof Fold_InstanceOf01;
        }
        if (arg == 2) {
            return null instanceof Fold_InstanceOf01;
        }
        return false;
    }
}