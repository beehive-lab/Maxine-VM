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
 * Inspector's canonical surrogate for an object of type {@link ClassMethodRefConstant} in the VM.
 */
public abstract class TeleClassMethodRefConstant extends TelePoolConstant {

    public TeleClassMethodRefConstant(TeleVM vm, Reference classMethodRefConstantReference) {
        super(vm, classMethodRefConstantReference);
    }

    @Override
    public String maxineTerseRole() {
        return "ClassMethodRefConst";
    }

    /**
     * Inspector's canonical surrogate for an object of type {@link ClassMethodRefConstant.Resolved} in the VM.
     *
     */
    public static final class Resolved extends TeleClassMethodRefConstant {

        private TeleClassMethodActor teleClassMethodActor;

        public Resolved(TeleVM vm, Reference resolvedClassMethodRefConstantReference) {
            super(vm, resolvedClassMethodRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return true;
        }

        /**
         * @return surrogate for the {@ClassMethodActor} in the VM to which the constant was resolved
         */
        public TeleClassMethodActor getTeleClassMethodActor() {
            if (teleClassMethodActor == null) {
                final Reference methodActorReference = vm().teleFields().ResolvedMethodRefConstant_methodActor.readReference(reference());
                teleClassMethodActor = (TeleClassMethodActor) heap().makeTeleObject(methodActorReference);
            }
            return teleClassMethodActor;
        }

        @Override
        public TeleClassMethodActor getTeleClassMethodActorForObject() {
            return getTeleClassMethodActor();
        }

        @Override
        public String maxineRole() {
            return "Resolved ClassMethodRefConstant";
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link ClassMethodRefConstant.Unresolved} in the VM.
     *
     */
    public static final class Unresolved extends TeleClassMethodRefConstant {

        public Unresolved(TeleVM vm, Reference resolvedClassMethodRefConstantReference) {
            super(vm, resolvedClassMethodRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public String maxineRole() {
            return "Unresolved ClassMethodRefConstant";
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link ClassMethodRefConstant.UnresolvedIndices} in the VM.
     *
     */
    public static final class UnresolvedIndices extends TeleClassMethodRefConstant {

        public UnresolvedIndices(TeleVM vm, Reference resolvedClassMethodRefConstantReference) {
            super(vm, resolvedClassMethodRefConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public String maxineRole() {
            return "UnresolvedIndices ClassMethodRefConstant";
        }

    }

}
