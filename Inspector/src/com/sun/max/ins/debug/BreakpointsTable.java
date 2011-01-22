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
package com.sun.max.ins.debug;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

/**
 * A table specialized for displaying code breakpoints in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class BreakpointsTable extends InspectorTable {

    private final BreakpointsTableModel tableModel;
    private BreakpointsColumnModel columnModel;

    public BreakpointsTable(Inspection inspection, BreakpointsViewPreferences viewPreferences) {
        super(inspection);
        tableModel = new BreakpointsTableModel(inspection);
        columnModel = new BreakpointsColumnModel(viewPreferences);
        configureDefaultTable(tableModel, columnModel);
    }

    @Override
    protected InspectorPopupMenu getPopupMenu(int row, int col, MouseEvent mouseEvent) {
        final BreakpointData breakpointData = tableModel.get(row);
        final InspectorPopupMenu menu = new InspectorPopupMenu("Breakpoints");
        final String shortName = breakpointData.shortName();
        menu.add(actions().removeBreakpoint(breakpointData.breakpoint(), "Remove: " + shortName));
        if (breakpointData.isEnabled()) {
            menu.add(actions().disableBreakpoint(breakpointData.breakpoint(), "Disable: " + shortName));
        } else {
            menu.add(actions().enableBreakpoint(breakpointData.breakpoint(), "Enable: " + shortName));
        }
        menu.addSeparator();
        final JMenu methodEntryBreakpoints = new JMenu("Break at Method Entry");
        methodEntryBreakpoints.add(actions().setMachineCodeBreakpointAtEntriesByName());
        methodEntryBreakpoints.add(actions().setBytecodeBreakpointAtMethodEntryByName());
        methodEntryBreakpoints.add(actions().setBytecodeBreakpointAtMethodEntryByKey());
        menu.add(methodEntryBreakpoints);
        menu.add(actions().setMachineCodeBreakpointAtObjectInitializer());
        menu.add(actions().removeAllBreakpoints());
        return menu;
    }

    @Override
    public void updateFocusSelection() {
        // Sets table selection to breakpoint, if any, that is the current user focus.
        final MaxBreakpoint breakpoint = focus().breakpoint();
        final int row = tableModel.findRow(breakpoint);
        updateSelection(row);
    }

    @Override
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        // Suppress row selection when clicking on the "Enabled" checkbox;
        final int modelColumnIndex = convertColumnIndexToModel(columnIndex);
        if (modelColumnIndex != BreakpointsColumnKind.ENABLED.ordinal()) {
            super.changeSelection(rowIndex, columnIndex, toggle, extend);
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        // TODO: Add MaxBreakpoint interface and generalize this code (cf. WatchpointsTable)
        // Row selection changed, perhaps by user mouse click or navigation;
        // update user focus to follow the selection.
        super.valueChanged(listSelectionEvent);
        if (!listSelectionEvent.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0) {
                final BreakpointData breakpointData = tableModel.get(row);
                if (breakpointData != null) {
                    final MaxBreakpoint teleBreakpoint = breakpointData.breakpoint();
                    focus().setBreakpoint(teleBreakpoint);
                }
            }
        }
    }

    /**
     * {@inheritDoc}.
     * <br>
     * color the text specially in the row where a triggered breakpoint is displayed
     */
    @Override
    public Color cellForegroundColor(int row, int col) {
        return (tableModel.get(row).triggerThread() == null) ? null : style().debugIPTagColor();
    }

    private final class BreakpointsColumnModel extends InspectorTableColumnModel<BreakpointsColumnKind>  {

        private BreakpointsColumnModel(BreakpointsViewPreferences viewPreferences) {
            super(BreakpointsColumnKind.values().length, viewPreferences);
            addColumn(BreakpointsColumnKind.TAG, new TagCellRenderer(inspection()), null);
            addColumn(BreakpointsColumnKind.ENABLED, null, new DefaultCellEditor(new JCheckBox()));
            addColumn(BreakpointsColumnKind.DESCRIPTION, new DescriptionCellRenderer(inspection()), null);
            addColumn(BreakpointsColumnKind.LOCATION, new LocationCellRenderer(inspection()), null);
            addColumn(BreakpointsColumnKind.CONDITION, new ConditionCellRenderer(), new DefaultCellEditor(new JTextField()));
            addColumn(BreakpointsColumnKind.TRIGGER_THREAD, new TriggerThreadCellRenderer(inspection()), null);
        }
    }

    /**
     * A table data model built around the list of current breakpoints in the VM.
     *
     * @author Michael Van De Vanter
     */
    private final class BreakpointsTableModel extends InspectorTableModel {

        public BreakpointsTableModel(Inspection inspection) {
            super(inspection);
        }

        // Cache of information objects for each known breakpoint
        private final Set<BreakpointData> breakpoints = new TreeSet<BreakpointData>();

        public int getColumnCount() {
            return BreakpointsColumnKind.values().length;
        }

        public int getRowCount() {
            // This gets called during superclass initialization, before the local
            // data has been initialized, even  if you try to set row size to 0
            // in the constructor.
            return breakpoints == null ? 0 : breakpoints.size();
        }

        public Object getValueAt(int row, int col) {
            final BreakpointData breakpointData = get(row);
            switch (BreakpointsColumnKind.values()[col]) {
                case TAG:
                    return breakpointData.kindTag();
                case ENABLED:
                    return breakpointData.isEnabled();
                case DESCRIPTION:
                    return breakpointData.shortName();
                case LOCATION:
                    return breakpointData.location();
                case CONDITION:
                    return breakpointData.condition();
                case TRIGGER_THREAD:
                    return  breakpointData.triggerThreadName();
                default:
                    throw InspectorError.unexpected("Unexpected Breakpoint Data column");
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            final BreakpointData breakpointData = get(row);

            switch (BreakpointsColumnKind.values()[column]) {
                case ENABLED:
                    final Boolean newState = (Boolean) value;
                    try {
                        breakpointData.setEnabled(newState);
                        inspection().settings().save();
                    } catch (MaxVMBusyException maxVMBusyException) {
                        final DefaultCellEditor editor = (DefaultCellEditor) columnModel.columnAt(column).getCellEditor();
                        final JCheckBox checkBox = (JCheckBox) editor.getComponent();
                        // System.out.println("Reset enabled checkbox at row=" + row + ", col=" + column);
                        checkBox.setSelected(!newState);
                        inspection().announceVMBusyFailure("Breakpoint ENABLED setting");
                    }
                    break;

                case CONDITION:
                    final String conditionText = (String) value;
                    try {
                        breakpointData.setCondition(conditionText);
                        inspection().settings().save();
                    } catch (MaxVMBusyException maxVMBusyException) {
                        inspection().announceVMBusyFailure("Breakpoint condition setting");
                    }

                    break;

                default:
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            switch (BreakpointsColumnKind.values()[col]) {
                case ENABLED:
                    return true;
                case CONDITION:
                    // TODO (mlvdv) machinery is now in place for bytecode breakpoints to be conditional; untested.
                    return get(row) instanceof TargetBreakpointData;
                default:
                    break;
            }
            return false;
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            switch (BreakpointsColumnKind.values()[c]) {
                case TAG:
                    return String.class;
                case ENABLED:
                    return Boolean.class;
                case DESCRIPTION:
                    return String.class;
                case LOCATION:
                    return Number.class;
                case CONDITION:
                    return String.class;
                case TRIGGER_THREAD:
                    return String.class;
                default:
                    throw InspectorError.unexpected("Unexected Breakpoint Data column");
            }
        }

        @Override
        public void refresh() {
            // Check for current and added breakpoints
            // Initially assume all deleted
            for (BreakpointData breakpointData : breakpoints) {
                breakpointData.markDeleted(true);
            }
            for (MaxBreakpoint breakpoint : vm().breakpointManager().breakpoints()) {
                if (breakpoint.isBytecodeBreakpoint()) {
                    // Bytecodes breakpoint
                    final BreakpointData breakpointData = findBytecodeBreakpoint(breakpoint.codeLocation());
                    if (breakpointData == null) {
                        // new breakpoint since last refresh
                        breakpoints.add(new BytecodeBreakpointData(breakpoint));
                        //fireTableDataChanged();
                    } else {
                        // mark as not deleted
                        breakpointData.markDeleted(false);
                    }
                } else {
                    // Machine code breakpoint
                    final BreakpointData breakpointData = findTargetBreakpoint(breakpoint.codeLocation().address());
                    if (breakpointData == null) {
                        // new breakpoint in VM since last refresh
                        breakpoints.add(new TargetBreakpointData(breakpoint));
                        //fireTableDataChanged();
                    } else {
                        // mark as not deleted
                        breakpointData.markDeleted(false);
                    }
                }
            }
            // now remove the breakpoints that are still marked as deleted
            final Iterator iter = breakpoints.iterator();
            while (iter.hasNext()) {
                final BreakpointData breakpointData = (BreakpointData) iter.next();
                if (breakpointData.isDeleted()) {
                    iter.remove();
                }
            }
            super.refresh();
        }

        @Override
        public String getRowDescription(int row) {
            return get(row).kindName() + " in " + get(row).shortName();
        }

        BreakpointData get(int row) {
            int count = 0;
            for (BreakpointData breakpointData : breakpoints) {
                if (count == row) {
                    return breakpointData;
                }
                count++;
            }
            throw InspectorError.unexpected("BreakpointsInspector.get(" + row + ") failed");
        }

        /**
         * Return the table row in which a breakpoint is displayed.
         */
        private int findRow(MaxBreakpoint breakpoint) {
            int row = 0;
            for (BreakpointData breakpointData : breakpoints) {
                if (breakpointData.breakpoint() == breakpoint) {
                    return row;
                }
                row++;
            }
            return -1;
        }

        /**
         * Locates a target code breakpoint already known to the inspector.
         */
        TargetBreakpointData findTargetBreakpoint(Address address) {
            for (BreakpointData breakpointData : breakpoints) {
                if (breakpointData instanceof TargetBreakpointData) {
                    final TargetBreakpointData targetBreakpointData = (TargetBreakpointData) breakpointData;
                    if (targetBreakpointData.address().toLong() == address.toLong()) {
                        return targetBreakpointData;
                    }
                }
            }
            return null;
        }

        /**
         * Locates a bytecode breakpoint already known to the inspector.
         */
        BytecodeBreakpointData findBytecodeBreakpoint(MaxCodeLocation codeLocation) {
            for (BreakpointData breakpointData : breakpoints) {
                if (breakpointData instanceof BytecodeBreakpointData) {
                    if (breakpointData.codeLocation().isSameAs(codeLocation)) {
                        return (BytecodeBreakpointData) breakpointData;
                    }
                }
            }
            return null;
        }

    }

    private final class TagCellRenderer extends PlainLabel implements TableCellRenderer {

        TagCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final BreakpointData breakpointData = tableModel.get(row);
            setIcon((breakpointData.triggerThread() == null) ? null : style().debugIPTagIcon());
            setText(breakpointData.kindTag());
            setToolTipPrefix(htmlify(tableModel.getRowDescription(row)));
            setWrappedToolTipText("<br>" + "Enabled=" + (breakpointData.isEnabled() ? "true" : "false"));
            setForeground(cellForegroundColor(row, column));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class DescriptionCellRenderer extends JavaNameLabel implements TableCellRenderer {

        public DescriptionCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final BreakpointData breakpointData = tableModel.get(row);
            setToolTipPrefix(htmlify(tableModel.getRowDescription(row)));
            setValue(breakpointData.shortName(), "<br>" + htmlify(breakpointData.longName()));
            setToolTipSuffix("<br>" + htmlify(breakpointData.locationDescription()));
            setForeground(cellForegroundColor(row, column));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class LocationCellRenderer extends PlainLabel implements TableCellRenderer {

        public LocationCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final BreakpointData breakpointData = tableModel.get(row);
            setToolTipPrefix(htmlify(tableModel.getRowDescription(row)));
            setText(Integer.toString(breakpointData.location()));
            setWrappedToolTipText("<br>" + htmlify(breakpointData.locationDescription()));
            setForeground(cellForegroundColor(row, column));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class ConditionCellRenderer extends DefaultTableCellRenderer implements Prober {

        ConditionCellRenderer() {
            setFont(style().defaultFont());
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final JComponent component = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (tableModel.isCellEditable(row, column)) {
                component.setToolTipText("<html>" + InspectorLabel.htmlify(tableModel.getRowDescription(row)) + "<br>Breakpoint condition (editable)");
            } else {
                component.setToolTipText("<html>" + InspectorLabel.htmlify(tableModel.getRowDescription(row)) + "<br>Breakpoint conditions not supported");
            }
            component.setForeground(cellForegroundColor(row, column));
            component.setBackground(cellBackgroundColor(isSelected));
            return component;
        }

        public void redisplay() {
            setFont(style().defaultFont());
        }

        public void refresh(boolean force) {
        }
    }

    private final class TriggerThreadCellRenderer extends PlainLabel implements TableCellRenderer {

        TriggerThreadCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final BreakpointData breakpointData = tableModel.get(row);
            setToolTipPrefix(htmlify(tableModel.getRowDescription(row)));
            if (breakpointData.triggerThread() != null) {
                setText(breakpointData.triggerThreadName());
                setWrappedToolTipText("<br>Thread \"" + breakpointData.triggerThreadName() + "\" stopped at this breakpoint");
            } else {
                setText("");
                setWrappedToolTipText("<br>No Thread stopped at this breakpoint");
            }
            setForeground(cellForegroundColor(row, column));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    /**
     * Summary of information about a breakpoint that is useful for inspection.
     *
     * @author Michael Van De Vanter
     */
    private abstract class BreakpointData implements Comparable {

        // TODO (mlvdv) The need for subclasses here has diminished, and the differences between the two kinds of breakpoints
        // could be reflected in renderer code.  However, one value of this approach is that some values are cached and can
        // still display correctly after the process has terminated.

        private final MaxBreakpoint breakpoint;

        private boolean deleted = false;

        BreakpointData(MaxBreakpoint breakpoint) {
            this.breakpoint = breakpoint;
        }

        /**
         * @return the breakpoint in the VM being described
         */
        MaxBreakpoint breakpoint() {
            return breakpoint;
        }

        /**
         * @return the location of the breakpoint in the VM in a standard format.
         */
        final MaxCodeLocation codeLocation() {
            return breakpoint.codeLocation();
        }

        /**
         * @return textual expression of the condition associated with this breakpoint, if any.
         */
        final String condition() {
            return breakpoint.getCondition() == null ? "" : breakpoint.getCondition().toString();
        }

        final void setCondition(String condition) throws MaxVMBusyException {
            try {
                breakpoint.setCondition(condition);
                inspection().settings().save();
            } catch (BreakpointCondition.ExpressionException expressionException) {
                gui().errorMessage(String.format("Error parsing saved breakpoint condition:%n  expression: %s%n       error: " + condition, expressionException.getMessage()), "Breakpoint Condition Error");
            }
        }

        /**
         * @return message describing the status of the condition, if any, associated with this breakpoint;
         * suitable for a ToolTip.
         */
        final String conditionStatus() {
            final String condition = condition();
            if (!condition.equals("")) {
                return "Breakpoint condition= \"" + condition + "\"";
            }
            return "No breakpoint condition set";
        }

        /**
         * @return is this breakpoint currently enabled in the VM?
         */
        final boolean isEnabled() {
            return breakpoint.isEnabled();
        }

        /**
         * Updates the enabled state of this breakpoint.
         *
         * @param enabled new state for this breakpoint
         * @throws MaxVMBusyException
         */
        final void setEnabled(boolean enabled) throws MaxVMBusyException {
            breakpoint.setEnabled(enabled);
        }

        /**
         * @return whether this breakpoint is still marked deleted after the most recent sweep
         */
        final boolean isDeleted() {
            return deleted;
        }

        /**
         * @return the thread in the VM, if any, that is currently stopped at this breakpoint.
         */
        final MaxThread triggerThread() {
            for (MaxBreakpointEvent breakpointEvent : vm().state().breakpointEvents()) {
                final MaxBreakpoint triggeredBreakpoint = breakpointEvent.breakpoint();
                if (triggeredBreakpoint == breakpoint || triggeredBreakpoint.owner() == breakpoint) {
                    return breakpointEvent.thread();
                }
            }
            return null;
        }

        /**
         * @return display name of the thread, if any, that is currently stopped at this breakpoint.
         */
        final String triggerThreadName() {
            return inspection().nameDisplay().longName(triggerThread());
        }

        /**
         * sets the "deleted" state, used to update the list of breakpoints in the VM.
         */
        final void markDeleted(boolean deleted) {
            this.deleted = deleted;
        }

        /**
         * @return short string identifying the kind of this breakpoint
         */
        abstract String kindTag();

        /**
         * @return longer string identifying the kind of this breakpoint
         */
        abstract String kindName();

        /**
         * @return name of the breakpoint, suitable for display in a table cell
         */
        abstract String shortName();

        /**
         * @return longer textual description suitable for tool tip
         */
        abstract String longName();

        /**
         * @return difference between the breakpoint and the beginning of the method,
         * described in units appropriate to each kind of breakpoint.
         */
        abstract int location();

        /**
         * @return a description of the breakpoint location in the VM, specifying units
         */
        abstract String locationDescription();


        @Override
        public final String toString() {
            return shortName();
        }

        public int compareTo(Object o) {
            // per {@link TreeSet}, comparison must be consistent with equals
            int result = 0;
            if (!this.equals(o)) {
                if (o instanceof BreakpointData) {
                    final BreakpointData breakpointData = (BreakpointData) o;
                    result = shortName().compareTo(breakpointData.shortName());
                    if (result == 0) {
                        result = longName().compareTo(breakpointData.longName());
                    }
                }
                if (result == 0) {
                    result = 1;
                }
            }
            return result;
        }
    }

    private final class TargetBreakpointData extends BreakpointData {

        private Address codeStart;
        private int location = 0;
        private String shortName;
        private String longName;

        TargetBreakpointData(MaxBreakpoint targetBreakpoint) {
            super(targetBreakpoint);
            final Address address = codeLocation().address();
            final MaxCompiledCode compiledCode = vm().codeCache().findCompiledCode(address);
            if (compiledCode != null) {
                shortName = inspection().nameDisplay().shortName(compiledCode);
                final StringBuilder sb = new StringBuilder();
                sb.append("(");
                if (breakpoint().getDescription() == null) {
                    sb.append("Method");
                } else {
                    sb.append(breakpoint().getDescription());
                }
                sb.append(") ");
                sb.append(inspection().nameDisplay().longName(compiledCode, address));
                longName = sb.toString();
                codeStart = compiledCode.getCodeStart();
                location = address.minus(codeStart.asAddress()).toInt();
            } else {
                final MaxExternalCode externalCode = vm().codeCache().findExternalCode(address);
                if (externalCode != null) {
                    codeStart = externalCode.getCodeStart();
                    location = address.minus(codeStart.asAddress()).toInt();
                    shortName = inspection().nameDisplay().shortName(externalCode);
                    longName = inspection().nameDisplay().longName(externalCode);
                } else {
                    // Must be an address in an unknown area of native code
                    shortName = address.to0xHexString();
                    longName = "unknown native code at " + address.to0xHexString();
                    codeStart = address;
                    location = 0;
                }
            }
        }

        @Override
        String kindTag() {
            return "T";
        }

        @Override
        String kindName() {
            return "Target Code breakpoint";
        }

        @Override
        String shortName() {
            return shortName;
        }

        @Override
        String longName() {
            return longName;
        }

        @Override
        int location() {
            return location;
        }

        @Override
        String locationDescription() {
            return "Offset=" + (location > 0 ? "+" : "") + location + ", Address=" + codeLocation().address().toHexString();
        }

        Address address() {
            return codeLocation().address();
        }
    }

    private final class BytecodeBreakpointData extends BreakpointData {

        final MaxCodeLocation codeLocation;
        String shortName;
        String longName;

        BytecodeBreakpointData(MaxBreakpoint bytecodeBreakpoint) {
            super(bytecodeBreakpoint);
            codeLocation = bytecodeBreakpoint.codeLocation();
            final MethodKey key = codeLocation.methodKey();
            final int position = codeLocation.bytecodePosition();
            shortName = key.holder().toJavaString(false) + "." + key.name().toString() + key.signature().toJavaString(false,  false);
            final StringBuilder longBuilder = new StringBuilder("Method: ");
            longBuilder.append(key.signature().resultDescriptor().toJavaString(false)).append(" ");
            longBuilder.append(key.name().toString());
            longBuilder.append(key.signature().toJavaString(false,  false));
            if (position == -1) {
                longBuilder.append("(prologue)");
            } else if (position == 0) {
                longBuilder.append("(entry)");
            } else {
                longBuilder.append(" + ").append(position);
            }
            longBuilder.append(" in ").append(key.holder().toJavaString());
            longName = longBuilder.toString();
        }

        @Override
        String kindTag() {
            return "B";
        }

        @Override
        String kindName() {
            return "Bytecodes breakpoint";
        }

        @Override
        String shortName() {
            return shortName;
        }

        @Override
        String longName() {
            return longName;
        }

        @Override
        int location() {
            return codeLocation.bytecodePosition();
        }

        @Override
        String locationDescription() {
            if (location() > 0) {
                return "Position = " + location() + " bytes from entry";
            }
            return "Position = method entry";
        }

    }

}
