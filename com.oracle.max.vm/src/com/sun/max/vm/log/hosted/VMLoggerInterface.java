/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.log.hosted;

import java.lang.annotation.*;

import com.sun.max.vm.log.*;


/**
 * Identifies an interface as a {@link VMLogger} for auto-generation.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface VMLoggerInterface {
    /**
     * Identifies the parent class for the auto-generated implementation.
     */
    Class parent() default VMLogger.class;
    /**
     * If {@code true} creates a default no-arg constructor that can be used to completely hide and disable the logger.
     */
    boolean defaultConstructor() default false;
    /**
     * Suppresses all tracing aspects when {@code true}.
     */
    boolean noTrace() default false;
    /**
     * Special logger that is hidden to the user and controlled by VM code.
     */
    boolean hidden() default false;

    /**
     * Includes the thread id as an argument to the generated trace method.
     */
    boolean traceThread() default false;

}
