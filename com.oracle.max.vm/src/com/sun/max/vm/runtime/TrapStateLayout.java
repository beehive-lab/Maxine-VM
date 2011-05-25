/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.runtime;

/**
 * This class describes the layout of a frame which contains either trap state (i.e. the
 * contents of registers saved upon a trap) or another callee-save frame such as a
 * {@link com.sun.max.vm.trampoline.TrampolineGenerator trampoline} or global stub.
 */
public abstract class TrapStateLayout {

    /**
     * Gets the offset of the instruction pointer within the trap state.
     * @return the offset of the instruction pointer
     */
    public abstract int getInstructionPointerOffset();

    /**
     * Gets the offset of the stack pointer within the trap state.
     * @return the offset of the stack pointer
     */
    public abstract int getStackPointerOffset();

    /**
     * Gets the offset of the frame pointer within the trap state.
     * @return the offset of the frame pointer
     */
    public abstract int getFramePointerOffset();

    /**
     * Gets the offset of the trap number within the trap state.
     * @return the offset of the trap number
     */
    public abstract int getTrapNumberOffset();

    /**
     * Gets the offset of the safepoint latch within the trap state.
     * @return the offset of the safepoint latch
     */
    public abstract int getSafepointLatchOffset();
}
