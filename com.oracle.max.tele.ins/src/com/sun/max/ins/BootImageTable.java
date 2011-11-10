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
package com.sun.max.ins;


import static com.sun.max.platform.Platform.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.hosted.*;

/**
 * A table specialized for displaying {@link VMConfiguration}  information in the VM boot image.
 */
public final class BootImageTable extends InspectorTable {

    public BootImageTable(Inspection inspection, BootImageViewPreferences viewPreferences) {
        super(inspection);
        configureMemoryTable(new BootImageTableModel(inspection), new BootImageColumnModel(viewPreferences));
        setRowSelectionAllowed(false);
    }

    /**
     * A table for displaying configuration parameters for the VM instance.
     * <br>
     * This model contains the display labels, contrary to the usual convention.
     */
    private final class BootImageColumnModel extends InspectorTableColumnModel<BootImageColumnKind> {

        private BootImageColumnModel(BootImageViewPreferences viewPreferences) {
            super(BootImageColumnKind.values().length, viewPreferences);
            addColumn(BootImageColumnKind.NAME, new NameCellRenderer(inspection()), null);
            addColumn(BootImageColumnKind.VALUE, new ValueCellRenderer(), null);
            addColumn(BootImageColumnKind.REGION, new RegionCellRenderer(), null);
        }
    }

    private final class BootImageTableModel extends InspectorTableModel {

        private final List<String> names = new ArrayList<String>(50);
        private final List<InspectorLabel> valueLabels = new ArrayList<InspectorLabel>(50);
        private final List<InspectorLabel> regionLabels = new ArrayList<InspectorLabel>(50);
        private final InspectorLabel dummyLabel;

