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
package com.sun.max.vm.bytecode.refmaps;

/**
 * An iterator over a list of BCIs sorted in ascending order at which the JVM state is of interest
 * to a {@link ReferenceSlotVisitor} when interpreting a bytecode method with a {@link ReferenceMapInterpreter}.
 *
 * @author Doug Simon
 */
public interface BCIIterator {

    /**
     * Gets the BCI in the sequence at which this iterator is positioned.
     * The initial position of an iterator is the first BCI in the sequence.
     *
     * @return {@code -1} if this iterator is already at the end of the sequence
     */
    int bci();

    /**
     * Advances this iterator to the next entry in the sequence.
     *
     * To iterate over the entries in the sequence with a {@code BytecodePositionIterator} instance {@code iter}, use
     * the following loop:
     *
     * <pre>
     * iter.reset();
     * for (int bci = iter.bci(); bci != -1; bci = iter.next()) {
     *     // operate on 'bci'
     * }
     * </pre>
     *
     * Note that the call to {@link #reset()} above is unnecessary if {@link #next()} has never been invoked on {@code
     * iter} since it was constructed or since the last call to {@link #reset()}.
     *
     * @return the BCI of the entry to which this iterator was advanced or {@code -1} if this iterator is
     *         already at the end of the sequence
     */
    int next();

    /**
     * Resets this iterator to the first BCI in the sequence.
     */
    void reset();
}
