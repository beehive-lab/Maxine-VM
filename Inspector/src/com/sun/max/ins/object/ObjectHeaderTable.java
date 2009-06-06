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
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.value.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.object.TeleObject.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A table that displays the header in a Maxine heap object; for use in an instance of {@link ObjectInspector}.
 *
 * @author Michael Van De Vanter
 */
public class ObjectHeaderTable extends InspectorTable {

    private static final int MAX_ROW_COUNT = 3;
    private static final int HEADER_HUB_ROW = 0;
    private static final int HEADER_MISC_ROW = 1;
    private static final int HEADER_ARRAY_LENGTH_ROW = 2;

    private final ObjectInspector _objectInspector;
    private final Inspection _inspection;
    private final TeleObject _teleObject;
    private Pointer _objectOrigin;
    private TeleHub _teleHub;
    private final int _hubReferenceOffset; // Property of the layout scheme; doesn't change
    private final int _miscWordOffset; // Property of the layout scheme; doesn't change
    private final int _arrayLengthOffset; // Property of the layout scheme; doesn't change

    private final ObjectHeaderTableModel _model;
    private final ObjectHeaderTableColumnModel _columnModel;
    private final TableColumn[] _columns;

    /**
     * A {@link JTable} specialized to display Maxine object fields.
     *
     * @param objectInspector parent that contains this panel
     */
    public ObjectHeaderTable(final ObjectInspector objectInspector) {
        super(objectInspector.inspection());
        _objectInspector = objectInspector;
        _inspection = objectInspector.inspection();
        _teleObject = objectInspector.teleObject();
        final LayoutScheme layoutScheme = maxVM().vmConfiguration().layoutScheme();
        _hubReferenceOffset = layoutScheme.generalLayout().getOffsetFromOrigin(HeaderField.HUB).toInt();
        _miscWordOffset = layoutScheme.generalLayout().getOffsetFromOrigin(HeaderField.MISC).toInt();
        _arrayLengthOffset = layoutScheme.arrayHeaderLayout().getOffsetFromOrigin(HeaderField.LENGTH).toInt();

        _model = new ObjectHeaderTableModel();
        _columns = new TableColumn[ObjectFieldColumnKind.VALUES.length()];
        _columnModel = new ObjectHeaderTableColumnModel(_objectInspector);
        setModel(_model);
        setColumnModel(_columnModel);
        setFillsViewportHeight(true);
        setShowHorizontalLines(style().memoryTableShowHorizontalLines());
        setShowVerticalLines(style().memoryTableShowVerticalLines());
        setIntercellSpacing(style().memoryTableIntercellSpacing());
        setRowHeight(style().memoryTableRowHeight());
        setRowSelectionAllowed(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, _inspection.style().defaultBorderColor()));
        addMouseListener(new TableCellMouseClickAdapter(_inspection, this) {

            @Override
            public void procedure(final MouseEvent mouseEvent) {
                final int selectedRow = getSelectedRow();
                final int selectedColumn = getSelectedColumn();
                if (selectedRow != -1 && selectedColumn != -1) {
                    // Left button selects a table cell; also cause an address selection at the row.
                    if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON1) {
                        _inspection.focus().setAddress(_model.rowToAddress(selectedRow));
                    }
                }
                super.procedure(mouseEvent);
            }
        });
        refresh(true);
        JTableColumnResizer.adjustColumnPreferredWidths(this);
    }

    @Override
    public void paintChildren(Graphics g) {
        // Draw a box around the selected row in the table
        super.paintChildren(g);
        final int row = getSelectedRow();
        if (row >= 0) {
            g.setColor(style().memorySelectedAddressBorderColor());
            g.drawRect(0, row * getRowHeight(row), getWidth() - 1, getRowHeight(row) - 1);
        }
    }

    /**
     * Add tool tip text to the column headers, as specified by {@link ObjectFieldColumnKind}.
     *
     * @see javax.swing.JTable#createDefaultTableHeader()
     */
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(_columnModel) {

            @Override
            public String getToolTipText(java.awt.event.MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = _columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = _columnModel.getColumn(index).getModelIndex();
                return ObjectFieldColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    /**
     * Models the words/rows in an object header; the value of each cell is simply the word/row number.
     */
    private final class ObjectHeaderTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return ObjectFieldColumnKind.VALUES.length();
        }

        public int getRowCount() {
            switch (_teleObject.getObjectKind()) {
                case TUPLE:
                    return 2;
                case ARRAY:
                    return 3;
                case HYBRID:
                    return 3;
            }
            ProgramError.unexpected("unrecognized object kind");
            return -1;
        }

        public Object getValueAt(int row, int col) {
            return row;
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return Integer.class;
        }

        public int rowToOffset(int row) {
            switch (row) {
                case HEADER_HUB_ROW:
                    return _hubReferenceOffset;
                case HEADER_MISC_ROW:
                    return _miscWordOffset;
                case HEADER_ARRAY_LENGTH_ROW:
                    return _arrayLengthOffset;
                default:
                    ProgramError.unexpected();
            }
            return -1;
        }

        public Address rowToAddress(int row) {
            return _objectOrigin.plus(rowToOffset(row)).asAddress();
        }

        public TypeDescriptor rowToType(int row) {
            switch (row) {
                case HEADER_HUB_ROW:
                    return _teleHub == null ? null : JavaTypeDescriptor.forJavaClass(_teleHub.hub().getClass());
                case HEADER_MISC_ROW:
                    return JavaTypeDescriptor.WORD;
                case HEADER_ARRAY_LENGTH_ROW:
                    return JavaTypeDescriptor.INT;
                default:
                    ProgramError.unexpected();
            }
            return null;
        }

        public String rowToName(int row) {
            switch (row) {
                case HEADER_HUB_ROW:
                    return HeaderField.HUB.toString();
                case HEADER_MISC_ROW:
                    return HeaderField.MISC.toString();
                case HEADER_ARRAY_LENGTH_ROW:
                    return HeaderField.LENGTH.toString();
                default:
                    ProgramError.unexpected();
            }
            return null;
        }

        public int addressToRow(Address address) {
            if (!address.isZero()) {
                final int wordSize = maxVM().wordSize();
                final Address endAddress = _teleObject.getObjectKind() == ObjectKind.TUPLE ? _objectOrigin.plus(_miscWordOffset + wordSize) : _objectOrigin.plus(_arrayLengthOffset +
                    wordSize);
                if (address.greaterEqual(_objectOrigin) && address.lessThan(endAddress)) {
                    return address.minus(_objectOrigin).dividedBy(wordSize).toInt();
                }
            }
            return -1;
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
            _columns[col] = new TableColumn(col, 0, renderer, null);
            _columns[col].setHeaderValue(columnKind.label());
            _columns[col].setMinWidth(columnKind.minWidth());
            if (isVisible) {
                addColumn(_columns[col]);
            }
            _columns[col].setIdentifier(columnKind);
        }
    }

    private final class TagRenderer extends PlainLabel implements TableCellRenderer, TextSearchable, Prober {

        TagRenderer() {
            super(_inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object ignore, boolean isSelected, boolean hasFocus, int row, int col) {
            String registerNameList = null;
            final TeleNativeThread thread = focus().thread();
            if (thread != null) {
                final TeleIntegerRegisters teleIntegerRegisters = thread.integerRegisters();
                final Address address = _model.rowToAddress(row);
                final Sequence<Symbol> registerSymbols = teleIntegerRegisters.find(address, address.plus(maxVM().wordSize()));
                if (registerSymbols.isEmpty()) {
                    setText("");
                    setToolTipText("");
                    setForeground(style().memoryDefaultTagTextColor());
                } else {
                    for (Symbol registerSymbol : registerSymbols) {
                        final String name = registerSymbol.name();
                        if (registerNameList == null) {
                            registerNameList = name;
                        } else {
                            registerNameList = registerNameList + "," + name;
                        }
                    }
                    setText(registerNameList + "--->");
                    setToolTipText("Register(s): " + registerNameList + " in thread " + inspection().nameDisplay().longName(thread) + " point at this location");
                    setForeground(style().memoryRegisterTagTextColor());
                }
            }
            return this;
        }
    }

    private final class AddressRenderer extends LocationLabel.AsAddressWithOffset implements TableCellRenderer {

        AddressRenderer() {
            super(_inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(_model.rowToOffset(row), _objectOrigin);
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public PositionRenderer() {
            super(_inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(_model.rowToOffset(row), _objectOrigin);
            return this;
        }
    }

    private final class TypeRenderer extends TypeLabel implements TableCellRenderer {

        public TypeRenderer() {
            super(_inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setValue(_model.rowToType(row));
            return this;
        }
    }

    private final class NameRenderer extends JavaNameLabel implements TableCellRenderer {

        public NameRenderer() {
            super(_inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(_model.rowToName(row));
            return this;
        }
    }

    private final class ValueRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] _labels = new InspectorLabel[MAX_ROW_COUNT];

        public ValueRenderer() {

            // Hub
            _labels[HEADER_HUB_ROW] = new WordValueLabel(_inspection, WordValueLabel.ValueMode.REFERENCE) {

                @Override
                public Value fetchValue() {
                    if (_teleHub != null) {
                        return WordValue.from(_teleHub.getCurrentOrigin());
                    }
                    return WordValue.ZERO;
                }
            };

            // Misc word
            _labels[HEADER_MISC_ROW] = new MiscWordLabel(_inspection, _teleObject);

            // Array length (only for array and hybrid objects).
            switch (_teleObject.getObjectKind()) {
                case ARRAY: {
                    final TeleArrayObject teleArrayObject = (TeleArrayObject) _teleObject;
                    _labels[HEADER_ARRAY_LENGTH_ROW] = new PrimitiveValueLabel(_inspection, Kind.INT) {

                        @Override
                        public Value fetchValue() {
                            return IntValue.from(teleArrayObject.getLength());
                        }
                    };
                    break;
                }
                case HYBRID: {
                    final TeleHybridObject teleHybridObject = (TeleHybridObject) _teleObject;
                    _labels[HEADER_ARRAY_LENGTH_ROW] = new PrimitiveValueLabel(_inspection, Kind.INT) {

                        @Override
                        public Value fetchValue() {
                            return IntValue.from(teleHybridObject.readArrayLength());
                        }
                    };
                    break;
                }
                case TUPLE: {
                    // Dummy; never used
                    _labels[HEADER_ARRAY_LENGTH_ROW] = new TextLabel(_inspection, "");
                    break;
                }
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return _labels[row];
        }

        public void redisplay() {
            for (InspectorLabel label : _labels) {
                label.redisplay();
            }
        }

        public void refresh(boolean force) {
            for (InspectorLabel label : _labels) {
                label.refresh(force);
            }
        }
    }

    private final class RegionRenderer implements TableCellRenderer, Prober {

        private final InspectorLabel _regionLabel;
        private final InspectorLabel _dummyLabel;

        public RegionRenderer() {
            _regionLabel = new MemoryRegionValueLabel(_inspection) {

                @Override
                public Value fetchValue() {
                    if (_teleHub != null) {
                        return WordValue.from(_teleHub.getCurrentOrigin());
                    }
                    return WordValue.ZERO;
                }
            };
            _dummyLabel = new PlainLabel(_inspection, "");
        }

        public void refresh(boolean force) {
            _regionLabel.refresh(force);
        }

        public void redisplay() {
            _regionLabel.redisplay();
            _dummyLabel.redisplay();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return row == HEADER_HUB_ROW ? _regionLabel : _dummyLabel;
        }
    }

    public void redisplay() {
        for (TableColumn column : _columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        invalidate();
        repaint();
    }

    private MaxVMState _lastRefreshedState = null;

    public void refresh(boolean force) {
        if (maxVMState().newerThan(_lastRefreshedState) || force) {
            _lastRefreshedState = maxVMState();
            _objectOrigin = _teleObject.getCurrentOrigin();
            _teleHub = _teleObject.getTeleHub();
            final int oldSelectedRow = getSelectedRow();
            final int newRow = _model.addressToRow(focus().address());
            if (newRow >= 0) {
                getSelectionModel().setSelectionInterval(newRow, newRow);
            } else {
                if (oldSelectedRow >= 0) {
                    getSelectionModel().clearSelection();
                }
            }
            for (TableColumn column : _columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(force);
            }
        }

    }

}
