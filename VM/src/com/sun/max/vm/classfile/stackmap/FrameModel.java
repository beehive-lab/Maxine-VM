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
package com.sun.max.vm.classfile.stackmap;

import com.sun.max.vm.verifier.types.*;

/**
 * A model of frame to which a stack map frame can be {@link StackMapFrame#applyTo(FrameModel) applied}.
 *
 * @author Doug Simon
 */
public interface FrameModel {

    /**
     * Gets the maximum number of locals that have initialized values. That is, {@code activeLocals() - 1} is the
     * highest index of a local variable in this frame whose value is not {@linkplain VerificationType#TOP}.
     */
    int activeLocals();

    /**
     * Resets the stack to be empty.
     */
    void clearStack();

    /**
     * Stores a value to a local variable.
     *
     * @param type
     *            the type of the value being stored to the local variable at {@code index}
     * @param index
     *            an index into the local variables array
     */
    void store(VerificationType type, int index);

    /**
     * Adjusts the number of {@linkplain #activeLocals() active locals} down by a given amount.
     *
     * @param numberOfLocals
     *            the number local variables whose definitions are to be killed
     */
    void chopLocals(int numberOfLocals);

    /**
     * Resets the stack to be empty and the number of {@linkplain #activeLocals() active locals} to be 0.
     */
    void clear();

    /**
     * Pushes a value to the stack.
     *
     * @param type the type of the value being pushed
     */
    void push(VerificationType type);
}
