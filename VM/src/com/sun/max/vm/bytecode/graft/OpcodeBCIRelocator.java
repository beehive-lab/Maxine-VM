/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
public interface OpcodeBCIRelocator {

    /**
     * Gets the post-transformation BCI of the instruction that was located at a given pre-transformation BCI.
     * If {@code opcodeBCI} corresponds to the length of the pre-transformation bytecode array then the length of
     * the transformed bytecode array is returned.
     *
     * @param opcodeBCI
     *                the pre-transformation BCI of an instruction or the length of the pre-transformation bytecode array
     * @return the relocated value of {@code opcodeBCI}
     * @throws IllegalArgumentException
     *                 if {@code opcodeBCI} does not correspond with a pre-transformation instruction BCI or the
     *                 length of the pre-transformation bytecode array
     */
    int relocate(int opcodeBCI) throws IllegalArgumentException;
}
