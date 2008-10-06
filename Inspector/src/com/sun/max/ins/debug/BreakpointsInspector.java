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
/*VCSID=1c180035-5d06-4e9d-b973-2a742c51e8ce*/
package com.sun.max.ins.debug;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * Access to information about all kinds of breakpoints that might be set in the {@link TeleVM}.
 * Wrappers with extra information about each breakpoint are kept in a model.
 *
 * @author Mick Jordan
 * @author Michael Van De Vanter
 */
public final class BreakpointsInspector extends UniqueInspector {

    /**
     * @return the singleton instance, if it exists
     */
    private static BreakpointsInspector getInspector(Inspection inspection) {
        return UniqueInspector.find(inspection, BreakpointsInspector.class);
    }

    /**
     * Displays and highlights the (singleton) breakpoints inspector.
     * @return  The breakpoints inspector, possibly newly created.
     */
    public static BreakpointsInspector make(Inspection inspection) {
        BreakpointsInspector breakpointsInspector = getInspector(inspection);
        if (breakpointsInspector == null) {
            Trace.begin(1, "initializing BreakpointsInspector");
            breakpointsInspector = new BreakpointsInspector(inspection, Residence.INTERNAL);
            Trace.end(1, "initializing BreakpointsInspector");
        }
        breakpointsInspector.highlight();
        return breakpointsInspector;
    }

    enum ColumnKind {
        TAG,
        ENABLED,
        METHOD,
        LOCATION,
        CONDITION,
        TRIGGER_THREAD;

        public static final IndexedSequence<ColumnKind> VALUES = new ArraySequence<ColumnKind>(values());
    }

    /**
     * The data model is a list that is kept sorted in ThreadID order.
     */
    private final Set<BreakpointData> _model = new TreeSet<BreakpointData>();
    private final JTable _table = new BreakpointJTable();

    private BreakpointsInspector(Inspection inspection, Residence residence) {
        super(inspection, residence);
        createFrame(null);
    }

    private final SaveSettingsListener _saveSettingsListener = createBasicSettingsClient(this, "breakpointsInspector");

    @Override
    public SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    public String getTitle() {
        return "Breakpoints";
    }

    @Override
    public void createView(long epoch) {
        _table.setRowSelectionAllowed(true);
        _table.setColumnSelectionAllowed(false);
        _table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _table.addMouseListener(new BreakpointInspectorMouseClickAdapter(inspection()));
        final JScrollPane scrollPane = new JScrollPane(_table);
        scrollPane.setPreferredSize(inspection().geometry().breakpointsFramePrefSize());
        frame().setLocation(inspection().geometry().breakpointsFrameDefaultLocation());
        frame().setContentPane(scrollPane);
        frame().add(new BreakpointMenuItems());
        refreshView(epoch);
    }

    private final class BreakpointMenuItems implements InspectorMenuItems {

        public void addTo(InspectorMenu menu) {
            final JMenu methodEntryBreakpoints = new JMenu("Break at Method Entry");
            methodEntryBreakpoints.add(inspection().inspectionMenus().getBreakAtTargetMethodAction());
            methodEntryBreakpoints.add(inspection().inspectionMenus().getBreakAtMethodAction());
            methodEntryBreakpoints.add(inspection().inspectionMenus().getBreakAtMethodKeyAction());
            menu.add(methodEntryBreakpoints);
            menu.add(inspection().inspectionMenus().getBreakAtObjectInitializersAction());
            menu.addSeparator();
            menu.add(inspection().inspectionMenus().getClearSelectedBreakpointAction());
            menu.add(inspection().inspectionMenus().getClearAllBreakpointsAction());
        }

        public Inspection inspection() {
            return BreakpointsInspector.this.inspection();
        }

        public void refresh(long epoch) {
        }

        public void redisplay() {
        }

    }

    private final class BreakpointJTable extends JTable {
        final TableCellRenderer _tagCellRenderer;
        final TableCellRenderer _methodCellRenderer;
        final TableCellRenderer _locationCellRenderer;
        final TableCellRenderer _conditionCellRenderer;
        final TableCellRenderer _triggerThreadCellRenderer;

