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

/**
 * Indicates that a method declaration is intended to be a substitute for a
 * method declaration in another class. A substitute method must be declared
 * in a class annotated with {@link METHOD_SUBSTITUTIONS} as the
 * {@link METHOD_SUBSTITUTIONS#value() value} element of that annotation specifies
 * the class containing the method to be substituted (the <i>substitutee</i> class).
 * <p>
 * The method to be substituted is determined based on a name and a list of parameter types.
 * The name is specified by the {@link #value()}
 * element of this annotation. If this element is not specified, then the
 * name of the substitution method is used. The parameter types are those of the
 * substitution method.
 * <p>
 * There must never be an explicit call to a non-static method annotated with SUBSTITUTE
 * unless it is from another non-static method in the same class.
 * 
 * @see METHOD_SUBSTITUTIONS
 * 
 * @author Bernd Mathiske
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SUBSTITUTE {
    String value() default "";
}
