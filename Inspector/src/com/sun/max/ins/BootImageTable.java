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
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.prototype.*;


/**
 * A table specialized for displaying {@link VMConfiguration}  information in the VM boot image.
 *
 * @author Michael Van De Vanter
 */
public class BootImageTable extends InspectorTable {

    private final BootImageTableModel _model;
    private BootImageColumnModel _columnModel;
    private final TableColumn[] _columns;

    public BootImageTable(Inspection inspection, TableColumnVisibilityPreferences<BootImageColumnKind> viewPreferences) {
        super(inspection);
        _model = new BootImageTableModel(inspection);
        _columns = new TableColumn[BootImageColumnKind.VALUES.length()];
        _columnModel = new BootImageColumnModel(viewPreferences);

        setModel(_model);
        setColumnModel(_columnModel);
        setShowHorizontalLines(style().memoryTableShowHorizontalLines());
        setShowVerticalLines(style().memoryTableShowVerticalLines());
        setIntercellSpacing(style().memoryTableIntercellSpacing());
        setRowHeight(style().memoryTableRowHeight());
        setRowSelectionAllowed(false);
        setColumnSelectionAllowed(false);
        addMouseListener(new TableCellMouseClickAdapter(inspection(), this));
        refresh(vm().epoch(), true);
        JTableColumnResizer.adjustColumnPreferredWidths(this);
    }

    private long _lastRefreshEpoch = -1;

