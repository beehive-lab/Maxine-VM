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
package com.sun.max.ins.debug;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;


final public class MarkBitsTable extends InspectorTable {

    static public enum MarkBitsColumnKind implements ColumnKind {
        BIT_INDEX("bit #", "Index of first bit of the mark"),
        BITMAP_WORD_ADDRESS("@bit", "Address of word containing first bit of mark"),
        HEAP_ADDRESS("Address", "Heap address associated to this the mark"),
        COLOR("Color", "Color of the mark");

        private final String label;
        private final String toolTipText;

        MarkBitsColumnKind(String label, String toolTipText) {
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

    class MarkBitColor extends PlainLabel {
        final int bitIndex;

        MarkBitColor(Inspection inspection, int bitIndex) {
            super(inspection, markBitsInfo.getMarkColor(bitIndex).name);
            this.bitIndex = bitIndex;
        }

        @Override
        public void refresh(boolean force) {
            setValue(markBitsInfo.getMarkColor(bitIndex).name);
        }
    }

    class MarkBit {
        final PlainLabel bitIndexLabel;
        final WordValueLabel heapAddress;
        final WordValueLabel bitmapWordAddress;
        final MarkBitColor color;

        MarkBit(Inspection inspection, Address address) {
            final int bitIndex = markBitsInfo.getBitIndexOf(address);
            bitIndexLabel = new PlainLabel(inspection, bitIndex);
            heapAddress = new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE,  address, MarkBitsTable.this);
            bitmapWordAddress = new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE,  markBitsInfo.bitmapWord(bitIndex), MarkBitsTable.this);
            color = new MarkBitColor(inspection, bitIndex);
        }
    }

    final class MarkBitsTableModel extends  InspectorTableModel  implements TableCellRenderer{

        public MarkBitsTableModel(Inspection inspection) {
            super(inspection);
            markBits = new ArrayList<MarkBit>();
        }

        List<MarkBit> markBits;

        public int findRow(Address address) {
            final int bitIndex = markBitsInfo.getBitIndexOf(address);
            int row = 0;
            for (MarkBit m : markBits) {
                if (m.color.bitIndex == bitIndex) {
                    return row;
                }
                row++;
            }
            return -1;
        }

        @Override
        public int getColumnCount() {
            return MarkBitsColumnKind.values().length;
        }

        @Override
        public int getRowCount() {
            return markBits.size();
        }

        private Component getTableCellRendererComponent(int row, int col) {
            MarkBit markBit = markBits.get(row);
            switch (MarkBitsColumnKind.values()[col]) {
                case BIT_INDEX:
                    return markBit.bitIndexLabel;
                case BITMAP_WORD_ADDRESS:
                    return markBit.bitmapWordAddress;
                case HEAP_ADDRESS:
                    return markBit.heapAddress;
                case COLOR:
                    return markBit.color;
                default:
                    throw InspectorError.unexpected("Unexpected MarkBit Data column");
            }
        }

        @Override
        public Object getValueAt(int row, int col) {
            return getTableCellRendererComponent(row, col);
        }

        @Override
        public Component getTableCellRendererComponent(JTable  table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            return getTableCellRendererComponent(row, col);
        }

    }

    static final class MarkBitsViewPreferences extends com.sun.max.ins.gui.TableColumnVisibilityPreferences<MarkBitsColumnKind> {
        private static final String MARK_BITS_COLUMN_PREFERENCE = "markBitsViewColumn";
        public MarkBitsViewPreferences(Inspection inspection) {
            super(inspection, MARK_BITS_COLUMN_PREFERENCE, MarkBitsColumnKind.values());
        }
        private static MarkBitsViewPreferences globalPreferences;
        static MarkBitsViewPreferences globalPreferences(Inspection inspection) {
            if (globalPreferences == null) {
                globalPreferences = new MarkBitsViewPreferences(inspection);
            }
            return globalPreferences;
        }
    }

    static final class MarkBitsColumnModel extends InspectorTableColumnModel<MarkBitsColumnKind> {
        MarkBitsColumnModel(TableCellRenderer cellRenderer, MarkBitsViewPreferences viewPreferences) {
            super(MarkBitsColumnKind.values().length, viewPreferences);
            addColumn(MarkBitsColumnKind.BIT_INDEX, cellRenderer, null);
            addColumn(MarkBitsColumnKind.BITMAP_WORD_ADDRESS, cellRenderer, null);
            addColumn(MarkBitsColumnKind.HEAP_ADDRESS, cellRenderer, null);
            addColumn(MarkBitsColumnKind.COLOR, cellRenderer, null);
        }
    }

    private final InspectorView view;

    /**
     * Shortcut to heap markBitsInfo.
     */
    final MaxMarkBitmap markBitsInfo;

    public MarkBitsTable(Inspection inspection, InspectorView view) {
        super(inspection);
        this.view = view;
        markBitsInfo = inspection.vm().heap().markBitMap();
    }

    public InspectorView getView() {
        return view;
    }
}
