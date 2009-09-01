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
package com.sun.max.ins.object;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.value.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A table that displays the header in a Maxine heap object; for use in an instance of {@link ObjectInspector}.
 *
 * @author Michael Van De Vanter
 */
public final class ObjectHeaderTable extends InspectorTable {

    private final ObjectInspector objectInspector;
    private final Inspection inspection;
    private final TeleObject teleObject;
    private final Layout.HeaderField[] headerFields;
    private TeleHub teleHub;

    private final ObjectHeaderTableModel model;
    private final ObjectHeaderTableColumnModel columnModel;
    private final TableColumn[] columns;

    private MaxVMState lastRefreshedState = null;


    private final class ToggleObjectHeaderWatchpointAction extends InspectorAction {

        private final int row;

        public ToggleObjectHeaderWatchpointAction(Inspection inspection, String name, int row) {
            super(inspection, name);
            this.row = row;
        }

        @Override
        protected void procedure() {
            final Sequence<MaxWatchpoint> watchpoints = model.getWatchpoints(row);
            if (watchpoints.isEmpty()) {
                final HeaderField headerField = headerFields[row];
                actions().setHeaderWatchpoint(teleObject, headerField, "Watch this field's memory").perform();
            } else {
                for (MaxWatchpoint watchpoint : watchpoints) {
                    watchpoint.dispose();
                }
            }
        }
    }

