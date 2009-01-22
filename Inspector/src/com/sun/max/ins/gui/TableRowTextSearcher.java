/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.ins.gui;

import java.awt.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;


/**
 * A row-based textual search engine for locating rows in a {@link JTable} that match a regexp.
 * Requires that cell renderers in the table implement {@link TextSearchable}.
 *
 * @author Michael Van De Vanter
 * @see {@link java.util.regexp.Pattern}
 */
public class TableRowTextSearcher extends AbstractInspectionHolder implements RowTextSearcher {

    private final JTable _table;

    /**
     * Create a search session for a table.
     *
     * @param inspection
     * @param jTable a table whose cell renderers implement {@link TextSearchable}.
     */
    public TableRowTextSearcher(Inspection inspection, JTable jTable) {
        super(inspection);
        _table = jTable;
    }

    public IndexedSequence<Integer> search(Pattern pattern) {
        final AppendableIndexedSequence<Integer> matchingRows = new VectorSequence<Integer>(64);
        final TableModel tableModel = _table.getModel();
        final int rowCount = tableModel.getRowCount();
        final TableColumnModel columnModel = _table.getColumnModel();
        final int columnCount = columnModel.getColumnCount();
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                final TableColumn column = columnModel.getColumn(col);
                final TableCellRenderer cellRenderer = column.getCellRenderer();
                final int columnIndex = column.getModelIndex();
                final Object valueAt = tableModel.getValueAt(row, columnIndex);
                final Component tableCellRendererComponent = cellRenderer.getTableCellRendererComponent(_table, valueAt, false, false, row, columnIndex);
                final TextSearchable searchable = (TextSearchable) tableCellRendererComponent;
                if (pattern.matcher(searchable.getSearchableText()).find()) {
                    matchingRows.append(row);
                    break;
                }
            }
        }
        return matchingRows;
    }
}
