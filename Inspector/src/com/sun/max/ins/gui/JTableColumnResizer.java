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
                maxwidth = Math.max(comp.getPreferredSize().width, maxwidth);
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
