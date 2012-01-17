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
package com.sun.max.ins.gui;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxWatchpoint.WatchpointSettings;
import com.sun.max.tele.MaxWatchpointManager.MaxDuplicateWatchpointException;
import com.sun.max.tele.MaxWatchpointManager.MaxTooManyWatchpointsException;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Utilities for managing {@linkplain MaxWatchpoint memory watchpoint}s in the VM.
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
            final List<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
            if (watchpoints.isEmpty()) {
                final MaxWatchpoint newWatchpoint = setWatchpoint();
                if (newWatchpoint != null) {
                    inspection.focus().setWatchpoint(newWatchpoint);
                }
            } else {
                if (watchpoints.size() > 1) {
                    if (!inspection.gui().yesNoDialog(Integer.toString(watchpoints.size()) + " watchpoints active here:  DELETE ALL?")) {
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
    public static Object createRemoveActionOrMenu(Inspection inspection, final List<MaxWatchpoint> watchpoints) {
        if (watchpoints.isEmpty()) {
            return InspectorAction.dummyAction(inspection, "Remove memory watchpoint");
        }
        if (watchpoints.size() == 1) {
            final MaxWatchpoint watchpoint = watchpoints.get(0);
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
    public static JMenu createEditMenu(Inspection inspection, final List<MaxWatchpoint> watchpoints) {
        final JMenu menu = new JMenu("Modify memory watchpoint");
        if (watchpoints.isEmpty()) {
            menu.setEnabled(false);
        } else if (watchpoints.size() == 1) {
            final MaxWatchpoint watchpoint = watchpoints.get(0);
            buildWatchpointMenu(inspection, menu, watchpoint);
            final String description = watchpoint.description();
            final String title = description == null ? "Modify watchpoint" : "Modify watchpoint: " + description;
            menu.setText(title);
        } else {
            menu.setText("Modify watchpoints");
            for (MaxWatchpoint watchpoint : watchpoints) {
                final JMenu subMenu = new JMenu(watchpoint.description());
                buildWatchpointMenu(inspection, subMenu, watchpoint);
                menu.add(subMenu);
            }
        }
        return menu;
    }


    /**
     * Populates a menu for editing settings of a single watchpoint.
     */
    private static void buildWatchpointMenu(final Inspection inspection, JMenu menu, final MaxWatchpoint watchpoint) {
        assert watchpoint != null;
        final WatchpointSettings settings = watchpoint.getSettings();
        final JCheckBoxMenuItem readItem = new JCheckBoxMenuItem("Trap on read", settings.trapOnRead);

        readItem.addItemListener(new ItemListener() {

            private boolean undoing = false;

            public void itemStateChanged(ItemEvent itemEvent) {
                final JCheckBoxMenuItem item = (JCheckBoxMenuItem) itemEvent.getItem();
                if (undoing) {
                    // This is recursive notification that the state has changed when we reverse a user action; ignore it.
                } else {
                    // This is a real user-initiated change to the item.
                    final boolean newState = item.getState();
                    try {
                        watchpoint.setTrapOnRead(newState);
                    } catch (MaxVMBusyException maxVMBusyException) {
                        // Can't carry out the change; revert the state of the item.
                        undoing = true;
                        // Record the fact that we're deliberately reversing the user's action so that we can
                        // ignore the recursive notification of this change.
                        item.setSelected(!newState);
                        undoing = false;
                        inspection.announceVMBusyFailure("Watchpoint READ setting");
                    }
                }
            }
        });
        menu.add(readItem);
        final JCheckBoxMenuItem writeItem = new JCheckBoxMenuItem("Trap on write", settings.trapOnWrite);
        writeItem.addItemListener(new ItemListener() {

            private boolean undoing = false;

            public void itemStateChanged(ItemEvent itemEvent) {
                final JCheckBoxMenuItem item = (JCheckBoxMenuItem) itemEvent.getItem();
                if (undoing) {
                    // This is recursive notification that the state has changed when we reverse a user action; ignore it.
                } else {
                    // This is a real user-initiated change to the item.
                    final boolean newState = item.getState();
                    try {
                        watchpoint.setTrapOnWrite(newState);
                    } catch (MaxVMBusyException maxVMBusyException) {
                        // Can't carry out the change; revert the state of the item.
                        undoing = true;
                        // Record the fact that we're deliberately reversing the user's action so that we can
                        // ignore the recursive notification of this change.
                        item.setSelected(!newState);
                        undoing = false;
                        inspection.announceVMBusyFailure("Watchpoint WRITE setting");
                    }
                }
            }
        });
        menu.add(writeItem);
        final JCheckBoxMenuItem execItem = new JCheckBoxMenuItem("Trap on exec", settings.trapOnExec);
        execItem.addItemListener(new ItemListener() {

            private boolean undoing = false;

            public void itemStateChanged(ItemEvent itemEvent) {
                final JCheckBoxMenuItem item = (JCheckBoxMenuItem) itemEvent.getItem();
                if (undoing) {
                    // This is recursive notification that the state has changed when we reverse a user action; ignore it.
                } else {
                    // This is a real user-initiated change to the item.
                    final boolean newState = item.getState();
                    try {
                        watchpoint.setTrapOnExec(newState);
                    } catch (MaxVMBusyException maxVMBusyException) {
                        // Can't carry out the change; revert the state of the item.
                        undoing = true;
                        // Record the fact that we're deliberately reversing the user's action so that we can
                        // ignore the recursive notification of this change.
                        item.setSelected(!newState);
                        undoing = false;
                        inspection.announceVMBusyFailure("Watchpoint EXEC setting");
                    }
                }
            }
        });
        menu.add(execItem);
        final JCheckBoxMenuItem enabledDuringGCItem = new JCheckBoxMenuItem("Enabled during GC", settings.enabledDuringGC);
        enabledDuringGCItem.addItemListener(new ItemListener() {

            private boolean undoing = false;

            public void itemStateChanged(ItemEvent itemEvent) {
                final JCheckBoxMenuItem item = (JCheckBoxMenuItem) itemEvent.getItem();
                if (undoing) {
                    // This is recursive notification that the state has changed when we reverse a user action; ignore it.
                } else {
                    // This is a real user-initiated change to the item.
                    final boolean newState = item.getState();
                    try {
                        watchpoint.setEnabledDuringGC(newState);
                    } catch (MaxVMBusyException maxVMBusyException) {
                        // Can't carry out the change; revert the state of the item.
                        undoing = true;
                        // Record the fact that we're deliberately reversing the user's action so that we can
                        // ignore the recursive notification of this change.
                        item.setSelected(!newState);
                        undoing = false;
                        inspection.announceVMBusyFailure("Watchpoint GC setting");
                    }
                }
            }
        });
        menu.add(enabledDuringGCItem);
    }

    /*
     * Thread Local Watchtpoint support.
     * Ideally, we want a logical watchpoint on a given thread local variable name, such that a
     * watchpoint is set on the corresponding thread local variable of every  thread.
     * This includes setting the watchpoint for any new thread entering the system. Thus, when selecting the check box, breakpoint must
     * be set on thread start to automatically add a watchpoint as soon as the variable is initialized and remove it when the thread exits.
     * This isn't done at the moment. We only set the watchpoint on thread in the current VM state.
     */

    static final class ThreadLocalWatchpointItemListener implements ItemListener {
        static final WatchpointSettings watchpointSettings = new WatchpointSettings(false, true, false, true);

        static class ThreadEventListener implements MaxVMThreadEntryListener,  MaxVMThreadDetachedListener {
            /**
             * Set used to track thread local variables being watched. This is used to automatically set/remove watchpoint on thread entry/exit.
             */
            final HashSet<ThreadLocalWatchpointItemListener> watchSet = new HashSet<ThreadLocalWatchpointItemListener>();
            private String  tracePrefix() {
                return "[ThreadEventListener: " + Thread.currentThread().getName() + "] ";
            }
            public  void entered(MaxThread thread) {
                handleTriggerEvent(thread, true,  "thread Start Event");
            }
            public void detached(MaxThread thread) {
                handleTriggerEvent(thread, false,  "thread Detach Event");

            }
            private void handleTriggerEvent(MaxThread thread, boolean setWatchpoint, String eventName) {
                if (watchSet.isEmpty()) {
                    System.out.println("WARNING: " + tracePrefix() + eventName + " triggered with empty watchSet");
                }
                for (ThreadLocalWatchpointItemListener listener : watchSet) {
                    if (listener.watched) {
                        final MaxThreadLocalVariable threadLocalVariable = listener.threadLocalVariable(thread);
                        if (setWatchpoint) {
                            listener.setWatchpoint(threadLocalVariable);
                        } else {
                            listener.removeWatchpoint(threadLocalVariable);
                        }
                    } else {
                        System.out.println("WARNING: " + tracePrefix() +  eventName +
                                        " found unwatched thread local \"" + listener.watchedVariableName() + "\" in watchSet");
                    }
                }
            }

            void add(Inspection inspection, ThreadLocalWatchpointItemListener watched) {
                if (watchSet.isEmpty()) {
                    try {
                        inspection.vm().addThreadEnterListener(this);
                        inspection.vm().addThreadDetachedListener(this);
                    } catch (MaxVMBusyException e) {
                        // FIXME: revisit what to do here.
                        e.printStackTrace();
                    }
                }
                watchSet.add(watched);
            }

            void remove(Inspection inspection, ThreadLocalWatchpointItemListener watched) {
                assert !watchSet.isEmpty() : "watch set must not be empty";
                watchSet.remove(watched);
                if (watchSet.isEmpty()) {
                    try {
                        inspection.vm().removeThreadEnterListener(this);
                        inspection.vm().removeThreadDetachedListener(this);
                    } catch (MaxVMBusyException e) {
                        // FIXME: revisit what to do here.
                        e.printStackTrace();
                    }
                }
            }
        }

        static ThreadEventListener threadEventListener = new ThreadEventListener();

        final Inspection inspection;
        final int threadLocalIndex;
        final String description;

        boolean watched;
        // TODO clean this up. inspection == null when watchedVariableName is called from buildThreadLocalWatchpointMenu
        private String watchedVariableName() {
            assert inspection != null;
            return watchedVariableName(inspection);
        }
        private String watchedVariableName(Inspection inspection) {
            return TeleThreadLocalsArea.Static.values(inspection.vm()).get(threadLocalIndex).name;
        }
        private MaxThreadLocalVariable threadLocalVariable(MaxThread thread) {
            return  thread.localsBlock().tlaFor(SafepointPoll.State.ENABLED).getThreadLocalVariable(threadLocalIndex);
        }

        ThreadLocalWatchpointItemListener(Inspection inspection, VmThreadLocal threadLocal) {
            threadLocalIndex = threadLocal.index;
            description = "Write access to " + watchedVariableName(inspection);
            this.inspection = inspection;
            watched = false;
        }

        private void setWatchpoint(MaxThreadLocalVariable threadLocalVariable) {
            try {
                inspection.vm().watchpointManager().createVmThreadLocalWatchpoint(description, threadLocalVariable, watchpointSettings);
            } catch (MaxTooManyWatchpointsException e) {
                e.printStackTrace();
            } catch (MaxDuplicateWatchpointException e) {
                // ignore
            } catch (MaxVMBusyException e) {
                inspection.announceVMBusyFailure("Watchpoint for thread local \"" +  threadLocalVariable.variableName() + "\"");
                e.printStackTrace();
            }
        }
        private void removeWatchpoint(MaxThreadLocalVariable threadLocalVariable) {
            List<MaxWatchpoint> watchpoints = inspection.vm().watchpointManager().findWatchpoints(threadLocalVariable.memoryRegion());
            try {
                for (MaxWatchpoint watchpoint : watchpoints) {
                    if (watchpoint.description().equals(description)) {
                        watchpoint.remove();
                    }
                }
            } catch (MaxVMBusyException e) {
                inspection.announceVMBusyFailure("Watchpoint for thread local \"" +  threadLocalVariable.variableName() + "\"");
                e.printStackTrace();
            }
        }

        public void itemStateChanged(ItemEvent itemEvent) {
            final JCheckBoxMenuItem item = (JCheckBoxMenuItem) itemEvent.getItem();
            boolean state = item.getState();

            if (state != watched) {
                if (state) {
                    for (MaxThread thread : inspection.vm().state().threads()) {
                        setWatchpoint(threadLocalVariable(thread));
                    }
                    threadEventListener.add(inspection, this);
                } else {
                    for (MaxThread thread : inspection.vm().state().threads()) {
                        removeWatchpoint(threadLocalVariable(thread));
                    }
                    threadEventListener.remove(inspection, this);
                }
                watched = state;
            }
        }
    }

    /**
     * Populate a menu for setting / unsetting thread local watchpoints.
     *
     */
    public static void buildThreadLocalWatchpointMenu(final Inspection inspection, JMenu menu) {
        for (VmThreadLocal threadLocal : TeleThreadLocalsArea.Static.values(inspection.vm())) {
            JCheckBoxMenuItem cbox = new JCheckBoxMenuItem(threadLocal.name, false);
            cbox.addItemListener(new ThreadLocalWatchpointItemListener(inspection, threadLocal));
            menu.add(cbox);
        }
    }
}
