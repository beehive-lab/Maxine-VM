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
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
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

    @PROTOTYPE_ONLY
    protected MethodActor getFoldingMethodActor(Class holder, String name, boolean fatalIfMissing) {
        final String className = Naming.toClassName(name);
        final String methodName = Naming.toMethodName(className);
        final Method[] methods = holder.getDeclaredMethods();
        for (Method method : methods) {
            final BUILTIN builtinAnnotation = method.getAnnotation(BUILTIN.class);
            if (builtinAnnotation != null && builtinAnnotation.builtinClass().getSimpleName().equals(className)) {
                return MethodActor.fromJava(method);
            }
            if (method.getName().equals(methodName) && method.isAnnotationPresent(SNIPPET.class)) {
                assert !method.isAnnotationPresent(SURROGATE.class);
                return MethodActor.fromJava(method);
            }
        }
        if (fatalIfMissing) {
            ProgramError.unexpected("Could not find method named '" + methodName + "' in " + className);
        }
        return null;
    }

    private final String _name;
    private final MethodActor _foldingMethodActor;
    private final Kind _resultKind;

    /**
     * This is used to enforce the constraint that only a single instance of any concrete
     * IrRoutine subclass will be created.
     */
    private static final Map<Class<? extends IrRoutine>, IrRoutine> _singletonInstances = new HashMap<Class<? extends IrRoutine>, IrRoutine>();

    @PROTOTYPE_ONLY
    protected IrRoutine(Class foldingMethodHolder) {
        _name = Naming.toFieldName(getClass().getSimpleName());
        if (foldingMethodHolder != null) {
            _foldingMethodActor = getFoldingMethodActor(foldingMethodHolder, _name, true);
        } else {
            _foldingMethodActor = getFoldingMethodActor(getClass(), _name, true);
        }
        _resultKind = _foldingMethodActor.descriptor().resultKind();
        assert _singletonInstances.put(getClass(), this) == null;
    }

    public String name() {
        return _name;
    }

    public MethodActor foldingMethodActor() {
        return _foldingMethodActor;
    }

    private ClassMethodActor _classMethodActor;

    public ClassMethodActor classMethodActor() {
        if (_classMethodActor == null) {
            _classMethodActor = (ClassMethodActor) foldingMethodActor();
        }
        return _classMethodActor;
    }

    public Kind resultKind() {
        return _resultKind;
    }

    private Kind[] _parameterKinds;

    public Kind[] parameterKinds() {
        if (_parameterKinds == null) {
            _parameterKinds = classMethodActor().getParameterKinds();
        }
        return _parameterKinds;
    }

    public boolean isFoldable(IrValue[] arguments) {
        return IrValue.Static.areConstant(arguments);
    }
}
