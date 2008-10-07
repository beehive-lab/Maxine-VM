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
/*VCSID=5835b6a0-42f9-4d94-bcb3-d7f7c17c78ca*/

package com.sun.max.vm.verifier.types;

import com.sun.max.vm.type.*;



/**
 * @author David Liu
 * @author Doug Simon
 */
public final class FloatType extends Category1Type {

    FloatType() {
        // Ensures that only the one singleton instance of this class is created.
        assert FLOAT == null;
    }

    @Override
    public boolean isAssignableFromDifferentType(VerificationType from) {
        return from == this;
    }

    @Override
    public TypeDescriptor typeDescriptor() {
        return JavaTypeDescriptor.FLOAT;
    }

    @Override
    public String toString() {
        return "float";
    }

    @Override
    public int classfileTag() {
        return ITEM_Float;
    }
}
