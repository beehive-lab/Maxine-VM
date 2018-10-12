/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.gui;

import com.sun.max.ins.*;
import com.sun.max.tele.*;

/**
 * A label that displays the name of a known VM memory region and acts as a drag source.
 */
public final class MemoryRegionNameLabel extends AbstractMemoryRegionLabel implements Prober {

    /**
     * Creates a label that displays the name of a known memory region and
     * acts as a drag source.
     *
     * @param inspection
     * @param memoryRegion a memory region in the VM
     */
    public MemoryRegionNameLabel(Inspection inspection, MaxMemoryRegion memoryRegion) {
        super(inspection, memoryRegion);
        redisplay();
        refresh(true);
    }

    public void redisplay() {
        setFont(preference().style().javaNameFont());
    }

    public void refresh(boolean force) {
        final String regionName = memoryRegion.regionName();
        if (memoryRegion instanceof MaxEntityMemoryRegion<?>) {
            final MaxEntityMemoryRegion<?> entityMemoryRegion = (MaxEntityMemoryRegion<?>) memoryRegion;
            final Object owner = entityMemoryRegion.owner();
            if (owner instanceof MaxEntity<?>) {
                final MaxEntity<?> entity = (MaxEntity<?>) owner;
                setText(entity.entityName());
                setToolTipText("Memory region for: " + entity.entityDescription());
                return;
            }
        }
        setText(regionName);
        setToolTipText("Memory region \"" + regionName + "\"");
    }
}

