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
 * @author Bernd Mathiske
 */
class MillNameAndTypeConstant extends MillConstant {

    final int _nameIndex;
    final int _descriptorIndex;

    MillNameAndTypeConstant(MillUtf8Constant name, MillUtf8Constant descriptor) {
        super(CONSTANT_NameAndType, 5, name._hashValue ^ descriptor._hashValue);
        this._nameIndex = name._index;
        this._descriptorIndex = descriptor._index;
    }

    @Override
    public boolean equals(Object x) {
        if (!(x instanceof MillNameAndTypeConstant)) {
            return false;
        }
        final MillNameAndTypeConstant c = (MillNameAndTypeConstant) x;
        return _nameIndex == c._nameIndex && _descriptorIndex == c._descriptorIndex;
    }

}
