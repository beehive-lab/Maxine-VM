/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All rights reserved.
 * 
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product that is
 * described in this document. In particular, and without limitation, these intellectual property rights may include one
 * or more of the U.S. patents listed at http://www.sun.com/patents and one or more additional patents or pending patent
 * applications in the U.S. and in other countries.
 * 
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or registered
 * trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks are used under license and
 * are trademarks or registered trademarks of SPARC International, Inc. in the U.S. and other countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open Company, Ltd.
 */
package com.sun.mockvm;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 
 * @author Thomas Wuerthinger
 * 
 *         Class to unify constructors and methods as they are separated in the reflection API. Here, dynamic AOP to add
 *         an interface would be nice!
 * 
 */
public final class InvokeTarget {

    public final String name;
    public final String signature;
    public final Class< ? > declaringClass;
    public final int modifiers;

    protected InvokeTarget(String name, String signature, Class< ? > declaringClass, int modifiers) {
        this.name = name;
        this.signature = signature;
        this.declaringClass = declaringClass;
        this.modifiers = modifiers;
    }

    public static InvokeTarget create(AccessibleObject o) {
        if (o instanceof Method) {
            Method method = (Method) o;
            return new InvokeTarget(method.getName(), MockSignature.toSignature(method), method.getDeclaringClass(), method.getModifiers());
        } else if (o instanceof Constructor<?>) {
            Constructor< ? > constructor = (Constructor< ? >) o;
            return new InvokeTarget("<init>", MockSignature.toSignature(constructor), constructor.getDeclaringClass(), constructor.getModifiers());
        }
        throw new IllegalArgumentException();
    }
}
