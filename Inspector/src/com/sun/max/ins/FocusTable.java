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
package com.sun.max.ins;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.memory.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.value.*;


/**
 * A table specialized for displaying aspects of the current {@link InspectorFocus}.
 * Intended for Inspector testing.
 *
 * @author Michael Van De Vanter
 */
public class FocusTable extends InspectorTable implements ViewFocusListener {

    /**
     * Columns to display when viewing focus data.
     */
    public enum FocusColumnKind {

        NAME("Name", "Inspector user focus kind", -1),
        VALUE("Value", "Value/status of Inspector user focus", 25);

        private final String _label;
        private final String _toolTipText;
        private final int _minWidth;

        private FocusColumnKind(String label, String toolTipText, int minWidth) {
            _label = label;
            _toolTipText = toolTipText;
            _minWidth = minWidth;
        }

        /**
         * @return text to appear in the column header
         */
        public String label() {
            return _label;
        }

        /**
         * @return text to appear in the column header's toolTip, null if none specified.
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
            return _label;
        }

        public static final IndexedSequence<FocusColumnKind> VALUES = new ArraySequence<FocusColumnKind>(values());
    }


    /**
     * Rows to display when viewing focus data: one for each aspect of the focus.
     */
    public enum FocusRowKind {

        THREAD("Thread", "Current thread of interest in the Inspector"),
        FRAME("Stack Frame", "Current stack frame of interest in the Inspector"),
        CODE("Code Location", "Current code location of interest in the Inspector"),
        BREAKPOINT("Breakpoint", "Current breakpoint of interest in the Inspector"),
        ADDRESS("Memory Address", "Current memory address of interest in the Inspector"),
        OBJECT("Heap Object", "Current heap object of interest in the Inspector"),
        REGION("Memory Region", "Current memory region of interest in the Inspector");

        private final String _label;
        private final String _toolTipText;

        private FocusRowKind(String label, String toolTipText) {
            _label = label;
            _toolTipText = toolTipText;
        }

        /**
         * @return text to appear in the row's name field.
         */
        public String label() {
            return _label;
        }

        /**
         * @return text to appear in the row's name field toolTip.
         */
        public String toolTipText() {
            return _toolTipText;
        }

        @Override
        public String toString() {
            return _label;
        }

        public static final IndexedSequence<FocusRowKind> VALUES = new ArraySequence<FocusRowKind>(values());
    }

    private final FocusTableModel _model;
    private final FocusColumnModel _columnModel;
    private final TableColumn[] _columns;

