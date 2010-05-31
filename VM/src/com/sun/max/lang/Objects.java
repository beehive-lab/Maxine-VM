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

import static com.sun.max.unsafe.WithoutAccessCheck.*;

import java.lang.reflect.*;

import com.sun.max.*;
import com.sun.max.program.*;

/**
 * Basic generic utilities for objects.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class Objects {

    private Objects() {
    }

    /**
     * Compares two given objects for equality using {@link Object#equals(Object)}.
     *
     * @return true if both {@code o1} and {@code o2} are {@code null} || {@code o1.equals(o2)}
     */
    public static boolean equal(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    /**
     * Copies the values of the instance fields in one object to another object.
     *
     * @param fromObject the object from which the field values are to be copied
     * @param toObject the object to which the field values are to be copied
     */
    public static void copy(Object fromObject, Object toObject) {
        assert fromObject.getClass() == toObject.getClass();
        Class c = fromObject.getClass();
        while (c != null) {
            for (Field field : c.getDeclaredFields()) {
                if ((field.getModifiers() & Modifier.STATIC) == 0) {
                    field.setAccessible(true);
                    try {
                        final Object value = field.get(fromObject);
                        field.set(toObject, value);
                    } catch (IllegalArgumentException illegalArgumentException) {
                        // This should never occur
                        throw ProgramError.unexpected(illegalArgumentException);
                    } catch (IllegalAccessException illegalAccessException) {
                        // This should never occur
                        throw ProgramError.unexpected(illegalAccessException);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    /**
     * Creates a new instance of a given class without calling any constructors. This call also ensures that {@code javaClass}
     * has been initialized.
     *
     * @param javaClass the class to construct an instance of
     * @return an uninitialized of {@code javaClass}
     * @throws InstantiationException if the instantiation fails for any of the reasons described
     *             {@linkplain InstantiationException here}
     */
    public static Object allocateInstance(Class<?> javaClass) throws InstantiationException {
        unsafe.ensureClassInitialized(javaClass);
        return unsafe.allocateInstance(javaClass);
    }

    public static <T> T allocateObject(Class<T> javaClass) throws InstantiationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Constructor constructor = javaClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        final Object object = constructor.newInstance();
        return Utils.cast(javaClass, object);
    }
}
