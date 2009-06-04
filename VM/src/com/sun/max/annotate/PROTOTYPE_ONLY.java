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

import com.sun.max.vm.*;
import com.sun.max.vm.prototype.*;

/**
 * Indicates that a method, field or class is omitted from the target VM.
 * <p>
 * Typically this is used when the annotated method, field or class is only used by code guarded (directly or
 * indirectly) by a call to {@link MaxineVM#isPrototyping()}. For example:
 *
 * <pre>
 * @PROTOTYPE_ONLY
 * private int _counter;
 *
 * public void getValue() {
 *     if (VM.isPrototyping()) {
 *         _counter++;
 *     }
 *     return _value;
 * }
 *
 * @PROTOTYPE_ONLY
 * public int counter() {
 *     return _counter;
 * }
 * </pre>
 * <p>
 * A second usage of this annotation is for testing compilation of code that references a potentially unresolved symbol.
 * For example, to test the implementation of {@link com.sun.max.vm.bytecode.Bytecode#INVOKEVIRTUAL INVOKEVIRTUAL}, a test
 * case can be written that calls a method in a class annotated with this annotation.
 * <p>
 * During {@linkplain BinaryImageGenerator boot image generation}, all such annotated entities are omitted from the
 * generated image.
 *
 * @author Doug Simon
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface PROTOTYPE_ONLY {
}
