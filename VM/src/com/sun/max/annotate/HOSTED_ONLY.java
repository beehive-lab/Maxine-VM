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

/**
 * Indicates that a method, field or class exists only for the purpose of hosted execution.
 * <p>
 * Typically this is used when the annotated method, field or class is only used by code guarded (directly or
 * indirectly) by a call to {@link com.sun.max.vm.MaxineVM#isHosted()}. For example:
 *
 * <pre>
 * @HOSTED_ONLY
 * private int counter;
 *
 * public void getValue() {
 *     if (MaxineVM.isHosted()) {
 *         counter++;
 *     }
 *     return value;
 * }
 *
 * @HOSTED_ONLY
 * public int counter() {
 *     return counter;
 * }
 * </pre>
 * <p>
 * A second usage of this annotation is for testing compilation of code that references a potentially unresolved symbol.
 * For example, to test the implementation of {@link com.sun.max.vm.bytecode.Bytecodes#INVOKEVIRTUAL INVOKEVIRTUAL}, a test
 * case can be written that calls a method in a class annotated with this annotation.
 * <p>
 * During {@linkplain com.sun.max.vm.hosted.BootImageGenerator boot image generation}, all such annotated entities are omitted from the
 * generated image.
 *
 * @author Doug Simon
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface HOSTED_ONLY {
}
