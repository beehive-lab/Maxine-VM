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
package com.sun.max.vm.classfile.create;


/**
 * This class represents Utf8 string entries in constant pools.
 * 
 * @see MillClass#makeUtf8Constant
 * 
 * @author Bernd Mathiske
 * @version 1.0
 */
public class MillUtf8Constant extends MillConstant {

    final String _string;

    MillUtf8Constant(String string) {
        super(CONSTANT_Utf8, 3 + string.length(), string.hashCode());
        this._string = string;
    }

    /**
     * Compares two objects for equality.
     * 
     * @param other
     *            The reference object with which to compare.
     * @return {@code true} if this object is the same as the
     *         {@code obj} argument; {@code false} otherwise.
     */
    @Override  public boolean equals(Object other) {
        if (!(other instanceof MillUtf8Constant)) {
            return false;
        }
        return _string.equals(((MillUtf8Constant) other)._string);
    }

}
