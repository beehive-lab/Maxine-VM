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
import com.sun.max.ins.value.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;


/**
 * A table that displays Maxine array elements; for use in an instance of {@link ObjectInspector}.
 * Null array elements can be hidden from the display.
 *
 * @author Michael Van De Vanter
 */
public final class ArrayElementsTable extends InspectorTable {

    public static final int MAXIMUM_ROWS_FOR_COMPUTING_COLUMN_WIDTHS = 1000;

    private final ObjectInspector _objectInspector;
    private final Inspection _inspection;
    private final TeleObject _teleObject;
    private final Reference _objectReference;  // Reference to an object is canonical; doesn't change
    private Pointer _objectOrigin;  // Origin may change via GC
    private final Kind _elementKind;
    private final int _elementSize;
    private final int _startOffset;
    private final int _startIndex;
    private final int _arrayLength;
    private final String _indexPrefix;
    private final WordValueLabel.ValueMode _wordValueMode;

    /** Maps display rows to element rows (indexes) in the table. */
    private int[] _rowToElementMap;
    private int _visibleElementCount = 0;  // number of array elements being displayed

    private final ArrayElementsTableModel _model;
    private final ArrayElementsTableColumnModel _columnModel;
    private final TableColumn[] _columns;

    /**
     * A {@link JTable} specialized to display Maxine array elements.
     *
     * @param objectInspector the parent of this component
     * @param kind the Maxine value "kind" of the array.
     * @param startOffset memory position relative to the object origin where the displayed array starts
     * @param startIndex index into the displayed array where the display starts
     * @param length number of elements to display
     * @param indexPrefix text to prepend to the displayed name(index) of each element.
     * @param wordValueMode how to display word values, based on their presumed use in the VM.
     */
    ArrayElementsTable(final ObjectInspector objectInspector, final Kind kind, int startOffset, int startIndex, int length, String indexPrefix, WordValueLabel.ValueMode wordValueMode) {
        super(objectInspector.inspection());
        _objectInspector = objectInspector;
        _inspection = objectInspector.inspection();
        _teleObject = objectInspector.teleObject();
        _objectReference = _teleObject.reference();
        _elementKind = kind;
        _elementSize = kind.size();
        _startOffset = startOffset;
        _startIndex = startIndex;
        _arrayLength = length;
        _indexPrefix = indexPrefix;
        _wordValueMode = wordValueMode;

        // Initialize map so that all elements will display
        _rowToElementMap = new int[_arrayLength];
        for (int index = 0; index < _arrayLength; index++) {
            _rowToElementMap[index] = index;
        }
        _visibleElementCount = _arrayLength;

        _model = new ArrayElementsTableModel();
        _columns = new TableColumn[ArrayElementColumnKind.VALUES.length()];
        _columnModel = new ArrayElementsTableColumnModel(_objectInspector);
        setModel(_model);
        setColumnModel(_columnModel);
        setFillsViewportHeight(true);
        setShowHorizontalLines(style().memoryTableShowHorizontalLines());
        setShowVerticalLines(style().memoryTableShowVerticalLines());
        setIntercellSpacing(style().memoryTableIntercellSpacing());
        setRowHeight(style().memoryTableRowHeight());
        setRowSelectionAllowed(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        refresh(_inspection.teleVM().epoch(), true);
        JTableColumnResizer.adjustColumnPreferredWidths(this, MAXIMUM_ROWS_FOR_COMPUTING_COLUMN_WIDTHS);
    }

    @Override
    public void paintChildren(Graphics g) {
        // Draw a box around the selected row in the table
        super.paintChildren(g);
        final int row = getSelectedRow();
        if (row >= 0) {
            g.setColor(style().debugSelectedCodeBorderColor());
            g.drawRect(0, row * getRowHeight(row), getWidth() - 1, getRowHeight(row) - 1);
        }
    }

    /**
     * Add tool tip text to the column headers, as specified by {@link ArrayElementsColumnKind}.
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
                return ArrayElementColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    /**
     * Models (a possible subset of) words/rows in a sequence of array elements;
     * the value of each cell is simply the index into the array
     * elements being displayed.
     */
    private final class ArrayElementsTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return ArrayElementColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return _visibleElementCount;
        }

        public Object getValueAt(int row, int col) {
            return _rowToElementMap[row];
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            return Integer.class;
        }

        /**
         * @param row index of a displayed row in the table
         * @return the offset in memory of the displayed element, relative to the object origin.
         */
        public int rowToOffset(int row) {
            return _startOffset + (_rowToElementMap[row] * _elementSize);
        }

        /**
         * @param row index of a displayed row in the table
         * @return the memory location of the displayed element in the {@link TeleVM}.
         */
        public Address rowToAddress(int row) {
            return _objectOrigin.plus(rowToOffset(row)).asAddress();
        }

