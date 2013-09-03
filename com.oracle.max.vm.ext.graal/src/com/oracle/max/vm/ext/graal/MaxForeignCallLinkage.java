/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

public class MaxForeignCallLinkage implements ForeignCallLinkage {
    /**
     * Constants for specifying whether a foreign call destroys or preserves registers. A foreign
     * call will always destroy {@link MaxForeignCallLinkage#getCallingConvention() its}
     * {@linkplain ForeignCallLinkage#getTemporaries() temporary} registers.
     */
    public enum RegisterEffect {
        DESTROYS_REGISTERS, PRESERVES_REGISTERS
    }

    /**
     * Constants for specifying whether a call is a leaf or not. A leaf function does not lock, GC
     * or throw exceptions. That is, the thread's execution state during the call is never inspected
     * by another thread.
     */
    public enum Transition {
        LEAF, NOT_LEAF;
    }

    private final MaxForeignCallDescriptor descriptor;
    private final CallingConvention outGoingCallingConvention;
    private final CallingConvention incomingCallingConvention;
    private final LocationIdentity[] killedLocations;
    private final RegisterEffect effect;
    private final Transition transition;
    private final boolean reexecutable;


    @HOSTED_ONLY
    MaxForeignCallLinkage(MaxForeignCallDescriptor descriptor, RegisterEffect effect, Transition transition, boolean reexecutable, LocationIdentity[] killedLocations) {
        this.descriptor = descriptor;
        this.effect = effect;
        this.transition = transition;
        this.killedLocations = killedLocations;
        this.reexecutable = reexecutable;
        RegisterConfig registerConfig = MaxRegisterConfig.get(MaxineVM.vm().registerConfigs.standard);
        JavaType resType = MaxForeignCallsMap.runtime.lookupJavaType(descriptor.getResultType());
        JavaType[] argTypes = MetaUtil.lookupJavaTypes(MaxForeignCallsMap.runtime, descriptor.getArgumentTypes());
        if (descriptor.getMethodActor() instanceof VirtualMethodActor) {
            JavaType[] newArgTypes = new JavaType[argTypes.length + 1];
            System.arraycopy(argTypes, 0, newArgTypes, 1, argTypes.length);
            newArgTypes[0] = MaxResolvedJavaType.get(ClassActor.fromJava(Object.class));
            argTypes = newArgTypes;
        }
        this.outGoingCallingConvention = registerConfig.getCallingConvention(CallingConvention.Type.JavaCall, resType, argTypes,
                        MaxForeignCallsMap.runtime.getTarget(), false);
        this.incomingCallingConvention = registerConfig.getCallingConvention(CallingConvention.Type.JavaCallee, resType, argTypes,
                        MaxForeignCallsMap.runtime.getTarget(), false);
    }

    @Override
    public long getMaxCallTargetOffset() {
        // TODO Check
        return -1;
    }

    @Override
    public ForeignCallDescriptor getDescriptor() {
        return descriptor;
    }

    public MaxForeignCallDescriptor getMaxRuntimeCall() {
        return descriptor;
    }

    public MethodActor getMethodActor() {
        return descriptor.getMethodActor();
    }

    @Override
    public Value[] getTemporaries() {
        return AllocatableValue.NONE;
    }

    @Override
    public boolean destroysRegisters() {
        return effect == RegisterEffect.DESTROYS_REGISTERS;
    }

    public boolean isReexecutable() {
        return reexecutable;
    }

    public LocationIdentity[] getKilledLocations() {
        return killedLocations;
    }

    /**
     * Determines if this is call to a function that does not lock, GC or throw exceptions. That is,
     * the thread's execution state during the call is never inspected by another thread.
     */
    public boolean canDeoptimize() {
        return transition != Transition.LEAF;
    }

    public CallingConvention getOutgoingCallingConvention() {
        return outGoingCallingConvention;
    }

    public CallingConvention getIncomingCallingConvention() {
        return incomingCallingConvention;
    }

}
