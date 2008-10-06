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
/*VCSID=a370b22a-4bf8-40c9-8eb8-f73a9a693a7e*/
package com.sun.max.vm.runtime;

import java.security.*;
import java.util.Arrays;

import sun.reflect.*;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;

/**
 * An abstract description of Java execution state, including information about:
 * - byte code location,
 * - locations that represent Java locals,
 * - locations that represent Java stack slots,
 * - inlining and the abstract state of logical caller frames.
 *
 * One of these descriptors needs to be held at every safepoint and at every call site.
 * When a method has been inlined, all the descriptors in its inlined body
 * must be extended with a "parent" link to the former call site's descriptor.
 * This supports reconstructing Java frames of inlined methods.
 *
 * This will be used to implement dynamic deoptimization,
 * i.e. the transformation of an optimized stack frame at a safepoint
 * to a series of JIT stack frames that mimic the Java execution model.
 *
 * Meanwhile, it is also used to implement such features as {@link AccessController#getStackAccessControlContext()}
 * and {@link Reflection#getCallerClass(int)}.
 *
 * @author Bernd Mathiske
 */
public class JavaFrameDescriptor<Slot_Type> {

    private final JavaFrameDescriptor<Slot_Type> _parent;

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

    private final BytecodeLocation _bytecodeLocation;

    public final BytecodeLocation bytecodeLocation() {
        return _bytecodeLocation;
    }

    private final Slot_Type[] _locals;

    /**
     * @return an array of locations corresponding to the Java locals with the same index as the respective array index
     */
    public final Slot_Type[] locals() {
        return _locals;
    }

    private final Slot_Type[] _stackSlots;

    /**
     * @return an array of locations corresponding to the Java expression stack slots with the same index as the respective array index
     */
    public final Slot_Type[] stackSlots() {
        return _stackSlots;
    }

    public final int maxSlots() {
        return Math.max(_stackSlots.length, _locals.length);
    }

    /**
     * Creates a descriptor with a parent descriptor due to inlining.
     *
     * @param parent the descriptor of the logical caller Java frame
     * @param bytecodeLocation the bytecode location in the callee
     * @param locals the locations of all local variables in the Java frame, with corresponding indices in the array
     * @param stackSlots the locations of all Java expression stack slots in the Java frame, with corresponding indices in the array
     */
    public JavaFrameDescriptor(JavaFrameDescriptor<Slot_Type> parent, BytecodeLocation bytecodeLocation, Slot_Type[] locals, Slot_Type[] stackSlots) {
        _parent = parent;
        _bytecodeLocation = bytecodeLocation;
        _locals = locals;
        _stackSlots = stackSlots;
    }

    protected boolean slotsEqual(Slot_Type[] slots1, Slot_Type[] slots2) {
        return Arrays.equals(slots1, slots2);
    }

    @Override
    public boolean equals(Object other) {
        if (getClass().isInstance(other)) {
            final Class<JavaFrameDescriptor<Slot_Type>> type = null;
            final JavaFrameDescriptor<Slot_Type> descriptor = StaticLoophole.cast(type, other);
            if (_bytecodeLocation.equals(descriptor._bytecodeLocation) &&
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

    @Override
    public final int hashCode() {
        return _locals.length ^ _stackSlots.length;
    }

    public final String toMultiLineString() {
        String s = "";
        JavaFrameDescriptor javaFrameDescriptor = this;
        do {
            if (!s.isEmpty()) {
                s += "\n  ---parent---\n";
            }
            final ClassMethodActor classMethodActor = javaFrameDescriptor._bytecodeLocation.classMethodActor();
            s += " where: " + classMethodActor.holder() + "." + classMethodActor.name() + classMethodActor.descriptor().toJavaString(false, true) + "@" + _bytecodeLocation.position();
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
            final ClassMethodActor classMethodActor = javaFrameDescriptor._bytecodeLocation.classMethodActor();
            s += String.format("<<%s@%s locals:[%s] stack:[%s]>>", classMethodActor.name(), _bytecodeLocation.position(),
                            com.sun.max.lang.Arrays.toString(javaFrameDescriptor._locals, ", "),
                            com.sun.max.lang.Arrays.toString(javaFrameDescriptor._stackSlots, ", "));
            javaFrameDescriptor = javaFrameDescriptor.parent();
        } while (javaFrameDescriptor != null);
        return s;
    }
}
