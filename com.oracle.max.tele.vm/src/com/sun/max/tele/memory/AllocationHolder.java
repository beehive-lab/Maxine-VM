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
package com.sun.max.tele.memory;

import java.util.*;

import com.sun.max.tele.*;


//TODO (mlvdv) decide whether to expose this in the VMI interfaces

/**
 * A VM entity that can <em>own</em> regions of memory allocated from the OS.
 */
public interface AllocationHolder {

    /**
     * Gets the regions of memory allocated from the OS that this entity
     * <em>owns</em>, in the sense that this entity tracks its allocation
     * and possible deallocation.
     * <p>
     * This includes both regions that are allocated directly by the VM,
     * as well as those allocated implicitly, for example by the creation
     * of threads and the loading of shared libraries.
     */
    List<MaxMemoryRegion> memoryAllocations();
}
