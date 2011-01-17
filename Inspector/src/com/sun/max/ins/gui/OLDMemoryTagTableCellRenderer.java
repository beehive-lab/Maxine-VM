/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.tele.*;

/**
 * A renderer suitable for a table "Tag" cell in an {@link Inspector} display where each row corresponds to a memory region.
 * <br>
 * Displays text identifying registers, if any, that point into this region; displays a special border if there is a
 * watchpoint at the location, and displays a pointer icon if a watchpoint is currently triggered at this location.
 *
 * @deprecated
 * @author Michael Van De Vanter
 */
@Deprecated
public abstract class OLDMemoryTagTableCellRenderer extends InspectorLabel implements TableCellRenderer, Prober {

    public OLDMemoryTagTableCellRenderer(Inspection inspection) {
        super(inspection, null);
        setOpaque(true);
    }

    /**
     * Returns a cell render suitable for the "Tag" cell in an Inspector display where each row corresponds
     * to a memory region.  The text and tooltip text of this label/renderer to display informative strings
     * if one or more integer registers in the specified thread point at this location.
     * The text is empty if no registers point at this location.
     * The tooltip text is "unwrapped", set by {@link InspectorLabel#setToolTipText(String)}.
     *
     * @param memoryRegion a memory location in the VM
     * @param thread the thread from which to read registers
     * @param watchpoints the watchpoints at this location, null if none.
     * @return a component for displaying the cell
     */
    public final InspectorLabel getRenderer(MaxMemoryRegion memoryRegion, MaxThread thread, List<MaxWatchpoint> watchpoints) {
        String labelText = "";
        String toolTipText = "";
        setFont(style().defaultFont());
        // See if any registers point here
        if (thread != null) {
            final List<MaxRegister> registers = thread.registers().find(memoryRegion);
            if (registers.isEmpty()) {
                setForeground(style().memoryDefaultTagTextColor());
            } else {
                final String registerNameList = inspection().nameDisplay().registerNameList(registers);
                labelText += registerNameList + "-->";
                toolTipText += "Register(s): " + registerNameList + " in thread " + inspection().nameDisplay().longName(thread) + " point here";
                setForeground(style().memoryRegisterTagTextColor());
            }
        }
        // If a watchpoint is currently triggered here, add a pointer icon.
        if (vm().state().watchpointEvent() != null && memoryRegion.contains(inspection().vm().state().watchpointEvent().address())) {
            setIcon(style().debugIPTagIcon());
            setForeground(style().debugIPTagColor());
        } else {
            setIcon(null);
            setForeground(null);
        }
        if (!watchpoints.isEmpty()) {
            if (!toolTipText.equals("")) {
                toolTipText += "<br>";
            }
            if (watchpoints.size() > 1) {
                toolTipText += "Multiple watchpoints set in memory";
            } else {
                MaxWatchpoint watchpoint = watchpoints.get(0);
                final StringBuilder sb = new StringBuilder();
                sb.append("Watchpoint set @ ").append(watchpoint.memoryRegion().start().to0xHexString());
                sb.append(", size=").append(InspectorLabel.sizeToDecimalAndHex(watchpoint.memoryRegion().size())).append("bytes, ");
                sb.append(watchpoint.isEnabled() ? "enabled" : "disabled");
                toolTipText += sb.toString();
            }
            setText(labelText);
            setWrappedToolTipText(toolTipText);
            setBorder(style().debugDisabledTargetBreakpointTagBorder());
            for (MaxWatchpoint watchpoint : watchpoints) {
                if (watchpoint.isEnabled()) {
                    setBorder(style().debugEnabledTargetBreakpointTagBorder());
                    break;
                }
            }
        } else {
            setBorder(null);
        }
        setText(labelText);
        // Just set standard tool tip text; this will be fetched and used wrapped.
        setToolTipText(toolTipText);
        return this;
    }

    public void redisplay() {
    }

    public void refresh(boolean force) {
    }
}

