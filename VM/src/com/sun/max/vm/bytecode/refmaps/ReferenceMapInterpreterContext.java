/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
