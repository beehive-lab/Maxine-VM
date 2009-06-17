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

/**
 * An iterator over a list of the bytecode positions sorted in ascending order at which the JVM state is of interest
 * to a {@link ReferenceSlotVisitor} when interpreting a bytecode method with a {@link ReferenceMapInterpreter}.
 *
 * @author Doug Simon
 */
public interface BytecodePositionIterator {

    /**
     * Gets the bytecode position in the sequence at which this iterator is positioned.
     * The initial position of an iterator is the first bytecode position in the sequence.
     *
     * @return {@code -1} if this iterator is already at the end of the sequence
     */
    int bytecodePosition();

    /**
     * Advances this iterator to the next entry in the sequence.
     *
     * To iterate over the entries in the sequence with a {@code BytecodePositionIterator} instance {@code iter}, use
     * the following loop:
     *
     * <pre>
     * iter.reset();
     * for (int bcp = iter.bytecodePosition(); bcp != -1; bcp = iter.next()) {
     *     // operate on 'bcp'
     * }
     * </pre>
     *
     * Note that the call to {@link #reset()} above is unnecessary if {@link #next()} has never been invoked on {@code
     * iter} since it was constructed or since the last call to {@link #reset()}.
     *
     * @return the bytecode position of the entry to which this iterator was advanced or {@code -1} if this iterator is
     *         already at the end of the sequence
     */
    int next();

    /**
     * Resets this iterator to the first bytecode position in the sequence.
     */
    void reset();
}
