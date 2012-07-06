/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.cri.bytecode.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * A registry of canonical instances for objects used by a verifier such as verification types and subroutines.
 */
public interface VerificationRegistry {

    /**
     * Gets the canonical object type for a TypeDescriptor.
     *
     * @return null if {@code typeDescriptor} denotes a primitive type
     */
    VerificationType getObjectType(TypeDescriptor typeDescriptor);

    /**
     * Gets the canonical type of an uninitialized object created by a {@link Bytecodes#NEW} instruction at a given
     * BCI.
     */
    UninitializedNewType getUninitializedNewType(int bci);

    /**
     * Gets the canonical type for a TypeDescriptor.
     */
    VerificationType getVerificationType(TypeDescriptor typeDescriptor);

    /**
     * Gets the canonical representation of a subroutine entered at a given BCI.
     */
    Subroutine getSubroutine(int entryBCI, int maxLocals);

    /**
     * Clears all recorded subroutines.
     *
     * @return the number of recorded subroutines cleared
     */
    int clearSubroutines();

    ConstantPool constantPool();

    ClassActor resolve(TypeDescriptor type);
}
