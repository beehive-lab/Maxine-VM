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

import java.awt.event.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.memory.*;
import com.sun.max.tele.*;

/**
 * Utilities for managing {@linkplain MaxWatchpoint memory watchpoint}s in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class Watchpoints {

    public abstract static class ToggleWatchpointRowAction extends InspectorAction {

        final Inspection inspection;
        final InspectorMemoryTableModel tableModel;
        final int row;

        /**
         * An action that will toggle a memory watchpoint at the location described in a memory-based table.
         */
        public ToggleWatchpointRowAction(Inspection inspection, InspectorMemoryTableModel tableModel, int row, String title) {
            super(inspection, title);
            this.inspection = inspection;
            this.tableModel = tableModel;
            this.row = row;
        }

        /**
         * Sets a watchpoint if none exists at location.
         *
         * @return newly created watchpoint; null if failed.
         */
        public abstract MaxWatchpoint setWatchpoint();

        @Override
        protected void procedure() {
            final Sequence<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
            if (watchpoints.isEmpty()) {
                final MaxWatchpoint newWatchpoint = setWatchpoint();
                if (newWatchpoint != null) {
                    inspection.focus().setWatchpoint(newWatchpoint);
                }
            } else {
                if (watchpoints.length() > 1) {
                    if (!inspection.gui().yesNoDialog(Integer.toString(watchpoints.length()) + " watchpoints active here:  DELETE ALL?")) {
                        return;
                    }
                }
                inspection.actions().removeWatchpoints(watchpoints, null).perform();
            }
        }

    }

    /**
     *  Creates a menu entry for removing a possibly empty collection of VM memory watchpoints.
     *
     * @return either a single {@link InspectorActoin} or a {@link JMenu} to be used as a sub-menu.
     */
    public static Object createRemoveActionOrMenu(Inspection inspection, final Sequence<MaxWatchpoint> watchpoints) {
        if (watchpoints.isEmpty()) {
            return InspectorAction.dummyAction(inspection, "Remove memory watchpoint");
        }
        if (watchpoints.length() == 1) {
            final MaxWatchpoint watchpoint = watchpoints.first();
            final String description = watchpoint.description();
            final String title = description == null ? "Remove watchpont" : "Remove watchpoint: " + description;
            return inspection.actions().removeWatchpoint(watchpoint, title);
        }
        final JMenu menu = new JMenu("Remove watchpoint");
        for (MaxWatchpoint watchpoint : watchpoints) {
            final MaxWatchpoint finalWatchpoint = watchpoint;
            menu.add(inspection.actions().removeWatchpoint(finalWatchpoint, watchpoint.description()));
        }
        menu.add(inspection.actions().removeWatchpoints(watchpoints, "Remove all"));
        return menu;
    }

    /**
     * Creates a menu entry for editing a possibly empty collection of VM memory watchpoints.
     */
    public static JMenu createEditMenu(Inspection inspection, final Sequence<MaxWatchpoint> watchpoints) {
        final JMenu menu = new JMenu("Modify memory watchpoint");
        if (watchpoints.isEmpty()) {
            menu.setEnabled(false);
        } else if (watchpoints.length() == 1) {
            final MaxWatchpoint watchpoint = watchpoints.first();
            buildWatchpointMenu(menu, watchpoint);
            final String description = watchpoint.description();
            final String title = description == null ? "Modify watchpont" : "Modify watchpoint: " + description;
            menu.setText(title);
        } else {
            menu.setText("Modify watchpoints");
            for (MaxWatchpoint watchpoint : watchpoints) {
                final JMenu subMenu = new JMenu(watchpoint.description());
                buildWatchpointMenu(subMenu, watchpoint);
                menu.add(subMenu);
            }
        }
        return menu;
    }
    /**
     * Populates a menu for editing settings of a single watchpoint.
     */
    private static void buildWatchpointMenu(JMenu menu, final MaxWatchpoint watchpoint) {
        assert watchpoint != null;
        final JCheckBoxMenuItem readItem = new JCheckBoxMenuItem("Trap on read", watchpoint.isRead());
        readItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getItem();
                watchpoint.setRead(item.getState());
            }
        });
        menu.add(readItem);
        final JCheckBoxMenuItem writeItem = new JCheckBoxMenuItem("Trap on write", watchpoint.isWrite());
        writeItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getItem();
                watchpoint.setWrite(item.getState());
            }
        });
        menu.add(writeItem);
        final JCheckBoxMenuItem execItem = new JCheckBoxMenuItem("Trap on exec", watchpoint.isExec());
        execItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getItem();
                watchpoint.setExec(item.getState());
            }
        });
        menu.add(execItem);
        final JCheckBoxMenuItem enabledDuringGCItem = new JCheckBoxMenuItem("Enabled during GC", watchpoint.isEnabledDuringGC());
        enabledDuringGCItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getItem();
                watchpoint.setEnabledDuringGC(item.getState());
            }
        });
        menu.add(enabledDuringGCItem);
    }

}
