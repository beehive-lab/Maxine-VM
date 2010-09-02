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

package com.sun.max.vm.verifier.types;

import java.io.*;

import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Models the Java types that are really unboxed words. These types are incompatible
 * with {@link ReferenceType}s. Furthermore, all different word types at the source
 * level are equivalent at the verifier and VM level as they can be freely cast
 * between each other.
 *
 * @author Doug Simon
 */
public final class WordType extends ReferenceOrWordType {

    WordType() {
        // Ensures that only the one singleton instance of this class is created.
        assert WORD == null || getClass() != WordType.class;
    }

    @Override
    public boolean isAssignableFromDifferentType(VerificationType from) {
        return from instanceof WordType;
    }

    @Override
    public TypeDescriptor typeDescriptor() {
        return JavaTypeDescriptor.WORD;
    }

    @Override
    public int classfileTag() {
        return ITEM_Object;
    }

    @Override
    public void writeInfo(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.writeShort(constantPoolEditor.indexOf(PoolConstantFactory.createClassConstant(typeDescriptor()), true));
    }

    @Override
    public String toString() {
        return "word";
    }
}
