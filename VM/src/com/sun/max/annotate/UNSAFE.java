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
 * Methods with this annotation must be compiled with the bootstrap compiler, not the JIT.
 * Neither can they be interpreted.
 * <p>
 * This is the case when a method uses values of type {@link Word} or
 * any constant folding or dead code elimination must take place
 * before the code makes sense in the target VM.
 * <p>
 * Most of these methods are recognized automatically.
 * Only those not captured during {@linkplain Intrinsics#run() intrinsification}
 * need to be annotated.
 * <p>
 * Some other annotations imply UNSAFE:
 * <ul>
 * <li>{@link BUILTIN}</li>
 * <li>{@link C_FUNCTION}</li>
 * <li>{@link VM_ENTRY_POINT}</li>
 * <li>{@link ACCESSOR}</li>
 * <li>{@link SUBSTITUTE}: the substitutee is unsafe</li>
 * <li>{@link LOCAL_SUBSTITUTION}: the substitutee is unsafe</li>
 * </ul>
 * <p>
 * However, some must be pointed out manually with this annotation.
 *
 * @see ClassfileReader
 *
 * @author Bernd Mathiske
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface UNSAFE {
}
