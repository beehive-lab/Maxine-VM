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
 * Canonical surrogate for an object of type {@link Utf8Constant} in the VM.
 */
public final class TeleUtf8Constant extends TelePoolConstant {

    protected TeleUtf8Constant(TeleVM vm, Reference utf8ConstantReference) {
        super(vm, utf8ConstantReference);
    }

    // The field is final once non-null; cache it.
    private Utf8Constant utf8Constant;

    /**
     * @return a local copy of this object in the VM.
     */
    public Utf8Constant utf8Constant() {
        if (utf8Constant == null) {
            Reference reference = fields().Utf8Constant_string.readReference(reference());
            TeleString teleString = (TeleString) objects().makeTeleObject(reference);
            if (teleString != null) {
                utf8Constant = SymbolTable.makeSymbol(teleString.getString());
            }
        }
        return utf8Constant;
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return utf8Constant();
    }

    @Override
    public String maxineRole() {
        return "Utf8Constant";
    }

    @Override
    public String maxineTerseRole() {
        return "Utf8Const";
    }

}
