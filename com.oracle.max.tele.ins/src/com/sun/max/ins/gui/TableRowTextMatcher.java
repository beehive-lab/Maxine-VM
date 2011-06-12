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
import java.util.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;

/**
 * A row-based engine for locating rows in a {@link JTable} that match a regexp.
 * Requires that cell renderers in the table implement {@link TextSearchable}.
 * @see {@link java.util.regexp.Pattern}
 */
public class TableRowTextMatcher extends AbstractInspectionHolder implements RowTextMatcher {

    private final JTable table;

    /**
     * The text rows being searched.
     */
    private String[] rowsOfText;

    /**
     * Create a search session for a table.
     *
     * @param inspection
     * @param jTable a table whose cell renderers implement {@link TextSearchable}.
     */
    public TableRowTextMatcher(Inspection inspection, JTable jTable) {
        super(inspection);
        this.table = jTable;
        rowsOfText = rowsOfText(table);
    }

    /**
     * Builds a list of strings corresponding to the current contents
     * of the specified table.
     * <br>
     * Note that this will only see the whole table if no filtering on the table
     * model is in place.
     *
     * @param table
     * @return the text being displayed by each row
     */
    private static String[] rowsOfText(JTable table) {
        final TableModel tableModel = table.getModel();
        final int rowCount = tableModel.getRowCount();
        final TableColumnModel columnModel = table.getColumnModel();
        final int columnCount = columnModel.getColumnCount();
        String[] rowsOfText = new String[rowCount];
        for (int row = 0; row < rowCount; row++) {
            StringBuilder rowText = new StringBuilder();
            for (int col = 0; col < columnCount; col++) {
                final TableColumn column = columnModel.getColumn(col);
                final TableCellRenderer cellRenderer = column.getCellRenderer();
                final int columnIndex = column.getModelIndex();
                final Object valueAt = tableModel.getValueAt(row, columnIndex);
                final Component tableCellRendererComponent = cellRenderer.getTableCellRendererComponent(table, valueAt, false, false, row, columnIndex);
                final TextSearchable searchable = (TextSearchable) tableCellRendererComponent;
                rowText.append(' ').append(searchable.getSearchableText());
            }
            rowsOfText[row] = rowText.toString();
        }
        return rowsOfText;
    }

    public void refresh() {
        rowsOfText = rowsOfText(table);
    }

    public int rowCount() {
        return rowsOfText.length;
    }

    public int[] findMatches(Pattern pattern) {
        String[] rowsOfText = this.rowsOfText;
        final int textRowCount = rowsOfText.length;
        int[] matchingRows = new int[textRowCount];
        int matchingRowCount = 0;
        for (int row = 0; row < textRowCount; row++) {
            if (pattern.matcher(rowsOfText[row]).find()) {
                matchingRows[matchingRowCount++] = row;
            }
        }
        return Arrays.copyOf(matchingRows, matchingRowCount);
    }
}
