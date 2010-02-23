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
