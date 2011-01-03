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
package com.sun.max.vm.classfile.create;

import com.sun.max.annotate.*;

/**
 * The common super class of all constant pool entries.
 *
 * @author Bernd Mathiske
 * @version 1.0
 */
abstract class MillConstant {

    static final byte CONSTANT_Utf8 = 1;
    static final byte CONSTANT_Integer = 3;
    static final byte CONSTANT_Float = 4;
    static final byte CONSTANT_Long = 5;
    static final byte CONSTANT_Double = 6;
    static final byte CONSTANT_Class = 7;
    static final byte CONSTANT_String = 8;
    static final byte CONSTANT_FieldRef = 9;
    static final byte CONSTANT_MethodRef = 10;
    static final byte CONSTANT_InterfaceMethodRef = 11;
    static final byte CONSTANT_NameAndType = 12;

    @CONSTANT
    byte tag;

    @CONSTANT
    int numberOfBytes;

    @CONSTANT
    int hashValue;

    MillConstant next;
    int index = -1;

    protected MillConstant(byte tag, int nBytes, int hashValue) {
        this.tag = tag;
        this.numberOfBytes = nBytes;
        this.hashValue = hashValue;
    }

    /**
     * Returns a hash code value for the object.
     * 
     * @return A hash code value for this object..
     */
    @Override
    public int hashCode() {
        return hashValue;
    }

}
