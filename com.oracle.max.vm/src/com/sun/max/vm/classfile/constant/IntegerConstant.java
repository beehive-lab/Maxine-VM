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

import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.value.*;

/**
 * #4.4.4.
 */
public final class IntegerConstant extends AbstractPoolConstant<IntegerConstant> implements PoolConstantKey<IntegerConstant>, ValueConstant {

    @Override
    public Tag tag() {
        return Tag.INTEGER;
    }

    private final int value;

    IntegerConstant(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public String valueString(ConstantPool pool) {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof IntegerConstant) {
            final IntegerConstant key = (IntegerConstant) other;
            return value == key.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value;
    }

    public Value value(ConstantPool pool, int index) {
        return IntValue.from(value);
    }

    @Override
    public IntegerConstant key(ConstantPool pool) {
        return this;
    }
}
