/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.value.*;

/**
 * A table specialized for displaying information derived from a VM's heap region info, to ease GC debugging.
 */
final class HeapRegionInfoTable extends InspectorTable {

    static public enum HeapRegionInfoColumnKind implements ColumnKind {
        NAME("Info", "Heap Region Info"),
        VALUE("Value", "value");
        private final String label;
        private final String toolTipText;

        private HeapRegionInfoColumnKind(String label, String toolTipText) {
            this.label = label;
            this.toolTipText = toolTipText;
        }
        @Override
        public String label() {
            return label;
        }

        @Override
        public String toolTipText() {
            return toolTipText;
        }

        @Override
        public boolean canBeMadeInvisible() {
            return false;
        }

        @Override
        public boolean defaultVisibility() {
            return true;
        }

        @Override
        public int minWidth() {
            return -1;
        }
    }

    /**
     * Persistent preferences for viewing heap regions info.
      */
    static final class HeapRegionInfoViewPreferences extends com.sun.max.ins.gui.TableColumnVisibilityPreferences<HeapRegionInfoColumnKind> {
        private static HeapRegionInfoViewPreferences globalPreferences;

        /**
         * @return the global, persistent set of user preferences for viewing a table of memory HeapRegionInfo.
         */
        static HeapRegionInfoViewPreferences globalPreferences(Inspection inspection) {
            if (globalPreferences == null) {
                globalPreferences = new HeapRegionInfoViewPreferences(inspection);
            }
            return globalPreferences;
        }

        // Prefix for all persistent column preferences in view
        private static final String HEAP_REGION_INFO_COLUMN_PREFERENCE = "heapRegionInfoViewColumn";

        /**
         * @return a GUI panel suitable for setting global preferences for this kind of view.
         */
        public static JPanel globalPreferencesPanel(Inspection inspection) {
            return globalPreferences(inspection).getPanel();
        }

        /**
        * Creates a set of preferences specified for use by singleton instances, where local and
        * persistent global choices are identical.
        */
        private HeapRegionInfoViewPreferences(Inspection inspection) {
            super(inspection, HEAP_REGION_INFO_COLUMN_PREFERENCE, HeapRegionInfoColumnKind.values());
            // There are no view preferences beyond the column choices, so no additional machinery needed here.
        }
    }

    final class FlagsRenderer extends PlainLabel {

        private void updateText() {
            int flags = teleHeapRegionInfo.flags();
            setValue(HeapRegionInfo.flagsToString(flags), intTo0xHex(flags));
        }

        public FlagsRenderer(Inspection inspection) {
            super(inspection, "");
            updateText();
        }


        @Override
        public void refresh(boolean force) {
            updateText();
        }


        @Override
        public void redisplay() {
            updateText();
        }
    }

    final class TagRenderer extends PlainLabel {

        private void updateText() {
            int tag = teleHeapRegionInfo.tag();
            //setValue(teleHeapScheme.tagName(tag), Integer.toString(tag));
            setValue(Integer.toString(tag), Integer.toString(tag));
        }

        public TagRenderer(Inspection inspection) {
            super(inspection, "");
            updateText();
        }

        @Override
        public void refresh(boolean force) {
            updateText();
        }


        @Override
        public void redisplay() {
            updateText();
        }
    }

    static final String [] infoNames = {"start", "end", "firstFreeChunk", "state", "tag"};

    final class HeapRegionInfoTableModel extends InspectorTableModel  implements TableCellRenderer, Prober {
        TextLabel [] nameLabels = new TextLabel[infoNames.length];
        InspectorLabel[] valueLabels = new InspectorLabel[infoNames.length];

        public HeapRegionInfoTableModel(Inspection inspection) {
            super(inspection);
            valueLabels[0] = new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE, teleHeapRegionInfo.regionStart(), HeapRegionInfoTable.this);
            valueLabels[1] = new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE, teleHeapRegionInfo.regionEnd(), HeapRegionInfoTable.this);
            valueLabels[2] =  new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE, teleHeapRegionInfo.firstFreeChunk(), HeapRegionInfoTable.this) {
                @Override
                protected Value fetchValue() {
                    return new WordValue(teleHeapRegionInfo.firstFreeChunk());
                }
            };
            valueLabels[3] = new FlagsRenderer(inspection);
            valueLabels[4] = new TagRenderer(inspection);

            for (int i = 0; i < infoNames.length; i++) {
                nameLabels[i] = new TextLabel(inspection, infoNames[i]);
            }
        }

        @Override
        public int getColumnCount() {
            return HeapRegionInfoColumnKind.values().length;
        }

        @Override
        public int getRowCount() {
            return infoNames.length;
        }

        private Component getTableCellRendererComponent(int row, int col) {
            switch (HeapRegionInfoColumnKind.values()[col]) {
                case NAME:
                    return nameLabels[row];
                case VALUE:
                    return  valueLabels[row];
            }
            return null;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return getTableCellRendererComponent(row, col);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            return getTableCellRendererComponent(row, col);
        }

        @Override
        public void refresh(boolean force) {
            valueLabels[2].refresh(force);
            valueLabels[3].refresh(force);
        }

        @Override
        public void redisplay() {
        }

    }

    static final class HeapRegionInfoColumnModel extends InspectorTableColumnModel<HeapRegionInfoColumnKind> {
        HeapRegionInfoColumnModel(TableCellRenderer cellRenderer, HeapRegionInfoViewPreferences viewPreferences) {
            super(HeapRegionInfoColumnKind.values().length, viewPreferences);
            addColumn(HeapRegionInfoColumnKind.NAME, cellRenderer, null);
            addColumn(HeapRegionInfoColumnKind.VALUE, cellRenderer, null);
        }
    }

    private final InspectorView view;
    private final HeapRegionInfoTableModel tableModel;
    final TeleHeapRegionInfo teleHeapRegionInfo;

    public HeapRegionInfoTable(Inspection inspection, InspectorView view, TeleHeapRegionInfo teleHeapRegionInfo) {
        super(inspection);
        this.view = view;
        this.teleHeapRegionInfo = teleHeapRegionInfo;
        tableModel = new HeapRegionInfoTableModel(inspection);
        HeapRegionInfoColumnModel columnModel = new HeapRegionInfoColumnModel(tableModel, HeapRegionInfoViewPreferences.globalPreferences(inspection));
        configureMemoryTable(tableModel, columnModel);
    }

    public InspectorView getView() {
        return view;
    }

    public InspectorScrollPane makeHeapRegionInfoPane() {
        return new InspectorScrollPane(inspection(), this) {
            @Override
            public void refresh(boolean force) {
                HeapRegionInfoTable.this.refresh(force);
            }
            @Override
            public void redisplay() {
                HeapRegionInfoTable.this.redisplay();
            }
        };
    }
}
