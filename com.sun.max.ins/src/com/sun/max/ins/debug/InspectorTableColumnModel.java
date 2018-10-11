/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.ins.debug;

import java.util.*;

import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;

/**
 * An abstract table column model specialized for Inspector table-based views.
 */
public abstract class InspectorTableColumnModel<ColumnKind_Type extends  ColumnKind> extends DefaultTableColumnModel implements Prober {

    private final Inspection inspection;

    /**
     * The supported columns that have been created, indexed by the kind's enum ordinal.
     * There will be null values for unsupported column kinds, and these will not show up in the kind map
     */
    private final TableColumn[] columns;

    /**
     * Map:  ColumnKind.ordinal() -> ColumnKind
     * Contains only the kinds that are supported .
     */
    private final Map<Integer, ColumnKind_Type> columnKinds;
    private final TableColumnVisibilityPreferences<ColumnKind_Type> viewPreferences;

    /**
     * Creates a specialized table column model.
     * @param columnKindCount the number of table columns that will be created
     * @param viewPreferences
     */
    public InspectorTableColumnModel(Inspection inspection, int columnKindCount, TableColumnVisibilityPreferences<ColumnKind_Type> viewPreferences) {
        this.inspection = inspection;
        this.columns = new TableColumn[columnKindCount];
        this.columnKinds = new IdentityHashMap<Integer, ColumnKind_Type>();
        this.viewPreferences = viewPreferences;
    }

    public final void refresh(boolean force) {
        for (TableColumn column : columns) {
            if (column != null) {
                final TableCellRenderer cellRenderer = column.getCellRenderer();
                if (cellRenderer instanceof Prober) {
                    final Prober prober = (Prober) cellRenderer;
                    prober.refresh(force);
                }
            }
        }
    }

    public final void redisplay() {
        for (TableColumn column : columns) {
            if (column != null) {
                final TableCellRenderer cellRenderer = column.getCellRenderer();
                if (cellRenderer instanceof Prober) {
                    final Prober prober = (Prober) cellRenderer;
                    prober.redisplay();
                }
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
     * Adds a column to this model, if supported for the current VM, which may or not be visible depending on the initial description of the column kind.
     */
    protected final void addColumnIfSupported(ColumnKind_Type columnKind, TableCellRenderer renderer, TableCellEditor editor) {
        if (columnKind.isSupported(inspection.vm())) {
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
    }

    public String toolTipTextForColumn(int index) {
        return columnKinds.get(index).toolTipText();
    }

}
