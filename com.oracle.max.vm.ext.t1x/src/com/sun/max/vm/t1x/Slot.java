/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.t1x;

import java.lang.annotation.*;

/**
 * Specifies the {@linkplain #value() index} of the operand stack slot providing a parameter or
 * return value of a {@linkplain T1X_TEMPLATE template}. Every non-void template method has an
 * implicit {@code @Slot(0)} annotation applied to it to indicate that the return value
 * is pushed to the stack. As such {@code @Slot(-1)} can be used to denote that a non-void
 * template method does not push it's return value to the stack.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface Slot {
    /**
     * The index of the slot relative to the top of the operand stack. 0 denotes the value
     * on top of the stack. The index takes into account the fact that {@code long}
     * and {@code double} values occupy 2 slots. For example, the figure below shows
     * a stack with the annotation instance corresponding to each value on the stack:
     * <pre>
     * tos -> int        @Slot(0)
     *        long       @Slot(1)
     *        float      @Slot(3)
     *        double     @Slot(4)
     *        int        @Slot(6)
     * </pre>
     * In the signature of a template method, the parameters need not follow
     * the order of the stack. The template can have extra parameters that
     * are not derived from the stack and it can ignore stack values. For example,
     * here is a template method signature that only uses two of the values from the
     * above stack and has additional non-stack-derived parameters:
     * <pre>
     *        public static void template(@Slot(4) double val1, Object obj, @Slot(0) int val2) { ... }
     * </pre>
     * A value of {@code -1} implies that the parameter or return value does not occupy
     * a stack slot.
     */
    int value();
}
