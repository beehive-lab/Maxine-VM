/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;

@HOSTED_ONLY
public class MapUtil {

    static abstract class ClassHandler {
        abstract Class<?> correspondingDefiningClass(Class<?> klass);
        abstract Class<?> correspondingFieldClass(Class<?> klass);
        abstract void map(Object object, Object correspondingObject);
    }

    /**
     * Search {@code definingClass} for declarations of type {@code fieldClass} and populate the maps with the
     * corresponding field from the {@link #correspondingClass(Class)}.
     *
     * @param definingClass
     * @param fieldClass
     */
    static void populate(Class< ? > definingClass, Class< ? > fieldClass, ClassHandler classHandler) {
        Field[] fields = definingClass.getDeclaredFields();
        Class<?> correspondingDefiningClass = classHandler.correspondingDefiningClass(definingClass);
        Class<?> correspondingFieldClass = classHandler.correspondingFieldClass(fieldClass);
        for (Field field : fields) {
            if (field.getType() == fieldClass) {
                Object correspondingObject = getCorrespondingObject(correspondingDefiningClass, correspondingFieldClass, field);
                try {
                    Object object = field.get(null);
                    classHandler.map(object, correspondingObject);
                } catch (IllegalAccessException x) {
                    ProgramError.unexpected("populate access failure");
                }
            }
        }
    }

    private static Object getCorrespondingObject(Class<?> definingClass, Class<?> fieldClass, Field field) {
        Field[] fields = definingClass.getDeclaredFields();
        for (Field correspondingField : fields) {
            if (correspondingField.getType() == fieldClass &&
                correspondingField.getName().equals(field.getName())) {
                try {
                    return correspondingField.get(null);
                } catch (IllegalAccessException x) {
                    ProgramError.unexpected("correspondingObject access failure");
                }
            }
        }
        ProgramError.unexpected("correspondingObject search failure");
        return null;
    }

}

