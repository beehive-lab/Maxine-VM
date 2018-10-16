/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.vm.methodhandle;
import static com.sun.max.vm.actor.member.InjectedReferenceFieldActor.*;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;

/**
 * Injected into MemberName instances to provide the linkage from method handles
 * to target method and field actors. Whereas HotSpot uses 2 injected fields, Maxine
 * currently combines both into the one VMTarget which is injected using
 * an {@link InjectedReferenceFieldActor}.
 */
public final class VMTarget {

    /**
     * The target member actor. This is analogous to the vmtarget oop which HotSpot injects into MemberName instances.
     */
    private MemberActor vmTarget;

    /** vtable / itable or field offset. */
    private int vmindex = VirtualMethodActor.INVALID_VTABLE_INDEX;

    public static final VMTarget VMTarget = new VMTarget();

    /**  */
    private VMTarget() {
    }

    /**
     * Creates a VMTarget and injects it into the MemberName argument.
     *
     * @param memberName
     * @return
     */
    public static VMTarget create(Object memberName) {
        VMTarget vmtarget = fromMemberName(memberName);

        if (vmtarget == null) {
            vmtarget = new VMTarget();
            MemberName_VmTarget.setObject(memberName, vmtarget);
        }
        Trace.line(1, "VMTarget.create: memberName id=", System.identityHashCode(memberName));
        return vmtarget;
    }

    /**
     * Returns the VMTarget which has been previously injected into a MemberName.
     *
     * @param memberName
     * @return
     */
    public static VMTarget fromMemberName(Object memberName) {
        if (MaxineVM.isHosted()) {
            return VMTarget;
        }
        VMTarget vmt = UnsafeCast.asVMTarget(MemberName_VmTarget.getObject(memberName));
        if (vmt != null) {
            Trace.line(1, "VMTarget.fromMemberName: memberName id=", System.identityHashCode(memberName));
        }
        return vmt;
    }

    /**
     * Returns true if the target represents a method.
     *
     * @return
     */
    public boolean isMethod() {
        return vmTarget != null && vmTarget instanceof MethodActor;
    }

    /**
     * Returns true if the target represents a field.
     *
     * @return
     */
    public boolean isField() {
        return vmTarget != null && vmTarget instanceof FieldActor;
    }

    /**
     * Returns the target as a ClassMethodActor.
     *
     * @return
     */
    public ClassMethodActor asClassMethodActor() {
        try {
            return (ClassMethodActor) vmTarget;
        } catch (ClassCastException x) {
            throw ProgramError.unexpected("vmtarget is not Method.");
        }
    }

    /**
     * Returns the target as a FieldActor.
     *
     * @return
     */
    public FieldActor asFieldActor() {
        try {
            return (FieldActor) vmTarget;
        } catch (ClassCastException x) {
            throw ProgramError.unexpected("vmtarget is not Field.");
        }
    }

    /**
     * Returns the target MemberActor.
     *
     * @return
     */
    public MemberActor getVmTarget() {
        return vmTarget;
    }

    /**
     *
     * @param m
     */
    public void setVmTarget(MemberActor m) {
        this.vmTarget = m;
    }

    /**
     *
     * @param index
     */
    public void setVMindex(int index) {
        this.vmindex = index;
    }

    /**
     * Returns the value of the vmindex field.
     *
     * @return
     */
    public int getVMindex() {
        return vmindex;
    }

    @Override
    public String toString() {
        return "vmtarget=" + ((vmTarget == null) ? "null" : vmTarget.toString()) + ", vmindex=" + vmindex;
    }
}
