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
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;


/**
 * Forwards mouse events to the appropriate table cell.
 *
 * @author Michael Van De Vanter
 */
public class TableCellMouseClickAdapter extends InspectorMouseClickAdapter {

    private final JTable _table;

    /**
     * A listener that forwards mouse events over a table to
     * the particular cell at the mouse location.
     */
    public TableCellMouseClickAdapter(Inspection inspection, JTable table) {
        super(inspection);
        _table = table;
    }

    /**
     * Forwards a mouse event to the table cell at the event location.
     */
    @Override
    public void procedure(final MouseEvent mouseEvent) {
        // Locate the renderer under the event location and pass along the event.
        final Point p = mouseEvent.getPoint();
        final int hitColumnIndex = _table.columnAtPoint(p);
        final int hitRowIndex = _table.rowAtPoint(p);
        if ((hitColumnIndex != -1) && (hitRowIndex != -1)) {
            final TableCellRenderer tableCellRenderer = _table.getCellRenderer(hitRowIndex, hitColumnIndex);
            final Object cellValue = _table.getValueAt(hitRowIndex, hitColumnIndex);
            final Component component = tableCellRenderer.getTableCellRendererComponent(_table, cellValue, false, true, hitRowIndex, hitColumnIndex);
            if (component != null) {
                component.dispatchEvent(mouseEvent);
            }
        }
    }
}
