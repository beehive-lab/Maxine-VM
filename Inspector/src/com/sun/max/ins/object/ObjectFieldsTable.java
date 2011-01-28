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
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A table that displays object fields; for use in an instance of {@link ObjectInspector}.
 *
 * @author Michael Van De Vanter
 */
public final class ObjectFieldsTable extends InspectorTable {

    /**
     * information prefix to identify the kind of field, e.g. "Hub" or "Object"
     */
    private final String fieldKindPrefix;

    private final TeleObject teleObject;
    private final FieldActor[] fieldActors;
    private final boolean isTeleActor;

    /** an offset in bytes for the first field being displayed. */
    private final int startOffset;
    /** an offset in bytes that is one past the last field being displayed.*/
    private final int endOffset;

    private final ObjectFieldsTableModel tableModel;
    private final ObjectFieldsTableColumnModel columnModel;

    private final ObjectViewPreferences instanceViewPreferences;

    /**
     * A table specialized to display object fields.
     *
     * @param inspection
     * @param fieldKindPrefix information prefix to identify the kind of field, e.g. "Hub" or "Object"
     * @param teleObject the VM object that holds the fields
     * @param fieldActors description of the fields to be displayed
     * @param instanceViewPreferences
     */
    public ObjectFieldsTable(Inspection inspection, String fieldKindPrefix, TeleObject teleObject, Collection<FieldActor> fieldActors, ObjectViewPreferences instanceViewPreferences) {
        super(inspection);
        this.fieldKindPrefix = fieldKindPrefix;
        this.teleObject = teleObject;
        this.instanceViewPreferences = instanceViewPreferences;
        this.isTeleActor = teleObject instanceof TeleActor;
        this.fieldActors = new FieldActor[fieldActors.size()];
        // Sort fields by offset in object layout.
        fieldActors.toArray(this.fieldActors);
        java.util.Arrays.sort(this.fieldActors, new Comparator<FieldActor>() {
            public int compare(FieldActor a, FieldActor b) {
                final Integer aOffset = a.offset();
                return aOffset.compareTo(b.offset());
            }
        });
        if (!fieldActors.isEmpty()) {
            startOffset =  this.fieldActors[0].offset();
            final FieldActor lastFieldActor = this.fieldActors[this.fieldActors.length - 1];
            endOffset = lastFieldActor.offset() + lastFieldActor.kind.width.numberOfBytes;
        } else {
            // moot if there aren't any field actors
            startOffset = 0;
            endOffset = 0;
        }
        this.tableModel = new ObjectFieldsTableModel(inspection, teleObject.origin());
        this.columnModel = new ObjectFieldsTableColumnModel(this, this.tableModel, instanceViewPreferences);
        configureMemoryTable(tableModel, columnModel);
        updateFocusSelection();
    }

