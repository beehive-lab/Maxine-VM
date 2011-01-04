/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;

/**
 * Interface denoting a field or method entry in a constant pool.
 *
 * @author Doug Simon
 */
public interface MemberRefConstant<PoolConstant_Type extends PoolConstant<PoolConstant_Type>> extends ResolvableConstant<PoolConstant_Type, MemberActor> {

    /**
     * Gets the descriptor of the class (i.e. holder) in which this method or field is declared. Note that the actual
     * holder after resolution may be a super class of the class described by the descriptor returned by this method.
     *
     * @param pool the constant pool that maybe be required to convert a constant pool index to the holder's descriptor
     */
    TypeDescriptor holder(ConstantPool pool);

    /**
     * Gets the name of this field or method.
     *
     * @param pool the constant pool that maybe be required to convert a constant pool index to a name
     */
    Utf8Constant name(ConstantPool pool);

    /**
     * Gets the type of this field or signature of this method.
     *
     * @param pool the constant pool that maybe be required to convert a constant pool index to this field/method's type/signature.
     */
    Descriptor descriptor(ConstantPool pool);
}
