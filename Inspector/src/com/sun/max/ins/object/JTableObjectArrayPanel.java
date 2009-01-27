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
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;


/**
 * A table-based panel that displays array members in a Maxine low level heap object (in arrays or hybrids) as a list of rows.
 *
 * @author Michael Van De Vanter
 */
public class JTableObjectArrayPanel extends InspectorPanel {

    /**
     * Defines the columns supported by the inspector; the view includes one of each
     * kind.  The visibility of them, however, may be changed by the user.
     */
    private enum ArrayElementColumnKind {
        ADDRESS("Addr.", "Memory address of element", -1),
        POSITION("Pos.", "Relative position of element (bytes)", 10),
        NAME("Elem.", "Array element name", 10),
        VALUE("Value", "Element value", 5),
        REGION("Region", "Memory region pointed to by value", -1);

        private final String _columnLabel;
        private final String _toolTipText;
        private final int _minWidth;

        private ArrayElementColumnKind(String label, String toolTipText, int minWidth) {
            _columnLabel = label;
            _toolTipText = toolTipText;
            _minWidth = minWidth;
        }

        /**
         * @return text to appear in the column header
         */
        public String label() {
            return _columnLabel;
        }

        /**
         * @return text to appear in the column header's toolTip, null if none specified
         */
        public String toolTipText() {
            return _toolTipText;
        }

        /**
         * @return minimum width allowed for this column when resized by user; -1 if none specified.
         */
        public int minWidth() {
            return _minWidth;
        }

        @Override
        public String toString() {
            return _columnLabel;
        }

        public static final IndexedSequence<ArrayElementColumnKind> VALUES = new ArraySequence<ArrayElementColumnKind>(values());
    }

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

    private final JTable _table;
    private final MyTableModel _model;
    private final MyTableColumnModel _columnModel;
    private final TableColumn[] _columns;

    JTableObjectArrayPanel(final ObjectInspector objectInspector, final Kind kind, int startOffset, int startIndex, int length, String indexPrefix, WordValueLabel.ValueMode wordValueMode) {
        super(objectInspector.inspection(), new BorderLayout());
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

        _model = new MyTableModel();
        _columns = new TableColumn[ArrayElementColumnKind.VALUES.length()];
        _columnModel = new MyTableColumnModel();
        _table = new MyTable(_model, _columnModel);
        _table.setOpaque(true);
        _table.setBackground(style().defaultBackgroundColor());
        _table.setFillsViewportHeight(true);
        _table.setShowHorizontalLines(false);
        _table.setShowVerticalLines(false);
        _table.setIntercellSpacing(new Dimension(0, 0));
        _table.setRowHeight(20);
        _table.addMouseListener(new MyInspectorMouseClickAdapter(_inspection));

        refresh(_inspection.teleVM().epoch(), true);
        JTableColumnResizer.adjustColumnPreferredWidths(_table);
        final JScrollPane scrollPane = new JScrollPane(_table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBackground(style().defaultBackgroundColor());
        scrollPane.setOpaque(true);
        add(_table.getTableHeader(), BorderLayout.NORTH);
        add(_table, BorderLayout.CENTER);
    }

    private final class MyTableModel extends AbstractTableModel {

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

    private final class MyTable extends JTable {

        MyTable(TableModel model, TableColumnModel tableColumnModel) {
            super(model, tableColumnModel);
        }

        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(_columnModel) {
                @Override
                public String getToolTipText(MouseEvent mouseEvent) {
                    final Point p = mouseEvent.getPoint();
                    final int index = _columnModel.getColumnIndexAtX(p.x);
                    final int modelIndex = _columnModel.getColumn(index).getModelIndex();
                    return ArrayElementColumnKind.VALUES.get(modelIndex).toolTipText();
                }
            };
        }
    }

    /**
     * A column model for array elements, to be used in an {@link ObjectInspector}.
     * Column selection is driven by choices in the parent {@link ObjectInspector}.
     * This implementation cannot update column choices dynamically.
     */
    private final class MyTableColumnModel extends DefaultTableColumnModel {

        MyTableColumnModel() {
            createColumn(ArrayElementColumnKind.ADDRESS, new AddressRenderer(), _objectInspector.showAddresses());
            createColumn(ArrayElementColumnKind.POSITION, new PositionRenderer(), _objectInspector.showOffsets());
            createColumn(ArrayElementColumnKind.NAME, new NameRenderer(), true);
            createColumn(ArrayElementColumnKind.VALUE, new ValueRenderer(), true);
            createColumn(ArrayElementColumnKind.REGION, new RegionRenderer(), _objectInspector.showMemoryRegions());
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

    private final class MyInspectorMouseClickAdapter extends InspectorMouseClickAdapter {
        MyInspectorMouseClickAdapter(Inspection inspection) {
            super(inspection);
        }
        @Override
        public void procedure(final MouseEvent mouseEvent) {
            // Locate the renderer under the event location, and pass along the mouse click if appropriate
            final Point p = mouseEvent.getPoint();
            final int hitColumnIndex = _table.columnAtPoint(p);
            final int hitRowIndex = _table.rowAtPoint(p);
            if ((hitColumnIndex != -1) && (hitRowIndex != -1)) {
                final TableCellRenderer tableCellRenderer = _table.getCellRenderer(hitRowIndex, hitColumnIndex);
                final Object cellValue = _table.getValueAt(hitRowIndex, hitColumnIndex);
                final Component component = tableCellRenderer.getTableCellRendererComponent(_table, cellValue, false, true, hitRowIndex, hitColumnIndex);
                if (component != null) {
                    component.dispatchEvent(mouseEvent);
                }
            }
        }
    }

    private final class AddressRenderer extends LocationLabel.AsAddressWithOffset implements TableCellRenderer {

        AddressRenderer() {
            super(_inspection, 0, Address.zero());
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

            final int index = _rowToElementMap[row];
            setValue(_startOffset + (index * _elementSize), _objectOrigin);
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public PositionRenderer() {
            super(_inspection, 0, Address.zero());
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

        @Override
        public void redisplay() {
            for (InspectorLabel label : _labels) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        @Override
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

        @Override
        public void redisplay() {
            for (InspectorLabel label : _labels) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        @Override
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

    @Override
    public void redisplay() {
        for (TableColumn column : _columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        invalidate();
        repaint();
    }

    private long _lastRefreshEpoch = -1;

    @Override
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
