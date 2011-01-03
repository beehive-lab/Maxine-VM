/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Prefabricated routine IR building material for reuse when translating byte codes.
 * These are {@linkplain Builtin builtins} and {@linkplain Snippet snippets}.
 * Concrete subclasses are singleton classes.
 *
 * @see Builtin
 * @see Snippet
 *
 * @author Bernd Mathiske
 */
public abstract class Routine {

    @HOSTED_ONLY
    protected ClassMethodActor getExecutable(Class holder, String name, boolean fatalIfMissing) {
        final String className = Strings.firstCharToUpperCase(name);
        final String methodName = Strings.firstCharToLowerCase(className);
        final Method[] methods = holder.getDeclaredMethods();
        for (Method method : methods) {
            final BUILTIN builtinAnnotation = method.getAnnotation(BUILTIN.class);
            if (builtinAnnotation != null && builtinAnnotation.value().getSimpleName().equals(className)) {
                return ClassMethodActor.fromJava(method);
            }
            if (method.getName().equals(methodName) && method.isAnnotationPresent(SNIPPET.class)) {
                assert !method.isAnnotationPresent(LOCAL_SUBSTITUTION.class);
                return ClassMethodActor.fromJava(method);
            }
        }
        if (fatalIfMissing) {
            ProgramError.unexpected("Could not find method named '" + methodName + "' in " + holder);
        }
        return null;
    }

    /**
     * The method that is executed or invoked when emulating or meta-evaluating a call to this routine.
     */
    public final ClassMethodActor executable;

    /**
     * The name of this routine.
     */
    public final String name;

    /**
     * The kind of value returned when {@linkplain #executable} executing this routine.
     */
    public final Kind resultKind;

    /**
     * This is used to enforce the constraint that only a single instance of any concrete
     * IrRoutine subclass will be created.
     */
    private static final Map<Class<? extends Routine>, Routine> singletonInstances = new HashMap<Class<? extends Routine>, Routine>();

    @HOSTED_ONLY
    protected Routine(Class executableHolder) {
        name = getClass().getSimpleName();
        if (executableHolder != null) {
            executable = getExecutable(executableHolder, name, true);
        } else {
            executable = getExecutable(getClass(), name, true);
        }
        resultKind = executable.descriptor().resultKind();
        FatalError.check(singletonInstances.put(getClass(), this) == null, "Cannot have mulitple instances of IR routine type: " + getClass().getName());
    }

    private Kind[] parameterKinds;

    public Kind[] parameterKinds() {
        if (parameterKinds == null) {
            parameterKinds = executable.getParameterKinds();
        }
        return parameterKinds;
    }
}