        BreakpointJTable() {
            super(new BreakpointTableModel());
            _tagCellRenderer = new TagCellRenderer(inspection());
            _methodCellRenderer = new MethodCellRenderer(inspection());
            _locationCellRenderer = new LocationCellRenderer(inspection());
            _conditionCellRenderer = new ConditionCellRenderer();
            _triggerThreadCellRenderer = new TriggerThreadCellRenderer(inspection());
            getColumnModel().getColumn(ColumnKind.TAG.ordinal()).setPreferredWidth(20);
            getColumnModel().getColumn(ColumnKind.ENABLED.ordinal()).setPreferredWidth(20);
            getColumnModel().getColumn(ColumnKind.METHOD.ordinal()).setPreferredWidth(190);
            getColumnModel().getColumn(ColumnKind.LOCATION.ordinal()).setPreferredWidth(30);
            getColumnModel().getColumn(ColumnKind.CONDITION.ordinal()).setPreferredWidth(80);
            getColumnModel().getColumn(ColumnKind.TRIGGER_THREAD.ordinal()).setPreferredWidth(80);
            final TableCellEditor enabledCellEditor = new DefaultCellEditor(new JCheckBox());
            getColumnModel().getColumn(ColumnKind.ENABLED.ordinal()).setCellEditor(enabledCellEditor);
            final TableCellEditor conditionCellEditor = new DefaultCellEditor(new JTextField());
            getColumnModel().getColumn(ColumnKind.CONDITION.ordinal()).setCellEditor(conditionCellEditor);
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            switch (ColumnKind.VALUES.get(column)) {
                case TAG:
                    return _tagCellRenderer;
                case ENABLED:
                    return super.getCellRenderer(row, column);
                case METHOD:
                    return _methodCellRenderer;
                case LOCATION:
                    return _locationCellRenderer;
                case CONDITION:
                    return _conditionCellRenderer;
                    //return super.getCellRenderer(row, column);
                case TRIGGER_THREAD:
                    return _triggerThreadCellRenderer;
                default:
                    Problem.error("Unexpected Breakpoint Data column");
            }
            return null;
        }
    }

    private final class TagCellRenderer extends PlainLabel implements TableCellRenderer {

        TagCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final BreakpointData breakpointData = get(row);
            setText(breakpointData.kindTag());
            setToolTipText(breakpointData.kindName() + ", Enabled=" + (breakpointData.enabled() ? "true" : "false"));
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class MethodCellRenderer extends PlainLabel implements TableCellRenderer {

        public MethodCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final BreakpointData breakpointData = get(row);
            setText(breakpointData.shortName());
            setToolTipText(breakpointData.longName());
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class LocationCellRenderer extends PlainLabel implements TableCellRenderer {

        public LocationCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final BreakpointData breakpointData = get(row);
            setText(Integer.toString(breakpointData.location()));
            setToolTipText("Location: " + breakpointData.locationDescription());
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class ConditionCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setToolTipText(get(row).conditionStatus());
            final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                component.setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                component.setBackground(style().defaultTextBackgroundColor());
            }
            return component;
        }
    }

    private final class TriggerThreadCellRenderer extends PlainLabel implements TableCellRenderer {

        TriggerThreadCellRenderer(Inspection inspection) {
            super(inspection, null);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final BreakpointData breakpointData = get(row);
            if (breakpointData.triggerThread() != null) {
                setText(breakpointData.triggerThreadName());
                setToolTipText("Thread \"" + breakpointData.triggerThreadName() + "\" stopped at this breakpoint");
            } else {
                setText("");
                setToolTipText("No Thread stopped at this breakpoint");
            }
            if (row == _table.getSelectionModel().getMinSelectionIndex()) {
                setBackground(style().defaultCodeAlternateBackgroundColor());
            } else {
                setBackground(style().defaultTextBackgroundColor());
            }
            return this;
        }
    }

    private final class BreakpointTableModel extends DefaultTableModel {

        @Override
        public int getColumnCount() {
            return ColumnKind.VALUES.length();
        }

        @Override
        public int getRowCount() {
            return _model.size();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            switch (ColumnKind.VALUES.get(col)) {
                case ENABLED:
                    return true;

                case CONDITION:
                    return get(row) instanceof TargetBreakpointData;

                default:
                    break;
            }
            return false;
        }

