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
package com.sun.max.jdwp.vm.proxy;

import com.sun.max.jdwp.vm.core.*;

/**
 * Interface to a field in the VM.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface FieldProvider extends Provider {

    /**
     * Only valid when it is a static field.
     *
     * @return the static value of the field
     */
    VMValue getStaticValue();

    /**
     * Only valid when it is a static field. Sets the static value of a field.
     *
     * @param value the value that should be the new value of the field
     */
    void setStaticValue(VMValue value);

    /**
     * Accesses the value of the field for a specific instance object.
     *
     * @param object the instance object whose field value to access
     * @return the value of the field
     */
    VMValue getValue(ObjectProvider object);

    /**
     * Sets the value of a field for a specific instance object.
     *
     * @param object the instance object whose field should be set
     * @param value the value that should be the new value of the field
     */
    void setValue(ObjectProvider object, VMValue value);

    /**
     * The enclosing reference type of the field. For instance fields this can only be a class type.
     *
     * @return the enclosing reference type
     */
    ReferenceTypeProvider getReferenceTypeHolder();

    /**
     * @return the type of the field
     */
    VMValue.Type getType();

    /**
     * @return the flags of the field according to the Java VM specification
     */
    int getFlags();

    /**
     * @return the name of the field
     */
    String getName();

    /**
     * @return the signature of the field according to the Java VM specification
     */
    String getSignature();

    /**
     * @return the generic signature of the field according to the Java VM specification
     */
    String getGenericSignature();
}
