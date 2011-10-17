/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.platform.*;

/**
 * This annotation is used to filter out classes, methods and fields from the boot image
 * based on the target platform. For example, to specify that a method should only be
 * included in an image built for SPARC platform, this annotation is used as follows:
 * <pre>
 *     @PLATFORM(cpu = "sparc")
 *     public native void flushRegisterWindows();
 * </pre>
 *
 * The value of each annotation element is a string used to match against the
 * named platform component. The {@link Platform#isAcceptedBy(PLATFORM)} method
 * performs the test.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface PLATFORM {
    /**
     * Specifies an operating system constraint on the annotated element.
     * Example values are {@code "!windows"} and {@code "darwin"}.
     */
    String os() default "";

    /**
     * Specifies a processor model constraint on the annotated element.
     * Example values are {@code "(amd64|ia32)"}, {@code "sparcv9"}, {@code "!arm.*"}.
     */
    String cpu() default "";
}
