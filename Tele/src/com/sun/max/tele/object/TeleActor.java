/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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

    protected TeleActor(TeleVM teleVM, Reference actorReference) {
        super(teleVM, actorReference);
    }

    /**
     * @return the generic name of this {@link Actor} copied from the
     *         {@link TeleVM}.
     */
    public String readName() {
        final Reference utf8ConstantReference = teleVM().fields().Actor_name.readReference(reference());
        final TeleUtf8Constant teleUtf8Constant = (TeleUtf8Constant) TeleObject.make(teleVM(), utf8ConstantReference);
        return teleUtf8Constant.getString();
    }

    /**
     * @return contents of the {@link #_flags} field, read from this
     *         {@link Actor} in the {@link TeleVM}.
     */
    public int readFlags() {
        return teleVM().fields().Actor_flags.readInt(reference());
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
        final int flagsValue = readFlags();
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
        if ((flagsValue & Actor.CLASS_INITIALIZER) != 0) {
            // 0x00400000, doubly defined
            sb.append("CLASS_INITIALIZER/GENERATED, ");
        }
        if ((flagsValue & Actor.INSTANCE_INITIALIZER) != 0) {
            // 0x00800000
            sb.append("INSTANCE_INITIALIZER, ");
        }
        if ((flagsValue & Actor.C_FUNCTION) != 0) {
            // 0x01000000
            sb.append("C_FUNCTION, ");
        }
        if ((flagsValue & Actor.JNI_FUNCTION) != 0) {
            // 0x02000000
            sb.append("JNI_FUNCTION, ");
        }
        if ((flagsValue & Actor.FOLD) != 0) {
            // 0x04000000
            sb.append("FOLD, ");
        }
        if ((flagsValue & Actor.BUILTIN) != 0) {
            // 0x08000000
            sb.append("BUILTIN, ");
        }
        if ((flagsValue & Actor.SURROGATE) != 0) {
            // 0x10000000
            sb.append("SURROGATE, ");
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
