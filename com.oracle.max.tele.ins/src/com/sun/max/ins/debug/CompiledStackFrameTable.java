/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.ValueMode;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.VMFrameLayout.Slot;
import com.sun.max.vm.stack.VMFrameLayout.Slots;
import com.sun.max.vm.value.*;

/**
 * A table that displays the contents of a VM compiled method stack frame in the VM.
 */
public class CompiledStackFrameTable extends InspectorTable {

    private final InspectorView view;
    private final MaxStackFrame.Compiled compiledStackFrame;
    private final CompiledStackFrameViewPreferences viewPreferences;
    private final CompiledStackFrameTableModel tableModel;

    /**
     * A table specialized to display the slots in a Java method stack frame in the VM.
     * <br>
     * Each slot is assumed to occupy one word in memory.
     *
     * @param view the view that owns the table
     * @param compiledStackFrame a stack frame to display
     * @param viewPreferences preferences for this view
     */
    public CompiledStackFrameTable(Inspection inspection, InspectorView view, MaxStackFrame.Compiled compiledStackFrame, CompiledStackFrameViewPreferences viewPreferences) {
        super(inspection);
        this.view = view;
        this.compiledStackFrame = compiledStackFrame;
        this.viewPreferences = viewPreferences;
        this.tableModel = new CompiledStackFrameTableModel(inspection, compiledStackFrame);
        CompiledStackFrameTableColumnModel columnModel = new CompiledStackFrameTableColumnModel(this, this.tableModel, viewPreferences);
        configureMemoryTable(tableModel, columnModel);
    }

