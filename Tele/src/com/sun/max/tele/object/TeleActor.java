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
import com.sun.max.vm.actor.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link Actor} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleActor extends TeleTupleObject {

    protected TeleActor(TeleVM vm, Reference actorReference) {
        super(vm, actorReference);
    }

    /**
     * @return the generic name of this {@link Actor} copied from the {@link TeleVM}.
     */
    public final String getName() {
        return getTeleName().utf8Constant().string;
    }

    /**
     * Field is final once non-null so cache it.
     */
    private TeleUtf8Constant name;

    public final TeleUtf8Constant getTeleName() {
        if (name == null) {
            Reference utf8ConstantReference = vm().teleFields().Actor_name.readReference(reference());
            name = (TeleUtf8Constant) heap().makeTeleObject(utf8ConstantReference);
        }
        return name;
    }

    /**
     * Local copy of the actor.
     */
    private Actor actor;

    /**
     * Subclasses override this method to create the local actor of the relevant type.
     */
    protected abstract Actor initActor();

    /**
     * Gets the local {@link Actor} instance corresponding to this tele actor.
     */
    public final Actor actor() {
        if (actor == null) {
            actor = initActor();
        }
        return actor;
    }

    @Override
    protected final Object createDeepCopy(DeepCopier context) {
        return actor();
    }

    /**
     * @return contents of the {@link #flags} field, read from this
     *         {@link Actor} in the {@link TeleVM}.
     */
    public final int getFlags() {
        return vm().teleFields().Actor_flags.readInt(reference());
    }

    /**
     * Creates a textual display showing which flags are set.
     *
     * Note that the the flag bit definitions for different subclasses of
     * {@link Actor} may vary, and different flags are specified as appropriate
     * for different entities. The approach here is to make no assumptions about
     * the correctness of the flags, but simply report what flags are set. If a
     * particular bit position is defined in more than one way, then all
     * definitions will be shown, connected by "/" to show that they define the
     * same bit position.  In other words, this label does not verify the
     * <strong>correctness</strong> in any way.
     *
     * @return a string listing the names of the constants associated with each
     *         flag that is set.
     */
    public final String flagsAsString() {
        final int flagsValue = getFlags();
        final StringBuilder sb = new StringBuilder(100);
        if ((flagsValue & Actor.ACC_PUBLIC) != 0) {
            // 0x00000001
            sb.append("ACC_PUBLIC, ");
        }
        if ((flagsValue & Actor.ACC_PRIVATE) != 0) {
            // 0x00000002
            sb.append("ACC_PRIVATE, ");
        }
        if ((flagsValue & Actor.ACC_PROTECTED) != 0) {
            // 0x00000004
            sb.append("ACC_PROTECTED, ");
        }
        if ((flagsValue & Actor.ACC_STATIC) != 0) {
            // 0x00000008
            sb.append("ACC_STATIC, ");
        }
        if ((flagsValue & Actor.ACC_FINAL) != 0) {
            // 0x00000010
            sb.append("ACC_FINAL, ");
        }
        if ((flagsValue & Actor.ACC_SUPER) != 0) {
            // 0x00000020, doubly defined
            sb.append("ACC_SUPER/ACC_SYNCHRONIZED, ");
        }
        if ((flagsValue & Actor.ACC_BRIDGE) != 0) {
            // 0x00000040, doubly defined
            sb.append("ACC_BRIDGE/ACC_VOLATILE, ");
        }
        if ((flagsValue & Actor.ACC_TRANSIENT) != 0) {
            // 0x00000080, doubly defined
            sb.append("ACC_TRANSIENT/ACC_VARARGS, ");
        }
        if ((flagsValue & Actor.ACC_NATIVE) != 0) {
            // 0x00000100
            sb.append("ACC_NATIVE, ");
        }
        if ((flagsValue & Actor.ACC_INTERFACE) != 0) {
            // 0x00000200
            sb.append("ACC_INTERFACE, ");
        }
        if ((flagsValue & Actor.ACC_ABSTRACT) != 0) {
            // 0x00000400
            sb.append("ACC_ABSTRACT, ");
        }
        if ((flagsValue & Actor.ACC_STRICT) != 0) {
            // 0x00000800
            sb.append("ACC_STRICT, ");
        }
        if ((flagsValue & Actor.ACC_SYNTHETIC) != 0) {
            // 0x00001000
            sb.append("ACC_SYNTHETIC, ");
        }
        if ((flagsValue & Actor.ACC_ANNOTATION) != 0) {
            // 0x00002000, doubly defined
            sb.append("ACC_ANNOTATION/SIGNAL_HANDLER, ");
        }
        if ((flagsValue & Actor.ACC_ENUM) != 0) {
            // 0x00004000, doubly defined
            sb.append("ACC_ENUM/NO_SAFEPOINTS, ");
        }
        if ((flagsValue & Actor.DEPRECATED) != 0) {
            // 0x00008000, doubly defined
            sb.append("DEPRECATED/SIGNAL_STUB, ");
        }
        if ((flagsValue & Actor.INJECTED) != 0) {
            // 0x00010000, doubly defined
            sb.append("INJECTED/INLINE_AFTER_SNIPPETS_ARE_COMPILED, ");
        }
        if ((flagsValue & Actor.CONSTANT) != 0) {
            // 0x00020000
            sb.append("CONSTANT, ");
        }
        if ((flagsValue & Actor.CONSTANT_WHEN_NOT_ZERO) != 0) {
            // 0x00040000
            sb.append("CONSTANT_WHEN_NOT_ZERO, ");
        }
        if ((flagsValue & Actor.INNER_CLASS) != 0) {
            // 0x00100000, doubly defined
            sb.append("INNER_CLASS/WRAPPER, ");
        }
        if ((flagsValue & Actor.TEMPLATE) != 0) {
            // 0x00200000
            sb.append("TEMPLATE, ");
        }
        if ((flagsValue & Actor.INITIALIZER) != 0) {
            // 0x00400000, doubly defined
            sb.append("INITIALIZER/GENERATED, ");
        }
        if ((flagsValue & Actor.C_FUNCTION) != 0) {
            // 0x01000000
            sb.append("C_FUNCTION, ");
        }
        if ((flagsValue & Actor.VM_ENTRY_POINT) != 0) {
            // 0x02000000
            sb.append("VM_ENTRY_POINT, ");
        }
        if ((flagsValue & Actor.FOLD) != 0) {
            // 0x04000000
            sb.append("FOLD, ");
        }
        if ((flagsValue & Actor.BUILTIN) != 0) {
            // 0x08000000
            sb.append("BUILTIN, ");
        }
        if ((flagsValue & Actor.LOCAL_SUBSTITUTE) != 0) {
            // 0x10000000
            sb.append("SUBSTITUTE, ");
        }
        if ((flagsValue & Actor.UNSAFE) != 0) {
            // 0x20000000
            sb.append("UNSAFE, ");
        }
        if ((flagsValue & Actor.INLINE) != 0) {
            // 0x40000000
            sb.append("INLINE, ");
        }
        if ((flagsValue & Actor.NEVER_INLINE) != 0) {
            // 0x80000000
            sb.append("NEVER_INLINE, ");
        }
        return sb.toString();
    }
}
