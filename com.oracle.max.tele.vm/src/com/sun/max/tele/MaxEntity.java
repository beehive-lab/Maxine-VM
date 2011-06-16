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

import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * Objects that represent an entity that exists in the VM, for example a thread,
 * stack, register, or heap object.
 * <br>
 * These are intended to be modeled in a simple ownership hierarchy,
 * for example that a VM contains threads, which in turn owns a stack and thread locals block.
 * These relationships are now implicit, not expressed explicitly via this interface.
 * <br>
 * VM entities are often, but not always, represented in a region of memory.  Note, however, that a
 * region of VM memory is not the same thing as a VM entity.  In particular, memory regions occupy
 * a separate containment hierarchy, depending on how each VM entity manages memory.
 */
public interface MaxEntity<Entity_Type extends MaxEntity> {

    /**
     * @return the VM
     */
    MaxVM vm();

    /**
     * Returns a short human-readable name for this entity, suitable for appearance in a table
     * cell or menu item.
     */
    String entityName();

    /**
     * Returns a short description for this entity, suitable for a appearance in a tooltip
     * (mouse roll-over) when more information than just the name is desired.
     */
    String entityDescription();

    /**
     * Gets description of the VM memory, if any, that currently represents this entity.
     * Some VM entities are not explicitly represented by a region of VM memory, even if they
     * own entities that are represented by VM memory regions (for example, a thread owns a
     * stack).
     *
     * @return the memory region in which this entity is represented at this time, null if none.
     */
    MaxEntityMemoryRegion<Entity_Type> memoryRegion();

    /**
     * Determines whether the VM memory representing this entity (if any) includes the
     * specified VM memory location <strong>or</strong> any of the entities "owned" by this
     * entity contain the specified VM memory location.
     * <br>
     * Note that this entity can report that it holds no memory for its own representation but
     * still return true if one of the entities it owns (the way a thread owns a stack) that does.
     *
     * @param address a location in VM memory
     * @return whether this entity or any owned entity is represented by VM memory that includes
     * the specified VM memory location.
     */
    boolean contains(Address address);

    /**
     * Gets the object, if any, that the VM uses to represent this entity; null if none.
     *
     * @return surrogate for the VM object that represents this entity
     */
    TeleObject representation();

}
