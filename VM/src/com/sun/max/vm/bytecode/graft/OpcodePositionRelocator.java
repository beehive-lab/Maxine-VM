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
package com.sun.max.vm.bytecode.graft;

/**
 * A mechanism for communicating how instructions have changed location as the result of a
 * {@linkplain BytecodeTransformer bytecode transformation}.
 * 
 * @author Doug Simon
 */
public interface OpcodePositionRelocator {

    /**
     * Gets the post-transformation position of the instruction that was located at a given pre-transformation position.
     * If {@code opcodePosition} corresponds to the length of the pre-transformation bytecode array then the length of
     * the transformed bytecode array is returned.
     * 
     * @param opcodePosition
     *                the pre-transformation position of an instruction or the length of the pre-transformation bytecode array
     * @return the relocated value of {@code opcodePosition}
     * @throws IllegalArgumentException
     *                 if {@code opcodePosition} does not correspond with a pre-transformation instruction position or the
     *                 length of the pre-transformation bytecode array
     */
    int relocate(int opcodePosition) throws IllegalArgumentException;
}
