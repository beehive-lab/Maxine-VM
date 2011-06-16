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
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.value.*;

/**
 * #4.4.3.
 */
public final class StringConstant extends AbstractPoolConstant<StringConstant> implements PoolConstantKey<StringConstant>, ValueConstant{

    @Override
    public Tag tag() {
        return Tag.STRING;
    }

    @INSPECTED
    public final String value;

    StringConstant(String value) {
        this.value = value;
    }

    public String valueString(ConstantPool pool) {
        return '"' + value + '"';
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof StringConstant) {
            final StringConstant key = (StringConstant) other;
            return value.equals(key.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public Value value(ConstantPool pool, int index) {
        return ReferenceValue.from(value);
    }

    @Override
    public StringConstant key(ConstantPool pool) {
        return this;
    }
}
