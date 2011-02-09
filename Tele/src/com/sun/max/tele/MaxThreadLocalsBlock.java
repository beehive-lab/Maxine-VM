/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.runtime.*;

/**
 * Access to the "thread locals block" of storage for a thread in the VM.
 *
 * @author Michael Van De Vanter
 */
public interface MaxThreadLocalsBlock extends MaxEntity<MaxThreadLocalsBlock> {

    /**
     * Gets the thread that owns the thread locals block; doesn't change.
     * <br>
     * Thread-safe
     *
     * @return the thread that owns this thread locals block.
     */
    MaxThread thread();

    /**
     * Gets the VM thread locals area corresponding to a given safepoint state.
     */
    MaxThreadLocalsArea tlaFor(Safepoint.State state);

    /**
     * Gets the thread locals area in this thread, if any, that includes
     * a specified memory address in the VM.
     * <br>
     * Thread-safe
     *
     * @param address a memory location in the VM
     * @return the thread locals area in this thread that contains the address, null if none.
     */
    MaxThreadLocalsArea findTLA(Address address);

}
