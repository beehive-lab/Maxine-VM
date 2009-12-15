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

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

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
        tableModel = new BreakpointsTableModel();
        columnModel = new BreakpointsColumnModel(viewPreferences);
        configureDefaultTable(tableModel, columnModel);
    }

    @Override
    protected InspectorPopupMenu getPopupMenu(int row, int col, MouseEvent mouseEvent) {
        final BreakpointData breakpointData = tableModel.get(row);
        final InspectorPopupMenu menu = new InspectorPopupMenu("Breakpoints");
        final String shortName = breakpointData.shortName();
        menu.add(inspection().actions().removeBreakpoint(breakpointData.breakpoint(), "Remove: " + shortName));
        if (breakpointData.isEnabled()) {
            menu.add(inspection().actions().disableBreakpoint(breakpointData.breakpoint(), "Disable: " + shortName));
        } else {
            menu.add(inspection().actions().enableBreakpoint(breakpointData.breakpoint(), "Enable: " + shortName));
        }
        menu.addSeparator();
        final JMenu methodEntryBreakpoints = new JMenu("Break at Method Entry");
        methodEntryBreakpoints.add(inspection().actions().setTargetCodeBreakpointAtMethodEntriesByName());
        methodEntryBreakpoints.add(inspection().actions().setBytecodeBreakpointAtMethodEntryByName());
        methodEntryBreakpoints.add(inspection().actions().setBytecodeBreakpointAtMethodEntryByKey());
        menu.add(methodEntryBreakpoints);
        menu.add(inspection().actions().setTargetCodeBreakpointAtObjectInitializer());
        menu.add(inspection().actions().removeAllBreakpoints());
        menu.addSeparator();
        menu.add(inspection().actions().removeAllTargetCodeBreakpoints());
        menu.addSeparator();
        menu.add(inspection().actions().removeAllBytecodeBreakpoints());
        return menu;
    }

    @Override
    public void updateFocusSelection() {
        // Sets table selection to breakpoint, if any, that is the current user focus.
        final MaxBreakpoint breakpoint = inspection().focus().breakpoint();
        final int row = tableModel.findRow(breakpoint);
        updateSelection(row);
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

    private final class BreakpointsColumnModel extends InspectorTableColumnModel<BreakpointsColumnKind>  {

        private BreakpointsColumnModel(BreakpointsViewPreferences viewPreferences) {
            super(BreakpointsColumnKind.VALUES.length(), viewPreferences);
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

        // Cache of information objects for each known breakpoint
        private final Set<BreakpointData> breakpoints = new TreeSet<BreakpointData>();

        public int getColumnCount() {
            return BreakpointsColumnKind.VALUES.length();
        }

        public int getRowCount() {
            // This gets called during superclass initialization, before the local
            // data has been initialized, even  if you try to set row size to 0
            // in the constructor.
            return breakpoints == null ? 0 : breakpoints.size();
        }

        public Object getValueAt(int row, int col) {
            final BreakpointData breakpointData = get(row);
            switch (BreakpointsColumnKind.VALUES.get(col)) {
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
                    throw FatalError.unexpected("Unexpected Breakpoint Data column");
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            final BreakpointData breakpointData = get(row);

            switch (BreakpointsColumnKind.VALUES.get(column)) {
                case ENABLED:
                    final Boolean newState = (Boolean) value;
                    if (breakpointData.setEnabled(newState)) {
                        inspection().settings().save();
                    }
                    break;

                case CONDITION:
                    final String conditionText = (String) value;
                    breakpointData.setCondition(conditionText);
                    inspection().settings().save();
                    break;

                default:
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            switch (BreakpointsColumnKind.VALUES.get(col)) {
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
            switch (BreakpointsColumnKind.VALUES.get(c)) {
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
                    throw FatalError.unexpected("Unexected Breakpoint Data column");
            }
        }

        @Override
        public void refresh() {
            // Check for current and added breakpoints
            // Initially assume all deleted
            for (BreakpointData breakpointData : breakpoints) {
                breakpointData.markDeleted(true);
            }
            // add new and mark previous as not deleted
            for (MaxBreakpoint targetBreakpoint : maxVM().targetBreakpoints()) {
                final BreakpointData breakpointData = findTargetBreakpoint(targetBreakpoint.getCodeLocation().targetCodeInstructionAddress());
                if (breakpointData == null) {
                    // new breakpoint in VM since last refresh
                    breakpoints.add(new TargetBreakpointData(targetBreakpoint));
                    //fireTableDataChanged();
                } else {
                    // mark as not deleted
                    breakpointData.markDeleted(false);
                }
            }
            for (MaxBreakpoint bytecodeBreakpoint : maxVM().bytecodeBreakpoints()) {
                final BreakpointData breakpointData = findBytecodeBreakpoint(bytecodeBreakpoint.getCodeLocation().key());
                if (breakpointData == null) {
                    // new breakpoint since last refresh
                    breakpoints.add(new BytecodeBreakpointData(bytecodeBreakpoint));
                    //fireTableDataChanged();
                } else {
                    // mark as not deleted
                    breakpointData.markDeleted(false);
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

        private BreakpointData get(int row) {
            int count = 0;
            for (BreakpointData breakpointData : breakpoints) {
                if (count == row) {
                    return breakpointData;
                }
                count++;
            }
            throw FatalError.unexpected("BreakpointsInspector.get(" + row + ") failed");
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
        BytecodeBreakpointData findBytecodeBreakpoint(TeleBytecodeBreakpoint.Key key) {
            for (BreakpointData breakpointData : breakpoints) {
                if (breakpointData instanceof BytecodeBreakpointData) {
                    final BytecodeBreakpointData bytecodeBreakpointData = (BytecodeBreakpointData) breakpointData;
                    if (bytecodeBreakpointData.key() == key) {
                        return bytecodeBreakpointData;
                    }
                }
            }
            return null;
        }

    }

    /**
     * @return color the text specially in the row where a triggered breakpoint is displayed
     */
    private Color getRowTextColor(int row) {
        return (tableModel.get(row).triggerThread() == null) ? null : inspection().style().debugIPTagColor();
    }

    private final class TagCellRenderer extends PlainLabel implements TableCellRenderer {

        TagCellRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final BreakpointData breakpointData = tableModel.get(row);
            setIcon((breakpointData.triggerThread() == null) ? null : inspection().style().debugIPTagIcon());
            setText(breakpointData.kindTag());
            setToolTipText(breakpointData.kindName() + ", Enabled=" + (breakpointData.isEnabled() ? "true" : "false"));
            setForeground(getRowTextColor(row));
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
            setValue(breakpointData.shortName(), breakpointData.longName());
            setForeground(getRowTextColor(row));
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
            setText(Integer.toString(breakpointData.location()));
            setToolTipText("Location: " + breakpointData.locationDescription());
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class ConditionCellRenderer extends DefaultTableCellRenderer implements Prober {

        ConditionCellRenderer() {
            setFont(inspection().style().defaultFont());
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setToolTipText(tableModel.get(row).conditionStatus());
            final JComponent component = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return component;
        }

        public void redisplay() {
            setFont(inspection().style().defaultFont());
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
            if (breakpointData.triggerThread() != null) {
                setText(breakpointData.triggerThreadName());
                setToolTipText("Thread \"" + breakpointData.triggerThreadName() + "\" stopped at this breakpoint");
            } else {
                setText("");
                setToolTipText("No Thread stopped at this breakpoint");
            }
            setForeground(getRowTextColor(row));
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
        final TeleCodeLocation getCodeLocation() {
            return breakpoint.getCodeLocation();
        }

        /**
         * @return textual expression of the condition associated with this breakpoint, if any.
         */
        final String condition() {
            return breakpoint.getCondition() == null ? "" : breakpoint.getCondition().toString();
        }

        final void setCondition(String condition) {
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
         * @return true if the state was actually changed
         */
        final boolean setEnabled(boolean enabled) {
            return breakpoint.setEnabled(enabled);
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
            for (MaxThread thread : maxVMState().threads()) {
                if (thread.breakpoint() == breakpoint) {
                    return thread;
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
            final Address address = getCodeLocation().targetCodeInstructionAddress();
            final TeleTargetMethod teleTargetMethod = maxVM().makeTeleTargetMethod(address);
            if (teleTargetMethod != null) {
                shortName = inspection().nameDisplay().shortName(teleTargetMethod);
                longName = "method: " + inspection().nameDisplay().longName(teleTargetMethod, address);
                codeStart = teleTargetMethod.getCodeStart();
                location = address.minus(codeStart.asAddress()).toInt();
            } else {
                final TeleNativeTargetRoutine teleNativeTargetRoutine = maxVM().findTeleTargetRoutine(TeleNativeTargetRoutine.class, address);
                if (teleNativeTargetRoutine != null) {
                    codeStart = teleNativeTargetRoutine.getCodeStart();
                    location = address.minus(codeStart.asAddress()).toInt();
                    shortName = inspection().nameDisplay().shortName(teleNativeTargetRoutine);
                    longName = inspection().nameDisplay().longName(teleNativeTargetRoutine);
                } else {
                    // Must be an address in an unknown area of native code
                    shortName = "0x" + address.toHexString();
                    longName = "unknown native code at 0x" + address.toHexString();
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
            String description = breakpoint().getDescription();
            return description != null && !description.equals("") ?  description  : shortName;
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
            return "Offset=" + (location > 0 ? "+" : "") + location + ", Address=" + getCodeLocation().targetCodeInstructionAddress().toHexString();
        }

        Address address() {
            return getCodeLocation().targetCodeInstructionAddress();
        }
    }

    private final class BytecodeBreakpointData extends BreakpointData {

        private final TeleBytecodeBreakpoint.Key key;
        String shortName;
        String longName;

        BytecodeBreakpointData(MaxBreakpoint bytecodeBreakpoint) {
            super(bytecodeBreakpoint);
            key = bytecodeBreakpoint.getCodeLocation().key();
            shortName = key.holder().toJavaString(false) + "." + key.name().toString() + key.signature().toJavaString(false,  false);

            longName = "Method: " + key.signature().resultDescriptor().toJavaString(false) + " " + key.name().toString() + key.signature().toJavaString(false,  false);
            if (key.position() > 0) {
                longName += " + " + key.position();
            }
            longName = longName + " in " + key.holder().toJavaString();
        }

        @Override
        String kindTag() {
            return "B";
        }

        @Override
        String kindName() {
            return "Bytecode breakpoint";
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
            return key.position();
        }

        @Override
        String locationDescription() {
            return "Bytecode position=" + key.position();
        }

        TeleBytecodeBreakpoint.Key key() {
            return key;
        }
    }

}
