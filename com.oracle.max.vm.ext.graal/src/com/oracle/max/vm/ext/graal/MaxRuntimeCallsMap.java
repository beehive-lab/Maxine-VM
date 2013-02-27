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

import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.code.RuntimeCallTarget.Descriptor;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;


public class MaxRuntimeCallsMap {

    static MaxRuntime runtime;

    private static Map<MaxRuntimeCall, MaxRuntimeCallTarget> map = new HashMap<>();
    private static Map<MethodActor, MaxRuntimeCallTarget> actorMap = new HashMap<>();

    /**
     * Creates a {@link MaxRuntimeCall} for a named method in a given class.
     */
    static MaxRuntimeCall findMethod(Class declaringClass, String methodName, boolean hasSideEffects) {
        Method foundMethod = null;
        for (Method method : declaringClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && hasRuntimeCallAttributes(method)) {
                assert foundMethod == null : "found more than one method " + declaringClass.getName() + "." + methodName;
                foundMethod = method;
            }
        }
        assert foundMethod != null : "did not find method " + declaringClass.getName() + "." + methodName;

        return new MaxRuntimeCall(foundMethod.getName(), foundMethod, hasSideEffects, foundMethod.getReturnType(), foundMethod.getParameterTypes());
    }

    private static boolean hasRuntimeCallAttributes(Method method) {
        return method.isAnnotationPresent(RUNTIME_ENTRY.class) || method.isAnnotationPresent(NEVER_INLINE.class);
    }



    @HOSTED_ONLY
    static void initialize(MaxRuntime runtime) {
        MaxRuntimeCallsMap.runtime = runtime;
        try {
            createCiRuntimeCall(CiRuntimeCall.RegisterFinalizer);
            createCiRuntimeCall(CiRuntimeCall.CreateNullPointerException);
            createCiRuntimeCall(CiRuntimeCall.CreateOutOfBoundsException);
            MethodActor slowPathAllocate = MethodActor.fromJava(HeapSchemeWithTLAB.class.getDeclaredMethod("slowPathAllocate", Size.class, Pointer.class));
            createRuntimeCall("slowPathAllocate", slowPathAllocate);
        } catch (Exception ex) {
            ProgramError.unexpected(ex);
        }
    }

    @HOSTED_ONLY
    private static void createCiRuntimeCall(CiRuntimeCall call) {
        ClassMethodActor cma = MaxRuntimeCalls.getClassMethodActor(call);
        String callName = call.name();
        createRuntimeCall(toFirstLower(callName), cma);
    }

    /**
     * Returns the argument with first character upper-cased.
     * @param s
     */
    @HOSTED_ONLY
    private static String toFirstLower(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }



    @HOSTED_ONLY
    private static void createRuntimeCall(String methodName, MethodActor cma) {
        Method method = cma.toJava();
        MaxRuntimeCall maxRuntimeCall = new MaxRuntimeCall(methodName, method, true, method.getReturnType(), method.getParameterTypes());
        MaxRuntimeCallTarget maxRuntimeCallTarget = new MaxRuntimeCallTarget(maxRuntimeCall);
        map.put(maxRuntimeCall, maxRuntimeCallTarget);
        actorMap.put(cma, maxRuntimeCallTarget);

    }

    /**
     * Gets the {@link RuntimeCallTarget} for a {@link Descriptor}.
     * @param descriptor must be a {@link MaxRuntimeCall}
     * @return
     */
    public static RuntimeCallTarget get(Descriptor descriptor) {
        return map.get(descriptor);
    }

    public static Descriptor get(ClassMethodActor cma) {
        MaxRuntimeCallTarget maxRuntimeCallTarget = actorMap.get(cma);
        if (maxRuntimeCallTarget == null) {
            MaxRuntimeCall maxRuntimeCall = findMethod(cma.holder().javaClass(), cma.name(), true);
            maxRuntimeCallTarget = new MaxRuntimeCallTarget(maxRuntimeCall);
            map.put(maxRuntimeCall, maxRuntimeCallTarget);
            actorMap.put(cma, maxRuntimeCallTarget);
        }
        return maxRuntimeCallTarget.getDescriptor();
    }

}
