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

import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
class MillMethod {

    final int modifiers;
    final int nameIndex;
    final int descriptorIndex;
    final MillCode code;
    final MillClassConstant[] exceptions;
    final int numberOfBytes;
    final MillMethod next;

    MillMethod(MillClass millClass, int modifiers, String name, SignatureDescriptor signatureDescriptor, MillCode code, MillClassConstant[] exceptions) {
        this.modifiers = modifiers;
        this.nameIndex = millClass.makeUtf8Constant(name).index;
        this.descriptorIndex = millClass.makeUtf8Constant(signatureDescriptor.toString()).index;
        this.code = code;
        this.exceptions = exceptions;
        this.numberOfBytes = 8 + 14 + code.nBytes() + 4 + ((exceptions.length > 0) ? 8 + (2 * exceptions.length) : 0);
        this.next = millClass.methodList;
        millClass.methodList = this;
    }

}
