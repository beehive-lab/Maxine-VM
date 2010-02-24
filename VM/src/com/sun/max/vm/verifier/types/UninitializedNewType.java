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

import com.sun.c1x.bytecode.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * @author David Liu
 * @author Doug Simon
 */
public final class UninitializedNewType extends UninitializedType {

    private final int position;

    public UninitializedNewType(int position) {
        assert position != -1;
        this.position = position;
    }

    /**
     * Gets the bytecode position of the {@link Bytecodes#NEW} instruction that created the uninitialized object denoted by this type.
     */
    public int position() {
        return position;
    }

    @Override
    public boolean isAssignableFromDifferentType(VerificationType from) {
        assert from != this;
        return false;
    }

    @Override
    public void writeInfo(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.writeShort(position);
    }

    @Override
    public String toString() {
        return "uninitialized[new@" + position + "]";
    }

    @Override
    public int classfileTag() {
        return ITEM_Uninitialized;
    }
}
