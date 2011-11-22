/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;

/**
 */
public final class NullType extends ObjectType {

    NullType() {
        // Ensures that only the one singleton instance of this class is created.
        assert NULL == null;
    }

    @Override
    public boolean isAssignableFromDifferentType(VerificationType source) {
        return source == this;
    }

    @Override
    public int classfileTag() {
        return ITEM_Null;
    }

    @Override
    public ClassActor resolve() {
        throw ProgramError.unexpected();
    }

    @Override
    public void writeInfo(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
    }

    @Override
    public String toString() {
        return "null";
    }
}