    /**
     * A {@link JTable} specialized to display Maxine object header fields.
     *
     * @param objectInspector parent that contains this panel
     */
    public ObjectHeaderTable(final ObjectInspector objectInspector) {
        super(objectInspector.inspection());
        this.objectInspector = objectInspector;
        this.inspection = objectInspector.inspection();
        this.teleObject = objectInspector.teleObject();
        headerFields = teleObject.getHeaderFields();
        this.model = new ObjectHeaderTableModel(inspection, teleObject.getCurrentOrigin());
        this.columns = new TableColumn[ObjectFieldColumnKind.VALUES.length()];
        this.columnModel = new ObjectHeaderTableColumnModel(objectInspector);
        configureMemoryTable(model, columnModel);
        setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, inspection.style().defaultBorderColor()));
    }

    @Override
    protected void mouseButton1Clicked(int row, int col, MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1 && maxVM().watchpointsEnabled()) {
            final InspectorAction action = new ToggleObjectHeaderWatchpointAction(inspection(), null, row);
            action.perform();
        }
    }

    @Override
    protected InspectorMenu getDynamicMenu(int row, int col, MouseEvent mouseEvent) {
        if (maxVM().watchpointsEnabled()) {
            final InspectorMenu menu = new InspectorMenu();
            menu.add(new ToggleObjectHeaderWatchpointAction(inspection(), "Toggle watchpoint (double-click)", row));
            final HeaderField headerField = headerFields[row];
            menu.add(actions().setHeaderWatchpoint(teleObject, headerField, "Watch this field's memory"));
            menu.add(actions().setObjectWatchpoint(teleObject, "Watch this object's memory"));
            menu.add(Watchpoints.createEditMenu(inspection, model.getWatchpoints(row)));
            menu.add(Watchpoints.createRemoveActionOrMenu(inspection(), model.getWatchpoints(row)));
            return menu;
        }
        return null;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // The selection in the table has changed; might have happened via user action (click, arrow) or
        // as a side effect of a focus change.
        super.valueChanged(e);
        if (!e.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0 && row < model.getRowCount()) {
                inspection().focus().setAddress(model.getAddress(row));
            }
        }
    }

    public void refresh(boolean force) {
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            model.setOrigin(teleObject.getCurrentOrigin());
            if (teleObject.isLive()) {
                teleHub = teleObject.getTeleHub();
                final int oldSelectedRow = getSelectedRow();
                final int newRow = model.findRow(focus().address());
                if (newRow >= 0) {
                    getSelectionModel().setSelectionInterval(newRow, newRow);
                } else {
                    if (oldSelectedRow >= 0) {
                        getSelectionModel().clearSelection();
                    }
                }
                for (TableColumn column : columns) {
                    final Prober prober = (Prober) column.getCellRenderer();
                    prober.refresh(force);
                }
            }
            model.refresh();
        }
    }

    public void redisplay() {
        for (TableColumn column : columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        invalidate();
        repaint();
    }

    /**
     * Add tool tip text to the column headers, as specified by {@link ObjectFieldColumnKind}.
     *
     * @see javax.swing.JTable#createDefaultTableHeader()
     */
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {

            @Override
            public String getToolTipText(java.awt.event.MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = columnModel.getColumn(index).getModelIndex();
                return ObjectFieldColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    /**
     * Models the words/rows in an object header; the value of each cell is simply the word/row number.
     * <br>
     * The origin of the model is the current origin of the object in memory, which can change due to GC.
     */
    private final class ObjectHeaderTableModel extends InspectorMemoryTableModel {

        public ObjectHeaderTableModel(Inspection inspection, Address origin) {
            super(inspection, origin);
        }

        public int getColumnCount() {
            return ObjectFieldColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return headerFields.length;
        }

        public Object getValueAt(int row, int col) {
            return row;
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return Integer.class;
        }

        @Override
        public Address getAddress(int row) {
            return getOrigin().plus(getOffset(row));
        }

        @Override
        public MemoryRegion getMemoryRegion(int row) {
            return teleObject.getCurrentMemoryRegion(headerFields[row]);
        }

        @Override
        public Offset getOffset(int row) {
            return teleObject.getHeaderOffset(headerFields[row]);
        }

        @Override
        public int findRow(Address address) {
            for (int row = 0; row < headerFields.length; row++) {
                if (getAddress(row).equals(address)) {
                    return row;
                }
            }
            return -1;
        }

        public TypeDescriptor rowToType(int row) {
            return teleObject.getHeaderType(headerFields[row]);
        }

        public String rowToName(int row) {
            return headerFields[row].toString();
        }
    }

    /**
     * A column model for object headers, to be used in an {@link ObjectInspector}. Column selection is driven by
     * choices in the parent {@link ObjectInspector}. This implementation cannot update column choices dynamically.
     */
    private final class ObjectHeaderTableColumnModel extends DefaultTableColumnModel {

        ObjectHeaderTableColumnModel(ObjectInspector objectInspector) {
            createColumn(ObjectFieldColumnKind.TAG, new TagRenderer(), true);
            createColumn(ObjectFieldColumnKind.ADDRESS, new AddressRenderer(), objectInspector.showAddresses());
            createColumn(ObjectFieldColumnKind.OFFSET, new PositionRenderer(), objectInspector.showOffsets());
            createColumn(ObjectFieldColumnKind.TYPE, new TypeRenderer(), objectInspector.showFieldTypes());
            createColumn(ObjectFieldColumnKind.NAME, new NameRenderer(), true);
            createColumn(ObjectFieldColumnKind.VALUE, new ValueRenderer(), true);
            createColumn(ObjectFieldColumnKind.REGION, new RegionRenderer(), objectInspector.showMemoryRegions());
        }

        private void createColumn(ObjectFieldColumnKind columnKind, TableCellRenderer renderer, boolean isVisible) {
            final int col = columnKind.ordinal();
            columns[col] = new TableColumn(col, 0, renderer, null);
            columns[col].setHeaderValue(columnKind.label());
            columns[col].setMinWidth(columnKind.minWidth());
            if (isVisible) {
                addColumn(columns[col]);
            }
            columns[col].setIdentifier(columnKind);
        }
    }

    /**
     * @return color the text specially in the row where a watchpoint is triggered
     */
    private Color getRowTextColor(int row) {
        final MaxWatchpointEvent watchpointEvent = maxVMState().watchpointEvent();
        if (watchpointEvent != null && model.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return style().defaultTextColor();
    }

    private final class TagRenderer extends MemoryTagTableCellRenderer implements TableCellRenderer {

        TagRenderer() {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Component renderer = getRenderer(model.getMemoryRegion(row), focus().thread(), model.getWatchpoints(row));
            renderer.setForeground(getRowTextColor(row));
            return renderer;
        }
    }

    private final class AddressRenderer extends LocationLabel.AsAddressWithOffset implements TableCellRenderer {

        AddressRenderer() {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(model.getOffset(row), model.getOrigin());
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public PositionRenderer() {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(model.getOffset(row), model.getOrigin());
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class TypeRenderer extends TypeLabel implements TableCellRenderer {

        public TypeRenderer() {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setValue(model.rowToType(row));
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class NameRenderer extends JavaNameLabel implements TableCellRenderer {

        public NameRenderer() {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(model.rowToName(row));
            setForeground(getRowTextColor(row));
            return this;
        }
    }

    private final class ValueRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] labels = new InspectorLabel[headerFields.length];

        public ValueRenderer() {

            for (int row = 0; row < headerFields.length; row++) {
                // Create a label suitable for the kind of header field
                switch(headerFields[row]) {
                    case HUB:
                        labels[row] = new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE, ObjectHeaderTable.this) {

                            @Override
                            public Value fetchValue() {
                                return teleHub == null ? WordValue.ZERO : WordValue.from(teleHub.getCurrentOrigin());
                            }
                        };
                        break;
                    case MISC:
                        labels[row] = new MiscWordLabel(inspection, teleObject);
                        break;
                    case LENGTH:
                        switch (teleObject.getObjectKind()) {
                            case ARRAY:
                                final TeleArrayObject teleArrayObject = (TeleArrayObject) teleObject;
                                labels[row] = new PrimitiveValueLabel(inspection, Kind.INT) {

                                    @Override
                                    public Value fetchValue() {
                                        return IntValue.from(teleArrayObject.getLength());
                                    }
                                };
                                break;
                            case HYBRID:
                                final TeleHybridObject teleHybridObject = (TeleHybridObject) teleObject;
                                labels[row] = new PrimitiveValueLabel(inspection, Kind.INT) {

                                    @Override
                                    public Value fetchValue() {
                                        return IntValue.from(teleHybridObject.readArrayLength());
                                    }
                                };
                                break;
                            case TUPLE:
                                // No length header field
                                break;
                            default:
                                ProgramError.unknownCase();
                        }
                        break;
                    default:
                        ProgramError.unknownCase();
                }
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return labels[row];
        }

        public void redisplay() {
            for (InspectorLabel label : labels) {
                label.redisplay();
            }
        }

        public void refresh(boolean force) {
            for (InspectorLabel label : labels) {
                label.refresh(force);
            }
        }
    }

    private final class RegionRenderer implements TableCellRenderer, Prober {

        private final InspectorLabel regionLabel;
        private final InspectorLabel dummyLabel;

        public RegionRenderer() {
            regionLabel = new MemoryRegionValueLabel(inspection) {

                @Override
                public Value fetchValue() {
                    if (teleHub != null) {
                        return WordValue.from(teleHub.getCurrentOrigin());
                    }
                    return WordValue.ZERO;
                }
            };
            dummyLabel = new PlainLabel(inspection, "");
        }

        public void refresh(boolean force) {
            regionLabel.refresh(force);
        }

        public void redisplay() {
            regionLabel.redisplay();
            dummyLabel.redisplay();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return headerFields[row] == HeaderField.HUB ? regionLabel : dummyLabel;
        }
    }

}
