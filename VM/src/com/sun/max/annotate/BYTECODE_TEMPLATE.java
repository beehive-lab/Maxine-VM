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

import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;

/**
 * Declares that a method is the source for a bytecode template. By default, a template generator may assume that the
 * source method for a bytecode shares the bytecode's mnemonic. Use of this annotation allows an method whose name does
 * not match the bytecode's mnemonic. This is useful in cases whether the mnemonic corresponds with a Java keyword (e.g.
 * {@code new}) or where the method implements a specialization of the bytecode (e.g. a method named {@code igetfield}
 * may be an implementation of {@link Bytecode#GETFIELD} specialized for reading integer fields). In these case, the
 * {@link #bytecode()} element specifies the mnemonic of the bytecode implemented by the annotated method.
 * <p>
 * In addition to correlating a method with a bytecode, this annotation can be employed to specify additional attributes
 * useful to a bytecode template generator such as the return kind, whether the template is instrumented or not, and
 * whether references are resolved/initialized.
 *
 * @see TEMPLATE
 * @author Laurent Daynes
 * @author Aziz Ghuloum
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BYTECODE_TEMPLATE {
    /**
     * Get the bytecode the annotated method is a template for.
     * @return the bytecode this template implements
     */
    Bytecode bytecode();

    TemplateChooser.Initialized initialized () default TemplateChooser.Initialized.DEFAULT;
    TemplateChooser.Instrumented instrumented () default TemplateChooser.Instrumented.DEFAULT;
    TemplateChooser.Resolved resolved () default TemplateChooser.Resolved.DEFAULT;
    TemplateChooser.Traced traced () default TemplateChooser.Traced.DEFAULT;

    KindEnum kind() default KindEnum.VOID;

    public static class Static {

        /**
         * Gets the {@linkplain Bytecode bytecode} implemented by a given method. A method is a template implementation
         * for a bytecode if its {@linkplain Method#getName() name} corresponds a bytecode mnemonic or it has a
         * {@link BYTECODE_TEMPLATE} annotation.
         *
         * @param method
         *                a method that may potentially be a template for a bytecode
         * @return the bytecode implemented by {@code method} or null if {@code method} does not implement a bytecode
         */
        public static Bytecode bytecodeImplementedBy(Method method) {
            final BYTECODE_TEMPLATE annotation = method.getAnnotation(BYTECODE_TEMPLATE.class);
            if (annotation != null) {
                return annotation.bytecode();
            }
            try {
                return Bytecode.valueOf(method.getName().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

    }
}
