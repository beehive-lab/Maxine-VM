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
import com.sun.max.ins.value.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.reference.*;
import com.sun.max.vm.value.*;


/**
 * A table cell renderer for tables with rows representing memory words: displays in the cell
 * the name of the VM's allocated memory region into which the first word of the area represented
 * by the table row points.
 */
public final class MemoryRegionPointerTableCellRenderer extends InspectorTableCellRenderer {

    private final InspectorTable inspectorTable;
    private final InspectorMemoryTableModel tableModel;

    // This kind of label has no interaction state, so we only need one, which we set up on demand.
    private final MemoryRegionValueLabel label;
    private final InspectorLabel[] labels = new InspectorLabel[1];

    /**
     * A renderer that displays the VM's memory region name, if any, into which the word value in the memory represented
     * by the table row points.
     *
     * @param inspection
     * @param inspectorTable the table holding the cell to be rendered
     * @param tableModel a table model in which rows represent memory regions
     */
    public MemoryRegionPointerTableCellRenderer(Inspection inspection, InspectorTable inspectorTable, InspectorMemoryTableModel tableModel) {
        super(inspection);
        this.inspectorTable = inspectorTable;
        this.tableModel = tableModel;
        this.label = new MemoryRegionValueLabel(inspection);
        this.labels[0] = this.label;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
        Value memoryValue = null;
        try {
            memoryValue = vm().memory().readWordValue(tableModel.getAddress(row));
        } catch (InvalidReferenceException invalidReferenceException) {
            return gui().getUnavailableDataTableCellRenderer();
        } catch (DataIOError dataIOError) {
            return gui().getUnavailableDataTableCellRenderer();
        }
        label.setValue(memoryValue);
        label.setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
        if (inspectorTable.isBoundaryRow(row)) {
            label.setBorder(preference().style().defaultPaneTopBorder());
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
