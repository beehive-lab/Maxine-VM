/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele;

import com.sun.max.unsafe.*;

/**
 * Access to the state of one register in a thread in the VM.
 */
public interface MaxRegister extends MaxEntity<MaxRegister> {

    /**
     * Gets the name of the register based on the platform specification.
     *
     * @return the name of the register.
     */
    String name();

    /**
     * Gets the current value of the register, determined the last time
     * when it was possible to read from the VM.
     *
     * @return the most recently read value of the register in the VM.
     */
    Address value();

    /**
     * Is this register serving as the instruction pointer for
     * the thread in the VM?
     *
     * @return whether this register holds the instruction pointer.
     */
    boolean isInstructionPointerRegister();

    /**
     * Is this register serving as the flags register for the thread in the VM?
     *
     * @return whether this register holds condition flags.
     */
    boolean isFlagsRegister();

}
