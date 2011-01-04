/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
/*VCSID=bade66b5-af5c-4565-a940-475c24e12bc7*/
package com.sun.max.vm.heap;
import com.sun.max.unsafe.*;

/**
 * Encapsulates a {@linkplain #visit(Pointer, int) procedure} for being informed
 * of an address expressed as a pointer and word-based index.
 *
 * @author Bernd Mathiske
 */
public abstract class PointerIndexVisitor {

    /**
     * Visits the address denoted by a given pointer and word-based index. That is, visits the effective address
     * computed by {@code pointer.plus(wordIndex * Word.size())}.
     *
     * @param pointer
     * @param wordIndex
     */
    public abstract void visit(Pointer pointer, int wordIndex);
}
