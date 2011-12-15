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
 * Inspector's canonical surrogate for an object of type {@link InterfaceMethodRefConstant} in the VM.
 */
public abstract class TeleInterfaceMethodRefConstant extends TelePoolConstant {

    public TeleInterfaceMethodRefConstant(TeleVM vm, Reference classMethodRefConstantReference) {
        super(vm, classMethodRefConstantReference);
    }

    @Override
    public String maxineTerseRole() {
        return "InterfaceMethodRefConst";
    }

    /**
     * Inspector's canonical surrogate for an object of type {@link InterfaceMethodRefConstant.Resolved} in the VM.
     *
     */
    public static final class Resolved extends TeleInterfaceMethodRefConstant {

        private TeleInterfaceMethodActor teleInterfaceMethodActor;

        public Resolved(TeleVM vm, Reference resolvedInterfaceMethodRefConstantReference) {
            super(vm, resolvedInterfaceMethodRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return true;
        }

        /**
         * @return surrogate for the {@InterfaceMethodActor} in the VM to which the constant was resolved
         */
        public TeleInterfaceMethodActor getTeleInterfaceMethodActor() {
            if (teleInterfaceMethodActor == null) {
                final Reference methodActorReference = fields().ResolvedMethodRefConstant_methodActor.readReference(reference());
                teleInterfaceMethodActor = (TeleInterfaceMethodActor) objects().makeTeleObject(methodActorReference);
            }
            return teleInterfaceMethodActor;
        }

        @Override
        public String maxineRole() {
            return "Resolved InterfaceMethodRefConstant";
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link InterfaceMethodRefConstant.Unresolved} in the VM.
     *
     */
    public static final class Unresolved extends TeleInterfaceMethodRefConstant {

        public Unresolved(TeleVM vm, Reference resolvedInterfaceMethodRefConstantReference) {
            super(vm, resolvedInterfaceMethodRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public String maxineRole() {
            return "Unresolved InterfaceMethodRefConstant";
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link InterfaceMethodRefConstant.UnresolvedIndices} in the VM.
     *
     */
    public static final class UnresolvedIndices extends TeleInterfaceMethodRefConstant {

        public UnresolvedIndices(TeleVM vm, Reference resolvedInterfaceMethodRefConstantReference) {
            super(vm, resolvedInterfaceMethodRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public String maxineRole() {
            return "UnresolvedIndices InterfaceMethodRefConstant";
        }

    }

}
