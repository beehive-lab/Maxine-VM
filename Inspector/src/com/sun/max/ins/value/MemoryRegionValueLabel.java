/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.value;

import java.awt.datatransfer.*;
import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.value.*;

/**
 * A label that displays the name of the {@linkplain MaxMemoryRegion memory region} to which a {@link Value}
 * might point, blank if it points into no known region.
 * <br>
 * The tooltip starts with an optionally set prefix, which is intended to allow specification of what
 * sort of value is being used, followed by "points into ", followed by the name of the region.
 * <br>
 * Updated every refresh.
 *
 * @author Michael Van De Vanter
 */
public class MemoryRegionValueLabel extends ValueLabel {

    private String regionName;
    private MaxMemoryRegion memoryRegion = null;

    private final class MemoryRegionMouseClickAdapter extends InspectorMouseClickAdapter {

        public MemoryRegionMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }

        @Override
        public void procedure(MouseEvent mouseEvent) {
            if (memoryRegion != null) {
                switch (inspection().gui().getButton(mouseEvent)) {
                    case MouseEvent.BUTTON1: {
                        focus().setMemoryRegion(memoryRegion);
                        break;
                    }
                    case MouseEvent.BUTTON3: {
                        final InspectorPopupMenu menu = new InspectorPopupMenu();
                        menu.add(views().memory().makeViewAction(memoryRegion, regionName, null));
                        menu.add(actions().selectMemoryRegion(memoryRegion));
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Creates an opaque label that displays which, if any, memory region into which an address points.
     * <br>
     * The address is set by overriding {@link ValueLabel#initializeValue()}.
     * <br>
     * The address can be updated dynamically if {@link ValueLabel#fetchValue()} is overridden.
     *
     * @param inspection the current inspection
     * @param toolTipPrefix optional prefix for every tooltip display
     * @see ValueLabel
     */
    public MemoryRegionValueLabel(Inspection inspection, String toolTipPrefix) {
        super(inspection);
        setToolTipPrefix(toolTipPrefix);
        setOpaque(true);
        initializeValue();
        addMouseListener(new MemoryRegionMouseClickAdapter(inspection));
        redisplay();
    }

    /**
     * Creates an opaque label that displays which, if any, memory region into which an address points.
     * <br>
     * The address is set by overriding {@link ValueLabel#initializeValue()}.
     * <br>
     * The address can be updated dynamically if {@link ValueLabel#fetchValue()} is overridden.
     *
     * @param inspection the current inspection
     * @see ValueLabel
     */
    public MemoryRegionValueLabel(Inspection inspection) {
        this(inspection, "");
    }

    /**
     * Creates an opaque label that displays which, if any, memory region into which an address points.
     * <br>
     * The address can be updated dynamically if {@link ValueLabel#fetchValue()} is overridden.
     *
     * @param inspection the current inspection
     * @param address a location in VM memory
     * @param toolTipPrefix optional prefix for every tooltip display
     */
    public MemoryRegionValueLabel(Inspection inspection, Address address, String toolTipPrefix) {
        super(inspection, new WordValue(address));
        setToolTipPrefix(toolTipPrefix);
        setOpaque(true);
        addMouseListener(new MemoryRegionMouseClickAdapter(inspection));
    }

    /**
     * Creates an opaque label that displays which, if any, memory region into which an address points.
     * <br>
     * The address can be updated dynamically if {@link ValueLabel#fetchValue()} is overridden.
     *
     * @param inspection the current inspection
     * @param address a location in VM memory.
     * @see ValueLabel
     */
    public MemoryRegionValueLabel(Inspection inspection, Address address) {
        this(inspection, address, "");
    }

    @Override
    protected void updateText() {
        memoryRegion = null;
        regionName = "";
        String toolTipText = "Points into no known memory region";
        if (value() != null && value() != VoidValue.VOID) {
            memoryRegion = vm().findMemoryRegion(value().toWord().asAddress());
            if (memoryRegion != null) {
                regionName = inspection().nameDisplay().shortName(memoryRegion);
                toolTipText = "Points into " + inspection().nameDisplay().longName(memoryRegion);
            }
        }
        setText(regionName);
        setWrappedToolTipText(toolTipText);
    }

    public void redisplay() {
        setFont(style().javaNameFont());
        updateText();
    }

    @Override
    public Transferable getTransferable() {
        if (memoryRegion != null) {
            return new InspectorTransferable.MemoryRegionTransferable(inspection(), memoryRegion);
        }
        return null;
    }

}
