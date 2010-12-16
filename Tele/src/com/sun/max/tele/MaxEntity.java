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
package com.sun.max.tele;

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
 *
 * @author Michael Van De Vanter
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

}
