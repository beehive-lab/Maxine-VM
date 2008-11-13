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

import sun.misc.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;

/**
 * Basic generic utilities for objects.
 *
 * @author Bernd Mathiske
 */
public final class Objects {

    private Objects() {
    }

    private static void copyFields(Class javaClass, Object fromObject, Object toObject) {
        try {
            Class c = javaClass;
            while (c != null) {
                for (Field field : c.getDeclaredFields()) {
                    if ((field.getModifiers() & Modifier.STATIC) == 0) {
                        field.setAccessible(true);
                        final Object value = field.get(fromObject);
                        field.set(toObject, value);
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable throwable) {
            ProgramError.unexpected(throwable);
        }
    }

    public static <T> T clone(T object) {
        if (MaxineVM.isPrototyping()) {
            Throwable t;
            final Class<T> type = null;
            try {
                final Object result = WithoutAccessCheck.invokeVirtual(object, "clone", new Class[]{}, new Object[]{});
                return StaticLoophole.cast(type, result);
            } catch (InvocationTargetException invocationTargetException) {
                t = invocationTargetException.getTargetException();
            } catch (Throwable throwable1) {
                t = throwable1;
            }
            if (t instanceof CloneNotSupportedException) {
                try {
                    final Unsafe unsafe = (Unsafe) WithoutAccessCheck.getStaticField(Unsafe.class, "theUnsafe");
                    final Object result = unsafe.allocateInstance(object.getClass());
                    copyFields(object.getClass(), object, result);
                    return StaticLoophole.cast(type, result);
                } catch (Throwable throwable2) {
                    t = throwable2;
                }
            }
            throw ProgramError.unexpected(t);
        }
        return Heap.clone(object);
    }

    public static Class[] getClasses(Object... objects) {
        return Arrays.map(objects, Class.class, new MapFunction<Object, Class>() {
            public Class map(Object object) {
                return object.getClass();
            }
        });
    }

}
