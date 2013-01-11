/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.max.vm.ext.graal.snippets;

import java.lang.reflect.*;

import com.oracle.graal.api.code.RuntimeCallTarget.Descriptor;

public class SnippetRuntime {

    static SnippetRuntimeCall findMethod(Class declaringClass, String methodName, boolean hasSideEffects) {
        Method foundMethod = null;
        for (Method method : declaringClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                assert foundMethod == null : "found more than one method " + declaringClass.getName() + "." + methodName;
                foundMethod = method;
            }
        }
        assert foundMethod != null : "did not find method " + declaringClass.getName() + "." + methodName;
        assert Modifier.isStatic(foundMethod.getModifiers());

        return new SnippetRuntimeCall(foundMethod, hasSideEffects, foundMethod.getReturnType(), foundMethod.getParameterTypes());
    }

    public static class SnippetRuntimeCall extends Descriptor {

        private final Method method;

        public SnippetRuntimeCall(Method method, boolean hasSideEffect, Class resultKind, Class[] arguments) {
            super(method.getName(), hasSideEffect, resultKind, arguments);
            this.method = method;
        }

        public Method getMethod() {
            return method;
        }
    }

}
