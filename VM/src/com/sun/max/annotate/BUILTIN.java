/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.annotate;
import java.lang.annotation.*;
import java.lang.reflect.*;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.builtin.*;

/**
 * Links a method to a Builtin.
 * The compiler then simply substitutes any call to the method by a call to the builtin.
 * The method may have a body so that it can be executed while interpreting an IR.
 *
 * @see Builtin
 *
 * @author Bernd Mathiske
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BUILTIN {

    /**
     * The {@link Builtin} subclass the annotated method is associated with.
     */
    Class<? extends Builtin> value();

    @HOSTED_ONLY
    public static final class Static {
        private Static() {
        }

        /**
         * Gets the singleton instance of a {@link Builtin} subclass. All such subclasses
         * must have a static field name "BUILTIN" of the same type as the subclass.
         */
        public static Builtin get(Class<? extends Builtin> builtinClass) {
            try {
                final Field field = builtinClass.getField("BUILTIN");
                assert field.getName().equals(BUILTIN.class.getSimpleName());
                return (Builtin) field.get(null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            ProgramError.unexpected();
            return null;
        }
    }
}
