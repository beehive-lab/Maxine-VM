/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.runtime;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * A token that "guards" the resolution of a symbol to an {@link Actor}.
 *
 * This pattern of use is intended:
 *
 * if (guard.value == null) { guard.value = ... } return guard.value;
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class ResolutionGuard {

    /**
     * A resolution guard for a symbol read from a constant pool. The constant pool is
     * used to provide the correct class loader and protection domain for resolving
     * the symbol.
     */
    public static class InPool extends ResolutionGuard {
        public final ConstantPool pool;
        public final int cpi;

        public InPool(ConstantPool constantPool, int cpi) {
            this.pool = constantPool;
            this.cpi = cpi;
            assert cpi >= 0 : "must be a valid constant pool index!";
        }
        @Override
        public String toString() {
            if (value != null) {
                return getClass().getSimpleName() + "[" + value + "]";
            }

            return getClass().getSimpleName() + "[" + pool.at(cpi).valueString(pool) + "]";
        }
    }

    /**
     * A class symbol resolution guard. The symbol is resolved using the loader and
     * protection domain of in the context of an accessing class.
     */
    public static class InAccessingClass extends ResolutionGuard {
        public final TypeDescriptor type;
        public final ClassActor accessingClass;

        public InAccessingClass(TypeDescriptor type, ClassActor accessingClass) {
            this.type = type;
            this.accessingClass = accessingClass;
        }

        public InAccessingClass(UnresolvedType.ByAccessingClass unresolvedType) {
            this.type = unresolvedType.typeDescriptor;
            this.accessingClass = unresolvedType.accessingClass;
        }

        public ClassActor resolve() {
            return type.resolve(accessingClass.classLoader);
        }

        @Override
        public String toString() {
            if (value != null) {
                return getClass().getSimpleName() + "[" + value + "]";
            }

            return getClass().getSimpleName() + "[" + type + "]";
        }
    }

    @CONSTANT_WHEN_NOT_ZERO
    public Actor value;
}