        BootImageTableModel(Inspection inspection) {
            super(inspection);
            final BootImage bootImage = vm().bootImage();
            final BootImage.Header header = bootImage.header;
            final VMConfiguration vmConfiguration = bootImage.vmConfiguration;
            final Platform platform = platform();
            final DataModel processorDataModel = platform.dataModel;
            dummyLabel = new PlainLabel(inspection, "");

            addRow("identification:", new DataLabel.IntAsHex(inspection(), header.identification), null);
            addRow("format version:", new DataLabel.IntAsDecimal(inspection(),  header.bootImageFormatVersion), null);
            addRow("random ID:", new DataLabel.IntAsHex(inspection(), header.randomID), null);

            addRow("build level:", new DataLabel.EnumAsText(inspection(), vmConfiguration.buildLevel), null);

            addRow("processor model:", new DataLabel.EnumAsText(inspection(), platform.cpu), null);
            addRow("instruction set:", new DataLabel.EnumAsText(inspection(), platform.isa), null);

            addRow("bits/word:", new DataLabel.IntAsDecimal(inspection(), processorDataModel.wordWidth.numberOfBits), null);
            addRow("endianness:", new DataLabel.EnumAsText(inspection(), processorDataModel.endianness), null);
            addRow("cache alignment:", new DataLabel.IntAsDecimal(inspection(), processorDataModel.cacheAlignment), null);

            addRow("operating system:", new DataLabel.EnumAsText(inspection(), platform.os), null);
            addRow("page size:", new DataLabel.IntAsDecimal(inspection(), platform.pageSize), null);

            addRow("reference scheme:", new JavaNameLabel(inspection(), vmConfiguration.referenceScheme().name(), vmConfiguration.referenceScheme().getClass().getName()), null);
            addRow("layout scheme:",  new JavaNameLabel(inspection(), vmConfiguration.layoutScheme().name, vmConfiguration.layoutScheme().getClass().getName()), null);
            addRow("heap scheme:", new JavaNameLabel(inspection(), vmConfiguration.heapScheme().name(), vmConfiguration.heapScheme().getClass().getName()), null);
            addRow("monitor scheme:", new JavaNameLabel(inspection(), vmConfiguration.monitorScheme().name(), vmConfiguration.monitorScheme().getClass().getName()), null);
            addRow("run scheme:", new JavaNameLabel(inspection(), vmConfiguration.runScheme().name(), vmConfiguration.runScheme().getClass().getName()), null);

            addRow("relocation data size:", new DataLabel.IntAsHex(inspection(), header.relocationDataSize), null);
            addRow("string data size:", new DataLabel.IntAsHex(inspection(), header.stringInfoSize), null);

            final Pointer bootImageStart = vm().bootImageStart();

            final Pointer bootHeapStart = bootImageStart;
            final Pointer bootHeapEnd = bootHeapStart.plus(header.heapSize);
            final String toolTipPrefix = "Value ";

            addRow("boot heap start:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootHeapStart, BootImageTable.this), new MemoryRegionValueLabel(inspection(), bootHeapStart, toolTipPrefix));
            addRow("boot heap size:", new DataLabel.IntAsHex(inspection(), header.heapSize), null);
            addRow("boot heap end:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootHeapEnd, BootImageTable.this), new MemoryRegionValueLabel(inspection(), bootHeapEnd, toolTipPrefix));

            final Pointer bootCodeStart = bootHeapEnd;
            final Pointer bootCodeEnd = bootCodeStart.plus(header.codeSize);

            addRow("boot code start:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootCodeStart, BootImageTable.this), new MemoryRegionValueLabel(inspection(), bootCodeStart, toolTipPrefix));
            addRow("boot code size:", new DataLabel.IntAsHex(inspection(), header.codeSize), null);
            addRow("boot code end:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootCodeEnd, BootImageTable.this), new MemoryRegionValueLabel(inspection(), bootCodeEnd, toolTipPrefix));

            final Pointer runMethodPointer = bootImageStart.plus(header.vmRunMethodOffset);
            addRow("MaxineVM.run():", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT,  runMethodPointer, BootImageTable.this), new MemoryRegionValueLabel(inspection(), runMethodPointer, toolTipPrefix));
            final Pointer threadRunMethodPointer = bootImageStart.plus(header.vmThreadRunMethodOffset);
            addRow("VmThread.run():", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT, threadRunMethodPointer, BootImageTable.this), new MemoryRegionValueLabel(inspection(), threadRunMethodPointer, toolTipPrefix));
            final Pointer threadAttachMethodPointer = bootImageStart.plus(header.vmThreadAttachMethodOffset);
            addRow("VmThread.attach():", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT, threadAttachMethodPointer, BootImageTable.this), new MemoryRegionValueLabel(inspection(), threadAttachMethodPointer, toolTipPrefix));
            final Pointer threadDetachMethodPointer = bootImageStart.plus(header.vmThreadDetachMethodOffset);
            addRow("VmThread.detach():", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT, threadDetachMethodPointer, BootImageTable.this), new MemoryRegionValueLabel(inspection(), threadDetachMethodPointer, toolTipPrefix));

            final Pointer classRegistryPointer = bootHeapStart.plus(header.classRegistryOffset);
            addRow("class registry:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE, classRegistryPointer, BootImageTable.this), new MemoryRegionValueLabel(inspection(), classRegistryPointer, toolTipPrefix));

            final Pointer dynamicHeapRegionsFieldPointer = bootHeapStart.plus(header.dynamicHeapRegionsArrayFieldOffset);
            addRow("dynamic heap regions array field:",
                            new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, dynamicHeapRegionsFieldPointer, BootImageTable.this),
                            new MemoryRegionValueLabel(inspection(), dynamicHeapRegionsFieldPointer, toolTipPrefix));

            final Pointer tlaListHead = bootImageStart.plus(header.tlaListHeadOffset);
            addRow("TLA list head:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, tlaListHead, BootImageTable.this), new MemoryRegionValueLabel(inspection(), tlaListHead, toolTipPrefix));
        }

        /**
         * Adds a row to the table.
         */
        private void addRow(String name, InspectorLabel valueLabel, InspectorLabel regionLabel) {
            names.add(name);
            valueLabels.add(valueLabel);
            regionLabels.add(regionLabel != null ? regionLabel : dummyLabel);
        }

        public int getColumnCount() {
            return BootImageColumnKind.values().length;
        }

        public int getRowCount() {
            return names.size();
        }

        public Object getValueAt(int row, int col) {
            switch (BootImageColumnKind.values()[col]) {
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
            switch (BootImageColumnKind.values()[c]) {
                case NAME:
                    return String.class;
                case VALUE:
                    return InspectorLabel.class;
                case REGION:
                    return InspectorLabel.class;
            }
            return null;
        }

        @Override
        public void refresh() {
            for (InspectorLabel label : valueLabels) {
                label.refresh(true);
            }
            for (InspectorLabel label : regionLabels) {
                label.refresh(true);
            }
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
