/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;


/**
 * A table cell renderer for tables with rows representing memory words: displays in the cell
 * the value of {@link MaxMemoryManagementInfo#status()}.
 */
public class MemoryMgtStatusRenderer extends InspectorTableCellRenderer {

    private final InspectorTable inspectorTable;
    private final InspectorMemoryTableModel tableModel;

    // This kind of label has no interaction state, so we only need one, which we set up on demand.
    private final InspectorLabel label;
    private final InspectorLabel[] labels = new InspectorLabel[1];

    public MemoryMgtStatusRenderer(Inspection inspection, final InspectorTable inspectorTable, InspectorMemoryTableModel tableModel) {
        super(inspection);
        this.inspectorTable = inspectorTable;
        this.tableModel = tableModel;
        this.label = new TextLabel(inspection, "");
        this.label.setOpaque(true);
        this.labels[0] = this.label;
        redisplay();
    }

    @Override
    public void redisplay() {
        label.setFont(preference().style().defaultFont());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Color backgroundColor = inspectorTable.cellBackgroundColor();
        Color foregroundColor = inspectorTable.cellForegroundColor(row, column);
        final Address memoryAddress = tableModel.getAddress(row);
        final MaxMemoryManagementInfo mmInfo = vm().heap().getMemoryManagementInfo(memoryAddress);
        final MaxMemoryManagementStatus status = mmInfo.status();
        switch(status) {
            case NONE:
                label.setText("");
                break;
            default:
                label.setText(status.toString());
        }
        label.setToolTipPrefix(tableModel.getRowDescription(row) + "<br>Heap status: ");
        label.setWrappedToolTipHtmlText(status.description());
        label.setBackground(backgroundColor);
        label.setForeground(foregroundColor);
        if (inspectorTable.isBoundaryRow(row)) {
            label.setBorder(preference().style().defaultPaneTopBorder());
        } else {
            label.setBorder(null);
        }
        return label;
    }

    @Override
    protected InspectorLabel[] getLabels() {
        return labels;
    }

}
