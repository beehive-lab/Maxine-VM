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
package com.sun.max.vm.classfile.stackmap;

import com.sun.max.vm.verifier.types.*;

/**
 * A model of frame to which a stack map frame can be {@link StackMapFrame#applyTo(FrameModel) applied}.
 * 
 * @author Doug Simon
 */
public interface FrameModel {

    /**
     * Gets the maximum number of locals that have initialized values. That is, {@code activeLocals() - 1} is the
     * highest index of a local variable in this frame whose value is not {@linkplain VerificationType#TOP}.
     */
    int activeLocals();

    /**
     * Resets the stack to be empty.
     */
    void clearStack();

    /**
     * Stores a value to a local variable.
     * 
     * @param type
     *            the type of the value being stored to the local variable at {@code index}
     * @param index
     *            an index into the local variables array
     */
    void store(VerificationType type, int index);

    /**
     * Adjusts the number of {@linkplain #activeLocals() active locals} down by a given amount.
     * 
     * @param numberOfLocals
     *            the number local variables whose definitions are to be killed
     */
    void chopLocals(int numberOfLocals);

    /**
     * Resets the stack to be empty and the number of {@linkplain #activeLocals() active locals} to be 0.
     */
    void clear();

    /**
     * Pushes a value to the stack.
     * 
     * @param type the type of the value being pushed
     */
    void push(VerificationType type);
}
