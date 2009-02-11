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

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
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

    private int[] _rowToElementMap;  // display row --> element index
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
        setShowHorizontalLines(style().objectTableShowHorizontalLines());
        setShowVerticalLines(style().objectTableShowVerticalLines());
        setIntercellSpacing(style().objectTableIntercellSpacing());
        setRowHeight(style().objectTableRowHeight());
        addMouseListener(new TableCellMouseClickAdapter(_inspection, this));

        refresh(_inspection.teleVM().epoch(), true);
        JTableColumnResizer.adjustColumnPreferredWidths(this);
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
     * Models the words/rows in a sequence of array elements;
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
    }

    /**
     * A column model for array elements, to be used in an {@link ObjectInspector}.
     * Column selection is driven by choices in the parent {@link ObjectInspector}.
     * This implementation cannot update column choices dynamically.
     */
    private final class ArrayElementsTableColumnModel extends DefaultTableColumnModel {

        ArrayElementsTableColumnModel(ObjectInspector objectInspector) {
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

    private final class AddressRenderer extends LocationLabel.AsAddressWithOffset implements TableCellRenderer {

        AddressRenderer() {
            super(_inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

            final int index = _rowToElementMap[row];
            setValue(_startOffset + (index * _elementSize), _objectOrigin);
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public PositionRenderer() {
            super(_inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final int index = _rowToElementMap[row];
            setValue(_startOffset + (index * _elementSize), _objectOrigin);
            return this;
        }
    }

    private final class NameRenderer extends LocationLabel.AsIndex implements TableCellRenderer {

        public NameRenderer() {
            super(_inspection, _indexPrefix, 0, 0, Address.zero());

        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final int index = _rowToElementMap[row];
            setValue(index, _startOffset + (index * _elementSize), _objectOrigin);
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
            final int index = _rowToElementMap[row];
            InspectorLabel label = _labels[index];
            if (label == null) {
                label = new MemoryRegionValueLabel(_inspection) {
                    @Override
                    public Value fetchValue() {
                        return teleVM().getElementValue(_elementKind, _objectReference, _startIndex + index);
                    }
                };
                _labels[index] = label;
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
            for (TableColumn column : _columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(epoch, force);
            }
        }
    }


}
