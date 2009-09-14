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
package com.sun.max.ins;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;
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

        public static final IndexedSequence<FocusColumnKind> VALUES = new ArraySequence<FocusColumnKind>(values());
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
            super(inspection, FOCUS_COLUMN_PREFERENCE, FocusColumnKind.VALUES);
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

        public static final IndexedSequence<FocusRowKind> VALUES = new ArraySequence<FocusRowKind>(values());
    }

    private final FocusTableModel tableModel;
    private final FocusColumnModel columnModel;

    FocusTable(Inspection inspection, FocusViewPreferences viewPreferences) {
        super(inspection);
        tableModel = new FocusTableModel();
        columnModel = new FocusColumnModel(viewPreferences);
        configureDefaultTable(tableModel, columnModel);
        setRowSelectionAllowed(false);
    }

    private final class FocusColumnModel extends InspectorTableColumnModel<FocusColumnKind> {

        private FocusColumnModel(FocusViewPreferences viewPreferences) {
            super(FocusColumnKind.VALUES.length(), viewPreferences);
            addColumn(FocusColumnKind.NAME, new NameCellRenderer(inspection()), null);
            addColumn(FocusColumnKind.VALUE, new ValueCellRenderer(inspection()), null);
        }
    }

    private final class FocusTableModel extends InspectorTableModel {

        public int getColumnCount() {
            return FocusColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return FocusRowKind.VALUES.length();
        }

        public Object getValueAt(int row, int col) {
            // Don't use cell values; all interaction is driven by row number.
            return null;
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
            final FocusRowKind focusRowKind = FocusRowKind.VALUES.get(row);
            setText(focusRowKind.label());
            setToolTipText(focusRowKind.toolTipText());
            return this;
        }
    }


    /**
     * Renders the current value of an aspect of the user focus.
     */
    private final class ValueCellRenderer  implements TableCellRenderer, Prober {

        // A "value" label per row, each suitable for the particular kind of value.
        private InspectorLabel[] labels = new InspectorLabel[FocusRowKind.VALUES.length()];

        public ValueCellRenderer(Inspection inspection) {
            labels[FocusRowKind.THREAD.ordinal()] = new JavaNameLabel(inspection, "") {
                @Override
                public void refresh(boolean force) {
                    final MaxThread thread = inspection().focus().thread();
                    if (thread == null) {
                        setValue(null, "No thread focus");
                    } else {
                        final String longName = inspection().nameDisplay().longNameWithState(thread);
                        setValue(longName, "Thread focus = " + longName);
                    }
                }
            };
            labels[FocusRowKind.FRAME.ordinal()] = new PlainLabel(inspection, "") {
                @Override
                public void refresh(boolean force) {
                    final StackFrame stackFrame = inspection().focus().stackFrame();
                    if (stackFrame == null) {
                        setValue(null, "No stack frame focus");
                    } else {
                        final TargetMethod targetMethod = stackFrame.targetMethod();
                        final String name = targetMethod == null ? "nativeMethod: 0x" + maxVM().getCodeAddress(stackFrame).toHexString() : targetMethod.toString();
                        setValue(name, "Stack frame focus = " + name);
                    }
                }
            };
            labels[FocusRowKind.CODE.ordinal()] = new PlainLabel(inspection, "") {
                @Override
                public void refresh(boolean force) {
                    final TeleCodeLocation teleCodeLocation = inspection().focus().codeLocation();
                    if (teleCodeLocation == null) {
                        setValue(null, "No code location focus");
                    } else {
                        final String longName = inspection().nameDisplay().longName(teleCodeLocation);
                        setValue(longName, "Code location focus = " + longName);
                    }
                }
            };
            labels[FocusRowKind.BREAKPOINT.ordinal()] = new PlainLabel(inspection, "") {
                @Override
                public void refresh(boolean force) {
                    final TeleBreakpoint teleBreakpoint = inspection().focus().breakpoint();
                    if (teleBreakpoint == null) {
                        setValue(null, "No breakpoint focus");
                    } else {
                        final String longName = inspection().nameDisplay().longName(teleBreakpoint.teleCodeLocation());
                        setValue(longName, "Breakpoint focus = " + longName);
                    }
                }
            };
            labels[FocusRowKind.WATCHPOINT.ordinal()] = new PlainLabel(inspection, "") {
                @Override
                public void refresh(boolean force) {
                    final MaxWatchpoint watchpoint = inspection().focus().watchpoint();
                    if (watchpoint == null) {
                        setValue(null, "No watchpoint focus");
                    } else {
                        final String longName = watchpoint.toString();
                        setValue(longName, "Watchpoint focus = " + longName);
                    }
                }
            };
            labels[FocusRowKind.ADDRESS.ordinal()] = new WordValueLabel(inspection, WordValueLabel.ValueMode.WORD, FocusTable.this) {
                @Override
                public Value fetchValue() {
                    Address address = inspection().focus().address();
                    if (address == null) {
                        address = Address.zero();
                    }
                    return new WordValue(address);
                }
            };
            labels[FocusRowKind.OBJECT.ordinal()] = new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE, FocusTable.this) {
                @Override
                public Value fetchValue() {
                    final TeleObject teleObject = inspection().focus().heapObject();
                    Address address = Address.zero();
                    if (teleObject != null) {
                        address = teleObject.getCurrentOrigin();
                    }
                    return new WordValue(address);
                }
            };
            labels[FocusRowKind.REGION.ordinal()] = new PlainLabel(inspection, "") {
                @Override
                public void refresh(boolean force) {
                    final MemoryRegion memoryRegion = inspection().focus().memoryRegion();
                    if (memoryRegion == null) {
                        setValue(null, "No memory region focus");
                    } else {
                        setValue(memoryRegion.description(), "Memory region focus = " + memoryRegion.description());
                    }
                }
            };
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

    public void codeLocationFocusSet(TeleCodeLocation teleCodeLocation, boolean interactiveForNative) {
        refresh(true);
    }

    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        refresh(true);
    }

    public void stackFrameFocusChanged(StackFrame oldStackFrame, MaxThread threadForStackFrame, StackFrame stackFrame) {
        refresh(true);
    }

    public void addressFocusChanged(Address oldAddress, Address address) {
        refresh(true);
    }

    public void memoryRegionFocusChanged(MemoryRegion oldMemoryRegion, MemoryRegion memoryRegion) {
        refresh(true);
    }

    public void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint) {
        refresh(true);
    }

    public  void watchpointFocusSet(MaxWatchpoint oldWatchpoint, MaxWatchpoint watchpoint) {
        refresh(true);
    }

    public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
        refresh(true);
    }

}
