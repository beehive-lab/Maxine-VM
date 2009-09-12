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
 * Any method annotated with this annotation exists solely to provide an escape hatch from Java's type checking. All
 * such methods are recognized by the compiler to simply be an unsafe coercion from one type to another.
 *
 * Any method annotated with this annotation must take exactly one parameter (which will be the receiver if the method
 * is non-static ), have a non-void return type and must not be generic. The type of the parameter is the type being
 * converted from and the return type is the type being converted to.
 *
 * The compiler must translate calls to these methods to simply replace the use of the result with the single parameter.
 *
 * @author Doug Simon
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UNSAFE_CAST {
}
