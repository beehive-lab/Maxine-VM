/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ri;

/**
 * The <code>RiOsrFrame</code> interface allows the compiler to query the runtime for
 * the locations of the state of locals, stacks, and locks when generating code to
 * transition from one frame layout during OSR (on-stack replacement).
 *
 * @author Ben L. Titzer
 */
public interface RiOsrFrame {
    /**
     * Get the offset of a local variable within the OSR frame.
     * @param local the local index
     * @return the offset in bytes of the local variable's location
     */
    int getLocalOffset(int local);

    /**
     * Checks whether the local variable is live at the OSR location.
     * @param local the local index
     * @return {@code true} if the local variable is live
     */
    boolean isLive(int local);

    /**
     * Checks whether the local variable is live at the OSR location, and is
     * also an object.
     * @param local the local index
     * @return {@code true} if the local variable is live and is an object
     */
    boolean isLiveObject(int local);

    /**
     * Gets the offset of a stack slot within the OSR frame.
     * @param index the index of the stack slot
     * @return the offset in bytes of the stack slot's location
     */
    int getStackOffset(int index);

    /**
     * Gets the offset of a lock within the OSR frame.
     * @param lock the index of the lock
     * @return the offset in bytes of the lock's location
     */
    int getLockOffset(int lock);

    /**
     * Gets the total size of the frame in bytes.
     * @return the size of the frame
     */
    int frameSize();
}