    FocusTable(Inspection inspection) {
        super(inspection);
        _model = new FocusTableModel();
        _columns = new TableColumn[FocusColumnKind.VALUES.length()];
        _columnModel = new FocusColumnModel();

        setModel(_model);
        setColumnModel(_columnModel);
        setShowHorizontalLines(style().defaultTableShowHorizontalLines());
        setShowVerticalLines(style().defaultTableShowVerticalLines());
        setIntercellSpacing(style().defaultTableIntercellSpacing());
        setRowHeight(style().defaultTableRowHeight());
        setRowSelectionAllowed(false);
        setColumnSelectionAllowed(false);
        //setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //addMouseListener(new FocusInspectorMouseClickAdapter(inspection()));

        refresh(teleVM().epoch(), true);
        JTableColumnResizer.adjustColumnPreferredWidths(this);
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(_columnModel) {
            @Override
            public String getToolTipText(MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = _columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = _columnModel.getColumn(index).getModelIndex();
                return FocusColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    private final class FocusColumnModel extends DefaultTableColumnModel {

        private FocusColumnModel() {
            createColumn(FocusColumnKind.NAME, new NameCellRenderer(inspection()));
            createColumn(FocusColumnKind.VALUE, new ValueCellRenderer(inspection()));
        }

        private void createColumn(FocusColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            _columns[col] = new TableColumn(col, 0, renderer, null);
            _columns[col].setHeaderValue(columnKind.label());
            _columns[col].setMinWidth(columnKind.minWidth());
            addColumn(_columns[col]);
            _columns[col].setIdentifier(columnKind);
        }
    }


    private final class FocusTableModel extends AbstractTableModel {

        public FocusTableModel() {
        }

        void refresh() {
            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return FocusColumnKind.VALUES.length();
        }

        @Override
        public int getRowCount() {
            return FocusRowKind.VALUES.length();
        }

        @Override
        public Object getValueAt(int row, int col) {
            // Don't use cell values; all interaction is driven by row number.
            return null;
        }

    }

    /**
     * Renders the name of an aspect of the user focus.
     */
    private final class NameCellRenderer extends PlainLabel implements TableCellRenderer {

        NameCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final FocusRowKind focusRowKind = FocusRowKind.VALUES.get(row);
            setText(focusRowKind.label());
            setToolTipText(focusRowKind.toolTipText());
            return this;
        }
    }


    /**
     * Renders the current value of an aspect of the user focus.
     */
    private final class ValueCellRenderer  implements TableCellRenderer, Prober {

        // A "value" label per row, each suitable for the particular kind of value.
        private InspectorLabel[] _labels = new InspectorLabel[FocusRowKind.VALUES.length()];

        public ValueCellRenderer(Inspection inspection) {
            _labels[FocusRowKind.THREAD.ordinal()] = new JavaNameLabel(inspection, "") {
                @Override
                public void refresh(long epoch, boolean force) {
                    final TeleNativeThread teleNativeThread = inspection().focus().thread();
                    if (teleNativeThread == null) {
                        setValue("null", "No thread focus");
                    } else {
                        final String longName = inspection().nameDisplay().longName(teleNativeThread);
                        setValue(longName, "Thread focus = " + longName);
                    }
                }
            };
            _labels[FocusRowKind.FRAME.ordinal()] = new PlainLabel(inspection, "") {
                @Override
                public void refresh(long epoch, boolean force) {
                    final StackFrame stackFrame = inspection().focus().stackFrame();
                    if (stackFrame == null) {
                        setValue(null, "No stack frame focus");
                    } else {
                        final TargetMethod targetMethod = stackFrame.targetMethod();
                        final String name = targetMethod == null ? "nativeMethod: 0x" + stackFrame.instructionPointer().toHexString() : targetMethod.toString();
                        setValue(name, "Stack frame focus = " + name);
                    }
                }
            };
            _labels[FocusRowKind.CODE.ordinal()] = new PlainLabel(inspection, "") {
                @Override
                public void refresh(long epoch, boolean force) {
                    final TeleCodeLocation teleCodeLocation = inspection().focus().codeLocation();
                    if (teleCodeLocation == null) {
                        setValue(null, "No code location focus");
                    } else {
                        final String longName = inspection().nameDisplay().longName(teleCodeLocation);
                        setValue(longName, "Code location focus = " + longName);
                    }
                }
            };
            _labels[FocusRowKind.BREAKPOINT.ordinal()] = new PlainLabel(inspection, "") {
                @Override
                public void refresh(long epoch, boolean force) {
                    final TeleBreakpoint teleBreakpoint = inspection().focus().breakpoint();
                    if (teleBreakpoint == null) {
                        setValue(null, "No code location focus");
                    } else {
                        final String longName = inspection().nameDisplay().longName(teleBreakpoint.teleCodeLocation());
                        setValue(longName, "Breakpoint focus = " + longName);
                    }
                }
            };
            _labels[FocusRowKind.ADDRESS.ordinal()] = new WordValueLabel(inspection, WordValueLabel.ValueMode.WORD) {
                @Override
                public Value fetchValue() {
                    Address address = inspection().focus().address();
                    if (address == null) {
                        address = Address.zero();
                    }
                    return new WordValue(address);
                }
            };
            _labels[FocusRowKind.OBJECT.ordinal()] = new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE) {
                @Override
                public Value fetchValue() {
                    final TeleObject teleObject = inspection().focus().heapObject();
                    Address address = Address.zero();
                    if (teleObject != null) {
                        address = teleObject.getCurrentOrigin();
                    }
                    return new WordValue(address);
                }
            };
            _labels[FocusRowKind.REGION.ordinal()] = new PlainLabel(inspection, "") {
                @Override
                public void refresh(long epoch, boolean force) {
                    final MemoryRegion memoryRegion = inspection().focus().memoryRegion();
                    if (memoryRegion == null) {
                        setValue(null, "No memory region focus");
                    } else {
                        setValue(memoryRegion.description(), "Memory region focus = " + memoryRegion.description());
                    }
                }
            };
        }

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

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (_labels[row] != null) {
                return _labels[row];
            }
            return new PlainLabel(inspection(), "unimplemented");
        }
    }

    public void codeLocationFocusSet(TeleCodeLocation teleCodeLocation, boolean interactiveForNative) {
        refresh(teleVM().epoch(), true);
    }

    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
        refresh(teleVM().epoch(), true);
    }

    public void stackFrameFocusChanged(StackFrame oldStackFrame, TeleNativeThread threadForStackFrame, StackFrame stackFrame) {
        refresh(teleVM().epoch(), true);
    }

    public void addressFocusChanged(Address oldAddress, Address address) {
        refresh(teleVM().epoch(), true);
    }

    public void memoryRegionFocusChanged(MemoryRegion oldMemoryRegion, MemoryRegion memoryRegion) {
        refresh(teleVM().epoch(), true);
    }

    public void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint) {
        refresh(teleVM().epoch(), true);
    }

    public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
        refresh(teleVM().epoch(), true);
    }

    public void redisplay() {
    }

    public void refresh(long epoch, boolean force) {
        // Pay no attention to VM epoch; only update when forced, i.e. when focus changes
        if (force) {
            for (TableColumn column : _columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(epoch, force);
            }
            _model.refresh();
        }
    }

}
