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
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.value.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;


/**
 * A table that displays Maxine object fields; for use in an instance of {@link ObjectInspector}.
 *
 * @author Michael Van De Vanter
 */
public final class ObjectFieldsTable extends InspectorTable {

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
     * A {@link JTable} specialized to display Maxine object fields.
     *
     * @param objectInspector parent that contains this panel
     * @param fieldActors description of the fields to be displayed
     */
    public ObjectFieldsTable(Inspection inspection, TeleObject teleObject, Collection<FieldActor> fieldActors, ObjectViewPreferences instanceViewPreferences) {
        super(inspection);
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
        if (fieldActors.size() > 0) {
            startOffset =  this.fieldActors[0].offset();
            final FieldActor lastFieldActor = this.fieldActors[this.fieldActors.length - 1];
            endOffset = lastFieldActor.offset() + lastFieldActor.kind.width.numberOfBytes;
        } else {
            // moot if there aren't any field actors
            startOffset = 0;
            endOffset = 0;
        }
        this.tableModel = new ObjectFieldsTableModel(inspection, teleObject.getCurrentOrigin());
        this.columnModel = new ObjectFieldsTableColumnModel(instanceViewPreferences);
        configureMemoryTable(tableModel, columnModel);
        updateFocusSelection();
    }

    @Override
    protected void mouseButton1Clicked(final int row, int col, MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1 && maxVM().watchpointsEnabled()) {
            final InspectorAction toggleAction = new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setFieldWatchpoint(teleObject, tableModel.rowToFieldActor(row), "Watch this field's memory").perform();
                    final Sequence<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (watchpoints.length() > 0) {
                        return watchpoints.first();
                    }
                    return null;
                }
            };
            toggleAction.perform();
        }
    }

    @Override
    protected InspectorPopupMenu getPopupMenu(final int row, int col, MouseEvent mouseEvent) {
        if (maxVM().watchpointsEnabled()) {
            final InspectorPopupMenu menu = new InspectorPopupMenu();
            menu.add(new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint (double-click)") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    actions().setFieldWatchpoint(teleObject, tableModel.rowToFieldActor(row), "Watch this field's memory").perform();
                    final Sequence<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (watchpoints.length() > 0) {
                        return watchpoints.first();
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
        final Address address = inspection().focus().address();
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
                inspection().focus().setAddress(tableModel.getAddress(row));
            }
        }
    }

    /**
     * A column model for object headers, to be used in an {@link ObjectInspector}.
     * Column selection is driven by choices in the parent {@link ObjectInspector}.
     * This implementation cannot update column choices dynamically.
     */
    private final class ObjectFieldsTableColumnModel extends InspectorTableColumnModel<ObjectColumnKind> {

        ObjectFieldsTableColumnModel(ObjectViewPreferences viewPreferences) {
            super(ObjectColumnKind.VALUES.length(), viewPreferences);
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
            return ObjectColumnKind.VALUES.length();
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
        public MemoryRegion getMemoryRegion(int row) {
            return teleObject.getCurrentMemoryRegion(fieldActors[row]);
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

        FieldActor rowToFieldActor(int row) {
            return fieldActors[row];
        }

        TypeDescriptor rowToType(int row) {
            return fieldActors[row].descriptor();
        }

        String rowToName(int row) {
            return fieldActors[row].name.string;
        }


        @Override
        public void refresh() {
            setOrigin(teleObject.getCurrentOrigin());
            super.refresh();
        }
    }

    /**
     * @return color the text specially in the row where a watchpoint is triggered
     */
    private Color getRowTextColor(int row) {
        final MaxWatchpointEvent watchpointEvent = maxVMState().watchpointEvent();
        if (watchpointEvent != null && tableModel.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return style().defaultTextColor();
    }

    private final class TagRenderer extends MemoryTagTableCellRenderer implements TableCellRenderer {

        TagRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final JLabel renderer = getRenderer(tableModel.getMemoryRegion(row), focus().thread(), tableModel.getWatchpoints(row));
            renderer.setOpaque(true);
            renderer.setForeground(getRowTextColor(row));
            renderer.setBackground(cellBackgroundColor(isSelected));
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

    private final class NameRenderer extends FieldActorNameLabel implements TableCellRenderer {

        public NameRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final FieldActor fieldActor = (FieldActor) value;
            setValue(fieldActor);
            setForeground(getRowTextColor(row));
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
            InspectorLabel label = labels[row];
            if (label == null) {
                final FieldActor fieldActor = (FieldActor) value;
                if (fieldActor.kind == Kind.REFERENCE) {
                    label = new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE, ObjectFieldsTable.this) {
                        @Override
                        public Value fetchValue() {
                            return teleObject.readFieldValue(fieldActor);
                        }
                    };
                } else if (fieldActor.kind == Kind.WORD) {
                    label = new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, ObjectFieldsTable.this) {
                        @Override
                        public Value fetchValue() {
                            return teleObject.readFieldValue(fieldActor);
                        }
                    };
                } else if (isTeleActor && fieldActor.name.toString().equals("flags")) {
                    final TeleActor teleActor = (TeleActor) teleObject;
                    label = new ActorFlagsValueLabel(inspection(), teleActor);
                } else {
                    label = new PrimitiveValueLabel(inspection(), fieldActor.kind) {
                        @Override
                        public Value fetchValue() {
                            return teleObject.readFieldValue(fieldActor);
                        }
                    };
                }
                label.setOpaque(true);
                labels[row] = label;
            }
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
        }
    }

    private final class RegionRenderer implements TableCellRenderer, Prober {

        public RegionRenderer(Inspection inspection) {
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
            InspectorLabel label = labels[row];
            if (label == null) {
                final FieldActor fieldActor = (FieldActor) value;
                label = new MemoryRegionValueLabel(inspection()) {
                    @Override
                    public Value fetchValue() {
                        return teleObject.readFieldValue(fieldActor);
                    }
                };
                label.setOpaque(true);
                labels[row] = label;
            }
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
        }
    }

}
