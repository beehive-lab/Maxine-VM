/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;

import com.sun.max.vm.classfile.constant.ConstantPool.Tag;
import com.sun.max.vm.value.*;

/**
 * A non-standard pool constant for putting arbitrary objects into the constant pool.
 */
public class ObjectConstant extends AbstractPoolConstant<ObjectConstant> implements PoolConstantKey<ObjectConstant> {

    @Override
    public Tag tag() {
        return Tag.OBJECT;
    }

    private final Object value;

    public ObjectConstant(Object value) {
        assert value != null;
        this.value = value;
    }

    public Object value() {
        return value;
    }

    public String valueString(ConstantPool pool) {
        return value.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ObjectConstant) {
            final ObjectConstant key = (ObjectConstant) other;
            return value == key.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public Value value(ConstantPool pool, int index) {
        return ObjectReferenceValue.from(value);
    }

    @Override
    public ObjectConstant key(ConstantPool pool) {
        return this;
    }

    @Override
    public void writeOn(DataOutputStream stream, ConstantPoolEditor editor, int index) throws IOException {
        throw new UnsupportedOperationException("Object constants cannot be written to a class file");
    }
}
