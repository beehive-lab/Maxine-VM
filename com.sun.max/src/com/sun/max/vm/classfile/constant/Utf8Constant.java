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
package com.sun.max.vm.classfile.constant;

import com.sun.max.annotate.*;
import com.sun.max.vm.classfile.constant.ConstantPool.Tag;
import com.sun.max.vm.classfile.constant.SymbolTable.Utf8ConstantEntry;

/**
 * Canonical representation of strings.
 *
 * #4.4.7.
 */
public abstract class Utf8Constant extends AbstractPoolConstant<Utf8Constant> implements PoolConstantKey<Utf8Constant>, Comparable<Utf8Constant> {

    @Override
    public final Tag tag() {
        return Tag.UTF8;
    }

    @INSPECTED
    public final String string;

    /**
     * Must only be called from {@link Utf8ConstantEntry#Utf8ConstantEntry(String)}.
     */
    Utf8Constant(String value) {
        // Can only be subclassed by Utf8ConstantEntry
        assert getClass() == Utf8ConstantEntry.class;
        string = value;
    }

    @Override
    public final String toString() {
        return string;
    }

    public final String valueString(ConstantPool pool) {
        return string;
    }

    @Override
    public final boolean equals(Object other) {
        return this == other;
    }

    public final boolean equals(String s) {
        return string.equals(s);
    }

    @Override
    public final int hashCode() {
        return string.hashCode();
    }

    @Override
    public final Utf8Constant key(ConstantPool pool) {
        return this;
    }

    public final int compareTo(Utf8Constant other) {
        return string.compareTo(other.string);
    }
}
