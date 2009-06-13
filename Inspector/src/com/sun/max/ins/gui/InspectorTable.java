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

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.tele.*;


/**
 * A table specialized for use in the Maxine Inspector.
 *
 * @author Michael Van De Vanter
 */
public abstract class InspectorTable extends JTable implements Prober, InspectionHolder {

    /**
     *   Notification service for table based views that allow columns to be turned on and off.
     */
    public interface ColumnChangeListener {

        /**
         * Notifies that the set of visible columns has been changed.
         */
        void columnPreferenceChanged();
    }

    private final Inspection _inspection;

    /**
     * Creates a new {@JTable} for use in the {@link Inspection}.
     */
    protected InspectorTable(Inspection inspection) {
        super();
        _inspection = inspection;
        initialize();
    }

    /**
     * Creates a new {@JTable} for use in the {@link Inspection}.
     *
     * @param model a model for the table
     * @param tableColumnModel a column model for the table
     */
    protected InspectorTable(Inspection inspection, TableModel model, TableColumnModel tableColumnModel) {
        super(model, tableColumnModel);
        _inspection = inspection;
        initialize();
    }

    private void initialize() {
        setOpaque(true);
        setBackground(_inspection.style().defaultBackgroundColor());
        getTableHeader().setBackground(_inspection.style().defaultBackgroundColor());
        getTableHeader().setFont(style().defaultTextFont());
    }
    public final Inspection inspection() {
        return _inspection;
    }

    public final MaxVM maxVM() {
        return _inspection.maxVM();
    }

    public final MaxVMState maxVMState() {
        return _inspection.maxVM().maxVMState();
    }

    public final InspectorGUI gui() {
        return _inspection.gui();
    }

    public final InspectorStyle style() {
        return _inspection.style();
    }

    public final InspectionFocus focus() {
        return _inspection.focus();
    }

    public InspectionActions actions() {
        return _inspection.actions();
    }

    private IdentityHashSet<ColumnChangeListener> _columnChangeListeners = new IdentityHashSet<ColumnChangeListener>();

    /**
     * Adds a listener for view update when column visibility changes.
     */
    public void addColumnChangeListener(ColumnChangeListener listener) {
        _columnChangeListeners.add(listener);
    }

    /**
     * Remove a listener for view update when column visibility changed.
     */
    public void removeColumnChangeListener(ColumnChangeListener listener) {
        _columnChangeListeners.remove(listener);
    }

    /**
     * Notifies listeners that the column visibility preferences for the table have changed.
     */
    public void fireColumnPreferenceChanged() {
        for (ColumnChangeListener listener : _columnChangeListeners.clone()) {
            listener.columnPreferenceChanged();
        }
    }

    /**
     * Notifies subclasses that some focus state of interest has changed, typically
     * causing the table's row selection to follow the new focus.
     */
    public void updateFocusSelection() {
    }

    /**
     * Scrolls the table to display the specified range (with a few rows before or after if possible).
     * @param firstRow first row of the range that should be made visible
     * @param lastRow last row of the range that should be made visible
     */
    protected void scrollToRows(int firstRow, int lastRow) {
        assert firstRow <= lastRow;
        final int width = getWidth() - 2;
        final int height = getRowHeight() - 2;
        // Create a rectangle in the table view to use as a scroll target; include
        // the row immediately before and the row immediately after so that the row of interest
        // doesn't land at the very beginning or very end of the view, if possible.
        final Rectangle rectangle = new Rectangle(0, (firstRow - 1) * getRowHeight(), width, 3 * height);
        // System.out.println("row=" + firstRow + " rect=" + rectangle);
        scrollRectToVisible(rectangle);
    }

}
