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

import java.awt.*;
import java.awt.event.*;
import java.lang.management.*;

import com.sun.max.ins.*;
import com.sun.max.tele.*;

/**
 * A label that displays the allocation percentage of a known memory region and acts as a drag source.
 */
public final class MemoryRegionAllocationLabel extends AbstractMemoryRegionLabel implements Prober {

    private enum DisplayMode {
        PERCENT,
        ALLOCATED;
    }

    private DisplayMode displayMode = DisplayMode.PERCENT;
    private final Component parent;

    /**
     * Returns a that displays the name of a known memory region
     * and acts as a drag source.
     *
     * @param inspection
     * @param memoryRegion a memory region in the VM
     * @param parent a component that should be repainted when the display state is toggled;
     */
    public MemoryRegionAllocationLabel(Inspection inspection, MaxMemoryRegion memoryRegion, Component parent) {
        super(inspection, memoryRegion);
        this.parent = parent;
        addMouseListener(new InspectorMouseClickAdapter(inspection()) {
            @Override
            public void procedure(MouseEvent mouseEvent) {
                switch (inspection().gui().getButton(mouseEvent)) {
                    case MouseEvent.BUTTON2: {
                        final InspectorAction cycleAction = getCycleDisplayTextAction();
                        if (cycleAction != null) {
                            cycleAction.perform();
                        }
                        break;
                    }
                    case MouseEvent.BUTTON3: {
                        final InspectorPopupMenu menu = new InspectorPopupMenu();
                        final InspectorAction cycleAction = getCycleDisplayTextAction();
                        if (cycleAction != null) {
                            menu.add(cycleAction);
                        }
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        });
        redisplay();
        refresh(true);
    }

    private InspectorAction getCycleDisplayTextAction() {
        DisplayMode alternateDisplayMode = displayMode;
        switch(displayMode) {
            case ALLOCATED:
                alternateDisplayMode = DisplayMode.PERCENT;
                break;
            case PERCENT:
                alternateDisplayMode = DisplayMode.ALLOCATED;
                break;
        }
        if (alternateDisplayMode != displayMode) {
            final DisplayMode newDisplayMode = alternateDisplayMode;
            return new InspectorAction(inspection(), "Cycle display (Middle-Button)") {

                @Override
                public void procedure() {
                    //Trace.line(TRACE_VALUE, "WVL: " + displayMode.toString() + "->" + newValueKind);
                    MemoryRegionAllocationLabel label = MemoryRegionAllocationLabel.this;
                    label.displayMode = newDisplayMode;
                    label.redisplay();
                    label.refresh(true);
                    if (label.parent != null) {
                        label.parent.repaint();
                    }
                }
            };
        }
        return null;
    }

    public void redisplay() {
        switch(displayMode) {
            case ALLOCATED:
                setFont(style().hexDataFont());
                break;
            case PERCENT:
                setFont(style().primitiveDataFont());
                break;
        }
        refresh(true);
    }

    public void refresh(boolean force) {
        final MemoryUsage usage = memoryRegion.getUsage();
        final long size = usage.getCommitted();
        if (size == 0) {
            setText(inspection().nameDisplay().unavailableDataShortText());
            setWrappedToolTipText(htmlify(inspection().nameDisplay().unavailableDataLongText()));
        } else {
            final long used = usage.getUsed();
            switch(displayMode) {
                case ALLOCATED:
                    setText(longTo0xHex(used));
                    setWrappedToolTipText(Long.toString(100 * used / size) + "% (" + used + "/" + size + ")");
                    break;
                case PERCENT:
                    setText(Long.toString(100 * used / size) + "%");
                    setWrappedToolTipText(longTo0xHex(used) + "/" + longTo0xHex(size) + " bytes (" + used + "/" + size + ")");
                    break;
            }
        }

    }
}

