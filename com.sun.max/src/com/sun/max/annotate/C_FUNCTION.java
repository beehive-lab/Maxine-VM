/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.vm.jni.*;

/**
 * Denotes a <i>private static native</i> method whose stub is lighter than a standard JNI stub. In particular,
 * the {@link NativeStubGenerator native stub} generated for such methods will:
 * <ul>
 * <li>marshal only the parameters explicit in the Java signature for the native function call (i.e. the JniEnv and
 * jclass parameters are omitted)</li>
 * </ul>
 * <p>
 * No parameter type or return type of VM entry or exit points may refer to object references - only primitive Java
 * values and 'Word' values are allowed.
 * <p>
 * This annotation should <b>never</b> be used for calling native code that can block.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface C_FUNCTION {
}
