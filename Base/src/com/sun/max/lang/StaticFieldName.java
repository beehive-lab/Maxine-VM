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
import java.util.*;

import com.sun.max.program.*;

public interface StaticFieldName {

    String name();

    void setName(String name);

    public interface StringFunction {
        String function(String string);
    }

    public interface Procedure {
        void procedure(StaticFieldName staticFieldName);
    }

    public static final class Static {

        private Static() {
        }

        public static List<StaticFieldName> initialize(Class staticNameFieldClass, StringFunction stringFunction, Procedure procedure) {
            final List<StaticFieldName> sequence = new LinkedList<StaticFieldName>();
            for (Field field : staticNameFieldClass.getDeclaredFields()) {
                if ((field.getModifiers() & Modifier.STATIC) != 0 && StaticFieldName.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        final StaticFieldName value = (StaticFieldName) field.get(null);
                        if (value.name() == null) {
                            String name = field.getName();
                            if (stringFunction != null) {
                                name = stringFunction.function(name);
                            }
                            value.setName(name);
                        }
                        if (procedure != null) {
                            procedure.procedure(value);
                        }
                        sequence.add(value);
                    } catch (IllegalAccessException illegalAccessException) {
                        ProgramError.unexpected("could not name value of field: " + field);
                    }
                }
            }
            return sequence;
        }

        public static List<StaticFieldName> initialize(Class staticNameFieldClass, StringFunction stringFunction) {
            return initialize(staticNameFieldClass, stringFunction, null);
        }

        public static List<StaticFieldName> initialize(Class staticNameFieldClass, Procedure procedure) {
            return initialize(staticNameFieldClass, null, procedure);
        }

        public static List<StaticFieldName> initialize(Class staticNameFieldClass) {
            return initialize(staticNameFieldClass, null, null);
        }
    }

}
