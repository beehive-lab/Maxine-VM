/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.ins.memory;

import java.awt.*;
import java.util.List;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;


/**
 * A table cell renderer for tables in which each row is associated with a memory region, and which displays
 * information about any registers in the current thread that point into the information, along with information
 * about any watchpoints set and possibly triggered n the region.
 */
public final class MemoryTagTableCellRenderer extends InspectorTableCellRenderer {

    private final InspectorTable inspectorTable;
    private final InspectorMemoryTableModel inspectorMemoryTableModel;

    // This kind of label has no interaction state, so we only need one, which we set up on demand.
    private final InspectorLabel label;
    private final InspectorLabel[] labels = new InspectorLabel[1];

    /**
     * A renderer that displays the type, if any, which is expected to apply to the contents of
     * a memory region in the VM corresponding to a row in a table.
     *
     * @param inspection
     * @param inspectorTable the table holding the cell to be rendered
     * @param inspectorMemoryTableModel a table model in which rows represent memory regions
     */
    public MemoryTagTableCellRenderer(Inspection inspection, InspectorTable inspectorTable, InspectorMemoryTableModel inspectorMemoryTableModel) {
        super(inspection);
        this.inspectorTable = inspectorTable;
        this.inspectorMemoryTableModel = inspectorMemoryTableModel;
        this.label = new PlainLabel(inspection, "");
        this.label.setOpaque(true);
        this.labels[0] = this.label;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
        String labelText = "";
        String toolTipText = inspectorMemoryTableModel.getRowDescription(row);

        final MaxMemoryRegion memoryRegionForRow = inspectorMemoryTableModel.getMemoryRegion(row);
        final MaxThread currentThread = focus().thread();
        final List<MaxWatchpoint> watchpointsInRegion = inspectorMemoryTableModel.getWatchpoints(row);

        // Add label and tooltip text describing any registers that might point into this region.
        if (currentThread != null) {
            final List<MaxRegister> registersPointingIntoRegion = currentThread.registers().find(memoryRegionForRow);
            if (!registersPointingIntoRegion.isEmpty()) {
                final String registerNameList = inspection().nameDisplay().registerNameList(registersPointingIntoRegion);
                labelText += registerNameList + "-->";
                toolTipText += "<br>Register(s): " + registerNameList + " in thread " + inspection().nameDisplay().longName(currentThread) + " point here";
            }
        }

        // Add a border and some tooltip text if any watchpoints exist in this region
        if (!watchpointsInRegion.isEmpty()) {
            // Set an appropriate border showing the presence of watchpoints, with a different
            // border style, depending on whether any of them are currently enabled.
            // Assume all are disabled until discovered otherwise.
            label.setBorder(style().debugDisabledMachineCodeBreakpointTagBorder());
            for (MaxWatchpoint watchpoint : watchpointsInRegion) {
                final StringBuilder sb = new StringBuilder();
                sb.append("<br>Watchpoint set @ ").append(watchpoint.memoryRegion().start().to0xHexString());
                sb.append(", size=").append(InspectorLabel.longToDecimalAndHex(watchpoint.memoryRegion().nBytes())).append("bytes, ");
                sb.append(watchpoint.isEnabled() ? "enabled" : "disabled");
                toolTipText += sb.toString();
                if (watchpoint.isEnabled()) {
                    label.setBorder(style().debugEnabledMachineCodeBreakpointTagBorder());
                }
            }
        } else {
            // If no special watchpoint border, then see if we need to set a boundary border;
            // it isn't to handy to try doing both.
            if (inspectorTable.isBoundaryRow(row)) {
                label.setBorder(style().defaultPaneTopBorder());
            } else {
                label.setBorder(null);
            }
        }

        // Add a pointer icon and some tool tip information if a watchpoint is triggered in this region.
        final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
        if (watchpointEvent != null) {
            if (memoryRegionForRow.contains(watchpointEvent.address())) {
                toolTipText += "<br>Watchpoint triggered here @ :" + watchpointEvent.address().to0xHexString();
                label.setIcon(style().debugIPTagIcon());
                label.setForeground(style().debugIPTagColor());
            } else {
                label.setIcon(null);
                label.setForeground(null);
            }
        }
        label.setText(labelText);
        label.setWrappedToolTipText(toolTipText);
        label.setBackground(inspectorTable.cellBackgroundColor(isSelected));
        label.setForeground(inspectorTable.cellForegroundColor(row, column));
        label.setFont(style().defaultFont());
        return label;
    }


    @Override
    protected InspectorLabel[] getLabels() {
        return labels;
    }
}
