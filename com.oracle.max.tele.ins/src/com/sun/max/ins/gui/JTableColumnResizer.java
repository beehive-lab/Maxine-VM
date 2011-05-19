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
package com.sun.max.ins.gui;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

/**
 * Sets the preferred width of columns in a {@link JTable}, based on both the
 * data present in the model and column header labels.
 *
 * @author Michael Van De Vanter
 *
 */
public class JTableColumnResizer {

    public static void adjustColumnPreferredWidths(JTable table) {
        adjustColumnPreferredWidths(table, 100);
    }

    public static void adjustColumnPreferredWidths(JTable table, int maxRows) {
        // strategy - get max width for cells in column and
        // make that the preferred width
        final TableColumnModel columnModel = table.getColumnModel();
        for (int col = 0; col < table.getColumnCount(); col++) {
            int maxwidth = 0;
            for (int row = 0; row < Math.min(maxRows, table.getRowCount()); row++) {
                final TableCellRenderer rend = table.getCellRenderer(row, col);
                final Object value = table.getValueAt(row, col);
                final Component comp = rend.getTableCellRendererComponent(table, value,  false,  false,  row, col);
                if (comp != null) {
                    maxwidth = Math.max(comp.getPreferredSize().width, maxwidth);
                }
            } // for row
            // Take the column header into consideration as well.
            final TableColumn column = columnModel.getColumn(col);
            TableCellRenderer headerRenderer = column.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            final Object headerValue = column.getHeaderValue();
            final Component headerComp = headerRenderer.getTableCellRendererComponent(table, headerValue, false, false, 0, col);
            maxwidth = Math.max(maxwidth, headerComp.getPreferredSize().width);
            column.setPreferredWidth(maxwidth);
        } // for col
    }
}
