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
