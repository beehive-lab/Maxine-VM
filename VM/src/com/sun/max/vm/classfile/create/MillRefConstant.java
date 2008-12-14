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
 * The common super class of field and method reference constants.
 *
 * @see MillFieldRefConstant
 * @see MillMethodRefConstant
 *
 * @author Bernd Mathiske
 * @version 1.0
 */
abstract class MillRefConstant extends MillConstant {

    final int _classIndex;
    final int _nameAndTypeIndex;

    protected MillRefConstant(byte tag, MillClassConstant clazz, MillNameAndTypeConstant nameAndType) {
        super(tag, 5, clazz._hashValue ^ nameAndType._hashValue);
        this._classIndex = clazz._index;
        this._nameAndTypeIndex = nameAndType._index;
    }

    /**
     * Compares two Objects for equality.
     *
     * @param obj The reference object with which to compare.
     * @return {@code true} if this object is the same
     *         as the {@code obj} argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MillRefConstant)) {
            return false;
        }
        final MillRefConstant c = (MillRefConstant) obj;
        return _tag == c._tag && _classIndex == c._classIndex && _nameAndTypeIndex == c._nameAndTypeIndex;
    }

}
