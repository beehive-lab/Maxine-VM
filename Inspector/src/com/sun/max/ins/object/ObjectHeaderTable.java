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
package com.sun.max.ins.object;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A table that displays the header in a heap object; for use in an instance of {@link ObjectInspector}.
 *
 * @author Michael Van De Vanter
 */
public final class ObjectHeaderTable extends InspectorTable {

    private final TeleObject teleObject;
    private final Layout.HeaderField[] headerFields;

    private final ObjectHeaderTableModel tableModel;
    private final ObjectHeaderColumnModel columnModel;

    private final class ToggleObjectHeaderWatchpointAction extends InspectorAction {

        private final int row;

        public ToggleObjectHeaderWatchpointAction(Inspection inspection, String name, int row) {
            super(inspection, name);
            this.row = row;
        }

        @Override
        protected void procedure() {
            final List<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
            if (watchpoints.isEmpty()) {
                final HeaderField headerField = headerFields[row];
                actions().setHeaderWatchpoint(teleObject, headerField, "Watch this field's memory").perform();
            } else {
                actions().removeWatchpoints(watchpoints, null).perform();
            }
        }
    }

    private final ObjectViewPreferences instanceViewPreferences;

    /**
     * A {@link JTable} specialized to display object header fields.
     *
     * @param objectInspector parent that contains this panel
     */
    public ObjectHeaderTable(Inspection inspection, TeleObject teleObject, ObjectViewPreferences instanceViewPreferences) {
        super(inspection);
        this.teleObject = teleObject;
        this.instanceViewPreferences = instanceViewPreferences;
        headerFields = teleObject.headerFields();
        this.tableModel = new ObjectHeaderTableModel(inspection, teleObject.origin());
        this.columnModel = new ObjectHeaderColumnModel(instanceViewPreferences);
        configureMemoryTable(tableModel, columnModel);
        setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, style().defaultBorderColor()));
        updateFocusSelection();
    }

    @Override
    protected void mouseButton1Clicked(int row, int col, MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1 && vm().watchpointManager() != null) {
            final InspectorAction action = new ToggleObjectHeaderWatchpointAction(inspection(), null, row);
            action.perform();
        }
    }

    @Override
    protected InspectorPopupMenu getPopupMenu(int row, int col, MouseEvent mouseEvent) {
        if (vm().watchpointManager() != null) {
            final InspectorPopupMenu menu = new InspectorPopupMenu();
            menu.add(new ToggleObjectHeaderWatchpointAction(inspection(), "Toggle watchpoint (double-click)", row));
            final HeaderField headerField = headerFields[row];
            menu.add(actions().setHeaderWatchpoint(teleObject, headerField, "Watch this field's memory"));
            menu.add(actions().setObjectWatchpoint(teleObject, "Watch this object's memory"));
            menu.add(Watchpoints.createEditMenu(inspection(), tableModel.getWatchpoints(row)));
            menu.add(Watchpoints.createRemoveActionOrMenu(inspection(), tableModel.getWatchpoints(row)));
            return menu;
        }
        return null;
    }

    @Override
    public void updateFocusSelection() {
        // Sets table selection to the memory word, if any, that is the current user focus.
        final Address address = focus().address();
        updateSelection(tableModel.findRow(address));
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // The selection in the table has changed; might have happened via user action (click, arrow) or
        // as a side effect of a focus change.
        super.valueChanged(e);
        if (!e.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0 && row < tableModel.getRowCount()) {
                focus().setAddress(tableModel.getAddress(row));
            }
        }
    }

    /**
     * A column model for object headers, to be used in an {@link ObjectInspector}. Column selection is driven by
     * choices in the parent {@link ObjectInspector}. This implementation cannot update column choices dynamically.
     */
    private final class ObjectHeaderColumnModel extends InspectorTableColumnModel<ObjectColumnKind> {

        ObjectHeaderColumnModel(ObjectViewPreferences viewPreferences) {
            super(ObjectColumnKind.values().length, viewPreferences);
            addColumn(ObjectColumnKind.TAG, new TagRenderer(inspection()), null);
            addColumn(ObjectColumnKind.ADDRESS, new AddressRenderer(inspection()), null);
            addColumn(ObjectColumnKind.OFFSET, new PositionRenderer(inspection()), null);
            addColumn(ObjectColumnKind.TYPE, new TypeRenderer(inspection()), null);
            addColumn(ObjectColumnKind.NAME, new NameRenderer(inspection()), null);
            addColumn(ObjectColumnKind.VALUE, new ValueRenderer(inspection()), null);
            addColumn(ObjectColumnKind.REGION, new RegionRenderer(inspection()), null);
        }
    }

    /**
     * Models the words/rows in an object header; the value of each cell is simply the word/row number.
     * <br>
     * The origin of the model is the current origin of the object in memory, which can change due to GC.
     */
    private final class ObjectHeaderTableModel extends InspectorMemoryTableModel {

        private TeleHub teleHub;
        private final String[] headerFieldDescriptions;

        public ObjectHeaderTableModel(Inspection inspection, Address origin) {
            super(inspection, origin);
            if (teleObject.isLive()) {
                teleHub = teleObject.getTeleHub();
            }
            headerFieldDescriptions = new String[headerFields.length];
            for (int row = 0; row < headerFields.length; row++) {
                headerFieldDescriptions[row] = "Object header field \"" + headerFields[row].name + "\": " + headerFields[row].description;
            }
        }

        public int getColumnCount() {
            return ObjectColumnKind.values().length;
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
        public MaxMemoryRegion getMemoryRegion(int row) {
            return teleObject.headerMemoryRegion(headerFields[row]);
        }

        @Override
        public Offset getOffset(int row) {
            return teleObject.headerOffset(headerFields[row]);
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
            return teleObject.headerType(headerFields[row]);
        }

        public String rowToName(int row) {
            return headerFields[row].toString();
        }

        public String rowToDescription(int row) {
            return headerFieldDescriptions[row];
        }

        public TeleHub teleHub() {
            return teleHub;
        }

        @Override
        public void refresh() {
            setOrigin(teleObject.origin());
            if (teleObject.isLive()) {
                teleHub = teleObject.getTeleHub();
            }
            super.refresh();
        }
    }

    /**
     * @return color the text specially in the row where a watchpoint is triggered
     */
    private Color getRowTextColor(int row) {
        final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
        if (watchpointEvent != null && tableModel.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return null;
    }

    private final class TagRenderer extends MemoryTagTableCellRenderer implements TableCellRenderer {

        TagRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Component renderer = getRenderer(tableModel.getMemoryRegion(row), focus().thread(), tableModel.getWatchpoints(row));
            renderer.setForeground(getRowTextColor(row));
            renderer.setBackground(cellBackgroundColor(isSelected));
            setToolTipText(tableModel.rowToDescription(row));
            return renderer;
        }
    }

    private final class AddressRenderer extends LocationLabel.AsAddressWithOffset implements TableCellRenderer {

        AddressRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(tableModel.getOffset(row), tableModel.getOrigin());
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public PositionRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(tableModel.getOffset(row), tableModel.getOrigin());
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class TypeRenderer extends TypeLabel implements TableCellRenderer {

        public TypeRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setValue(tableModel.rowToType(row));
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class NameRenderer extends JavaNameLabel implements TableCellRenderer {

        public NameRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(tableModel.rowToName(row), tableModel.rowToDescription(row));
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class ValueRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] labels = new InspectorLabel[headerFields.length];

        public ValueRenderer(Inspection inspection) {

            for (int row = 0; row < headerFields.length; row++) {
                // Create a label suitable for the kind of header field
                InspectorLabel label = null;
                HeaderField headerField = headerFields[row];
                if (headerField == HeaderField.HUB) {
                    label = new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE, ObjectHeaderTable.this) {

                        @Override
                        public Value fetchValue() {
                            final TeleHub teleHub = tableModel.teleHub();
                            return teleHub == null ? WordValue.ZERO : WordValue.from(vm().readWord(teleObject.origin().plus(Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB))).asPointer());
                        }
                    };
                } else if (headerField == HeaderField.MISC) {
                    label = new MiscWordLabel(inspection, teleObject);
                } else if (headerField == HeaderField.LENGTH) {
                    switch (teleObject.kind()) {
                        case ARRAY:
                            final TeleArrayObject teleArrayObject = (TeleArrayObject) teleObject;
                            label = new PrimitiveValueLabel(inspection, Kind.INT) {

                                @Override
                                public Value fetchValue() {
                                    return IntValue.from(teleArrayObject.getLength());
                                }
                            };
                            break;
                        case HYBRID:
                            final TeleHybridObject teleHybridObject = (TeleHybridObject) teleObject;
                            label = new PrimitiveValueLabel(inspection, Kind.INT) {

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
                            InspectorError.unknownCase();
                    }
                } else {
                    final HeaderField finalHeaderField = headerField;
                    label = new WordValueLabel(inspection, WordValueLabel.ValueMode.WORD, ObjectHeaderTable.this) {

                        @Override
                        public Value fetchValue() {
                            return  WordValue.from(vm().readWord(teleObject.origin().plus(Layout.generalLayout().getOffsetFromOrigin(finalHeaderField))).asPointer());
                        }
                    };
                }
                label.setOpaque(true);
                labels[row] = label;
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final InspectorLabel inspectorLabel = labels[row];
            inspectorLabel.setBackground(cellBackgroundColor(isSelected));
            return inspectorLabel;
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

        public RegionRenderer(Inspection inspection) {
            regionLabel = new MemoryRegionValueLabel(inspection) {

                @Override
                public Value fetchValue() {
                    final TeleHub teleHub = tableModel.teleHub();
                    if (teleHub != null) {
                        return WordValue.from(teleHub.origin());
                    }
                    return WordValue.ZERO;
                }
            };
            regionLabel.setOpaque(true);
            dummyLabel = new PlainLabel(inspection, "");
            dummyLabel.setOpaque(true);
        }

        public void refresh(boolean force) {
            regionLabel.refresh(force);
        }

        public void redisplay() {
            regionLabel.redisplay();
            dummyLabel.redisplay();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final InspectorLabel inspectorLabel =  (headerFields[row] == HeaderField.HUB) ? regionLabel : dummyLabel;
            inspectorLabel.setBackground(cellBackgroundColor(isSelected));
            return inspectorLabel;
        }
    }

}
