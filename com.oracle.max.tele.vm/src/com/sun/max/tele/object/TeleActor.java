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

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link Actor} in the VM.
 */
public abstract class TeleActor extends TeleTupleObject {

    /**
     * Field is final once non-null so cache it.
     */
    private Utf8Constant actorName;

    /**
     * This constructor follows no {@link References}. This avoids the infinite regress that can occur when the VM
     * object and another are mutually referential.
     */
    protected TeleActor(TeleVM vm, Reference actorReference) {
        super(vm, actorReference);
    }

    /**
     * @return contents of the {@link #flags} field, read from this
     *         {@link Actor} in the VM.
     */
    public final int getFlags() {
        return fields().Actor_flags.readInt(reference());
    }

    /**
     * Lists the textual names of flags that are set in this actor[
     *
     * Note that the the flag bit definitions for different subclasses of
     * {@link Actor} may vary, and different flags are specified as appropriate
     * for different entities. The approach here is to make no assumptions about
     * the correctness of the flags, but simply report what flags are set. If a
     * particular bit position is defined in more than one way, then all
     * definitions will be shown as a concatenation of the names, separated
     * by "/" to show that they define the
     * same bit position.  In other words, this method does not verify the
     * <strong>correctness</strong> in any way.
     *
     * @return strings containing the names of the constants associated with each
     *         flag that is set.
     */
    public final String[] getFlagNames() {
        final int flagsValue = getFlags();
        int nextFlagIndex = 0;
        final String[] flags = new String[vm().platform().nBytesInWord()];
        if ((flagsValue & Actor.ACC_PUBLIC) != 0) {
            // 0x00000001
            flags[nextFlagIndex++] = "ACC_PUBLIC";
        }
        if ((flagsValue & Actor.ACC_PRIVATE) != 0) {
            // 0x00000002
            flags[nextFlagIndex++] = "ACC_PRIVATE";
        }
        if ((flagsValue & Actor.ACC_PROTECTED) != 0) {
            // 0x00000004
            flags[nextFlagIndex++] = "ACC_PROTECTED";
        }
        if ((flagsValue & Actor.ACC_STATIC) != 0) {
            // 0x00000008
            flags[nextFlagIndex++] = "ACC_STATIC";
        }
        if ((flagsValue & Actor.ACC_FINAL) != 0) {
            // 0x00000010
            flags[nextFlagIndex++] = "ACC_FINAL";
        }
        if ((flagsValue & Actor.ACC_SUPER) != 0) {
            // 0x00000020, doubly defined
            flags[nextFlagIndex++] = "ACC_SUPER/ACC_SYNCHRONIZED";
        }
        if ((flagsValue & Actor.ACC_BRIDGE) != 0) {
            // 0x00000040, doubly defined
            flags[nextFlagIndex++] = "ACC_BRIDGE/ACC_VOLATILE";
        }
        if ((flagsValue & Actor.ACC_TRANSIENT) != 0) {
            // 0x00000080, doubly defined
            flags[nextFlagIndex++] = "ACC_TRANSIENT/ACC_VARARGS";
        }
        if ((flagsValue & Actor.ACC_NATIVE) != 0) {
            // 0x00000100
            flags[nextFlagIndex++] = "ACC_NATIVE";
        }
        if ((flagsValue & Actor.ACC_INTERFACE) != 0) {
            // 0x00000200
            flags[nextFlagIndex++] = "ACC_INTERFACE";
        }
        if ((flagsValue & Actor.ACC_ABSTRACT) != 0) {
            // 0x00000400
            flags[nextFlagIndex++] = "ACC_ABSTRACT";
        }
        if ((flagsValue & Actor.ACC_STRICT) != 0) {
            // 0x00000800
            flags[nextFlagIndex++] = "ACC_STRICT";
        }
        if ((flagsValue & Actor.ACC_SYNTHETIC) != 0) {
            // 0x00001000
            flags[nextFlagIndex++] = "ACC_SYNTHETIC";
        }
        if ((flagsValue & Actor.ACC_ANNOTATION) != 0) {
            // 0x00002000, doubly defined
            flags[nextFlagIndex++] = "ACC_ANNOTATION";
        }
        if ((flagsValue & Actor.ACC_ENUM) != 0) {
            // 0x00004000, doubly defined
            flags[nextFlagIndex++] = "ACC_ENUM/NO_SAFEPOINTS";
        }
        if ((flagsValue & Actor.DEPRECATED) != 0) {
            // 0x00008000, doubly defined
            flags[nextFlagIndex++] = "DEPRECATED";
        }
        if ((flagsValue & Actor.INJECTED) != 0) {
            // 0x00010000, doubly defined
            flags[nextFlagIndex++] = "INJECTED/VERIFIED";
        }
        if ((flagsValue & Actor.CONSTANT) != 0) {
            // 0x00020000
            flags[nextFlagIndex++] = "CONSTANT";
        }
        if ((flagsValue & Actor.CONSTANT_WHEN_NOT_ZERO) != 0) {
            // 0x00040000
            flags[nextFlagIndex++] = "CONSTANT_WHEN_NOT_ZERO";
        }
        if ((flagsValue & Actor.INNER_CLASS) != 0) {
            // 0x00100000, doubly defined
            flags[nextFlagIndex++] = "INNER_CLASS";
        }
        if ((flagsValue & Actor.TEMPLATE) != 0) {
            // 0x00200000
            flags[nextFlagIndex++] = "TEMPLATE";
        }
        if ((flagsValue & Actor.INITIALIZER) != 0) {
            // 0x00400000, doubly defined
            flags[nextFlagIndex++] = "INITIALIZER";
        }
        if ((flagsValue & Actor.C_FUNCTION) != 0) {
            // 0x01000000
            flags[nextFlagIndex++] = "C_FUNCTION";
        }
        if ((flagsValue & Actor.VM_ENTRY_POINT) != 0) {
            // 0x02000000
            flags[nextFlagIndex++] = "VM_ENTRY_POINT";
        }
        if ((flagsValue & Actor.FOLD) != 0) {
            // 0x04000000
            flags[nextFlagIndex++] = "FOLD";
        }
        if ((flagsValue & Actor.LOCAL_SUBSTITUTE) != 0) {
            // 0x10000000
            flags[nextFlagIndex++] = "LOCAL_SUBSTITUTE";
        }
        if ((flagsValue & Actor.UNSAFE) != 0) {
            // 0x20000000
            flags[nextFlagIndex++] = "UNSAFE";
        }
        if ((flagsValue & Actor.INLINE) != 0) {
            // 0x40000000
            flags[nextFlagIndex++] = "INLINE";
        }
        if ((flagsValue & Actor.NEVER_INLINE) != 0) {
            // 0x80000000
            flags[nextFlagIndex++] = "NEVER_INLINE";
        }
        return Arrays.copyOf(flags, nextFlagIndex);
    }

    public boolean isNative() {
        return Actor.isNative(getFlags());
    }

    /**
     * Gets the local {@link Actor} instance corresponding to this tele actor.
     */
    protected abstract Actor actor();

    /**
     * @return the generic name of this {@link Actor}
     */
    protected final Utf8Constant actorName() {
        if (actorName == null) {
            // Have to read the name using low level operations, because the name needed
            // to create the local instance of the Actor.
            Reference utf8ConstantReference = fields().Actor_name.readReference(reference());
            TeleUtf8Constant teleUtf8Constant = (TeleUtf8Constant) objects().makeTeleObject(utf8ConstantReference);
            actorName = teleUtf8Constant.utf8Constant();
        }
        return actorName;
    }

    /** {@inheritDoc}
     * <br>
     * For the purposes of inspection, use a locally loaded copy of each actor.
     */
    @Override
    protected Object createDeepCopy(DeepCopier context) {
        return actor();
    }
}
