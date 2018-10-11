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
package com.sun.max.ins.gui;

import java.awt.datatransfer.*;

import com.sun.max.ins.*;
import com.sun.max.tele.*;

/**
 * A label that displays some property of a known MV memory region and acts as a drag source.
 */
public abstract class AbstractMemoryRegionLabel extends InspectorLabel {

    protected final MaxMemoryRegion memoryRegion;

    public AbstractMemoryRegionLabel(Inspection inspection, MaxMemoryRegion memoryRegion) {
        super(inspection);
        assert memoryRegion != null;
        this.memoryRegion = memoryRegion;
        setOpaque(true);
    }

    @Override
    public Transferable getTransferable() {
        if (memoryRegion.start().isZero()) {
            return null;
        }
        return new InspectorTransferable.MemoryRegionTransferable(inspection(), memoryRegion);
    }

}