        /**
         * @param address a memory address in the {@link TeleVM}.
         * @return the displayed table row that shows the array element at an address;
         * -1 if the address is not in the array, or if that element is currently hidden..
         */
        public int addressToRow(Address address) {
            if (!address.isZero()) {
                final int offset = address.minus(_objectOrigin).minus(_startOffset).toInt();
                if (offset >= 0 && offset < _arrayLength * _elementSize) {
                    final int elementRow = offset / _elementSize;
                    for (int row = 0; row < _visibleElementCount; row++) {
                        if (_rowToElementMap[row] == elementRow) {
                            return elementRow;
                        }
                    }
                }
            }
            return -1;
        }
    }

    /**
     * A column model for array elements, to be used in an {@link ObjectInspector}.
     * Column selection is driven by choices in the parent {@link ObjectInspector}.
     * This implementation cannot update column choices dynamically.
     */
    private final class ArrayElementsTableColumnModel extends DefaultTableColumnModel {

        ArrayElementsTableColumnModel(ObjectInspector objectInspector) {
            createColumn(ArrayElementColumnKind.TAG, new TagRenderer(), true);
            createColumn(ArrayElementColumnKind.ADDRESS, new AddressRenderer(), objectInspector.showAddresses());
            createColumn(ArrayElementColumnKind.POSITION, new PositionRenderer(), objectInspector.showOffsets());
            createColumn(ArrayElementColumnKind.NAME, new NameRenderer(), true);
            createColumn(ArrayElementColumnKind.VALUE, new ValueRenderer(), true);
            createColumn(ArrayElementColumnKind.REGION, new RegionRenderer(), objectInspector.showMemoryRegions());
        }

        private void createColumn(ArrayElementColumnKind columnKind, TableCellRenderer renderer, boolean isVisible) {
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

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            String registerNameList = null;
            final TeleNativeThread thread = focus().thread();
            if (thread != null) {
                final TeleIntegerRegisters teleIntegerRegisters = thread.integerRegisters();
                final Address address = _model.rowToAddress(row);
                final Sequence<Symbol> registerSymbols = teleIntegerRegisters.find(address, address.plus(teleVM().wordSize()));
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

    private final class NameRenderer extends LocationLabel.AsIndex implements TableCellRenderer {

        public NameRenderer() {
            super(_inspection, _indexPrefix, 0, 0, Address.zero());
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(row, _model.rowToOffset(row), _objectOrigin);
            return this;
        }
    }

    private final class ValueRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] _labels = new InspectorLabel[_arrayLength];

        public void refresh(long epoch, boolean force) {
            for (InspectorLabel label : _labels) {
                if (label != null) {
                    label.refresh(epoch, force);
                }
            }
        }

        public void redisplay() {
            for (InspectorLabel label : _labels) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            final int index = _rowToElementMap[row];
            InspectorLabel label = _labels[index];
            if (label == null) {
                if (_elementKind == Kind.REFERENCE) {
                    label = new WordValueLabel(_inspection, WordValueLabel.ValueMode.REFERENCE) {
                        @Override
                        public Value fetchValue() {
                            return teleVM().getElementValue(_elementKind, _objectReference, _startIndex + index);
                        }
                    };
                } else if (_elementKind == Kind.WORD) {
                    label = new WordValueLabel(_inspection, _wordValueMode) {
                        @Override
                        public Value fetchValue() {
                            return teleVM().getElementValue(_elementKind, _objectReference, _startIndex + index);
                        }
                    };
                } else {
                    label = new PrimitiveValueLabel(_inspection, _elementKind) {
                        @Override
                        public Value fetchValue() {
                            return teleVM().getElementValue(_elementKind, _objectReference, _startIndex + index);
                        }
                    };
                }
                _labels[index] = label;
            }
            return label;
        }
    }

    private final class RegionRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] _labels = new InspectorLabel[_arrayLength];

        public void refresh(long epoch, boolean force) {
            for (InspectorLabel label : _labels) {
                if (label != null) {
                    label.refresh(epoch, force);
                }
            }
        }

        public void redisplay() {
            for (InspectorLabel label : _labels) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            final int elementRow = _rowToElementMap[row];
            InspectorLabel label = _labels[elementRow];
            if (label == null) {
                label = new MemoryRegionValueLabel(_inspection) {
                    @Override
                    public Value fetchValue() {
                        return teleVM().getElementValue(_elementKind, _objectReference, _startIndex + elementRow);
                    }
                };
                _labels[elementRow] = label;
            }
            return label;
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

    private long _lastRefreshEpoch = -1;

    public void refresh(long epoch, boolean force) {
        if (epoch > _lastRefreshEpoch || force) {
            _lastRefreshEpoch = epoch;
            _objectOrigin = _teleObject.getCurrentOrigin();
            // Update the mapping between array elements and displayed rows.
            if (_objectInspector.hideNullArrayElements()) {
                final int previousVisibleCount = _visibleElementCount;
                _visibleElementCount = 0;
                for (int index = 0; index < _arrayLength; index++) {
                    if (!teleVM().getElementValue(_elementKind, _objectReference, index).isZero()) {
                        _rowToElementMap[_visibleElementCount++] = index;
                    }
                }
                if (previousVisibleCount != _visibleElementCount) {
                    _model.fireTableDataChanged();
                }
            } else {
                if (_visibleElementCount != _arrayLength) {
                    // Previously hiding but no longer; reset map
                    for (int index = 0; index < _arrayLength; index++) {
                        _rowToElementMap[index] = index;
                    }
                    _visibleElementCount = _arrayLength;
                }
            }
            // Update selection, based on global address focus.
            final int oldSelectedRow = getSelectedRow();
            final int newRow = _model.addressToRow(focus().address());
            if (newRow >= 0) {
                getSelectionModel().setSelectionInterval(newRow, newRow);
            } else {
                if (oldSelectedRow >= 0) {
                    getSelectionModel().clearSelection();
                }
            }
            //
            for (TableColumn column : _columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(epoch, force);
            }
        }
    }


}
