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