    public void refresh(long epoch, boolean force) {
        if (epoch > _lastRefreshEpoch || force) {
            _lastRefreshEpoch = epoch;
            _model.refresh(epoch, force);
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

    @Override
    protected JTableHeader createDefaultTableHeader() {
        // Custom table header with tooltips that describe the column data.
        return new JTableHeader(_columnModel) {
            @Override
            public String getToolTipText(MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = _columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = _columnModel.getColumn(index).getModelIndex();
                return BootImageColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    private final class BootImageColumnModel extends DefaultTableColumnModel {

        private final TableColumnVisibilityPreferences<BootImageColumnKind> _viewPreferences;

        private BootImageColumnModel(TableColumnVisibilityPreferences<BootImageColumnKind> viewPreferences) {
            _viewPreferences = viewPreferences;
            createColumn(BootImageColumnKind.NAME, new NameCellRenderer(inspection()), null);
            createColumn(BootImageColumnKind.VALUE, new ValueCellRenderer(), null);
            createColumn(BootImageColumnKind.REGION, new RegionCellRenderer(), null);
        }

        private void createColumn(BootImageColumnKind columnKind, TableCellRenderer renderer, TableCellEditor editor) {
            final int col = columnKind.ordinal();
            _columns[col] = new TableColumn(col, 0, renderer, editor);
            _columns[col].setHeaderValue(columnKind.label());
            _columns[col].setMinWidth(columnKind.minWidth());
            if (_viewPreferences.isVisible(columnKind)) {
                addColumn(_columns[col]);
            }
            _columns[col].setIdentifier(columnKind);
        }
    }

    private final class BootImageTableModel extends AbstractTableModel {

        private final AppendableIndexedSequence<String> _names = new ArrayListSequence<String>(50);
        private final AppendableIndexedSequence<InspectorLabel> _valueLabels = new ArrayListSequence<InspectorLabel>(50);
        private final AppendableIndexedSequence<InspectorLabel> _regionLabels = new ArrayListSequence<InspectorLabel>(50);
        private final InspectorLabel _dummyLabel;

        /**
         * Adds a row to the table.
         */
        private void addRow(String name, InspectorLabel valueLabel, InspectorLabel regionLabel) {
            _names.append(name);
            _valueLabels.append(valueLabel);
            _regionLabels.append(regionLabel != null ? regionLabel : _dummyLabel);
        }

        BootImageTableModel(Inspection inspection) {
            final BootImage bootImage = vm().bootImage();
            final BootImage.Header header = bootImage.header();
            final VMConfiguration vmConfiguration = bootImage.vmConfiguration();
            final Platform platform = vmConfiguration.platform();
            final ProcessorKind processorKind = platform.processorKind();
            final DataModel processorDataModel = processorKind.dataModel();
            _dummyLabel = new PlainLabel(inspection, "");

            addRow("identification:", new DataLabel.IntAsHex(inspection(), header._identification), null);
            addRow("version:", new DataLabel.IntAsDecimal(inspection(),  header._version), null);
            addRow("random ID:", new DataLabel.IntAsHex(inspection(), header._randomID), null);

            addRow("build level:", new DataLabel.EnumAsText(inspection(), vmConfiguration.buildLevel()), null);

            addRow("processor model:", new DataLabel.EnumAsText(inspection(), processorKind.processorModel()), null);
            addRow("instruction set:", new DataLabel.EnumAsText(inspection(), processorKind.instructionSet()), null);

            addRow("bits/word:", new DataLabel.IntAsDecimal(inspection(), processorDataModel.wordWidth().numberOfBits()), null);
            addRow("endianness:", new DataLabel.EnumAsText(inspection(), processorDataModel.endianness()), null);
            addRow("alignment:", new DataLabel.IntAsDecimal(inspection(), processorDataModel.alignment().numberOfBytes()), null);

            addRow("operating system:", new DataLabel.EnumAsText(inspection(), platform.operatingSystem()), null);
            addRow("page size:", new DataLabel.IntAsDecimal(inspection(), platform.pageSize()), null);

            addRow("grip scheme:", new JavaNameLabel(inspection(), vmConfiguration.gripScheme().name(), vmConfiguration.gripScheme().getClass().getName()), null);
            addRow("reference scheme:", new JavaNameLabel(inspection(), vmConfiguration.referenceScheme().name(), vmConfiguration.referenceScheme().getClass().getName()), null);
            addRow("layout scheme:",  new JavaNameLabel(inspection(), vmConfiguration.layoutScheme().name(), vmConfiguration.layoutScheme().getClass().getName()), null);
            addRow("heap scheme:", new JavaNameLabel(inspection(), vmConfiguration.heapScheme().name(), vmConfiguration.heapScheme().getClass().getName()), null);
            addRow("monitor scheme:", new JavaNameLabel(inspection(), vmConfiguration.monitorScheme().name(), vmConfiguration.monitorScheme().getClass().getName()), null);
            addRow("compilation scheme:", new JavaNameLabel(inspection(), vmConfiguration.compilationScheme().name(), vmConfiguration.compilationScheme().getClass().getName()), null);
            addRow("optimizing compiler scheme:", new JavaNameLabel(inspection(), vmConfiguration.compilerScheme().name(), vmConfiguration.compilerScheme().getClass().getName()), null);
            addRow("JIT compiler scheme:", new JavaNameLabel(inspection(), vmConfiguration.jitScheme().name(), vmConfiguration.jitScheme().getClass().getName()), null);
            addRow("interpreter scheme:", new JavaNameLabel(inspection(), vmConfiguration.interpreterScheme().name(), vmConfiguration.interpreterScheme().getClass().getName()), null);
            addRow("trampoline scheme:", new JavaNameLabel(inspection(), vmConfiguration.trampolineScheme().name(), vmConfiguration.trampolineScheme().getClass().getName()), null);
            addRow("target ABIs scheme:", new JavaNameLabel(inspection(), vmConfiguration.targetABIsScheme().name(), vmConfiguration.targetABIsScheme().getClass().getName()), null);
            addRow("run scheme:", new JavaNameLabel(inspection(), vmConfiguration.runScheme().name(), vmConfiguration.runScheme().getClass().getName()), null);

            addRow("relocation scheme:", new DataLabel.IntAsHex(inspection(), header._relocationScheme), null);
            addRow("relocation data size:", new DataLabel.IntAsHex(inspection(), header._relocationDataSize), null);
            addRow("string data size:", new DataLabel.IntAsHex(inspection(), header._stringInfoSize), null);

            final Pointer bootImageStart = vm().bootImageStart();

            final Pointer bootHeapStart = bootImageStart;
            final Pointer bootHeapEnd = bootHeapStart.plus(header._bootHeapSize);

            addRow("boot heap start:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootHeapStart), new MemoryRegionValueLabel(inspection(), bootHeapStart));
            addRow("boot heap size:", new DataLabel.IntAsHex(inspection(), header._bootHeapSize), null);
            addRow("boot heap end:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootHeapEnd), new MemoryRegionValueLabel(inspection(), bootHeapEnd));

            final Pointer bootCodeStart = bootHeapEnd;
            final Pointer bootCodeEnd = bootCodeStart.plus(header._bootCodeSize);

            addRow("boot code start:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootCodeStart), new MemoryRegionValueLabel(inspection(), bootCodeStart));
            addRow("boot code size:", new DataLabel.IntAsHex(inspection(), header._bootCodeSize), null);
            addRow("boot code end:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootCodeEnd), new MemoryRegionValueLabel(inspection(), bootCodeEnd));

            addRow("code cache size:", new DataLabel.IntAsHex(inspection(), header._codeCacheSize), null);
            addRow("thread local space size:", new DataLabel.IntAsHex(inspection(), header._vmThreadLocalsSize), null);

            final Pointer runMethodPointer = bootImageStart.plus(header._vmRunMethodOffset);
            addRow("vmStartupMethod:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT,  runMethodPointer), new MemoryRegionValueLabel(inspection(), runMethodPointer));
            final Pointer threadRunMethodPointer = bootImageStart.plus(header._vmThreadRunMethodOffset);
            addRow("vmThreadRunMethod:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT, threadRunMethodPointer), new MemoryRegionValueLabel(inspection(), threadRunMethodPointer));
            final Pointer runSchemeRunMethodPointer = bootImageStart.plus(header._runSchemeRunMethodOffset);
            addRow("runSchemeRunMethod:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT, runSchemeRunMethodPointer), new MemoryRegionValueLabel(inspection(), runSchemeRunMethodPointer));

            final Pointer classRegistryPointer = bootHeapStart.plus(header._classRegistryOffset);
            addRow("class registry:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE, classRegistryPointer), new MemoryRegionValueLabel(inspection(), classRegistryPointer));
            final Pointer bootHeapPointer = bootHeapStart.plus(header._heapRegionsPointerOffset);
            addRow("heap regions pointer:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootHeapPointer), new MemoryRegionValueLabel(inspection(), bootHeapPointer));
            final Pointer bootCodePointer = bootCodeStart.plus(header._codeRegionsPointerOffset);
            addRow("code regions pointer:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootCodePointer), new MemoryRegionValueLabel(inspection(), bootCodePointer));

            final Pointer messengerInfoPointer = bootImageStart.plus(header._messengerInfoOffset);
            addRow("messenger info pointer:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, messengerInfoPointer), new MemoryRegionValueLabel(inspection(), messengerInfoPointer));
            final Pointer threadSpecificsListPointer = bootImageStart.plus(header._threadSpecificsListOffset);
            addRow("thread specifics list pointer:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, threadSpecificsListPointer), new MemoryRegionValueLabel(inspection(), threadSpecificsListPointer));


        }

        public void refresh(long epoch, boolean force) {
            for (InspectorLabel label : _valueLabels) {
                label.refresh(epoch, force);
            }
            for (InspectorLabel label : _regionLabels) {
                label.refresh(epoch, force);
            }
        }

        @Override
        public int getColumnCount() {
            return BootImageColumnKind.VALUES.length();
        }

        @Override
        public int getRowCount() {
            return _names.length();
        }

        @Override
        public Object getValueAt(int row, int col) {
            switch (BootImageColumnKind.VALUES.get(col)) {
                case NAME:
                    return _names.get(row);
                case VALUE:
                    return _valueLabels.get(row);
                case REGION:
                    return _regionLabels.get(row);
            }
            return null;
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            switch (BootImageColumnKind.VALUES.get(c)) {
                case NAME:
                    return String.class;
                case VALUE:
                    return InspectorLabel.class;
                case REGION:
                    return InspectorLabel.class;
            }
            return null;
        }
    }

    private final class NameCellRenderer extends TextLabel implements TableCellRenderer {

        NameCellRenderer(Inspection inspection) {
            super(inspection, null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final String name = (String) value;
            setText(name);
            return this;
        }

    }

    private final class ValueCellRenderer implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return (InspectorLabel) value;
        }
    }

    private final class RegionCellRenderer implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return (InspectorLabel) value;
        }
    }
}