        @Override
        public Object getValueAt(int row, int col) {
            final BreakpointData breakpointData = get(row);
            switch (ColumnKind.VALUES.get(col)) {
                case TAG:
                    return breakpointData.kindTag();
                case ENABLED:
                    return breakpointData.enabled();
                case METHOD:
                    return breakpointData.shortName();
                case LOCATION:
                    return breakpointData.location();
                case CONDITION:
                    return breakpointData.condition();
                case TRIGGER_THREAD:
                    return  breakpointData.triggerThreadName();
                default:
                    Problem.error("Unspected Breakpoint Data column");
            }
            return null;
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            switch (ColumnKind.VALUES.get(c)) {
                case TAG:
                    return String.class;
                case ENABLED:
                    return Boolean.class;
                case METHOD:
                    return String.class;
                case LOCATION:
                    return Number.class;
                case CONDITION:
                    return String.class;
                case TRIGGER_THREAD:
                    return String.class;
                default:
                    Problem.error("Unspected Breakpoint Data column");
            }
            return Object.class;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            final BreakpointData breakpointData = get(row);

            switch (ColumnKind.VALUES.get(column)) {
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
        public String getColumnName(int c) {
            switch (ColumnKind.VALUES.get(c)) {
                case TAG:
                    return "Kind";
                case METHOD:
                    return "Method";
                case LOCATION:
                    return "Locn";
                case ENABLED:
                    return "En";
                case CONDITION:
                    return "Condition";
                case TRIGGER_THREAD:
                    return "Thread";
            }
            return "";
        }

    }

    private final class BreakpointInspectorMouseClickAdapter extends InspectorMouseClickAdapter {

        BreakpointInspectorMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }

        @Override
        public void procedure(final MouseEvent mouseEvent) {
            switch (MaxineInspector.mouseButtonWithModifiers(mouseEvent)) {
                case MouseEvent.BUTTON1: {
                    final int row = _table.getSelectedRow();
                    final int column = _table.getSelectedColumn();
                    final BreakpointData breakpointData = get(row);
                    switch (ColumnKind.VALUES.get(column)) {
                        case TAG:
                        case METHOD:
                        case LOCATION:
                        case CONDITION:
                        case TRIGGER_THREAD:
                            focus().setBreakpoint(breakpointData.teleBreakpoint());
                            break;
                        case ENABLED:
                            break;
                    }
                }
            }
        }
    };

    @Override
    public void refreshView(long epoch) {
        // Check for current and added breakpoints
        // Initially assume all deleted
        for (BreakpointData breakpointData : _model) {
            breakpointData.markDeleted(true);
        }
        // add new and mark previous as not deleted
        for (TeleTargetBreakpoint breakpoint : teleProcess().targetBreakpointFactory().breakpoints(true)) {
            final BreakpointData breakpointData = findTargetBreakpoint(breakpoint.address());
            if (breakpointData == null) {
                // new breakpoint in {@link TeleVM} since last refresh
                _model.add(new TargetBreakpointData(breakpoint));
                final BreakpointTableModel breakpointTableModel = (BreakpointTableModel) _table.getModel();
                breakpointTableModel.fireTableDataChanged();
            } else {
                // mark as not deleted
                breakpointData.markDeleted(false);
            }
        }
        for (TeleBytecodeBreakpoint breakpoint : teleVM().bytecodeBreakpointFactory().breakpoints()) {
            final BreakpointData breakpointData = findBytecodeBreakpoint(breakpoint.key());
            if (breakpointData == null) {
                // new breakpoint since last refresh
                _model.add(new BytecodeBreakpointData(breakpoint));
                final BreakpointTableModel breakpointTableModel = (BreakpointTableModel) _table.getModel();
                breakpointTableModel.fireTableDataChanged();
            } else {
                // mark as not deleted
                breakpointData.markDeleted(false);
            }
        }
        // now remove the breakpoints that are still marked as deleted
        final Iterator iter = _model.iterator();
        while (iter.hasNext()) {
            final BreakpointData breakpointData = (BreakpointData) iter.next();
            if (breakpointData.isDeleted()) {
                iter.remove();
            }
        }
        final BreakpointTableModel breakpointTableModel = (BreakpointTableModel) _table.getModel();
        breakpointTableModel.fireTableDataChanged();
        updateSelectedBreakpoint(focus().breakpoint());
        super.refreshView(epoch);
    }

