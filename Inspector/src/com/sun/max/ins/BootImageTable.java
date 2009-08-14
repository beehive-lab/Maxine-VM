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
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.prototype.*;


/**
 * A table specialized for displaying {@link VMConfiguration}  information in the VM boot image.
 *
 * @author Michael Van De Vanter
 */
public final class BootImageTable extends InspectorTable {

    private final BootImageTableModel model;
    private BootImageColumnModel columnModel;
    private final TableColumn[] columns;

    private MaxVMState lastRefreshedState = null;

    public BootImageTable(Inspection inspection, TableColumnVisibilityPreferences<BootImageColumnKind> viewPreferences) {
        super(inspection);
        model = new BootImageTableModel(inspection);
        columns = new TableColumn[BootImageColumnKind.VALUES.length()];
        this.columnModel = new BootImageColumnModel(viewPreferences);
        configureMemoryTable(model, columnModel);
        setRowSelectionAllowed(false);
        addMouseListener(new TableCellMouseClickAdapter(inspection(), this));
    }

    public void refresh(boolean force) {
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            model.refresh(force);
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

    @Override
    protected JTableHeader createDefaultTableHeader() {
        // Custom table header with tooltips that describe the column data.
        return new JTableHeader(columnModel) {
            @Override
            public String getToolTipText(MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = columnModel.getColumn(index).getModelIndex();
                return BootImageColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    private final class BootImageColumnModel extends DefaultTableColumnModel {

        private final TableColumnVisibilityPreferences<BootImageColumnKind> viewPreferences;

        private BootImageColumnModel(TableColumnVisibilityPreferences<BootImageColumnKind> viewPreferences) {
            this.viewPreferences = viewPreferences;
            createColumn(BootImageColumnKind.NAME, new NameCellRenderer(inspection()), null);
            createColumn(BootImageColumnKind.VALUE, new ValueCellRenderer(), null);
            createColumn(BootImageColumnKind.REGION, new RegionCellRenderer(), null);
        }

        private void createColumn(BootImageColumnKind columnKind, TableCellRenderer renderer, TableCellEditor editor) {
            final int col = columnKind.ordinal();
            columns[col] = new TableColumn(col, 0, renderer, editor);
            columns[col].setHeaderValue(columnKind.label());
            columns[col].setMinWidth(columnKind.minWidth());
            if (viewPreferences.isVisible(columnKind)) {
                addColumn(columns[col]);
            }
            columns[col].setIdentifier(columnKind);
        }
    }

    private final class BootImageTableModel extends AbstractTableModel {

        private final AppendableIndexedSequence<String> names = new ArrayListSequence<String>(50);
        private final AppendableIndexedSequence<InspectorLabel> valueLabels = new ArrayListSequence<InspectorLabel>(50);
        private final AppendableIndexedSequence<InspectorLabel> regionLabels = new ArrayListSequence<InspectorLabel>(50);
        private final InspectorLabel dummyLabel;

        /**
         * Adds a row to the table.
         */
        private void addRow(String name, InspectorLabel valueLabel, InspectorLabel regionLabel) {
            names.append(name);
            valueLabels.append(valueLabel);
            regionLabels.append(regionLabel != null ? regionLabel : dummyLabel);
        }

        BootImageTableModel(Inspection inspection) {
            final BootImage bootImage = maxVM().bootImage();
            final BootImage.Header header = bootImage.header();
            final VMConfiguration vmConfiguration = bootImage.vmConfiguration();
            final Platform platform = vmConfiguration.platform();
            final ProcessorKind processorKind = platform.processorKind;
            final DataModel processorDataModel = processorKind.dataModel;
            dummyLabel = new PlainLabel(inspection, "");

            addRow("identification:", new DataLabel.IntAsHex(inspection(), header.identification), null);
            addRow("version:", new DataLabel.IntAsDecimal(inspection(),  header.version), null);
            addRow("random ID:", new DataLabel.IntAsHex(inspection(), header.randomID), null);

            addRow("build level:", new DataLabel.EnumAsText(inspection(), vmConfiguration.buildLevel()), null);

            addRow("processor model:", new DataLabel.EnumAsText(inspection(), processorKind.processorModel), null);
            addRow("instruction set:", new DataLabel.EnumAsText(inspection(), processorKind.instructionSet), null);

            addRow("bits/word:", new DataLabel.IntAsDecimal(inspection(), processorDataModel.wordWidth.numberOfBits), null);
            addRow("endianness:", new DataLabel.EnumAsText(inspection(), processorDataModel.endianness), null);
            addRow("cache alignment:", new DataLabel.IntAsDecimal(inspection(), processorDataModel.cacheAlignment), null);

            addRow("operating system:", new DataLabel.EnumAsText(inspection(), platform.operatingSystem), null);
            addRow("page size:", new DataLabel.IntAsDecimal(inspection(), platform.pageSize), null);

            addRow("grip scheme:", new JavaNameLabel(inspection(), vmConfiguration.gripScheme().name(), vmConfiguration.gripScheme().getClass().getName()), null);
            addRow("reference scheme:", new JavaNameLabel(inspection(), vmConfiguration.referenceScheme().name(), vmConfiguration.referenceScheme().getClass().getName()), null);
            addRow("layout scheme:",  new JavaNameLabel(inspection(), vmConfiguration.layoutScheme().name(), vmConfiguration.layoutScheme().getClass().getName()), null);
            addRow("heap scheme:", new JavaNameLabel(inspection(), vmConfiguration.heapScheme().name(), vmConfiguration.heapScheme().getClass().getName()), null);
            addRow("monitor scheme:", new JavaNameLabel(inspection(), vmConfiguration.monitorScheme().name(), vmConfiguration.monitorScheme().getClass().getName()), null);
            addRow("compilation scheme:", new JavaNameLabel(inspection(), vmConfiguration.compilationScheme().name(), vmConfiguration.compilationScheme().getClass().getName()), null);
            addRow("optimizing compiler scheme:", new JavaNameLabel(inspection(), vmConfiguration.compilerScheme().name(), vmConfiguration.compilerScheme().getClass().getName()), null);
            addRow("JIT compiler scheme:", new JavaNameLabel(inspection(), vmConfiguration.jitScheme().name(), vmConfiguration.jitScheme().getClass().getName()), null);
            addRow("trampoline scheme:", new JavaNameLabel(inspection(), vmConfiguration.trampolineScheme().name(), vmConfiguration.trampolineScheme().getClass().getName()), null);
            addRow("target ABIs scheme:", new JavaNameLabel(inspection(), vmConfiguration.targetABIsScheme().name(), vmConfiguration.targetABIsScheme().getClass().getName()), null);
            addRow("run scheme:", new JavaNameLabel(inspection(), vmConfiguration.runScheme().name(), vmConfiguration.runScheme().getClass().getName()), null);

            addRow("relocation scheme:", new DataLabel.IntAsHex(inspection(), header.relocationScheme), null);
            addRow("relocation data size:", new DataLabel.IntAsHex(inspection(), header.relocationDataSize), null);
            addRow("string data size:", new DataLabel.IntAsHex(inspection(), header.stringInfoSize), null);

            final Pointer bootImageStart = maxVM().bootImageStart();

            final Pointer bootHeapStart = bootImageStart;
            final Pointer bootHeapEnd = bootHeapStart.plus(header.bootHeapSize);

            addRow("boot heap start:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootHeapStart, BootImageTable.this), new MemoryRegionValueLabel(inspection(), bootHeapStart));
            addRow("boot heap size:", new DataLabel.IntAsHex(inspection(), header.bootHeapSize), null);
            addRow("boot heap end:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootHeapEnd, BootImageTable.this), new MemoryRegionValueLabel(inspection(), bootHeapEnd));

            final Pointer bootCodeStart = bootHeapEnd;
            final Pointer bootCodeEnd = bootCodeStart.plus(header.bootCodeSize);

            addRow("boot code start:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootCodeStart, BootImageTable.this), new MemoryRegionValueLabel(inspection(), bootCodeStart));
            addRow("boot code size:", new DataLabel.IntAsHex(inspection(), header.bootCodeSize), null);
            addRow("boot code end:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootCodeEnd, BootImageTable.this), new MemoryRegionValueLabel(inspection(), bootCodeEnd));

            addRow("code cache size:", new DataLabel.IntAsHex(inspection(), header.codeCacheSize), null);

            final Pointer runMethodPointer = bootImageStart.plus(header.vmRunMethodOffset);
            addRow("vmStartupMethod:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT,  runMethodPointer, BootImageTable.this), new MemoryRegionValueLabel(inspection(), runMethodPointer));
            final Pointer threadRunMethodPointer = bootImageStart.plus(header.vmThreadRunMethodOffset);
            addRow("vmThreadRunMethod:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT, threadRunMethodPointer, BootImageTable.this), new MemoryRegionValueLabel(inspection(), threadRunMethodPointer));
            final Pointer runSchemeRunMethodPointer = bootImageStart.plus(header.runSchemeRunMethodOffset);
            addRow("runSchemeRunMethod:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT, runSchemeRunMethodPointer, BootImageTable.this), new MemoryRegionValueLabel(inspection(), runSchemeRunMethodPointer));

            final Pointer classRegistryPointer = bootHeapStart.plus(header.classRegistryOffset);
            addRow("class registry:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE, classRegistryPointer, BootImageTable.this), new MemoryRegionValueLabel(inspection(), classRegistryPointer));
            final Pointer bootHeapPointer = bootHeapStart.plus(header.heapRegionsPointerOffset);
            addRow("heap regions pointer:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootHeapPointer, BootImageTable.this), new MemoryRegionValueLabel(inspection(), bootHeapPointer));

            final Pointer messengerInfoPointer = bootImageStart.plus(header.messengerInfoOffset);
            addRow("messenger info pointer:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, messengerInfoPointer, BootImageTable.this), new MemoryRegionValueLabel(inspection(), messengerInfoPointer));
            final Pointer vmThreadLocalsListHead = bootImageStart.plus(header.threadLocalsListHeadOffset);
            addRow("VM thread locals list head:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, vmThreadLocalsListHead, BootImageTable.this), new MemoryRegionValueLabel(inspection(), vmThreadLocalsListHead));


        }

        public void refresh(boolean force) {
            for (InspectorLabel label : valueLabels) {
                label.refresh(force);
            }
            for (InspectorLabel label : regionLabels) {
                label.refresh(force);
            }
        }

        public int getColumnCount() {
            return BootImageColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return names.length();
        }

        public Object getValueAt(int row, int col) {
            switch (BootImageColumnKind.VALUES.get(col)) {
                case NAME:
                    return names.get(row);
                case VALUE:
                    return valueLabels.get(row);
                case REGION:
                    return regionLabels.get(row);
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

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final String name = (String) value;
            setText(name);
            return this;
        }

    }

    private final class ValueCellRenderer implements TableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return (InspectorLabel) value;
        }
    }

    private final class RegionCellRenderer implements TableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return (InspectorLabel) value;
        }
    }
}
