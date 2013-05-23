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
import com.oracle.graal.api.meta.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;

public class MaxForeignCallsMap {

    static MaxRuntime runtime;

    private static Map<MaxForeignCall, MaxForeignCallLinkage> map = new HashMap<>();
    private static Map<MethodActor, MaxForeignCallLinkage> actorMap = new HashMap<>();

    /**
     * Creates a {@link MaxForeignCall} for a named method in a given class.
     */
    static MaxForeignCall findMethod(Class declaringClass, String methodName, boolean hasSideEffects) {
        Method foundMethod = null;
        for (Method method : declaringClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && hasRuntimeCallAttributes(method)) {
                assert foundMethod == null : "found more than one method " + declaringClass.getName() + "." + methodName;
                foundMethod = method;
            }
        }
        assert foundMethod != null : "did not find method " + declaringClass.getName() + "." + methodName;

        return new MaxForeignCall(foundMethod.getName(), foundMethod, hasSideEffects, foundMethod.getReturnType(), foundMethod.getParameterTypes());
    }

    private static boolean hasRuntimeCallAttributes(Method method) {
        return method.isAnnotationPresent(RUNTIME_ENTRY.class) || method.isAnnotationPresent(NEVER_INLINE.class);
    }

    @RUNTIME_ENTRY
    private static void unwindException(Object throwable) throws Throwable {
        Throw.raise(UnsafeCast.asThrowable(throwable));
    }

    @RUNTIME_ENTRY
    private static void deoptimize() {
        FatalError.unexpected("Deoptimize not implemented");
    }

    @HOSTED_ONLY
    static void initialize(MaxRuntime runtime) {
        MaxForeignCallsMap.runtime = runtime;
        try {
            createCiRuntimeCall(CiRuntimeCall.RegisterFinalizer, null);
            createCiRuntimeCall(CiRuntimeCall.CreateNullPointerException, null);
            createCiRuntimeCall(CiRuntimeCall.CreateOutOfBoundsException, null);
            createCiRuntimeCall(CiRuntimeCall.ArithmeticCos, "arithmeticCos");
            createCiRuntimeCall(CiRuntimeCall.ArithmeticSin, "arithmeticSin");
            createCiRuntimeCall(CiRuntimeCall.ArithmeticTan, "arithmeticTan");
            createCiRuntimeCall(CiRuntimeCall.ArithmeticFrem, "arithmeticFrem");
            createCiRuntimeCall(CiRuntimeCall.ArithmeticDrem, "arithmeticDrem");
            Method unwindException = MaxForeignCallsMap.class.getDeclaredMethod("unwindException", Object.class);
            createRuntimeCall(unwindException.getName(), MethodActor.fromJava(unwindException));
            new CriticalMethod(unwindException);
            Method deoptimize = MaxForeignCallsMap.class.getDeclaredMethod("deoptimize");
            createRuntimeCall(deoptimize.getName(), MethodActor.fromJava(deoptimize));
            new CriticalMethod(deoptimize);

        } catch (Exception ex) {
            ProgramError.unexpected(ex);
        }
    }

    @HOSTED_ONLY
    private static void createCiRuntimeCall(CiRuntimeCall call, String overrideName) {
        ClassMethodActor cma = MaxRuntimeCalls.getClassMethodActor(call);
        createRuntimeCall(overrideName != null ? overrideName : toFirstLower(call.name()), cma);
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
    private static MaxForeignCall createRuntimeCall(String methodName, MethodActor cma) {
        Method method = cma.toJava();
        MaxForeignCall maxRuntimeCall = new MaxForeignCall(methodName, method, true, method.getReturnType(), method.getParameterTypes());
        MaxForeignCallLinkage maxRuntimeCallTarget = new MaxForeignCallLinkage(maxRuntimeCall);
        map.put(maxRuntimeCall, maxRuntimeCallTarget);
        actorMap.put(cma, maxRuntimeCallTarget);
        return maxRuntimeCall;
    }

    /**
     * Gets the {@link RuntimeCallTarget} for a {@link Descriptor}.
     * @param descriptor must be a {@link MaxForeignCall}
     * @return
     */
    public static ForeignCallLinkage get(ForeignCallDescriptor descriptor) {
        return map.get(descriptor);
    }

    public static MaxForeignCall get(ClassMethodActor cma) {
        MaxForeignCallLinkage maxRuntimeCallTarget = actorMap.get(cma);
        if (maxRuntimeCallTarget == null) {
            MaxForeignCall maxRuntimeCall = findMethod(cma.holder().javaClass(), cma.name(), true);
            maxRuntimeCallTarget = new MaxForeignCallLinkage(maxRuntimeCall);
            map.put(maxRuntimeCall, maxRuntimeCallTarget);
            actorMap.put(cma, maxRuntimeCallTarget);
        }
        return maxRuntimeCallTarget.getMaxRuntimeCall();
    }

}
