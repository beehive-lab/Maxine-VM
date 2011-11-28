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
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Canonical surrogate for an object of type {@link ConstantPool} in the VM .
 */
public final class TeleConstantPool extends TeleTupleObject{

    private Reference constantsArrayReference;

    TeleConstantPool(TeleVM vm, Reference constantPoolReference) {
        super(vm, constantPoolReference);
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return getTeleHolder().classActor().constantPool();
    }

    private Reference constantsArrayReference() {
        if (constantsArrayReference == null) {
            constantsArrayReference = fields().ConstantPool_constants.readReference(reference());
        }
        return constantsArrayReference;
    }

    // TODO (mlvdv) fix this up when interfaces added
    /**
     * @param index specifies an entry in this pool
     * @return surrogate for the entry in the VM  of this pool
     * @throws MaxVMBusyException TODO
     */
    public TelePoolConstant readTelePoolConstant(int index) throws MaxVMBusyException {
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            final Reference poolConstantReference = objects().unsafeReadArrayElementValue(Kind.REFERENCE, constantsArrayReference(), index).asReference();
            final TeleObject teleObject = objects().makeTeleObject(poolConstantReference);
            if (!(teleObject instanceof TelePoolConstant)) {
                return null;
            }
            return (TelePoolConstant) teleObject;
        } finally {
            vm().releaseLegacyVMAccess();
        }
    }

    /**
     * @return surrogate for the {@link ClassActor} object in the VM  that includes this pool
     */
    public TeleClassActor getTeleHolder() {
        final Reference classActorReference = fields().ConstantPool_holder.readReference(reference());
        return (TeleClassActor) objects().makeTeleObject(classActorReference);
    }

    @Override
    public String maxineRole() {
        return "ConstantPool";
    }

    @Override
    public String maxineTerseRole() {
        return "ConstantPool";
    }

}
