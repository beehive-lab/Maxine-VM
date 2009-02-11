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
import java.util.*;
import java.util.Arrays;

import com.sun.max.lang.*;
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
    private final JavaFrameDescriptor<Slot_Type> _parent;

    private final Slot_Type[] _locals;

    private final Slot_Type[] _stackSlots;

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
        _parent = parent;
        _locals = locals;
        _stackSlots = stackSlots;
    }

    /**
     * Gets the Java frame state of the logical frame that called this frame where the call has been inlined.
     *
     * @return {@code null} to indicate this is top level frame of an inlining tree
     */
    public JavaFrameDescriptor<Slot_Type> parent() {
        return _parent;
    }

    /**
     * @return how many logical Java frames are represented by this descriptor
     */
    public final int depth() {
        int result = 1;
        JavaFrameDescriptor j = _parent;
        while (j != null) {
            result++;
            j = j._parent;
        }
        return result;
    }

    /**
     * Gets an iterator over all the frames in the sequence of frames resulting from inlining that are terminated by
     * this frame. That is, this frame is the inner most call in the inlined sequence. The first element produced by the
     * return iterator is this frame, the next is this frame's {@linkplain #parent() parent} and so on.
     */
    public Iterator<JavaFrameDescriptor<Slot_Type>> inlinedFrames() {
        return new Iterator<JavaFrameDescriptor<Slot_Type>>() {
            JavaFrameDescriptor<Slot_Type> _next = JavaFrameDescriptor.this;
            public boolean hasNext() {
                return _next != null;
            }
            public JavaFrameDescriptor<Slot_Type> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                final JavaFrameDescriptor<Slot_Type> next = _next;
                _next = _next._parent;
                return next;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Gets an array of locations mapping to the Java locals at the same index.
     */
    public final Slot_Type[] locals() {
        return _locals;
    }

    /**
     * Gets an array of locations mapping to the Java expression stack slots at the same index.
     */
    public final Slot_Type[] stackSlots() {
        return _stackSlots;
    }

    public final int maxSlots() {
        return Math.max(_stackSlots.length, _locals.length);
    }

    protected boolean slotsEqual(Slot_Type[] slots1, Slot_Type[] slots2) {
        return Arrays.equals(slots1, slots2);
    }

    @Override
    public boolean equals(Object other) {
        if (getClass().isInstance(other)) {
            final Class<JavaFrameDescriptor<Slot_Type>> type = null;
            final JavaFrameDescriptor<Slot_Type> descriptor = StaticLoophole.cast(type, other);
            if (classMethodActor().equals(descriptor.classMethodActor()) &&
                            bytecodePosition() == descriptor.bytecodePosition() &&
                            slotsEqual(_locals, descriptor._locals) &&
                            slotsEqual(_stackSlots, descriptor._stackSlots)) {
                if (_parent == null) {
                    return descriptor.parent() == null;
                }
                return _parent.equals(descriptor._parent);
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
            final ClassMethodActor classMethodActor = javaFrameDescriptor.classMethodActor();
            s += " where: " + classMethodActor.format("%H.%n(%p)") + "@" + javaFrameDescriptor.bytecodePosition();
            s += "\nlocals:";
            for (int i = 0; i < javaFrameDescriptor._locals.length; i++) {
                s += " [" + i + "]=" + javaFrameDescriptor._locals[i];
            }
            s += "\n stack:";
            for (int i = 0; i < javaFrameDescriptor._stackSlots.length; i++) {
                s += " [" + i + "]=" + javaFrameDescriptor._stackSlots[i];
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
            final ClassMethodActor classMethodActor = javaFrameDescriptor.classMethodActor();
            s += String.format("<<%s@%s locals:[%s] stack:[%s]>>", classMethodActor.format("%h.%n"), javaFrameDescriptor.bytecodePosition(),
                            com.sun.max.lang.Arrays.toString(javaFrameDescriptor._locals, ", "),
                            com.sun.max.lang.Arrays.toString(javaFrameDescriptor._stackSlots, ", "));
            javaFrameDescriptor = javaFrameDescriptor.parent();
        } while (javaFrameDescriptor != null);
        return s;
    }
}
