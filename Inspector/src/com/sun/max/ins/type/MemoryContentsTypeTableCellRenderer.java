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

package com.sun.max.ins.type;

import java.awt.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.vm.type.*;


/**
 * A table cell renderer for tables with rows representing memory regions that are expected
 * to contain data of a specified type; displays in the cell information about the type.
 *
 * @author Michael Van De Vanter
 */
public final class MemoryContentsTypeTableCellRenderer extends InspectorTableCellRenderer {

    private final InspectorTable inspectorTable;
    private final InspectorMemoryTableModel inspectorMemoryTableModel;

    // This kind of label has no interaction state, so we only need one, which we set up on demand.
    private final TypeLabel label;
    private final InspectorLabel[] labels = new InspectorLabel[1];

    /**
     * A renderer that displays the type, if any, which is expected to apply to the contents of
     * a memory region in the VM corresponding to a row in a table.
     *
     * @param inspection
     * @param inspectorTable the table holding the cell to be rendered
     * @param inspectorMemoryTableModel a table model in which rows represent memory regions
     */
    public MemoryContentsTypeTableCellRenderer(Inspection inspection, InspectorTable inspectorTable, InspectorMemoryTableModel inspectorMemoryTableModel) {
        super(inspection);
        this.inspectorTable = inspectorTable;
        this.inspectorMemoryTableModel = inspectorMemoryTableModel;
        this.label = new TypeLabel(inspection);
        this.labels[0] = this.label;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
        final TypeDescriptor rowType = inspectorMemoryTableModel.getRowType(row);
        if (rowType == null) {
            return gui().getUnavailableDataTableCellRenderer();
        }
        label.setToolTipPrefix(inspectorMemoryTableModel.getRowDescription(row) + "<br>Type = ");
        label.setValue(rowType);
        if (inspectorTable.isBoundaryRow(row)) {
            label.setBorder(style().defaultPaneTopBorder());
        } else {
            label.setBorder(null);
        }
        label.setBackground(inspectorTable.cellBackgroundColor(isSelected));
        label.setForeground(inspectorTable.cellForegroundColor(row, column));
        return label;
    }

    @Override
    protected InspectorLabel[] getLabels() {
        return labels;
    }
}
