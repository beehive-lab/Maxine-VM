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
package com.sun.max.unsafe;

import java.lang.reflect.*;

import sun.misc.*;

import com.sun.max.program.*;

/**
 * Bypass access checks for reflective operations.
 *
 * @author Bernd Mathiske
 */
public final class WithoutAccessCheck {
    public static final Unsafe unsafe = (Unsafe) getStaticField(Unsafe.class, "theUnsafe");

    private WithoutAccessCheck() {
    }

    private static Field findField(Class javaClass, String fieldName) {
        Class c = javaClass;
        while (c != null) {
            try {
                final Field field = c.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException noSuchFieldException) {
            }
            c = c.getSuperclass();
        }
        throw ProgramError.unexpected("could not find field " + fieldName);
    }

    private static void accessError(Field field) {
        throw new IllegalAccessError("could not access field " + field);
    }

    public static Object getInstanceField(Object tuple, String fieldName) {
        final Field field = findField(tuple.getClass(), fieldName);
        try {
            return field.get(tuple);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(field);
            return null;
        }
    }

    public static void setInstanceField(Object tuple, String fieldName, Object value) {
        final Field field = findField(tuple.getClass(), fieldName);
        try {
            field.set(tuple, value);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(field);
        }
    }

    public static Object getStaticField(Class javaClass, String fieldName) {
        final Field field = findField(javaClass, fieldName);
        try {
            return field.get(javaClass);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(field);
            return null;
        }
    }

    public static void setStaticField(Class javaClass, String fieldName, Object value) {
        final Field field = findField(javaClass, fieldName);
        try {
            field.set(javaClass, value);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(field);
        }
    }

    public static Object newInstance(Class<?> javaClass) {
        try {
            final Constructor constructor = javaClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw ProgramError.unexpected(e);
        }
    }

    /**
     * Return the named method with a method signature matching parameter classes from the given class.
     */
    private static Method getMethod(Class<?> instanceClass, String methodName, Class[] parameterClasses)
        throws NoSuchMethodException {
        if (instanceClass == null) {
            throw new NoSuchMethodException("Invalid method : " + methodName);
        }
        try {
            final Method declaredMethod = instanceClass.getDeclaredMethod(methodName, parameterClasses);
            return declaredMethod;
        } catch (NoSuchMethodException noSuchMethodException) {
            return getMethod(instanceClass.getSuperclass(), methodName, parameterClasses);
        }
    }

    private static Class getWrapperClass(Class primitiveClass) {
        assert primitiveClass.isPrimitive();
        String name = primitiveClass.getName();
        if (name.equals("int")) {
            name = "Integer";
        } else if (name.equals("char")) {
            name = "Character";
        } else {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        try {
            return Class.forName("java.lang." + name);
        } catch (Throwable throwable) {
            throw ProgramError.unexpected();
        }
    }

    private static boolean compatible(Class parameterClass, Object argument) {
        if (parameterClass == null) {
            return false;
        }
        if (parameterClass.isPrimitive()) {
            if (argument == null) {
                return false;
            }
            return getWrapperClass(parameterClass).isInstance(argument);
        } else if (argument == null) {
            return true;
        }
        return parameterClass.isInstance(argument);
    }

    private static boolean compatible(Class[] parameterClasses, Object[] arguments) {
        if (arguments == null) {
            return parameterClasses == null;
        }
        if (parameterClasses.length != arguments.length) {
            return false;
        }
        for (int i = 0; i < parameterClasses.length; i++) {
            if (!compatible(parameterClasses[i], arguments[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calls a method on the given object instance with the given arguments.
     */
    public static Object invokeVirtual(Object instance, String methodName, Class[] parameterClasses, Object[] arguments)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        assert compatible(parameterClasses, arguments);
        final Method method = getMethod(instance.getClass(), methodName, parameterClasses);
        method.setAccessible(true);
        return method.invoke(instance, arguments);
    }

    public static Object invokeStatic(Class instanceClass, String methodName, Class[] parameterClasses, Object[] arguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        assert compatible(parameterClasses, arguments);
        final Method method = getMethod(instanceClass, methodName, parameterClasses);
        method.setAccessible(true);
        return method.invoke(null, arguments);
    }

    public static Object invokeConstructor(Class<?> instanceClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final Constructor constructor = instanceClass.getDeclaredConstructor(new Class[]{});
        constructor.setAccessible(true);
        return constructor.newInstance(new Object[]{});
    }
}
