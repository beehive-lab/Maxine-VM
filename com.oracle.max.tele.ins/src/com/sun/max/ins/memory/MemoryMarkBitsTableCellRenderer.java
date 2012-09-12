/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.ins.memory;

import static com.sun.max.tele.MaxMarkBitmap.MarkColor.*;

import java.awt.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxMarkBitmap.*;
import com.sun.max.tele.data.*;
import com.sun.max.unsafe.*;

/**
 * A table cell renderer for tables with rows representing memory words: displays in the cell
 * the contents of the {@link MaxMarkBitmap} word containing the bit that is used to mark each word.
 * <p>
 * If there is no {@link MaMarkBitmap}, either because one is not in use or because it has not yet been initialized, or because
 * the word is not covered by the bitmap then the
 * cell displays the standard text representing unavailable data.
 * <p>
 * The bitmap word is displayed as a sequence of bytes in hex format, with the exception of the byte containing the mark bit
 * for the word.  That byte is displayed in binary, surrounded by '|' characters.  The specific bit used to mark the word is
 * bracketed with '<' and '>'.
 */
public final class MemoryMarkBitsTableCellRenderer extends InspectorTableCellRenderer {

    private final InspectorTable inspectorTable;
    private final InspectorMemoryTableModel tableModel;

    // This kind of label has no interaction state, so we only need one, which we set up on demand.
    private final InspectorLabel label;
    private final InspectorLabel[] labels = new InspectorLabel[1];

    /**
     * A renderer that displays the VM's memory region name, if any, into which the word value in the memory represented
     * by the table row points.
     *
     * @param inspection
     * @param inspectorTable the table holding the cell to be rendered
     * @param tableModel a table model in which rows represent memory regions
     */
    public MemoryMarkBitsTableCellRenderer(Inspection inspection, InspectorTable inspectorTable, InspectorMemoryTableModel tableModel) {
        super(inspection);
        this.inspectorTable = inspectorTable;
        this.tableModel = tableModel;
        this.label = new TextLabel(inspection, "");
        this.label.setOpaque(true);
        this.labels[0] = this.label;
        redisplay();
    }

    @Override
    public void redisplay() {
        label.setFont(preference().style().hexDataFont());
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
        final InspectorStyle style = inspection().preference().style();
        Color backgroundColor = inspectorTable.cellBackgroundColor();
        Color foregroundColor = inspectorTable.cellForegroundColor(row, column);

        InspectorLabel renderer = label;
        final MaxMarkBitmap markBitmap = vm().heap().markBitMap();
        final Address memoryAddress = tableModel.getAddress(row);

        if (markBitmap == null || !markBitmap.isCovered(memoryAddress)) {
            renderer = inspection().gui().getUnavailableDataTableCellRenderer();
            renderer.setToolTipPrefix(tableModel.getRowDescription(row));
        } else {
            final int bitIndex = markBitmap.getBitIndexOf(memoryAddress);
            final int bitIndexInWord = markBitmap.getBitIndexInWord(bitIndex);
            final int byteIndexInWord = 7 - (bitIndexInWord / 8);
            final int bitIndexInByte = 7 - (bitIndexInWord % 8);
            byte[] bytes = new byte[8];
            try {
                final long bitmapWord = markBitmap.readBitmapWord(bitIndex);
                bytes[0] = (byte) (bitmapWord >>> 56);
                bytes[1] = (byte) (bitmapWord >>> 48);
                bytes[2] = (byte) (bitmapWord >>> 40);
                bytes[3] = (byte) (bitmapWord >>> 32);
                bytes[4] = (byte) (bitmapWord >>> 24);
                bytes[5] = (byte) (bitmapWord >>> 16);
                bytes[6] = (byte) (bitmapWord >>>  8);
                bytes[7] = (byte) (bitmapWord >>>  0);
            } catch (DataIOError dataIOError) {
                return inspection().gui().getUnavailableDataTableCellRenderer();
            }
            final StringBuilder result = new StringBuilder(100);
            String prefix = "";
            for (int index = 0; index < 8; index++) {
                byte b = bytes[index];
                result.append(prefix);
                final StringBuffer str = new StringBuffer();
                if (index == byteIndexInWord) {
                    for (short mask = 0x80; mask != 0; mask >>>= 1) {
                        str.append((b & mask) == 0 ? "0" : "1");
                    }
                    str.insert(bitIndexInByte + 1, '>').insert(bitIndexInByte, '<');
                    result.append("|").append(str).append("|");
                } else {
                    result.append(String.format("%02X", b));
                }
                prefix = " ";
            }
            renderer.setWrappedHtmlText(result.toString());
            renderer.setToolTipPrefix(tableModel.getRowDescription(row));
            renderer.setToolTipText("Mark Bitmap word@" + markBitmap.bitmapWordAddress(bitIndex).to0xHexString());
            // Is this the first bit of a mark?
            MarkColor markColor = markBitmap.getMarkColor(bitIndex);
            if (markColor == null && row > 0) {
                // Is this the second bit of a mark?  If so, render the cell with the same style as as the first bit
                markColor = markBitmap.getMarkColor(row - 1);
            }
            if (markColor != null) {
                switch(markColor) {
                    case MARK_WHITE:
                        backgroundColor = style.markedWhiteBackgroundColor();
                        foregroundColor = Color.BLACK;
                        break;
                    case MARK_GRAY:
                        backgroundColor = style.markedGrayBackgroundColor();
                        foregroundColor = Color.WHITE;
                        break;
                    case MARK_BLACK:
                        backgroundColor = style.markedBlackBackgroundColor();
                        foregroundColor = Color.WHITE;
                        break;
                    case MARK_INVALID:
                        backgroundColor = style.markInvalidBackgroundColor();
                        foregroundColor = Color.WHITE;
                        break;
                    case MARK_UNAVAILABLE:
                        break;
                }
            } else if (markBitmap.isBitSet(row)) {
                // Not a valid location for a mark bit; shouldn't be set
                backgroundColor = style.markInvalidBackgroundColor();
                foregroundColor = Color.WHITE;
                markColor = MARK_INVALID;
            }
            final StringBuilder sb = new StringBuilder();
            sb.append("<br>Heap mark bit(");
            sb.append(bitIndex);
            sb.append(")=");
            sb.append(markBitmap.isBitSet(bitIndex) ? "1" : "0");
            sb.append(", color=");
            sb.append(markBitmap.getMarkColor(bitIndex));
            renderer.setToolTipSuffix(sb.toString());
        }
        renderer.setBackground(backgroundColor);
        renderer.setForeground(foregroundColor);
        if (inspectorTable.isBoundaryRow(row)) {
            renderer.setBorder(preference().style().defaultPaneTopBorder());
        } else {
            renderer.setBorder(null);
        }
        return renderer;
    }

    @Override
    protected InspectorLabel[] getLabels() {
        return labels;
    }

}
