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
package com.sun.max.vm.runtime;

import java.security.*;
import java.util.Arrays;

import com.sun.max.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;

/**
 * An abstract description of Java execution state, that encapsulates the following:
 * - method actor
 * - byte code location,
 * - locations that represent Java locals,
 * - locations that represent Java stack slots,
 * - inlining and the abstract state of logical caller frames.
 *
 * One of these descriptors needs to be held at every safepoint, call site and implicit exception points.
 * When a method has been inlined, all the descriptors in its inlined body
 * must be extended with a "parent" link to the former call site's descriptor.
 * This supports reconstructing Java frames of inlined methods.
 *
 * These frame are used to implement:
 * - de-optimization
 * - stack traces
 * - stack inspection (e.g. {@link AccessController#getStackAccessControlContext()})
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class JavaFrameDescriptor<Slot_Type> extends BytecodeLocation {

    /**
     * The Java frame state at the inlined call site of this frame.
     */
    public final JavaFrameDescriptor<Slot_Type> parent;

    /**
     * An array of locations mapping to the Java locals at the same index.
     */
    public final Slot_Type[] locals;

    /**
     * An array of locations mapping to the Java expression stack slots at the same index.
     */
    public final Slot_Type[] stackSlots;

    /**
     * Creates a Java frame descriptor.
     *
     * @param parent the descriptor of the logical caller Java frame. This value may be {@code null}.
     * @param classMethodActor the class method actor of the frame
     * @param bytecodePosition the bytecode position of the frame
     * @param locals the locations of all local variables in the Java frame, with corresponding indices in the array
     * @param stackSlots the locations of all Java expression stack slots in the Java frame, with corresponding indices in the array
     */
    public JavaFrameDescriptor(JavaFrameDescriptor<Slot_Type> parent, ClassMethodActor classMethodActor, int bytecodePosition, Slot_Type[] locals, Slot_Type[] stackSlots) {
        super(classMethodActor, bytecodePosition);
        assert classMethodActor != null;
        this.parent = parent;
        this.locals = locals;
        this.stackSlots = stackSlots;
    }

    /**
     * Gets the Java frame state of the logical frame that called this frame where the call has been inlined.
     *
     * @return {@code null} to indicate this is top level frame of an inlining tree
     */
    @Override
    public JavaFrameDescriptor<Slot_Type> parent() {
        return parent;
    }

    /**
     * @return how many logical Java frames are represented by this descriptor
     */
    public final int depth() {
        int result = 1;
        JavaFrameDescriptor j = parent;
        while (j != null) {
            result++;
            j = j.parent;
        }
        return result;
    }

    public final int maxSlots() {
        return Math.max(stackSlots.length, locals.length);
    }

    protected boolean slotsEqual(Slot_Type[] slots1, Slot_Type[] slots2) {
        return Arrays.equals(slots1, slots2);
    }

    @Override
    public boolean equals(Object other) {
        if (getClass().isInstance(other)) {
            final Class<JavaFrameDescriptor<Slot_Type>> type = null;
            final JavaFrameDescriptor<Slot_Type> descriptor = Utils.cast(type, other);
            if (classMethodActor.equals(descriptor.classMethodActor) &&
                            bytecodePosition == descriptor.bytecodePosition &&
                            slotsEqual(locals, descriptor.locals) &&
                            slotsEqual(stackSlots, descriptor.stackSlots)) {
                if (parent == null) {
                    return descriptor.parent() == null;
                }
                return parent.equals(descriptor.parent);
            }
        }
        return false;
    }

    public final String toMultiLineString() {
        String s = "";
        JavaFrameDescriptor javaFrameDescriptor = this;
        do {
            if (!s.isEmpty()) {
                s += "\n  ---parent---\n";
            }
            final ClassMethodActor classMethodActor = javaFrameDescriptor.classMethodActor;
            final int bytecodePosition = javaFrameDescriptor.bytecodePosition;
            s += " where: " + classMethodActor.toStackTraceElement(bytecodePosition) + "@" + bytecodePosition;
            s += "\nlocals:";
            for (int i = 0; i < javaFrameDescriptor.locals.length; i++) {
                s += " [" + i + "]=" + javaFrameDescriptor.locals[i];
            }
            s += "\n stack:";
            for (int i = 0; i < javaFrameDescriptor.stackSlots.length; i++) {
                s += " [" + i + "]=" + javaFrameDescriptor.stackSlots[i];
            }
            javaFrameDescriptor = javaFrameDescriptor.parent();
        } while (javaFrameDescriptor != null);
        return s;

    }

    @Override
    public final String toString() {
        String s = "";
        JavaFrameDescriptor javaFrameDescriptor = this;

        do {
            final ClassMethodActor classMethodActor = javaFrameDescriptor.classMethodActor;
            final int bytecodePosition = javaFrameDescriptor.bytecodePosition;
            s += String.format("<<%s@%s locals:[%s] stack:[%s]>>", classMethodActor.format("%h.%n(%s)", bytecodePosition),
                            bytecodePosition,
                            com.sun.max.Utils.toString(javaFrameDescriptor.locals, ", "),
                            com.sun.max.Utils.toString(javaFrameDescriptor.stackSlots, ", "));
            javaFrameDescriptor = javaFrameDescriptor.parent();
        } while (javaFrameDescriptor != null);
        return s;
    }
}