    @Override
    protected void mouseButton1Clicked(final int row, int col, MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1 && vm().watchpointManager() != null) {
            final InspectorAction toggleAction = new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setFieldWatchpoint(teleObject, tableModel.rowToFieldActor(row), "Watch this field's memory").perform();
                    final List<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (!watchpoints.isEmpty()) {
                        return watchpoints.get(0);
                    }
                    return null;
                }
            };
            toggleAction.perform();
        }
    }

    @Override
    protected InspectorPopupMenu getPopupMenu(final int row, int col, MouseEvent mouseEvent) {
        if (vm().watchpointManager() != null) {
            final InspectorPopupMenu menu = new InspectorPopupMenu();
            menu.add(new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint (double-click)") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setFieldWatchpoint(teleObject, tableModel.rowToFieldActor(row), "Watch this field's memory").perform();
                    final List<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (!watchpoints.isEmpty()) {
                        return watchpoints.get(0);
                    }
                    return null;
                }
            });
            final FieldActor fieldActor = tableModel.rowToFieldActor(row);
            menu.add(actions().setFieldWatchpoint(teleObject, fieldActor, "Watch this field's memory"));
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
     * {@inheritDoc}.
     * <br>
     * Color the text specially in the row where a watchpoint is triggered
     */
    @Override
    public Color cellForegroundColor(int row, int col) {
        final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
        if (watchpointEvent != null && tableModel.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return null;
    }

    /**
     * A column model for object headers, to be used in an {@link ObjectInspector}.
     * Column selection is driven by choices in the parent {@link ObjectInspector}.
     * This implementation cannot update column choices dynamically.
     */
    private final class ObjectFieldsTableColumnModel extends InspectorTableColumnModel<ObjectColumnKind> {

        ObjectFieldsTableColumnModel(InspectorTable table, InspectorMemoryTableModel tableModel, ObjectViewPreferences viewPreferences) {
            super(ObjectColumnKind.values().length, viewPreferences);
            addColumn(ObjectColumnKind.TAG, new MemoryTagTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(ObjectColumnKind.ADDRESS, new MemoryAddressLocationTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(ObjectColumnKind.OFFSET, new MemoryOffsetLocationTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(ObjectColumnKind.TYPE, new MemoryContentsTypeTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(ObjectColumnKind.NAME, new NameRenderer(inspection()), null);
            addColumn(ObjectColumnKind.VALUE, new ValueRenderer(inspection()), null);
            addColumn(ObjectColumnKind.BYTES,  new MemoryBytesTableCellRenderer(inspection(), table, tableModel), null);
            addColumn(ObjectColumnKind.REGION, new MemoryRegionPointerTableCellRenderer(inspection(), table, tableModel), null);
        }
    }

    /**
     * Models the fields/rows in a list of object fields;
     * the value of each cell is the {@link FieldActor} that describes the field.
     * <br>
     * The origin of the model is the origin of the object that contains these fields.
     * The origin may change because of GC.
     */
    private final class ObjectFieldsTableModel extends InspectorMemoryTableModel {

        public ObjectFieldsTableModel(Inspection inspection, Address origin) {
            super(inspection, origin);
        }

        public int getColumnCount() {
            return ObjectColumnKind.values().length;
        }

        public int getRowCount() {
            return fieldActors.length;
        }

        public Object getValueAt(int row, int col) {
            return fieldActors[row];
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            return FieldActor.class;
        }

        @Override
        public Address getAddress(int row) {
            return getOrigin().plus(getOffset(row));
        }

        @Override
        public MaxMemoryRegion getMemoryRegion(int row) {
            return teleObject.fieldMemoryRegion(fieldActors[row]);
        }

        @Override
        public Offset getOffset(int row) {
            return Offset.fromInt(fieldActors[row].offset());
        }

        @Override
        public int findRow(Address address) {
            if (!address.isZero()) {
                final int offset = address.minus(getOrigin()).toInt();
                if (offset >= startOffset && offset < endOffset) {
                    int currentOffset = startOffset;
                    for (int row = 0; row < fieldActors.length; row++) {
                        final int nextOffset = currentOffset + fieldActors[row].kind.width.numberOfBytes;
                        if (offset < nextOffset) {
                            return row;
                        }
                        currentOffset = nextOffset;
                    }
                }
            }
            return -1;
        }

        @Override
        public String getRowDescription(int row) {
            return fieldKindPrefix + " field \"" + fieldActors[row].name.string + "\"";
        }

        @Override
        public TypeDescriptor getRowType(int row) {
            return fieldActors[row].descriptor();
        }

        FieldActor rowToFieldActor(int row) {
            return fieldActors[row];
        }

        @Override
        public void refresh() {
            setOrigin(teleObject.origin());
            super.refresh();
        }
    }

    private final class NameRenderer extends FieldActorNameLabel implements TableCellRenderer {

        public NameRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final FieldActor fieldActor = (FieldActor) value;
            setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
            setValue(fieldActor);
            setForeground(cellForegroundColor(row, column));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class ValueRenderer implements TableCellRenderer, Prober {

        public ValueRenderer(Inspection inspection) {
        }

        private InspectorLabel[] labels = new InspectorLabel[fieldActors.length];

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
            if (labels[row] == null) {
                final FieldActor fieldActor = (FieldActor) value;
                if (fieldActor.kind.isReference) {
                    labels[row] = new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE, ObjectFieldsTable.this) {
                        @Override
                        public Value fetchValue() {
                            return teleObject.readFieldValue(fieldActor);
                        }
                    };
                    labels[row].setToolTipPrefix(tableModel.getRowDescription(row) + "<br>Value = ");
                } else if (fieldActor.kind.isWord) {
                    labels[row] = new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, ObjectFieldsTable.this) {
                        @Override
                        public Value fetchValue() {
                            return teleObject.readFieldValue(fieldActor);
                        }
                    };
                    labels[row].setToolTipPrefix(tableModel.getRowDescription(row) + "<br>Value = ");
                } else if (isTeleActor && fieldActor.name.toString().equals("flags")) {
                    final TeleActor teleActor = (TeleActor) teleObject;
                    labels[row] = new ActorFlagsValueLabel(inspection(), teleActor);
                    labels[row].setToolTipPrefix(tableModel.getRowDescription(row) + "<br>");
                } else {
                    labels[row] = new PrimitiveValueLabel(inspection(), fieldActor.kind) {
                        @Override
                        public Value fetchValue() {
                            return teleObject.readFieldValue(fieldActor);
                        }
                    };
                    labels[row].setToolTipPrefix(tableModel.getRowDescription(row) + "<br>Value = ");
                }

                labels[row].setOpaque(true);
            }
            labels[row].setBackground(cellBackgroundColor(isSelected));
            return labels[row];
        }
    }

}
