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
package com.sun.cri.ri;

/**
 * Allows the compiler to query the runtime for the locations of the state of locals, stacks, and locks when generating
 * code to transition from one frame layout during OSR (on-stack replacement).
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
