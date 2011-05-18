/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.unsafe;

import java.lang.reflect.*;

import sun.misc.*;

import com.sun.max.program.*;

/**
 * Bypass access checks for reflective operations.
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
