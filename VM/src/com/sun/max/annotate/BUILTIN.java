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
    Class<? extends Builtin> builtinClass();

    @PROTOTYPE_ONLY
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
