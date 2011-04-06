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
package com.sun.max.vm.classfile.constant;

import static com.sun.max.vm.classfile.ErrorContext.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.type.*;

/**
 * #4.4.6.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class NameAndTypeConstant extends AbstractPoolConstant<NameAndTypeConstant> implements PoolConstantKey<NameAndTypeConstant> {

    private final Utf8Constant name;

    private Object descriptor;

    @Override
    public Tag tag() {
        return Tag.NAME_AND_TYPE;
    }

    NameAndTypeConstant(Utf8Constant name, Utf8Constant descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    public NameAndTypeConstant(Utf8Constant name, Descriptor descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    public Utf8Constant name() {
        return name;
    }

    public String descriptorString() {
        return descriptor.toString();
    }

    public TypeDescriptor type() {
        if (descriptor instanceof Utf8Constant) {
            try {
                descriptor = JavaTypeDescriptor.parseTypeDescriptor(((Utf8Constant) descriptor).toString());
            } catch (ClassCastException e) {
                // This just means another thread beats us in the race to convert descriptor to a real Descriptor object.
                // If descriptor still has the wrong Descriptor type, then the cast in the return statement will catch it.
                // Using an exception handler obviates the need for synchronization.
            }
        }
        try {
            return (TypeDescriptor) descriptor;
        } catch (ClassCastException e) {
            throw classFormatError(descriptor + " is not a valid field type descriptor");
        }
    }

    public SignatureDescriptor signature() {
        if (descriptor instanceof Utf8Constant) {
            try {
                descriptor = SignatureDescriptor.create(((Utf8Constant) descriptor).toString());
            } catch (ClassCastException e) {
                // This just means another thread beats us in the race to convert descriptor to a real Descriptor object.
                // If descriptor still has the wrong Descriptor type, then the following cast will catch it.
                // Using an exception handler obviates the need for synchronization.
            }

        }
        try {
            return (SignatureDescriptor) descriptor;
        } catch (ClassCastException e) {
            throw classFormatError(descriptor + " is not a valid method signature descriptor");
        }
    }

    public Descriptor descriptor() {
        if (descriptor instanceof Descriptor) {
            return (Descriptor) descriptor;
        }
        if (descriptorString().charAt(0) == '(') {
            return signature();
        }
        return type();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof NameAndTypeConstant) {
            final NameAndTypeConstant nameAndType = (NameAndTypeConstant) object;
            return name.equals(nameAndType.name) && (descriptor == nameAndType.descriptor || descriptorString().equals(nameAndType.descriptorString()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ descriptorString().hashCode();
    }

    @Override
    public NameAndTypeConstant key(ConstantPool pool) {
        return this;
    }

    public String valueString(ConstantPool pool) {
        return "name=\"" + name() + "\",descriptor=\"" + descriptor + "\"";
    }
}
