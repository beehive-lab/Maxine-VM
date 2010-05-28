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

package com.sun.max.ins.debug;

import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;

/**
 * An abstract table column model specialized for Inspector table-based views.
 *
 * @author Hannes Payer
 * @author Michael Van De Vanter
 */
public abstract class InspectorTableColumnModel<ColumnKind_Type extends  ColumnKind> extends DefaultTableColumnModel implements Prober {

    private final TableColumn[] columns;
    private final GrowableMapping<Integer, ColumnKind_Type> columnKinds;
    private final TableColumnVisibilityPreferences<ColumnKind_Type> viewPreferences;

    /**
     * Creates a specialized table column model.
     *
     * @param columnKindCount the number of table columns that will be created
     * @param viewPreferences
     */
    public InspectorTableColumnModel(int columnKindCount, TableColumnVisibilityPreferences<ColumnKind_Type> viewPreferences) {
        this.columns = new TableColumn[columnKindCount];
        this.columnKinds = new IdentityHashMapping<Integer, ColumnKind_Type>();
        this.viewPreferences = viewPreferences;
    }

    public final void refresh(boolean force) {
        for (TableColumn column : columns) {
            final TableCellRenderer cellRenderer = column.getCellRenderer();
            if (cellRenderer instanceof Prober) {
                final Prober prober = (Prober) cellRenderer;
                prober.refresh(force);
            }
        }
    }

    public final void redisplay() {
        for (TableColumn column : columns) {
            final TableCellRenderer cellRenderer = column.getCellRenderer();
            if (cellRenderer instanceof Prober) {
                final Prober prober = (Prober) cellRenderer;
                prober.redisplay();
            }
        }
    }

    /**
     * Gets a column from the model per model index.
     *
     * @param col a column model index
     * @return the column
     */
    public TableColumn columnAt(int col) {
        return columns[col];
    }

    /**
     * Sets the visibility of a column already added to this model.
     *
     * @param col index of a column already added to this model
     * @param visible whether the column should now be visible
     */
    public void setColumnVisible(int col, boolean visible) {
        final TableColumn tableColumn = columns[col];
        if (visible) {
            addColumn(tableColumn);
        } else {
            removeColumn(tableColumn);
        }
    }

    /**
     * Adds a column to this model, which may or not be visible depending on the initial description of the column kind.
     */
    protected final void addColumn(ColumnKind_Type columnKind, TableCellRenderer renderer, TableCellEditor editor) {
        final TableColumn tableColumn = new TableColumn(columnKind.ordinal(), 0, renderer, editor);
        tableColumn.setHeaderValue(columnKind.label());
        tableColumn.setMinWidth(columnKind.minWidth());
        tableColumn.setIdentifier(columnKind);
        columnKinds.put(columnKind.ordinal(), columnKind);
        columns[columnKind.ordinal()] = tableColumn;
        if (viewPreferences.isVisible(columnKind)) {
            addColumn(tableColumn);
        }
    }

    public String toolTipTextForColumn(int index) {
        return columnKinds.get(index).toolTipText();
    }

}
