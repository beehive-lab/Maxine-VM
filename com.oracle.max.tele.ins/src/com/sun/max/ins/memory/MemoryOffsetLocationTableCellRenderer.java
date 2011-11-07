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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;


/**
 * A table cell renderer for tables with rows representing memory locations in VM memory: displays in the cell
 * a memory location specified as a byte offset from some origin.
 */
public final class MemoryOffsetLocationTableCellRenderer extends InspectorTableCellRenderer {

    private final InspectorTable inspectorTable;
    private final InspectorMemoryTableModel tableModel;

    // This kind of label has no interaction state, so we only need one, which we set up on demand.
    private final LocationLabel.AsOffset label;
    private final InspectorLabel[] labels = new InspectorLabel[1];

    public MemoryOffsetLocationTableCellRenderer(Inspection inspection, InspectorTable inspectorTable, InspectorMemoryTableModel tableModel, int indexScaleFactor) {
        super(inspection);
        this.inspectorTable = inspectorTable;
        this.tableModel = tableModel;
        this.label = new LocationLabel.AsOffset(inspection, indexScaleFactor);
        this.labels[0] = this.label;
    }

    public MemoryOffsetLocationTableCellRenderer(Inspection inspection, InspectorTable inspectorTable, InspectorMemoryTableModel tableModel) {
        this(inspection, inspectorTable, tableModel, 0);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        label.setToolTipPrefix(tableModel.getRowDescription(row) + " location<br>Address = ");
        label.setValue(tableModel.getOffset(row), tableModel.getOrigin());
        if (inspectorTable.isBoundaryRow(row)) {
            label.setBorder(style().defaultPaneTopBorder());
        } else {
            label.setBorder(null);
        }
        label.setForeground(inspectorTable.cellForegroundColor(row, column));
        label.setBackground(inspectorTable.cellBackgroundColor(isSelected));
        return label;
    }

    @Override
    protected InspectorLabel[] getLabels() {
        return labels;
    }

}
