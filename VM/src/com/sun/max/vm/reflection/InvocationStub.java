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
package com.sun.max.vm.reflection;

import java.lang.reflect.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * This class serves two purposes. Firstly, it is a marker for classes whose bytecodes should not be verified (a la
 * {@link sun.reflect.MagicAccessorImpl}). Secondly, it provides static methods for generating invocation stubs that
 * are used to implement {@link Method#invoke(Object, Object...)} and {@link Constructor#newInstance(Object...)}.
 *
 * The bytecode generation is derived from the JDK 1.4 mechanism for the same purpose.
 *
 * @author Doug Simon
 * @see sun.reflect.MethodAccessorGenerator
 */
public abstract class InvocationStub {

    InvocationStub() {
        // MUST BE EMPTY
    }

    /**
     * Determines if this stub can access a given type. Only used to determine if the stub can be run under HotSpot or
     * any other VM that will subject the stub to standard verification.
     */
    public boolean canAccess(Class type) {
        return Modifier.isPublic(type.getModifiers()) || getClass().getPackage().equals(type.getPackage());
    }

    /**
     * Determines if this stub can access all the given types.
     *
     * @see #canAccess(Class)
     */
    public boolean canAccess(Class... types) {
        for (Class type : types) {
            if (!canAccess(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A reference to the constructor or method for which this stub was generated.
     */
    private MethodActor target;

    final void setTarget(AccessibleObject target) {
        assert this.target == null;
        if (target instanceof Constructor) {
            this.target = MethodActor.fromJavaConstructor((Constructor) target);
        } else {
            assert target instanceof Method;
            this.target = MethodActor.fromJava((Method) target);
        }
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + target + "]";
    }

    /**
     * Generates a stub for invoking a given method reflectively.
     */
    public static MethodInvocationStub newMethodStub(Method method, Boxing boxing) {

        final MethodInvocationStub stub = new InvocationStubGenerator<MethodInvocationStub>(
                        method,
                        MethodInvocationStub.class,
                        MethodActor.fromJava(method).name,
                        method.getDeclaringClass(),
                        method.getReturnType(),
                        method.getParameterTypes(),
                        Modifier.isStatic(method.getModifiers()),
                        Modifier.isPrivate(method.getModifiers()),
                        null,
                        boxing).stub();
        stub.setTarget(method);
        return stub;
    }

    /**
     * Generates a stub for invoking a given constructor reflectively.
     * @param constructor that is to be invoked
     * @param classToInstantiate only non-null in the serialization context where it may differ from constructor.getDeclaringClass()
     */
    public static ConstructorInvocationStub newConstructorStub(Constructor constructor, Class classToInstantiate, Boxing boxing) {
        final ConstructorInvocationStub stub = new InvocationStubGenerator<ConstructorInvocationStub>(
                        constructor,
                        ConstructorInvocationStub.class,
                        SymbolTable.INIT,
                        constructor.getDeclaringClass(),
                        Void.TYPE,
                        constructor.getParameterTypes(),
                        false,
                        Modifier.isPrivate(constructor.getModifiers()),
                        classToInstantiate,
                        boxing).stub();
        stub.setTarget(constructor);
        return stub;
    }
}
