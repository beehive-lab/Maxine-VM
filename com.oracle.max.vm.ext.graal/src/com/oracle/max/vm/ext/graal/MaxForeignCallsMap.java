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


import static com.oracle.max.vm.ext.graal.MaxForeignCallLinkage.RegisterEffect;
import static com.oracle.max.vm.ext.graal.MaxForeignCallLinkage.Transition;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.api.meta.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Maintains a map from a {@link MaxForeignCallDescriptor} to the {@link MaxForeignCallLinkage}.
 * A foreign call in Maxine is a call to the the VM runtime and must be annotated with {@link SNIPPET_SLOWPATH}.
 * Some calls are created statically, the majority are created dynamically as part of the snippet creation process.
 * Currently most calls are marked as not reexecutable, destroys registers and not leaf, with the exception of
 * those that otherwise cause an FrameState assertion (e.g. {@link VmThread#loadExceptionForHandler}.
 *
 * Ideally this should be much simpler in a meta-circular VM like Maxine. It really should not be necessary to
 * introduce a completely different method invocation node for what are, module possible deoptimization issues,
 * nornal Java calls.
 *
 */
public class MaxForeignCallsMap {

    static MaxRuntime runtime;

    private static Map<MaxForeignCallDescriptor, MaxForeignCallLinkage> map = new HashMap<>();
    private static Map<MethodActor, MaxForeignCallLinkage> actorMap = new HashMap<>();

    @SNIPPET_SLOWPATH
    private static void unwindException(Object throwable) throws Throwable {
        Throw.raise(UnsafeCast.asThrowable(throwable));
    }

    @HOSTED_ONLY
    public static void initialize(MaxRuntime runtime) {
        MaxForeignCallsMap.runtime = runtime;
        try {
            createCiRuntimeCall(CiRuntimeCall.RegisterFinalizer, null, RegisterEffect.DESTROYS_REGISTERS, Transition.NOT_LEAF, true, ALL_LOCATIONS);
            createCiRuntimeCall(CiRuntimeCall.CreateNullPointerException, null, RegisterEffect.DESTROYS_REGISTERS, Transition.NOT_LEAF, true, ALL_LOCATIONS);
            createCiRuntimeCall(CiRuntimeCall.CreateOutOfBoundsException, null, RegisterEffect.DESTROYS_REGISTERS, Transition.NOT_LEAF, true, ALL_LOCATIONS);
            createCiRuntimeCall(CiRuntimeCall.ArithmeticCos, "arithmeticCos", RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF, true, NO_LOCATIONS);
            createCiRuntimeCall(CiRuntimeCall.ArithmeticSin, "arithmeticSin", RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF, true, NO_LOCATIONS);
            createCiRuntimeCall(CiRuntimeCall.ArithmeticTan, "arithmeticTan", RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF, true, NO_LOCATIONS);
            createCiRuntimeCall(CiRuntimeCall.ArithmeticFrem, "arithmeticFrem", RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF, true, NO_LOCATIONS);
            createCiRuntimeCall(CiRuntimeCall.ArithmeticDrem, "arithmeticDrem", RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF, true, NO_LOCATIONS);
            Method unwindException = MaxForeignCallsMap.class.getDeclaredMethod("unwindException", Object.class);
            createRuntimeCall(unwindException.getName(), MethodActor.fromJava(unwindException), RegisterEffect.DESTROYS_REGISTERS, Transition.NOT_LEAF, false, ALL_LOCATIONS);
            new CriticalMethod(unwindException);
            new CriticalMethod(VmThread.class.getDeclaredMethod("throwJniException"));

        } catch (Exception ex) {
            ProgramError.unexpected(ex);
        }
    }

    @HOSTED_ONLY
    private static void createCiRuntimeCall(CiRuntimeCall call, String overrideName,
                    RegisterEffect effect, Transition transition, boolean reexecutable, LocationIdentity[] killedLocations) {
        ClassMethodActor cma = MaxRuntimeCalls.getClassMethodActor(call);
        createRuntimeCall(overrideName != null ? overrideName : toFirstLower(call.name()), cma,
                        effect, transition, reexecutable, killedLocations);
    }

    /**
     * Returns the argument with first character upper-cased.
     * @param s
     */
    @HOSTED_ONLY
    private static String toFirstLower(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    private static final LocationIdentity[] ALL_LOCATIONS = new LocationIdentity[] {LocationIdentity.ANY_LOCATION};
    private static final LocationIdentity[] NO_LOCATIONS = new LocationIdentity[] {};


    @HOSTED_ONLY
    private static MaxForeignCallDescriptor createRuntimeCall(String methodName, MethodActor cma,
                    RegisterEffect effect, Transition transition, boolean reexecutable, LocationIdentity[] killedLocations) {
        Method method = cma.toJava();
        MaxForeignCallDescriptor maxRuntimeCall = new MaxForeignCallDescriptor(methodName, method, method.getReturnType(), method.getParameterTypes());
        MaxForeignCallLinkage maxRuntimeCallTarget = new MaxForeignCallLinkage(maxRuntimeCall, effect,
                        transition, reexecutable, killedLocations);
        map.put(maxRuntimeCall, maxRuntimeCallTarget);
        actorMap.put(cma, maxRuntimeCallTarget);
        return maxRuntimeCall;
    }

    /**
     * Gets the {@link RuntimeCallTarget} for a {@link Descriptor}.
     * @param descriptor must be a {@link MaxForeignCallDescriptor}
     * @return
     */
    public static MaxForeignCallLinkage get(ForeignCallDescriptor descriptor) {
        return map.get(descriptor);
    }

    public static MaxForeignCallDescriptor get(ClassMethodActor cma) {
        MaxForeignCallLinkage maxRuntimeCallTarget = actorMap.get(cma);
        if (maxRuntimeCallTarget == null) {
            MaxForeignCallDescriptor maxRuntimeCall = findMethod(cma);
            if (maxRuntimeCall == null) {
                return null;
            }
            Transition transition = Transition.NOT_LEAF;
            // This is a workaround for a problem with FrameStates for this call in the context of an exception branch
            if (cma.name().equals("loadExceptionForHandler")) {
                transition = Transition.LEAF;
            }
            maxRuntimeCallTarget = new MaxForeignCallLinkage(maxRuntimeCall, RegisterEffect.DESTROYS_REGISTERS, transition, false, ALL_LOCATIONS);
            map.put(maxRuntimeCall, maxRuntimeCallTarget);
            actorMap.put(cma, maxRuntimeCallTarget);
        }
        return maxRuntimeCallTarget.getMaxRuntimeCall();
    }

    /**
     * Creates a {@link MaxForeignCallDescriptor} for a named method in a given class.
     */
    static MaxForeignCallDescriptor findMethod(ClassMethodActor cma) {
        if (!(cma.isNative() || cma.getAnnotation(SNIPPET_SLOWPATH.class) != null || cma.getAnnotation(NEVER_INLINE.class) != null)) {
            Trace.line(1, "WARNING: snippet contains an invoke to " + cma.holder().name() + "." + cma.name() + " not annotated with SNIPPET_SLOWPATH or NEVER_INLINE");
        }
        if (cma.isInitializer()) {
            return null;
        } else {
            Method foundMethod = cma.toJava();
            return new MaxForeignCallDescriptor(foundMethod.getName(), foundMethod, foundMethod.getReturnType(), foundMethod.getParameterTypes());
        }
    }

    private static boolean hasRuntimeCallAttributes(Method method) {
        return method.isAnnotationPresent(SNIPPET_SLOWPATH.class) || method.isAnnotationPresent(NEVER_INLINE.class);
    }


}
