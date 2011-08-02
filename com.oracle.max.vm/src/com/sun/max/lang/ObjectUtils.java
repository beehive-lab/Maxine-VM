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
package com.sun.max.lang;

import static com.sun.max.vm.hosted.WithoutAccessCheck.*;

import java.lang.reflect.*;

import com.sun.max.*;
import com.sun.max.program.*;

/**
 * Basic generic utilities for objects.
 */
public final class ObjectUtils {

    private ObjectUtils() {
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
