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
 * might point; blank if no such region.  Updated with every refresh.
 *
 * @author Michael Van De Vanter
 */
public class MemoryRegionValueLabel extends ValueLabel {

    private Address address;
    private String regionName;
    private String toolTipText;
    private MaxMemoryRegion memoryRegion = null;

    private final class MemoryRegionMouseClickAdapter extends InspectorMouseClickAdapter {

        public MemoryRegionMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }

        @Override
        public void procedure(MouseEvent mouseEvent) {
            if (memoryRegion != null) {
                switch (Inspection.mouseButtonWithModifiers(mouseEvent)) {
                    case MouseEvent.BUTTON1: {
                        focus().setMemoryRegion(memoryRegion);
                        break;
                    }
                    case MouseEvent.BUTTON3: {
                        final InspectorPopupMenu menu = new InspectorPopupMenu();
                        menu.add(actions().inspectRegionMemoryWords(memoryRegion, regionName));
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

    public MemoryRegionValueLabel(Inspection inspection) {
        super(inspection);
        initializeValue();
        addMouseListener(new MemoryRegionMouseClickAdapter(inspection));
        redisplay();
    }

    public MemoryRegionValueLabel(Inspection inspection, Address address) {
        super(inspection, new WordValue(address));
        addMouseListener(new MemoryRegionMouseClickAdapter(inspection));
    }

    @Override
    protected void updateText() {
        memoryRegion = null;
        regionName = null;
        toolTipText = null;
        if (value() != null) {
            address = value().toWord().asAddress();
            memoryRegion = vm().findMemoryRegion(address);
        }
        if (memoryRegion != null) {
            regionName = inspection().nameDisplay().shortName(memoryRegion);
            toolTipText = "Value points into " + inspection().nameDisplay().longName(memoryRegion);
        }
        setText(regionName);
        setToolTipText(toolTipText);
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