    @Override
    protected void mouseButton1Clicked(final int row, final int col, MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1 && vm().watchpointManager() != null) {
            final InspectorAction toggleAction = new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint") {

                @Override
                public MaxWatchpoint setWatchpoint() {
                    final MaxMemoryRegion memoryRegion = tableModel.getMemoryRegion(row);
                    final String regionDescription =  "Stack: thread="  + inspection().nameDisplay().shortName(compiledStackFrame.stack().thread());
                    actions().setRegionWatchpoint(memoryRegion, "Set memory watchpoint", regionDescription).perform();
                    final List<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (!watchpoints.isEmpty()) {
                        return watchpoints.get(0);
                    }
                    return null;
                }
            };
            toggleAction.perform();
        }
    }

    @Override
    protected InspectorPopupMenu getPopupMenu(final int row, final int col, MouseEvent mouseEvent) {
        if (vm().watchpointManager() != null && col == CompiledStackFrameColumnKind.TAG.ordinal()) {
            final InspectorPopupMenu menu = new InspectorPopupMenu();
            final MaxMemoryRegion memoryRegion = tableModel.getMemoryRegion(row);
            final Slot slot = (Slot) tableModel.getValueAt(row, col);
            final String regionDescription =  "Stack slot : " + slot.name;
            menu.add(new Watchpoints.ToggleWatchpointRowAction(inspection(), tableModel, row, "Toggle watchpoint (double-click)") {

                @Override
                public MaxWatchpoint setWatchpoint() {

                    actions().setRegionWatchpoint(memoryRegion, "Set memory watchpoint", regionDescription).perform();
                    final List<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
                    if (!watchpoints.isEmpty()) {
                        return watchpoints.get(0);
                    }
                    return null;
                }
            });
            menu.add(actions().setRegionWatchpoint(memoryRegion, "Watch this memory location", regionDescription));
            menu.add(Watchpoints.createEditMenu(inspection(), tableModel.getWatchpoints(row)));
            menu.add(Watchpoints.createRemoveActionOrMenu(inspection(), tableModel.getWatchpoints(row)));
            return menu;
        }
        return null;
    }

    @Override
    public void updateFocusSelection() {
        // Sets table selection to the memory word, if any, that is the current user focus.
        final Address address = focus().address();
        updateSelection(tableModel.findRow(address));
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // The selection in the table has changed; might have happened via user action (click, arrow) or
        // as a side effect of a focus change.
        super.valueChanged(e);
        if (!e.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0 && row < tableModel.getRowCount()) {
                focus().setAddress(tableModel.getAddress(row));
            }
        }
    }

    /**
     * {@inheritDoc}.
     * <br>
     * Color the text specially in the row where a watchpoint is triggered
     */
    @Override
    public Color cellForegroundColor(int row, int col) {
        final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
        if (watchpointEvent != null && tableModel.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return preference().style().debugIPTagColor();
        }
        return null;
    }

    public InspectorView getView() {
        return view;
    }

    /**
     * A column model for Java stack frames.
     * Column selection is driven by choices in the parent.
     * This implementation cannot update column choices dynamically.
     */
    private final class CompiledStackFrameTableColumnModel extends InspectorTableColumnModel<CompiledStackFrameColumnKind> {

        CompiledStackFrameTableColumnModel(InspectorTable table, InspectorMemoryTableModel tableModel, CompiledStackFrameViewPreferences viewPreferences) {
            super(inspection(), CompiledStackFrameColumnKind.values().length, viewPreferences);
            addColumnIfSupported(CompiledStackFrameColumnKind.TAG, new MemoryTagTableCellRenderer(inspection(), table, tableModel), null);
            addColumnIfSupported(CompiledStackFrameColumnKind.NAME, new NameRenderer(inspection()), null);
            addColumnIfSupported(CompiledStackFrameColumnKind.ADDRESS, new MemoryAddressLocationTableCellRenderer(inspection(), table, tableModel), null);
            addColumnIfSupported(CompiledStackFrameColumnKind.OFFSET_SP, new OffsetSPRenderer(inspection()), null);
            addColumnIfSupported(CompiledStackFrameColumnKind.OFFSET_FP, new OffsetFPRenderer(inspection()), null);
            addColumnIfSupported(CompiledStackFrameColumnKind.VALUE, new ValueRenderer(inspection()), null);
            addColumnIfSupported(CompiledStackFrameColumnKind.REGION, new MemoryRegionPointerTableCellRenderer(inspection(), table, tableModel), null);
        }
    }

    /**
     * A table model that represents the information in a Java stack frame as a table of
     * slots, one per memory word.
     * <p>
     * For the purposes of memory in this view, the origin is assumed to be the Stack Pointer.
     * Note also that the frame slots are at descending memory addresses, so the origin is
     * not the first location in the memory table.
     */
    private final class CompiledStackFrameTableModel extends InspectorMemoryTableModel {

        private final MaxStackFrame.Compiled javaStackFrame;
        private final int frameSize;
        private final Slots slots;
        private final MaxMemoryRegion[] regions;
        private final String[] slotDescriptions;

        public CompiledStackFrameTableModel(Inspection inspection,  MaxStackFrame.Compiled javaStackFrame) {
            super(inspection, javaStackFrame.slotBase());
            this.javaStackFrame = javaStackFrame;
            frameSize = javaStackFrame.layout().frameSize();
            slots = javaStackFrame.layout().slots();
            regions = new MaxMemoryRegion[slots.size()];
            slotDescriptions = new String[slots.size()];
            int index = 0;
            for (Slot slot : slots) {
                regions[index] = new FixedMemoryRegion(inspection(), "", getOrigin().plus(slot.offset), vm().platform().nBytesInWord());
                slotDescriptions[index] = "Stack frame slot \"" + slot.name + "\"";
                index++;
            }
        }

        public int getColumnCount() {
            return CompiledStackFrameColumnKind.values().length;
        }

        public int getRowCount() {
            return javaStackFrame.layout().slots().size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return slots.slot(rowIndex);
        }

        @Override
        public Class< ? > getColumnClass(int col) {
            return Slot.class;
        }

        @Override
        public int findRow(Address address) {
            final int wordOffset = regions[0].start().minus(address).dividedBy(vm().platform().nBytesInWord()).toInt();
            return (wordOffset >= 0 && wordOffset < slots.size()) ? wordOffset : -1;
        }

        @Override
        public MaxMemoryRegion getMemoryRegion(int row) {
            return regions[row];
        }

        @Override
        public int getOffset(int row) {
            // Slot offsets are relative to Stack Pointer
            return slots.slot(row).offset;
        }

        @Override
        public String getRowDescription(int row) {
            return slotDescriptions[row];
        }

        /**
         * Offset of the slot relative to the Stack Pointer.
         *
         * @param row row number of the stack frame slot
         * @param biasOffset whether offsets should be biased
         * @return the slot offset relative to the SP
         */
        public int getSPOffset(int row, boolean biasOffset) {
            if (biasOffset) {
                return javaStackFrame.biasedFPOffset(getOffset(row)) + frameSize;
            }
            return getOffset(row);
        }

        /**
         * Offset of the slot relative to the Frame Pointer.
         *
         * @param row row number of the stack frame slot
         * @param biasOffset whether offsets should be biased
         * @return the slot offset relative to the FP
         */
        public int getFPOffset(int row, boolean biasOffset) {
            if (biasOffset) {
                return javaStackFrame.biasedFPOffset(getOffset(row));
            }
            return getOffset(row) - frameSize;
        }

        public String getSlotName(int row) {
            return slots.slot(row).name;
        }

        /**
         * Gets the Java source variable name (if any) for a given slot.
         *
         * @param row the slot for which the Java source variable name is being requested
         * @return the Java source name for {@code slot} or null if a name is not available
         */
        public String getSourceVariableName(int row) {
            return javaStackFrame.sourceVariableName(row);
        }
    }

    /**
     * @return a string representing the currently displayed data in the panel,
     * not clipped by scrolling.
     */
    public String getContentString() {
        final StringBuilder result = new StringBuilder();
        final int columnCount = getColumnCount();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            for (int col = 0; col < columnCount; col++) {
                if (col > 0) {
                    result.append("\t");
                }
                TableCellRenderer cellRenderer = this.getCellRenderer(row, col);
                Object valueAt = tableModel.getValueAt(row, col);
                final InspectorLabel label = (InspectorLabel) cellRenderer.getTableCellRendererComponent(this, valueAt, false, false, row, col);
                result.append(label.getTextDeHtmlify());
            }
            result.append("\n");
        }
        return result.toString();
    }
    private final class NameRenderer extends TextLabel implements TableCellRenderer {

        public NameRenderer(Inspection inspection) {
            super(inspection, null);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Slot slot = (Slot) value;
            if (slot == null) {
                return gui().getUnavailableDataTableCellRenderer();
            }
            setText(slot.name);

            String otherInfo = "";
            if (viewPreferences.biasSlotOffsets()) {
                final int biasedOffset = tableModel.getFPOffset(row, viewPreferences.biasSlotOffsets());
                otherInfo = String.format("(%%fp %+d)", biasedOffset);
            }
            final String sourceVariableName = tableModel.getSourceVariableName(row);
            final int offset = tableModel.getSPOffset(row, false);
            final String toolTipText = String.format("SP %+d%s%s", offset, otherInfo, sourceVariableName == null ? "" : " [" + sourceVariableName + "]");
            setWrappedToolTipHtmlText(tableModel.getRowDescription(row) + "<br>" + toolTipText);
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor());

            return this;
        }
    }

    private final class OffsetSPRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public OffsetSPRenderer(Inspection inspection) {
            super(inspection);
            setToolTipPrefix("Slot memory address");
            setOpaque(true);
        }

        // TODO (mlvdv) check address computations
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(tableModel.getSPOffset(row, viewPreferences.biasSlotOffsets()), tableModel.getOrigin());
            setToolTipPrefix("Stack frame slot \"" + tableModel.getSlotName(row) + "\" SP-relative location<br>Address= ");
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor());
            return this;
        }
    }

    private final class OffsetFPRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public OffsetFPRenderer(Inspection inspection) {
            super(inspection);
            setToolTipPrefix("Slot memory address");
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(tableModel.getFPOffset(row, viewPreferences.biasSlotOffsets()), tableModel.getAddress(0));
            setToolTipPrefix("Stack frame slot \"" + tableModel.getSlotName(row) + "\" FP-relative location<br>Address= ");
            setForeground(cellForegroundColor(row, col));
            setBackground(cellBackgroundColor());
            return this;
        }
    }

    private final class ValueRenderer extends DefaultTableCellRenderer implements Prober{

        private final Inspection inspection;
        // WordValueLabels have important user interaction state, so create one per memory location and keep them around,
        // even though they may not always appear in the same row.
        private final Map<Long, WordValueLabel> addressToLabelMap = new HashMap<Long, WordValueLabel>();

        public ValueRenderer(Inspection inspection) {
            this.inspection = inspection;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Address address = tableModel.getAddress(row);
            WordValueLabel label = addressToLabelMap.get(address.toLong());
            if (label == null) {
                label = new WordValueLabel(inspection, ValueMode.WORD, CompiledStackFrameTable.this) {
                    @Override
                    public Value fetchValue() {
                        return vm().memoryIO().readWordValue(address);
                    }
                };
                label.setOpaque(true);
                label.setToolTipPrefix(tableModel.getRowDescription(row) + " value = ");
                addressToLabelMap.put(address.toLong(), label);
            }
            label.setBackground(cellBackgroundColor());
            return label;
        }

        public void redisplay() {
            for (WordValueLabel label : addressToLabelMap.values()) {
                if (label != null) {
                    label.redisplay();
                }
            }
        }

        public void refresh(boolean force) {
            for (WordValueLabel label : addressToLabelMap.values()) {
                if (label != null) {
                    label.refresh(force);
                }
            }
        }
    }

}