    public void viewConfigurationChanged(long epoch) {
        //  All view configurations are applied dynamically in this inspector.
        refreshView(epoch);
    }

    @Override
    public void breakpointSetChanged() {
        refreshView();
    }

    @Override
    public void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint) {
        updateSelectedBreakpoint(teleBreakpoint);
    }
    /**
     * Global breakpoint focus has changed; revise selection in inspector if needed.
     */
    private void updateSelectedBreakpoint(TeleBreakpoint focusBreakpoint) {
        TeleBreakpoint selectedBreakpoint = null;
        final int selectedRow = _table.getSelectedRow();
        if (selectedRow >= 0) {
            selectedBreakpoint = get(selectedRow).teleBreakpoint();
        }
        if (selectedBreakpoint != focusBreakpoint) {
            final int row = breakpointToRow(focusBreakpoint);
            if (row >= 0) {
                _table.getSelectionModel().setSelectionInterval(row, row);
            } else {
                _table.getSelectionModel().clearSelection();
            }
        }
    }

    /**
     * Locates a target code breakpoint already known to the inspector.
     */
    private TargetBreakpointData findTargetBreakpoint(Address address) {
        for (BreakpointData breakpointData : _model) {
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
    private BytecodeBreakpointData findBytecodeBreakpoint(TeleBytecodeBreakpoint.Key key) {
        for (BreakpointData breakpointData : _model) {
            if (breakpointData instanceof BytecodeBreakpointData) {
                final BytecodeBreakpointData bytecodeBreakpointData = (BytecodeBreakpointData) breakpointData;
                if (bytecodeBreakpointData.key() == key) {
                    return bytecodeBreakpointData;
                }
            }
        }
        return null;
    }

    private BreakpointData get(int row) {
        int count = 0;
        for (BreakpointData breakpointData : _model) {
            if (count == row) {
                return breakpointData;
            }
            count++;
        }
        Problem.error("BreakpointsInspector.get(" + row + ") failed");
        return null;
    }

    /**
     * Return the table row in which a breakpoint is displayed.
     */
    private int breakpointToRow(TeleBreakpoint breakpoint) {
        int row = 0;
        for (BreakpointData breakpointData : _model) {
            if (breakpointData.teleBreakpoint() == breakpoint) {
                return row;
            }
            row++;
        }
        return -1;
    }

    /**
     * Summary of information about a breakpoint that is useful for inspection.
     *
     * @author Michael Van De Vanter
     */
    private abstract class BreakpointData implements Comparable{

        /**
         * @return the breakpoint in the {@link TeleVM} being described
         */
        abstract TeleBreakpoint teleBreakpoint();

        /**
         * @return the location of the breakpoint in the {@link TeleVM} in a standard format.
         */
        TeleCodeLocation teleCodeLocation() {
            return teleBreakpoint().teleCodeLocation();
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
         * @return a description of the breakpoint location in the {@link TeleVM}, specifying units
         */
        abstract String locationDescription();

        /**
         * @return is this breakpoint currently enabled in the {@link TeleVM}?
         */
        boolean enabled() {
            return teleBreakpoint().enabled();
        }

        /**
         * Updates the enabled state of this breakpoint.
         *
         * @param enabled new state for this breakpoint
         * @return true if the state was actually changed
         */
        public boolean setEnabled(boolean enabled) {
            return teleBreakpoint().setEnabled(enabled);
        }

         /**
         * @return textual expression of the condition associated with this breakpoint, if any.
         */
        String condition() {
            return teleBreakpoint().condition() == null ? "" : teleBreakpoint().condition().toString();
        }

        abstract void setCondition(String conditionText);

        /**
         * @return message describing the status of the condition, if any, assocaited with this breakpoint;
         * suitable for a ToolTip.
         */
        abstract String conditionStatus();

        /**
         * @return the thread in the {@link TeleVM}, if any, that is currently stopped at this breakpoint.
         */
        TeleNativeThread triggerThread() {
            for (TeleNativeThread thread : teleBreakpoint().teleProcess().threads()) {
                if (thread.breakpoint() == teleBreakpoint()) {
                    return thread;
                }
            }
            return null;
        }

        /**
         * @return display name of the thread, if any, that is currently stopped at this breakpoint.
         */
        String triggerThreadName() {
            return inspection().nameDisplay().longName(triggerThread());
        }

        private boolean _deleted = false;

        /**
         * @return whether this breakpoint is still marked deleted after the most recent sweep
         */
        boolean isDeleted() {
            return _deleted;
        }

        /**
         * sets the "deleted" state, used to update the list of breakpoints in the {@link TeleVM}.
         */
        void markDeleted(boolean deleted) {
            _deleted = deleted;
        }

        @Override
        public String toString() {
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

        private final TeleTargetBreakpoint _teleTargetBreakpoint;
        private Address _codeStart;
        private int _location = 0;
        private String _shortName;
        private String _longName;

        TargetBreakpointData(TeleTargetBreakpoint teleTargetBreakpoint) {
            _teleTargetBreakpoint = teleTargetBreakpoint;
            final Address address = teleTargetBreakpoint.address();
            if (TeleNativeTargetRoutine.get(teleVM(), address) != null) {
                _shortName = "0x" + address.toHexString();
                _longName = "native code at 0x" + address.toHexString();
                _codeStart = address;
                _location = 0;
            } else {
                final TeleTargetMethod teleTargetMethod = TeleTargetMethod.make(teleVM(), address);
                if (teleTargetMethod != null) {
                    _shortName = inspection().nameDisplay().shortName(teleTargetMethod, address);
                    _longName = inspection().nameDisplay().longName(teleTargetMethod, address);
                    _codeStart = teleTargetMethod.codeStart();
                    _location = address.minus(_codeStart.asAddress()).toInt();
                }
            }
        }

        @Override
        TeleBreakpoint teleBreakpoint() {
            return _teleTargetBreakpoint;
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
            return _shortName;
        }

        @Override
        String longName() {
            return _longName;
        }

        @Override
        int location() {
            return _location;
        }

        @Override
        String locationDescription() {
            return "Offset=" + (_location > 0 ? "+" : "") + _location + ", Address=" + _teleTargetBreakpoint.address().toHexString();
        }

        @Override
        void setCondition(String condition) {
            try {
                _teleTargetBreakpoint.setCondition(new BreakpointCondition(teleVM(), condition));
                inspection().settings().save();
            } catch (BreakpointCondition.ExpressionException expressionException) {
                inspection().errorMessage(String.format("Error parsing saved breakpoint condition:%n  expression: %s%n       error: " + condition, expressionException.getMessage()), "Breakpoint Condition Error");
            }
        }

        @Override
        String conditionStatus() {
            final String condition = condition();
            if (!condition.equals("")) {
                return "Breakpoint condition= \"" + condition + "\"";
            }
            return "No breakpoint condition set";
        }

        Address address() {
            return _teleTargetBreakpoint.address();
        }

    }

    private final class BytecodeBreakpointData extends BreakpointData {

        private final TeleBytecodeBreakpoint _teleBytecodeBreakpoint;
        private final TeleBytecodeBreakpoint.Key _key;
        String _shortName;
        String _longName;

        BytecodeBreakpointData(TeleBytecodeBreakpoint teleBytecodeBreakpoint) {
            _teleBytecodeBreakpoint = teleBytecodeBreakpoint;
            _key = teleBytecodeBreakpoint.key();
            _shortName = _key.holder().toJavaString(false) + "." + _key.name().toString() + _key.signature().toJavaString(false,  false);

            _longName = _key.signature().getResultDescriptor().toJavaString(false) + " " + _key.name().toString() + _key.signature().toJavaString(false,  false);
            if (_key.position() > 0) {
                _longName += " + " + _key.position();
            }
            _longName = _longName + " in " + _key.holder().toJavaString();
        }

        @Override
        TeleBreakpoint teleBreakpoint() {
            return _teleBytecodeBreakpoint;
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
            return _shortName;
        }

        @Override
        String longName() {
            return _longName;
        }

        @Override
        int location() {
            return _key.position();
        }

        @Override
        String locationDescription() {
            return "Bytecode position=" + _key.position();
        }

        @Override
        void setCondition(String conditionText) {
            Problem.unimplemented("Conditional bytecode breakpoints not supported yet");
        }

        @Override
        String conditionStatus() {
            return "Bytecode breakpoint conditions not supported yet";
        }

        TeleBytecodeBreakpoint.Key key() {
            return _key;
        }

    }


}

