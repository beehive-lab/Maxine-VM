/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
