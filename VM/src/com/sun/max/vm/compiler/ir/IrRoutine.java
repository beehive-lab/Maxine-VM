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
package com.sun.max.vm.compiler.ir;

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
 * Concrete IrRoutine subclasses are singleton classes.
 *
 * @see Builtin
 * @see Snippet
 *
 * @author Bernd Mathiske
 */
public abstract class IrRoutine {

    @HOSTED_ONLY
    protected MethodActor getFoldingMethodActor(Class holder, String name, boolean fatalIfMissing) {
        final String className = Strings.firstCharToUpperCase(name);
        final String methodName = Strings.firstCharToLowerCase(className);
        final Method[] methods = holder.getDeclaredMethods();
        for (Method method : methods) {
            final BUILTIN builtinAnnotation = method.getAnnotation(BUILTIN.class);
            if (builtinAnnotation != null && builtinAnnotation.builtinClass().getSimpleName().equals(className)) {
                return MethodActor.fromJava(method);
            }
            if (method.getName().equals(methodName) && method.isAnnotationPresent(SNIPPET.class)) {
                assert !method.isAnnotationPresent(LOCAL_SUBSTITUTION.class);
                return MethodActor.fromJava(method);
            }
        }
        if (fatalIfMissing) {
            ProgramError.unexpected("Could not find method named '" + methodName + "' in " + className);
        }
        return null;
    }

    private final String name;
    private final MethodActor foldingMethodActor;
    private final Kind resultKind;

    /**
     * This is used to enforce the constraint that only a single instance of any concrete
     * IrRoutine subclass will be created.
     */
    private static final Map<Class<? extends IrRoutine>, IrRoutine> singletonInstances = new HashMap<Class<? extends IrRoutine>, IrRoutine>();

    @HOSTED_ONLY
    protected IrRoutine(Class foldingMethodHolder) {
        name = getClass().getSimpleName();
        if (foldingMethodHolder != null) {
            foldingMethodActor = getFoldingMethodActor(foldingMethodHolder, name, true);
        } else {
            foldingMethodActor = getFoldingMethodActor(getClass(), name, true);
        }
        classMethodActor = (ClassMethodActor) foldingMethodActor;
        resultKind = foldingMethodActor.descriptor().resultKind();
        FatalError.check(singletonInstances.put(getClass(), this) == null, "Cannot have mulitple instances of IR routine type: " + getClass().getName());
    }

    public String name() {
        return name;
    }

    public MethodActor foldingMethodActor() {
        return foldingMethodActor;
    }

    private final ClassMethodActor classMethodActor;

    public ClassMethodActor classMethodActor() {
        return classMethodActor;
    }

    public Kind resultKind() {
        return resultKind;
    }

    private Kind[] parameterKinds;

    public Kind[] parameterKinds() {
        if (parameterKinds == null) {
            parameterKinds = classMethodActor().getParameterKinds();
        }
        return parameterKinds;
    }

    public boolean isFoldable(IrValue[] arguments) {
        return IrValue.Static.areConstant(arguments);
    }
}
