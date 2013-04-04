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
package com.oracle.max.vm.ext.vma;

import com.sun.max.vm.thread.*;

/**
 * Certain VM runtime events can be advised, notably garbage-collection, in addition to the
 * bytecode execution.
 *
 * Garbage Collection can be advised before and after.
 * a
 */

public abstract class RuntimeAdvice extends BytecodeAdvice {
    /**
     * Before Garbage collection.
     *
     */
    public abstract void adviseBeforeGC();

    /**
     * After Garbage collection.
     *
     */
    public abstract void adviseAfterGC();

    /**
     * Thread start.
     * @param adviceMode
     * @param vmThread
     */
    public abstract void adviseBeforeThreadStarting(VmThread vmThread);

    /**
     * Thread termination.
     * @param adviceMode
     * @param vmThread
     */
    public abstract void adviseBeforeThreadTerminating(VmThread vmThread);

    /**
     * Captures the case where a THROW bytecode is not handled in the throwing method,
     * causing a return to a method further down the stack.
     * @param bci location of throw
     * @param poppedFrames the number of stack frames being popped. Zero denotes that the exception was not handled.
     * @param throwable the {@link Throwable} causing the return.
     *
     */
    public abstract void adviseBeforeReturnByThrow(int bci, Throwable throwAble, int poppedFrames);
}
