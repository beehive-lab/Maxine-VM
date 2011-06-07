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
package com.sun.max.tele.method;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;


public final class TeleCompiledCodeRegion extends AbstractTeleVMHolder implements TeleVMCache, MaxCompiledCodeRegion {

    private static final int TRACE_LEVEL = 2;

    /**
     * Description of a compiled code region allocated by the VM code manager.
     * <br>
     * This region has no parent; it is allocated from the OS.
     * <br>
     * This region has no children.
     * We could decompose it into sub-regions containing a method compilation each, but we don't
     * do that at this time.
     *
     * @author Michael Van De Vanter
     */
    private static final class CompiledCodeRegionMemoryRegion extends TeleDelegatedMemoryRegion implements MaxEntityMemoryRegion<MaxCompiledCodeRegion> {

        private static final List<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY = Collections.emptyList();

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

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
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
    private final List<MaxCompilation> compilations = new ArrayList<MaxCompilation>();

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

    public void updateCache(long epoch) {
        teleCodeRegion.updateCache(epoch);
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

    public TeleObject representation() {
        return teleCodeRegion;
    }

    public boolean isBootRegion() {
        return compiledCodeRegionMemoryRegion.isBootRegion();
    }

    public int compilationCount() {
        return teleCodeRegion.teleTargetMethods().size();
    }

    public List<MaxCompilation> compilations() {
        // Assumes no code eviction; no movement; allocated linearly.
        final List<TeleTargetMethod> teleTargetMethods = teleCodeRegion.teleTargetMethods();
        if (compilations.size() < teleTargetMethods.size()) {
            for (int index = compilations.size(); index < teleTargetMethods.size(); index++) {
                compilations.add(vm().codeCache().findCompiledCode(teleTargetMethods.get(index).getRegionStart()));
            }
        }
        return Collections.unmodifiableList(compilations);
    }

    public List<TeleTargetMethod> teleTargetMethods() {
        return teleCodeRegion.teleTargetMethods();
    }

    public int loadedCompilationCount() {
        return teleCodeRegion.methodLoadedCount();
    }
}
