/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.tele.reference.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * Inspector's canonical surrogate for an object of type {@link ClassConstant} in the VM.
 */
public abstract class TeleClassConstant extends TelePoolConstant {

    protected TeleClassConstant(TeleVM vm, RemoteReference classConstantReference) {
        super(vm, classConstantReference);
    }

    /**
     * Inspector's canonical surrogate for an object of type {@link ClassConstant.Unresolved} in the VM.
     *
     */
    public static final class Unresolved extends TelePoolConstant {

        public Unresolved(TeleVM vm, RemoteReference unresolvedClassConstantReference) {
            super(vm, unresolvedClassConstantReference);
        }

        @Override
        public boolean isResolved() {
            return false;
        }

    }

    /**
     * Inspector's canonical surrogate for an object of type {@link ClassConstant.Resolved} in the VM.
     *
     */
    public static final class Resolved extends TeleClassConstant{

        private TeleClassActor teleClassActor;

        public Resolved(TeleVM vm, RemoteReference resolvedClassConstantReference) {
            super(vm, resolvedClassConstantReference);
        }

        @Override
        public boolean isResolved() {
            return true;
        }
        /**
         * @return surrogate for the {@ClassActor} in the VM to which the constant was resolved
         */
        public TeleClassActor getTeleClassActor() {
            if (teleClassActor == null) {
                final RemoteReference classActorReference = fields().ClassConstant$Resolved_classActor.readReference(reference());
                teleClassActor = (TeleClassActor) objects().makeTeleObject(classActorReference);
            }
            return teleClassActor;
        }

        @Override
        public String maxineRole() {
            return "Resolved ClassRefConstant";
        }

        @Override
        public String maxineTerseRole() {
            return "ClassRefConst";
        }

    }

}
