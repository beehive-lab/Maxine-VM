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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;

/**
 * Inspector's canonical surrogate for an object of type {@link FieldRefConstant} in the VM.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleFieldRefConstant extends TelePoolConstant {

    public TeleFieldRefConstant(TeleVM vm, Reference fieldRefConstantReference) {
        super(vm, fieldRefConstantReference);
    }

    @Override
    public String maxineTerseRole() {
        return "FieldRefConst";
    }

     /**
     * Inspector's canonical surrogate for an object of type {@link FieldRefConstant.Resolved} in the VM.
     *
     * @author Michael Van De Vanter
     */
    public static final class Resolved extends TeleFieldRefConstant {

        public Resolved(TeleVM vm, Reference resolvedFieldRefConstantReference) {
            super(vm, resolvedFieldRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return true;
        }

        /**
         * @return surrogate for the {@FieldActor} in the VM to which the constant was resolved
         */
        public TeleFieldActor getTeleFieldActor() {
            final Reference fieldActorReference = vm().teleFields().FieldRefConstant$Resolved_fieldActor.readReference(reference());
            return (TeleFieldActor) heap().makeTeleObject(fieldActorReference);
        }

        @Override
        public String maxineRole() {
            return "Resolved FieldRefConstant";
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link FieldRefConstant.Unresolved} in the VM.
     *
     * @author Michael Van De Vanter
     */
    public static final class Unresolved extends TeleFieldRefConstant {

        public Unresolved(TeleVM vm, Reference unresolvedFieldRefConstantReference) {
            super(vm, unresolvedFieldRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public String maxineRole() {
            return "Unresolved FieldRefConstant";
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link FieldRefConstant.Unresolved} in the VM.
     *
     * @author Michael Van De Vanter
     */
    public static final class UnresolvedIndices extends TeleFieldRefConstant {

        public UnresolvedIndices(TeleVM vm, Reference unresolvedFieldRefConstantReference) {
            super(vm, unresolvedFieldRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public String maxineRole() {
            return "UnresolvedIndices FieldRefConstant";
        }

    }

}
