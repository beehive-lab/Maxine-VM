/*
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
package com.sun.max.vm.compiler.target;

import com.sun.cri.ci.CiTargetMethod.Call;
import com.sun.cri.ri.*;
import com.sun.max.vm.actor.member.*;

/**
 * Static methods for operating on a {@link Call#target call target}.
 */
public class CallTarget {

    private CallTarget() {
    }

    public static final Object TEMPLATE_CALL = new Object() {
        @Override
        public String toString() {
            return "<template_call>";
        }
    };

    /**
     * Checks if a given target object is of a supported type.
     */
    public static void assertSupportedTarget(Object o) {
        assert
               o == TEMPLATE_CALL       || // template call
               o instanceof String      || // symbol for native function
               o instanceof RiMethod    || // normal method call
               o instanceof TargetMethod :  // stub
               "unsupported call target type: " + o.getClass().getName();
    }

    /**
     * Determines if a target is a symbol for linking a native function.
     */
    public static boolean isSymbol(Object target) {
        assertSupportedTarget(target);
        return target instanceof String;
    }

    /**
     * Determines if a target represents a template call.
     */
    public static boolean isTemplateCall(Object target) {
        assertSupportedTarget(target);
        return target == TEMPLATE_CALL;
    }

    /**
     * Gets the object for linking a direct call site.
     *
     * @param target the target of a direct call site
     * @param the object to be used for linking a direct call site or {@code null} if {@code target} does not represent
     *            a directly linkable target
     */
    public static Object directCallee(Object target) {
        assertSupportedTarget(target);
        if (target instanceof TargetMethod) {
            return target;
        }
        if (target instanceof ClassMethodActor) {
            return target;
        }
        return null;
    }

    /**
     * Converts a call target to a {@link MethodActor}.
     *
     * @param target the target of a call site
     * @return {@code target} cast to a {@link MethodActor} or {@code null} if {@code target} is not a {@link MethodActor} instance
     */
    public static MethodActor asMethodActor(Object target) {
        assertSupportedTarget(target);
        if (target instanceof MethodActor) {
            return (MethodActor) target;
        }
        return null;
    }
}
