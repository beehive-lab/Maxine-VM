/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.bytecode.refmaps;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;

/**
 * Provides the context when performing {@linkplain ReferenceMapInterpreter abstract interpretation} to compute which local
 * variables and operand stack slots contain references at some bytecode position in a method. This context details how
 * the bytecode is partitioned into control flow basic blocks and holds the frame state for entry to each basic block
 * during interpretation.
 * 
 * @author Doug Simon
 */
public interface ReferenceMapInterpreterContext {
    /**
     * Gets the basic block index for a given bytecode position.
     * 
     * @param bytecodePosition
     * @return the index of the basic block containing {@code bytecodePosition}
     */
    int blockIndexFor(int bytecodePosition);

    /**
     * Gets the bytecode position denoting the start of the basic block at a given index.
     * If {@code blockIndex == numberOfBlocks()}, then the length of the bytecode array is returned.
     */
    int blockStartBytecodePosition(int blockIndex);

    /**
     * Gets the entry frame states for the basic blocks. The returned value is encoded according to a {@link ReferenceMapInterpreterFrameFormat} value.
     */
    Object blockFrames();

    /**
     * Gets the list of exception handlers that may handle an exception thrown at a given bytecode position.
     * 
     * @return null if there are no exception handlers live at {@code bytecodePosition}
     */
    ExceptionHandler exceptionHandlersActiveAt(int bytecodePosition);

    /**
     * Gets the actor for the method being interpreted.
     * @return
     */
    ClassMethodActor classMethodActor();

    int numberOfBlocks();
}
