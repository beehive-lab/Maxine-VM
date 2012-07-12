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

import java.lang.reflect.*;

import com.sun.max.tele.*;
import com.sun.max.tele.heap.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Canonical surrogate for an object of type {@link ConstantPool} in the VM .
 */
public final class TeleConstantPool extends TeleTupleObject {

    private RemoteReference constantsArrayReference = vm().referenceManager().zeroReference();

    TeleConstantPool(TeleVM vm, RemoteReference constantPoolReference) {
        super(vm, constantPoolReference);
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        ConstantPool constantPool = getTeleHolder().classActor().constantPool();
        if (context instanceof TeleClassMethodActor.NativeStubCodeAttributeDeepCopier) {
            TeleArrayObject teleConstants = (TeleArrayObject) VmObjectAccess.make(vm()).makeTeleObject(constantsArrayReference());
            PoolConstant[] constants = (PoolConstant[]) teleConstants.deepCopy(context);
            Field constantsField = fields().ConstantPool_constants.fieldActor().toJava();
            constantsField.setAccessible(true);
            try {
                // Patch the constants
                constantsField.set(constantPool, constants);
            } catch (IllegalArgumentException e) {
                TeleWarning.message("Failed to patch local ContantPool with copied constants");
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                TeleWarning.message("Failed to patch local ConstantPool with copied constants");
                e.printStackTrace();
            }
        }
        return constantPool;
    }

    private RemoteReference constantsArrayReference() {
        if (constantsArrayReference.isZero()) {
            constantsArrayReference = jumpForwarder(fields().ConstantPool_constants.readReference(reference()));
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
            final RemoteReference poolConstantReference = jumpForwarder((RemoteReference) objects().unsafeReadArrayElementValue(Kind.REFERENCE, constantsArrayReference(), index).asReference());
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
        final RemoteReference classActorReference = jumpForwarder(fields().ConstantPool_holder.readReference(reference()));
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
