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
package com.sun.max.lang;

import java.lang.reflect.*;

import com.sun.max.program.*;

/**
 * Methods that might be members of java.lang.Class.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class Classes {

    private Classes() {
    }

    /**
     * Wraps a call to {@link ClassLoader#loadClass(String)} that is expected not to fail. Should a
     * {@link ClassNotFoundException} occur, it is converted to {@link NoClassDefFoundError}
     *
     * @return the value returned by calling {@link ClassLoader#loadClass(String)} on {@code classLoader} (which may be
     *         null)
     */
    public static Class load(ClassLoader classLoader, String name) {
        try {
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw (NoClassDefFoundError) new NoClassDefFoundError(name).initCause(e);
        }
    }

    /**
     * Wraps a call to {@link Class#forName(String)} that is expected not to fail. Should a
     * {@link ClassNotFoundException} occur, it is converted to {@link NoClassDefFoundError}
     */
    public static Class forName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw (NoClassDefFoundError) new NoClassDefFoundError(name).initCause(e);
        }
    }

    /**
     * Wraps a call to {@link Class#forName(String, boolean, ClassLoader)} that is expected not to fail. Should a
     * {@link ClassNotFoundException} occur, it is converted to {@link NoClassDefFoundError}
     */
    public static Class forName(String name, boolean initialize, ClassLoader loader) {
        try {
            return Class.forName(name, initialize, loader);
        } catch (ClassNotFoundException e) {
            throw (NoClassDefFoundError) new NoClassDefFoundError(name).initCause(e);
        }
    }

    /**
     * Links a given class. If the class {@code c} has already been linked, then this method simply returns. Otherwise,
     * the class is linked as described in the "Execution" chapter of the <a
     * href="http://java.sun.com/docs/books/jls/">Java Language Specification</a>.
     *
     * @param c the class to link
     */
    public static void link(Class c) {
        if (c != null) {
            try {
                final Class<?> linkedClass = Class.forName(c.getName(), false, c.getClassLoader());
                assert linkedClass == c;
            } catch (ClassNotFoundException classNotFoundException) {
                ProgramError.unexpected(classNotFoundException);
            }
        }
    }

    /**
     * Initializes a given class. If the class {@code c} has already been initialized, then this method simply returns. Otherwise,
     * the class is linked as described in the "Execution" chapter of the <a
     * href="http://java.sun.com/docs/books/jls/">Java Language Specification</a>.
     *
     * @param c the class to link
     */
    public static void initialize(Class c) {
        try {
            Class.forName(c.getName(), true, c.getClassLoader());
        } catch (ClassNotFoundException classNotFoundException) {
            ProgramError.unexpected(classNotFoundException);
        }
    }

    public static boolean areRelated(Class<?> class1, Class<?> class2) {
        return class1.isAssignableFrom(class2) || class2.isAssignableFrom(class1);
    }

    public static boolean areAssignableFrom(Class<?>[] superTypes, Class<?>... subTypes) {
        if (superTypes.length != subTypes.length) {
            return false;
        }
        for (int i = 0; i < superTypes.length; i++) {
            if (!superTypes[i].isAssignableFrom(subTypes[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extends the functionality of {@link Class#getConstructor(Class...)} to find a constructor whose formal parameter
     * types are assignable from the types of a list of arguments.
     *
     * @param javaClass
     *                the class to search in
     * @param arguments
     *                the list of arguments that will be passed to the constructor
     * @return the first constructor in {@link Class#getConstructors() javaClass.getConstructors()} that will accept
     *         {@code arguments} or null if no such constructor exists
     */
    public static Constructor<?> findConstructor(Class<?> javaClass, Object... arguments) {
    nextConstructor:
        for (Constructor<?> constructor : javaClass.getConstructors()) {
            final Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == arguments.length) {
                for (int i = 0; i != arguments.length; ++i) {
                    if (!parameterTypes[i].isAssignableFrom(arguments[i].getClass())) {
                        continue nextConstructor;
                    }
                }
                return constructor;
            }
        }
        return null;
    }

    /**
     * Extends the functionality of {@link Class#getMethod(String, Class...)} to find a method whose formal parameter
     * types are assignable from the types of a list of arguments.
     *
     * @param javaClass
     *                the class to search in
     * @param name
     *                the name of the method to search for
     * @param arguments
     *                the list of arguments that will be passed to the constructor
     * @return the first method in {@link Class#getMethods() javaClass.getMethods()} that will accept
     *         {@code arguments} or null if no such method exists
     */
    public static Method findMethod(Class<?> javaClass, String name, Object... arguments) {
    nextMethod:
        for (Method method : javaClass.getMethods()) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().equals(name) && parameterTypes.length == arguments.length) {
                for (int i = 0; i != arguments.length; ++i) {
                    if (!parameterTypes[i].isAssignableFrom(arguments[i].getClass())) {
                        continue nextMethod;
                    }
                }
                return method;
            }
        }
        return null;
    }

    /**
     * Extends the functionality of {@link Class#getDeclaredMethod(String, Class...)} to find a method whose formal parameter
     * types are assignable from the types of a list of arguments.
     *
     * @param javaClass
     *                the class to search in
     * @param name
     *                the name of the method to search for
     * @param arguments
     *                the list of arguments that will be passed to the constructor
     * @return the first method in {@link Class#getDeclaredMethods() javaClass.getDeclaredMethods()} that will accept
     *         {@code arguments} or null if no such method exists
     */
    public static Method findDeclaredMethod(Class<?> javaClass, String name, Object... arguments) {
    nextMethod:
        for (Method declaredMethod : javaClass.getDeclaredMethods()) {
            final Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
            if (declaredMethod.getName().equals(name) && parameterTypes.length == arguments.length) {
                for (int i = 0; i != arguments.length; ++i) {
                    if (!parameterTypes[i].isAssignableFrom(arguments[i].getClass())) {
                        continue nextMethod;
                    }
                }
                return declaredMethod;
            }
        }
        return null;
    }

    /**
     * Gets the class or interface that declares the method, field or constructor represented by {@code member}.
     */
    public static Class<?> getDeclaringClass(AccessibleObject member) {
        if (member instanceof Method) {
            final Method method = (Method) member;
            return method.getDeclaringClass();
        }
        if (member instanceof Field) {
            final Field field = (Field) member;
            return field.getDeclaringClass();
        }
        assert member instanceof Constructor;
        final Constructor constructor = (Constructor) member;
        return constructor.getDeclaringClass();
    }

    /**
     * Get hold of a non-public inner class.
     */
    public static Class getInnerClass(Class outerClass, String innerClassSimpleName) {
        for (Class innerClass : outerClass.getDeclaredClasses()) {
            if (innerClass.getSimpleName().equals(innerClassSimpleName)) {
                return innerClass;
            }
        }
        return null;
    }

    /**
     * Extracts a package name from a fully qualified class name.
     *
     * @return "" if {@code className} denotes a class in the unnamed package
     */
    public static String getPackageName(String className) {
        final int index = className.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return className.substring(0, index);
    }

    /**
     * Extends the functionality of {@link Class#getSimpleName()} to include a non-empty string for anonymous and local
     * classes.
     *
     * @param clazz
     *                the class for which the simple name is being requested
     * @param withEnclosingClass
     *                specifies if the returned name should be qualified with the name(s) of the enclosing class/classes
     *                of {@code clazz} (if any). This option is ignored if {@code clazz} denotes an anonymous or local class.
     * @return
     */
    public static String getSimpleName(Class<?> clazz, boolean withEnclosingClass) {
        final String simpleName = clazz.getSimpleName();
        if (simpleName.length() != 0) {
            if (withEnclosingClass) {
                String prefix = "";
                Class enclosingClass = clazz;
                while ((enclosingClass = enclosingClass.getEnclosingClass()) != null) {
                    prefix = prefix + enclosingClass.getSimpleName() + ".";
                }
                return prefix + simpleName;
            }
            return simpleName;
        }
        // Must be an anonymous or local class
        final String name = clazz.getName();
        int index = name.indexOf('$');
        if (index == -1) {
            return name;
        }
        index = name.lastIndexOf('.', index);
        if (index == -1) {
            return name;
        }
        return name.substring(index + 1);
    }

    /**
     * Similar to a call to {@link Class#getDeclaredMethod(String, Class...)} that must succeed with the addition that
     * the search takes into account the return type.
     */
    public static Method getDeclaredMethod(Class<?> clazz, Class returnType, String name, Class... parameterTypes) {
        for (Method javaMethod : clazz.getDeclaredMethods()) {
            if (javaMethod.getName().equals(name) && javaMethod.getReturnType().equals(returnType)) {
                final Class[] declaredParameterTypes = javaMethod.getParameterTypes();
                if (java.util.Arrays.equals(declaredParameterTypes, parameterTypes)) {
                    return javaMethod;
                }
            }
        }
        throw ProgramError.unexpected("could not find required method: " + returnType.getName() + " " + clazz.getName() + "." + name + "(" + Arrays.toString(parameterTypes, ",") + ")");
    }

    /**
     * A wrapper for a call to {@link Class#getDeclaredMethod(String, Class...)} that must succeed.
     */
    public static Method getDeclaredMethod(Class<?> clazz, String name, Class... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException noSuchMethodException) {
            throw ProgramError.unexpected("could not find required method: " + clazz.getName() + "." + name + "(" + Arrays.toString(parameterTypes, ",") + ")", noSuchMethodException);
        }
    }

    /**
     * A wrapper for a call to {@link Class#getDeclaredConstructor(Class...)} that must succeed.
     */
    public static Constructor getDeclaredConstructor(Class<?> clazz, Class... parameterTypes) {
        try {
            return clazz.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException noSuchMethodException) {
            throw ProgramError.unexpected("could not find required constructor: " + clazz.getName() + "(" + Arrays.toString(parameterTypes, ",") + ")", noSuchMethodException);
        }
    }

    /**
     * A wrapper for a call to {@link Class#getDeclaredField(String)} that must succeed.
     */
    public static Field getDeclaredField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException noSuchFieldException) {
            throw ProgramError.unexpected("could not find required field: " + clazz.getName() + "." + name, noSuchFieldException);
        }
    }

    /**
     * A wrapper for a call to {@link Class#getDeclaredField(String)} that must succeed.
     * Also checks that the field has an expected type.
     */
    public static Field getDeclaredField(Class<?> clazz, String name, Class type) {
        final Field field = getDeclaredField(clazz, name);
        ProgramError.check(field.getType().equals(type));
        return field;
    }

    public static void main(Class<?> classToRun, String[] args) {
        try {
            final Method mainMethod = classToRun.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (Exception e) {
            ProgramError.unexpected(e);
        }
    }

    public static void main(String classToRun, String[] args) {
        main(forName(classToRun), args);
    }

}
