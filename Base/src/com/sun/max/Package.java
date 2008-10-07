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
/*VCSID=6992dfe6-4aa7-44f2-a5e5-dc5475b0c824*/
package com.sun.max;

/**
 * @see MaxPackage
 *
 * @author Bernd Mathiske
 */
public class Package extends BasePackage {
    public Package() {
        super();
    }

    private static final String PACKAGE_NAME = new Package().name();

    /**
     * Determines if a given class is part of the Maxine code base.
     */
    public static boolean contains(Class javaClass) {
        return contains(javaClass.getName());
    }

    /**
     * Determines if a given class is part of the Maxine code base.
     */
    public static boolean contains(String javaClassName) {
        return javaClassName.startsWith(PACKAGE_NAME);
    }

}
