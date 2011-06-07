/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.value.*;

/**
 * A table specialized for displaying aspects of the current {@link InspectorFocus}.
 * Intended for Inspector testing.
 *
 * @author Michael Van De Vanter
 */
public final class FocusTable extends InspectorTable implements ViewFocusListener {

    /**
     * Columns to display when viewing focus data.
     */
    public enum FocusColumnKind implements ColumnKind {

        NAME("Name", "Inspector user focus kind", -1),
        VALUE("Value", "Value/status of Inspector user focus", 25);

        private final String label;
        private final String toolTipText;
        private final int minWidth;

        private FocusColumnKind(String label, String toolTipText, int minWidth) {
            this.label = label;
            this.toolTipText = toolTipText;
            this.minWidth = minWidth;
        }

        public String label() {
            return label;
        }

        public String toolTipText() {
            return toolTipText;
        }

        public boolean canBeMadeInvisible() {
            return false;
        }

        public boolean defaultVisibility() {
            return true;
        }

        public int minWidth() {
            return minWidth;
        }

        @Override
        public String toString() {
            return label;
        }

    }

    /**
     * View preferences for the Focus view.
     * <br>
     * There aren't any choices at this time, so this is a dummy.
     */
    public static final class FocusViewPreferences extends TableColumnVisibilityPreferences<FocusColumnKind> {

        private static FocusViewPreferences globalPreferences;

        /**
         * @return the global, persistent set of user preferences for viewing a table of watchpoints.
         */
        public static FocusViewPreferences globalPreferences(Inspection inspection) {
            if (globalPreferences == null) {
                globalPreferences = new FocusViewPreferences(inspection);
            }
            return globalPreferences;
        }

        // Prefix for all persistent column preferences in view
        private static final String FOCUS_COLUMN_PREFERENCE = "focusViewColumn";

        private FocusViewPreferences(Inspection inspection) {
            super(inspection, FOCUS_COLUMN_PREFERENCE, FocusColumnKind.values());
            // There are no view preferences beyond the column choices, so no additional machinery needed here.
        }
    }

    /**
     * Rows to display when viewing focus data: one for each aspect of the focus.
     */
    public enum FocusRowKind {

        THREAD("Thread", "Current thread of interest in the Inspector"),
        FRAME("Stack Frame", "Current stack frame of interest in the Inspector"),
        CODE("Code Location", "Current code location of interest in the Inspector"),
        BREAKPOINT("Breakpoint", "Current breakpoint of interest in the Inspector"),
        WATCHPOINT("Watchpoint", "Current watchpoint of interest in the Inspector"),
        ADDRESS("Memory Address", "Current memory address of interest in the Inspector"),
        OBJECT("Heap Object", "Current heap object of interest in the Inspector"),
        REGION("Memory Region", "Current memory region of interest in the Inspector");

        private final String label;
        private final String toolTipText;

        private FocusRowKind(String label, String toolTipText) {
            this.label = label;
            this.toolTipText = toolTipText;
        }

        /**
         * @return text to appear in the row's name field.
         */
        public String label() {
            return label;
        }

        /**
         * @return text to appear in the row's name field toolTip.
         */
        public String toolTipText() {
            return toolTipText;
        }

        @Override
        public String toString() {
            return label;
        }

    }

    private final FocusTableModel tableModel;
    private final FocusColumnModel columnModel;

    FocusTable(Inspection inspection, FocusViewPreferences viewPreferences) {
        super(inspection);
        tableModel = new FocusTableModel(inspection);
        columnModel = new FocusColumnModel(viewPreferences);
        configureDefaultTable(tableModel, columnModel);
        setRowSelectionAllowed(false);
    }

    private final class FocusColumnModel extends InspectorTableColumnModel<FocusColumnKind> {

        private FocusColumnModel(FocusViewPreferences viewPreferences) {
            super(FocusColumnKind.values().length, viewPreferences);
            addColumn(FocusColumnKind.NAME, new NameCellRenderer(inspection()), null);
            addColumn(FocusColumnKind.VALUE, new ValueCellRenderer(inspection()), null);
        }
    }

    private final class FocusTableModel extends InspectorTableModel {

        public FocusTableModel(Inspection inspection) {
            super(inspection);
        }

        public int getColumnCount() {
            return FocusColumnKind.values().length;
        }

        public int getRowCount() {
            return FocusRowKind.values().length;
        }

        public Object getValueAt(int row, int col) {
            // Don't use cell values; all interaction is driven by row number.
            return null;
        }

        @Override
        public String getRowDescription(int row) {
            return FocusRowKind.values()[row].label();
        }
    }

    /**
     * Renders the name of an aspect of the user focus.
     */
    private final class NameCellRenderer extends PlainLabel implements TableCellRenderer {

        NameCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final FocusRowKind focusRowKind = FocusRowKind.values()[row];
            setText(focusRowKind.label());
            setToolTipPrefix(tableModel.getRowDescription(row));
            setWrappedToolTipText("<br>" + focusRowKind.toolTipText());
            return this;
        }
    }

    /**
     * Renders the current value of an aspect of the user focus.
     */
    private final class ValueCellRenderer  implements TableCellRenderer, Prober {

        final int rowCount = FocusRowKind.values().length;

        // A "value" label per row, each suitable for the particular kind of value.
        private InspectorLabel[] labels = new InspectorLabel[rowCount];

        public ValueCellRenderer(final Inspection inspection) {
            for (int row = 0; row < rowCount; row++) {
                InspectorLabel label = null;
                switch(FocusRowKind.values()[row]) {
                    case THREAD:
                        label = new JavaNameLabel(inspection, "") {
                            @Override
                            public void refresh(boolean force) {
                                final MaxThread thread = focus().thread();
                                if (thread == null) {
                                    setValue(null);
                                    setWrappedToolTipText(htmlify("<none>"));
                                } else {
                                    final String longName = inspection.nameDisplay().longNameWithState(thread);
                                    setValue(longName);
                                    setWrappedToolTipText(longName);
                                }
                            }
                        };
                        break;
                    case FRAME:
                        label = new PlainLabel(inspection, "") {
                            @Override
                            public void refresh(boolean force) {
                                final MaxStackFrame stackFrame = focus().stackFrame();
                                if (stackFrame == null) {
                                    setValue(null);
                                    setWrappedToolTipText(htmlify("<none>"));
                                } else {
                                    final MaxCompilation compiledCode = stackFrame.compiledCode();
                                    final String name = compiledCode == null ? "nativeMethod: " + stackFrame.codeLocation().address().to0xHexString() : compiledCode.entityName();
                                    setValue(name);
                                    setWrappedToolTipText(htmlify(name));
                                }
                            }
                        };
                        break;
                    case CODE:
                        label = new PlainLabel(inspection, "") {
                            @Override
                            public void refresh(boolean force) {
                                final MaxCodeLocation codeLocation = focus().codeLocation();
                                if (codeLocation == null) {
                                    setValue(null);
                                    setWrappedToolTipText(htmlify("<none>"));
                                } else {
                                    final String longName = inspection().nameDisplay().longName(codeLocation);
                                    setValue(longName);
                                    setWrappedToolTipText(htmlify(longName));
                                }
                            }
                        };
                        break;
                    case BREAKPOINT:
                        label = new PlainLabel(inspection, "") {
                            @Override
                            public void refresh(boolean force) {
                                final MaxBreakpoint breakpoint = focus().breakpoint();
                                if (breakpoint == null) {
                                    setValue(null);
                                    setWrappedToolTipText(htmlify("<none>"));
                                } else {
                                    final String longName = inspection().nameDisplay().longName(breakpoint.codeLocation());
                                    setValue(longName);
                                    setWrappedToolTipText(htmlify(longName));
                                }
                            }
                        };
                        break;
                    case WATCHPOINT:
                        label = new PlainLabel(inspection, "") {
                            @Override
                            public void refresh(boolean force) {
                                final MaxWatchpoint watchpoint = focus().watchpoint();
                                if (watchpoint == null) {
                                    setValue(null);
                                    setWrappedToolTipText(htmlify("<none>"));
                                } else {
                                    final String longName = watchpoint.toString();
                                    setValue(longName);
                                    setWrappedToolTipText(htmlify(longName));
                                }
                            }
                        };
                        break;
                    case ADDRESS:
                        label = new WordValueLabel(inspection, WordValueLabel.ValueMode.WORD, FocusTable.this) {
                            @Override
                            public Value fetchValue() {
                                Address address = focus().address();
                                if (address == null) {
                                    address = Address.zero();
                                }
                                return new WordValue(address);
                            }
                        };
                        break;
                    case OBJECT:
                        label = new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE, FocusTable.this) {
                            @Override
                            public Value fetchValue() {
                                final TeleObject teleObject = focus().heapObject();
                                Address address = Address.zero();
                                if (teleObject != null) {
                                    address = teleObject.origin();
                                }
                                return new WordValue(address);
                            }
                        };
                        break;
                    case REGION:
                        label = new PlainLabel(inspection, "") {
                            @Override
                            public void refresh(boolean force) {
                                final MaxMemoryRegion memoryRegion = focus().memoryRegion();
                                if (memoryRegion == null) {
                                    setValue(null, "No memory region focus");
                                } else {
                                    setValue(memoryRegion.regionName(), "Memory region focus = " + memoryRegion.regionName());
                                }
                            }
                        };
                        break;
                }
                label.setToolTipPrefix(tableModel.getRowDescription(row) + "<br>Focus = ");
                labels[row] = label;
            }
        }

        public void refresh(boolean force) {
            for (InspectorLabel label : labels) {
                if (label != null) {
                    label.refresh(force);
                }
            }
        }

        public void redisplay() {
            for (InspectorLabel label : labels) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (labels[row] != null) {
                return labels[row];
            }
            return new PlainLabel(inspection(), "unimplemented");
        }
    }

    public void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative) {
        refresh(true);
    }

    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        refresh(true);
    }

    public void frameFocusChanged(MaxStackFrame oldStackFrame, MaxStackFrame stackFrame) {
        refresh(true);
    }

    public void addressFocusChanged(Address oldAddress, Address address) {
        refresh(true);
    }

    public void memoryRegionFocusChanged(MaxMemoryRegion oldMemoryRegion, MaxMemoryRegion memoryRegion) {
        refresh(true);
    }

    public void breakpointFocusSet(MaxBreakpoint oldBreakpoint, MaxBreakpoint breakpoint) {
        refresh(true);
    }

    public  void watchpointFocusSet(MaxWatchpoint oldWatchpoint, MaxWatchpoint watchpoint) {
        refresh(true);
    }

    public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
        refresh(true);
    }

}
