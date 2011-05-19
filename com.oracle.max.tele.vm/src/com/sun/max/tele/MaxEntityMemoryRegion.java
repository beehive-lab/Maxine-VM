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

import java.util.*;

/**
 * Description of a region of memory in the VM that is used to represent some entity of interest in the VM.
 *
 * <br>
 * These memory regions participate in a partial <em>containment hierarchy</em>:
 * <ul>
 * <li>
 * A memory region may have a <strong>parent</strong>, an explicitly represented area of memory in the VM
 * in which this memory region is contained.  If a memory region has no parent, then it is presumed to
 * have been allocated directly from the OS.</li>
 * <li>
 * A memory region may have zero or more children:
 * non-overlapping sub-regions of this region. There is no requirement that children
 * cover the region completely.</li>
 * </ul>
 * <br>
 * A memory region may have an owner, which would be a representation of the entity in the VM that
 * owns the memory, for example a thread, stack, or object.
 *
 * @author Michael Van De Vanter
 */
public interface MaxEntityMemoryRegion<Entity_Type extends MaxEntity> extends MaxMemoryRegion {

    /**
     * Gets the memory region in the VM, if any, in which this memory region is included.
     * If the parent is null, then the region is one of the top level memory regions allocated
     * by the VM.
     *
     * @return the closest enclosing memory region that represents an entity in the VM, null if none.
     */
    MaxEntityMemoryRegion<? extends MaxEntity> parent();

    /**
     * Gets zero or more memory regions representing entities in the VM that are within this region.
     * The children do not necessarily cover the parent region.
     *
     * @return enclosed memory regions that represent entities in the VM
     */
    List<MaxEntityMemoryRegion<? extends MaxEntity>> children();

    /**
     * Gets the VM entity that uses or is represented by this span of memory.
     *
     * @return the VM entity that owns this memory, null if none.
     */
    Entity_Type owner();

    /**
     * Returns whether the region is allocated in the boot image. This implies
     * that the region is fully utilized, and that nothing in it will be relocated.
     *
     * @return whether the region is part of the boot image.
     */
    boolean isBootRegion();

}
