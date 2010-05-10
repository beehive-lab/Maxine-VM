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
package com.sun.max.tele.method;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;


public final class TeleCompiledCodeRegion extends AbstractTeleVMHolder implements MaxCompiledCodeRegion {

    private static final int TRACE_LEVEL = 2;

    /**
     * Description of a compiled code region allocated by the VM code manager.
     * <br>
     * This region has no parent; it is allocated from the OS.
     * <br>
     * This region has no children.
     * We could decompose it into sub-regions containing a method compilation each, but we don't
     * do that at this time.
     */
    private static final class CompiledCodeRegionMemoryRegion extends TeleDelegatedMemoryRegion implements MaxEntityMemoryRegion<MaxCompiledCodeRegion> {

        private static final IndexedSequence<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY =
            new ArrayListSequence<MaxEntityMemoryRegion< ? extends MaxEntity>>(0);

        private final MaxCompiledCodeRegion owner;
        private final boolean isBootRegion;

        /**
         * Creates an object that describes a region of VM memory used to hold compiled code.
         *
         * @param owner the object that models the code allocation area
         * @param teleCodeRegion the VM object that describes this region of VM memory
         * @param isBootRegion whether this region is in the boot image
         */
        private CompiledCodeRegionMemoryRegion(TeleVM teleVM, MaxCompiledCodeRegion owner, TeleCodeRegion teleCodeRegion, boolean isBootRegion) {
            super(teleVM, teleCodeRegion);
            this.owner = owner;
            this.isBootRegion = isBootRegion;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // Compiled code regions are allocated from the OS, not part of any other region
            return null;
        }

        public IndexedSequence<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            // We don't break a compiled code memory region into any smaller entities.
            return EMPTY;
        }

        public MaxCompiledCodeRegion owner() {
            return owner;
        }

        public boolean isBootRegion() {
            return isBootRegion;
        }
    }

    private final TeleCodeRegion teleCodeRegion;
    private final CompiledCodeRegionMemoryRegion compiledCodeRegionMemoryRegion;
    private final String entityDescription;

    /**
     * Creates an object that models an allocation region in the VM that is used for compiled code.
     *
     * @param teleVM the VM
     * @param teleCodeRegion the VM object that describes the memory allocated
     * @param isBootRegion whether this region is in the boot image.
     */
    public TeleCompiledCodeRegion(TeleVM teleVM, TeleCodeRegion teleCodeRegion, boolean isBootRegion) {
        super(teleVM);
        this.teleCodeRegion = teleCodeRegion;
        this.compiledCodeRegionMemoryRegion = new CompiledCodeRegionMemoryRegion(teleVM, this, teleCodeRegion, isBootRegion);
        if (isBootRegion) {
            this.entityDescription = "The allocation area for pre-compiled methods in the " + teleVM.entityName() + " boot image";
        } else {
            this.entityDescription = "An allocation area for compiled methods in the " + teleVM.entityName();
        }
    }

    public String entityName() {
        return compiledCodeRegionMemoryRegion.regionName();
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxCompiledCodeRegion> memoryRegion() {
        return compiledCodeRegionMemoryRegion;
    }

    public boolean contains(Address address) {
        return compiledCodeRegionMemoryRegion.contains(address);
    }

    public boolean isBootRegion() {
        return compiledCodeRegionMemoryRegion.isBootRegion();
    }

    public List<TeleTargetMethod> teleTargetMethods() {
        return teleCodeRegion.teleTargetMethods();
    }
}
