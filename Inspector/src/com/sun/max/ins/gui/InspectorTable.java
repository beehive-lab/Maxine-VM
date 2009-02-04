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

import javax.swing.*;

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
        setOpaque(true);
        setBackground(inspection.style().defaultBackgroundColor());
        getTableHeader().setBackground(inspection.style().defaultBackgroundColor());
        getTableHeader().setFont(style().defaultTextFont());
    }

    public final Inspection inspection() {
        return _inspection;
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

    public TeleVM teleVM() {
        return _inspection.teleVM();
    }

    private IdentityHashSet<ColumnChangeListener> _columnChangeListeners = new IdentityHashSet<ColumnChangeListener>();

    /**
     * Adds a listener for view update when column visibility changes.
     */
    public void addColumnChangeListener(ColumnChangeListener listener) {
        synchronized (this) {
            _columnChangeListeners.add(listener);
        }
    }

    /**
     * Remove a listener for view update when column visibility changed.
     */
    public void removeColumnChangeListener(ColumnChangeListener listener) {
        synchronized (this) {
            _columnChangeListeners.remove(listener);
        }
    }

    /**
     * Notifies listeners that the column visibility preferences for the table have changed.
     */
    public void fireColumnPreferenceChanged() {
        for (ColumnChangeListener listener : _columnChangeListeners.clone()) {
            listener.columnPreferenceChanged();
        }
    }

}
