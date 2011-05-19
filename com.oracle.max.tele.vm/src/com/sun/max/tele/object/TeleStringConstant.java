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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link StringConstant} in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class TeleStringConstant extends TelePoolConstant {

    protected TeleStringConstant(TeleVM vm, Reference stringConstantReference) {
        super(vm, stringConstantReference);
    }

    // The field is final; cache it.
    private String value;

    /**
     * @return a local copy of the string contained in this object in the tele VM
     */
    public String getString() {
        if (value == null) {
            final Reference stringReference = vm().teleFields().StringConstant_value.readReference(reference());
            final TeleString teleString = (TeleString) heap().makeTeleObject(stringReference);
            value = teleString.getString();
        }
        return value;
    }

    @Override
    public String maxineRole() {
        return "StringConstant";
    }

    @Override
    public String maxineTerseRole() {
        return "StringConst";
    }
}
