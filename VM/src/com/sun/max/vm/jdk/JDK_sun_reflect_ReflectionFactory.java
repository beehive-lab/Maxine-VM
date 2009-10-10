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
package com.sun.max.vm.jdk;

import static com.sun.max.vm.reflection.GeneratedStub.*;

import java.lang.reflect.*;
import java.util.*;

import sun.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.reflection.*;

/**
 * Method substitutions for {@link sun.reflect.ReflectionFactory} that implement
 * reflection.
 *
 * @author Doug Simon
 * @author Mick Jordan
 */
@METHOD_SUBSTITUTIONS(ReflectionFactory.class)
public class JDK_sun_reflect_ReflectionFactory {

    /**
     * This field stores a map from method actor to method stubs, which is needed because
     * some method stubs are required to bootstrap the compiler at runtime.
     */
    private static Map<MethodActor, MethodAccessor> prePopulatedMethodStubs = new HashMap<MethodActor, MethodAccessor>();

    /**
     * This field stores a map from constructor to method stubs, which is needed because
     * some method stubs are required to bootstrap the compiler at runtime.
     */
    private static Map<MethodActor, ConstructorAccessor> prePopulatedConstructorStubs = new HashMap<MethodActor, ConstructorAccessor>();

    /**
     * Creates a method stub needed to invoke the specified method and adds it to the
     * pre-populated list while bootstrapping.
     * @param methodActor the method actor for which to create a method stub
     * @return the method stub for the specified method actor
     */
    @HOSTED_ONLY
    public static ClassActor createPrePopulatedMethodStub(MethodActor methodActor) {
        MethodAccessor stub = prePopulatedMethodStubs.get(methodActor);
        if (stub == null) {
            stub = newMethodStub(methodActor.toJava(), Boxing.JAVA);
            prePopulatedMethodStubs.put(methodActor, stub);
        }
        return ClassActor.fromJava(stub.getClass());
    }

    /**
     * Creates a method stub needed to invoke the specified constructor and adds it to the
     * pre-populated list while bootstrapping.
     * @param methodActor the method actor for which to create a method stub
     * @return the method stub for the specified method acto
     */
    public static ClassActor createPrePopulatedConstructorStub(MethodActor methodActor) {
        if (MaxineVM.isHosted()) {
            ConstructorAccessor stub = prePopulatedConstructorStubs.get(methodActor);
            if (stub == null) {
                stub = newConstructorStub(methodActor.toJavaConstructor(), null, Boxing.JAVA);
                prePopulatedConstructorStubs.put(methodActor, stub);
            }
            return ClassActor.fromJava(stub.getClass());
        }
        return null;
    }

    /**
     * Casts this reference to its corresponding {@code sun.reflect.ReflectionFactory} instance.
     * @return this object cast to a {@code sun.reflect.ReflectionFactory} instance
     */
    @UNSAFE_CAST
    private native ReflectionFactory thisReflectionFactory();

    /**
     * Creates a new method accessor for the specified method.
     * @see sun.reflect.ReflectionFactory#newMethodAccessor(Method)
     * @param method the method for which to create the accessor
     * @return a method accessor that is capable of invoking the method
     */
    @SUBSTITUTE
    public MethodAccessor newMethodAccessor(Method method) {
        MethodAccessor result = prePopulatedMethodStubs.get(MethodActor.fromJava(method));
        if (result == null) {
            result = newMethodStub(method, Boxing.JAVA);
        }
        return result;
    }

    /**
     * Creates a new constructor access for the specified constructor.
     * @see sun.reflect.ReflectionFactory#newConstructorAccessor(Constructor)
     * @param constructor the constructor for which to create the method stub
     * @return a method accessor that is capable of invoke the constructor
     */
    @SUBSTITUTE
    public ConstructorAccessor newConstructorAccessor(Constructor constructor) {
        ConstructorAccessor result = prePopulatedConstructorStubs.get(MethodActor.fromJavaConstructor(constructor));
        if (result == null) {
            final Class declaringClass = constructor.getDeclaringClass();
            if (Modifier.isAbstract(declaringClass.getModifiers())) {
                return new ConstructorAccessor() {
                    public Object newInstance(Object[] args) throws InstantiationException, IllegalArgumentException, InvocationTargetException {
                        throw new InstantiationException("Can not instantiate abstract class " + declaringClass.getName());
                    }
                };
            }
            if (declaringClass == Class.class) {
                return new ConstructorAccessor() {
                    public Object newInstance(Object[] args) throws InstantiationException, IllegalArgumentException, InvocationTargetException {
                        throw new InstantiationException("Can not instantiate java.lang.Class");
                    }
                };
            }
            result = newConstructorStub(constructor, null, Boxing.JAVA);
        }
        return result;
    }

    /**
     * Creates a new constructor that can be used to deserialize objects.
     * @see sun.reflect.ReflectionFactory#newConstructorForSerialization(Class, Constructor)
     * @param classToInstantiate the class to instantiate
     * @param constructorToCall the constructor to call
     * @return a new constructor capable of deserializing an object
     */
    @SUBSTITUTE
    public Constructor newConstructorForSerialization(Class classToInstantiate, Constructor constructorToCall) {
        // Fast path
        if (constructorToCall.getDeclaringClass() == classToInstantiate) {
            return constructorToCall;
        }

        final MethodActor constructorToCallActor = MethodActor.fromJavaConstructor(constructorToCall);
        final ConstructorAccessor accessor =  newConstructorStub(constructorToCall, classToInstantiate, Boxing.JAVA);
        final Constructor constructor = thisReflectionFactory().newConstructor(constructorToCall.getDeclaringClass(),
                        constructorToCall.getParameterTypes(),
                        constructorToCall.getExceptionTypes(),
                        constructorToCall.getModifiers(),
                        -1, // "java.lang.reflect.Constructor.slot", (apparently) not used throughout the JDK
                        constructorToCallActor.descriptor().toString(),
                        constructorToCallActor.runtimeVisibleAnnotationsBytes(),
                        constructorToCallActor.runtimeVisibleParameterAnnotationsBytes());
        thisReflectionFactory().setConstructorAccessor(constructor, accessor);
        return constructor;
    }
}
