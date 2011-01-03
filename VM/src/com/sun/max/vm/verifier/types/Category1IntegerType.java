/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Abstract super class for all the primitive types whose values are represented as integer at runtime. All these types
 * are {@linkplain #isAssignableFrom(VerificationType) compatible} with each other for the purpose of verification.
 * 
 * @author Doug Simon
 */
public abstract class Category1IntegerType extends Category1Type {

    @Override
    public final boolean isAssignableFromDifferentType(VerificationType from) {
        return from instanceof Category1IntegerType;
    }

    @Override
    protected
    final VerificationType mergeWithDifferentType(VerificationType other) {
        if (isAssignableFrom(other)) {
            return INTEGER;
        }
        return TOP;
    }

    @Override
    public final int classfileTag() {
        return ITEM_Integer;
    }

    @Override
    public abstract String toString();
}
