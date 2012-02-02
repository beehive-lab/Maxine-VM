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
package com.sun.max.vm.value;

/**
 * Primitive values.
 */
public abstract class PrimitiveValue<PrimitiveValue_Type extends Value<PrimitiveValue_Type>> extends Value<PrimitiveValue_Type> {

    protected PrimitiveValue() {
        super();
    }

    @Override
    public int hashCode() {
        return toInt();
    }

    @Override
    protected int compareSameKind(PrimitiveValue_Type other) {
        final long thisLong = toLong();
        final long otherLong = other.toLong();
        return (thisLong < otherLong) ? -1 : (thisLong == otherLong ? 0 : 1);
    }
}
