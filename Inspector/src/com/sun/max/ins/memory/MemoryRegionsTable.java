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
package com.sun.max.ins.memory;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.value.*;

/**
 * A table specialized for displaying the memory regions in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class MemoryRegionsTable extends InspectorTable {

    private final HeapScheme heapScheme;
    private final String heapSchemeName;

    private final MemoryRegionsTableModel tableModel;
    private final MemoryRegionsColumnModel columnModel;

    MemoryRegionsTable(Inspection inspection, MemoryRegionsViewPreferences viewPreferences) {
        super(inspection);
        heapScheme = inspection.vm().vmConfiguration().heapScheme();
        heapSchemeName = heapScheme.getClass().getSimpleName();
        tableModel = new MemoryRegionsTableModel(inspection);
        columnModel = new MemoryRegionsColumnModel(viewPreferences);
        configureDefaultTable(tableModel, columnModel);
    }

    @Override
    protected InspectorPopupMenu getPopupMenu(int row, int col, MouseEvent mouseEvent) {
        final InspectorPopupMenu menu = new InspectorPopupMenu();
        final MaxMemoryRegion memoryRegion = tableModel.getMemoryRegion(row);
        final String regionName = memoryRegion.regionName();
        menu.add(actions().inspectRegionMemoryWords(memoryRegion, regionName));
        // menu.add(actions().setRegionWatchpoint(memoryRegionDisplay, "Watch region memory"));
        menu.add(Watchpoints.createEditMenu(inspection(), tableModel.getWatchpoints(row)));
        menu.add(Watchpoints.createRemoveActionOrMenu(inspection(), tableModel.getWatchpoints(row)));
        return menu;
    }

    /**
     * Sets table selection to the memory region, if any, that is the current user focus.
     */
    @Override
    public void updateFocusSelection() {
        final MaxMemoryRegion memoryRegion = focus().memoryRegion();
        final int row = tableModel.findRow(memoryRegion);
        updateSelection(row);
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        // Row selection changed, perhaps by user mouse click or navigation;
        // update user focus to follow the selection.
        super.valueChanged(listSelectionEvent);
        if (!listSelectionEvent.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0) {
                focus().setMemoryRegion(tableModel.getMemoryRegion(row));
            }
        }
    }

    private final class MemoryRegionsColumnModel extends InspectorTableColumnModel<MemoryRegionsColumnKind> {

        private MemoryRegionsColumnModel(MemoryRegionsViewPreferences viewPreferences) {
            super(MemoryRegionsColumnKind.VALUES.size(), viewPreferences);
            addColumn(MemoryRegionsColumnKind.TAG, new TagCellRenderer(inspection()), null);
            addColumn(MemoryRegionsColumnKind.NAME, new NameCellRenderer(), null);
            addColumn(MemoryRegionsColumnKind.START, new StartAddressCellRenderer(), null);
            addColumn(MemoryRegionsColumnKind.END, new EndAddressCellRenderer(), null);
            addColumn(MemoryRegionsColumnKind.SIZE, new SizeCellRenderer(), null);
            addColumn(MemoryRegionsColumnKind.ALLOC, new AllocCellRenderer(), null);
        }
    }

    /**
     * A table data model built around the list of currently allocated memory regions in the VM.
     *
     * @author Michael Van De Vanter
     */
    private final class MemoryRegionsTableModel extends InspectorMemoryTableModel {

        private MaxMemoryRegion[] sortedRegions = null;

        public MemoryRegionsTableModel(Inspection inspection) {
            super(inspection, Address.zero());
            refresh();
        }

        @Override
        public void refresh() {
            final List<MaxMemoryRegion> memoryRegions = vm().state().memoryRegions();
            sortedRegions = memoryRegions.toArray(new MaxMemoryRegion[memoryRegions.size()]);
            Arrays.sort(sortedRegions, MaxMemoryRegion.Util.startComparator());
            super.refresh();
        }

        public int getRowCount() {
            return sortedRegions.length;
        }

        public int getColumnCount() {
            return MemoryRegionsColumnKind.VALUES.size();
        }

        @Override
        public Class<?> getColumnClass(int row) {
            return MaxMemoryRegion.class;
        }

        public Object getValueAt(int row, int col) {
            return getMemoryRegion(row);
        }

        @Override
        public MaxMemoryRegion getMemoryRegion(int row) {
            return sortedRegions[row];
        }

        @Override
        public Offset getOffset(int row) {
            return Offset.fromLong(getAddress(row).toLong());
        }

        @Override
        public int findRow(Address address) {
            for (int row = 0; row < sortedRegions.length; row++) {
                if (sortedRegions[row].contains(address)) {
                    return row;
                }
            }
            return -1;
        }

        int findRow(MaxMemoryRegion memoryRegion) {
            for (int row = 0; row < sortedRegions.length; row++) {
                if (sortedRegions[row].sameAs(memoryRegion)) {
                    return row;
                }
            }
            return -1;
        }
    }

    /**
     * @return foreground color for row; color the text specially in the row where a watchpoint is triggered
     */
    private Color getRowTextColor(int row) {
        final MaxWatchpointEvent watchpointEvent = vm().state().watchpointEvent();
        if (watchpointEvent != null && tableModel.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return null;
    }

    private final class TagCellRenderer extends MemoryTagTableCellRenderer implements TableCellRenderer {

        TagCellRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Component renderer = getRenderer(tableModel.getMemoryRegion(row), focus().thread(), tableModel.getWatchpoints(row));
            renderer.setForeground(getRowTextColor(row));
            renderer.setBackground(cellBackgroundColor(isSelected));
            return renderer;
        }

    }

    private final class NameCellRenderer implements TableCellRenderer, Prober  {

        // The labels have important user interaction state, so create one per memory region and keep them around,
        // even though they may not always appear in the same row.
        private final Map<MaxMemoryRegion, InspectorLabel> regionToLabel = new HashMap<MaxMemoryRegion, InspectorLabel>();

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxMemoryRegion memoryRegion = (MaxMemoryRegion) value;
            InspectorLabel label = regionToLabel.get(memoryRegion);
            if (label == null) {
                label = new MemoryRegionNameLabel(inspection(), memoryRegion);
                regionToLabel.put(memoryRegion, label);
            }
            label.setForeground(getRowTextColor(row));
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
        }

        public void redisplay() {
            for (Prober prober : regionToLabel.values()) {
                prober.redisplay();
            }
        }

        public void refresh(boolean force) {
            for (Prober prober : regionToLabel.values()) {
                prober.refresh(force);
            }
        }
    }

    private final class StartAddressCellRenderer implements TableCellRenderer, Prober {

        // ValueLabels have important user interaction state, so create one per memory region and keep them around,
        // even though they may not always appear in the same row.
        private final Map<MaxMemoryRegion, InspectorLabel> regionToLabel = new HashMap<MaxMemoryRegion, InspectorLabel>();

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxMemoryRegion memoryRegion = (MaxMemoryRegion) value;
            InspectorLabel label = regionToLabel.get(memoryRegion);
            if (label == null) {
                label = new WordValueLabel(inspection(), ValueMode.WORD, MemoryRegionsTable.this) {

                    @Override
                    public Value fetchValue() {
                        return WordValue.from(memoryRegion.start());
                    }
                };
                label.setOpaque(true);
                regionToLabel.put(memoryRegion, label);
            }
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
        }

        public void redisplay() {
            for (Prober prober : regionToLabel.values()) {
                prober.redisplay();
            }
        }

        public void refresh(boolean force) {
            for (Prober prober : regionToLabel.values()) {
                prober.refresh(force);
            }
        }
    }

    private final class EndAddressCellRenderer implements TableCellRenderer, Prober {

        // ValueLabels have important user interaction state, so create one per memory region and keep them around,
        // even though they may not always appear in the same row.
        private final Map<MaxMemoryRegion, InspectorLabel> regionToLabel = new HashMap<MaxMemoryRegion, InspectorLabel>();

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxMemoryRegion memoryRegion = (MaxMemoryRegion) value;
            InspectorLabel label = regionToLabel.get(memoryRegion);
            if (label == null) {
                label = new WordValueLabel(inspection(), ValueMode.WORD, MemoryRegionsTable.this) {

                    @Override
                    public Value fetchValue() {
                        return WordValue.from(memoryRegion.end());
                    }
                };
                label.setOpaque(true);
                regionToLabel.put(memoryRegion, label);
            }
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
        }

        public void redisplay() {
            for (Prober prober : regionToLabel.values()) {
                prober.redisplay();
            }
        }

        public void refresh(boolean force) {
            for (Prober prober : regionToLabel.values()) {
                prober.refresh(force);
            }
        }
    }

    private final class SizeCellRenderer implements TableCellRenderer, Prober {

        private final Map<MaxMemoryRegion, InspectorLabel> regionToLabel = new HashMap<MaxMemoryRegion, InspectorLabel>();

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxMemoryRegion memoryRegion = (MaxMemoryRegion) value;
            InspectorLabel label = regionToLabel.get(memoryRegion);
            if (label == null) {
                label = new MemoryRegionSizeLabel(inspection(), memoryRegion);
                regionToLabel.put(memoryRegion, label);
            }
            label.setForeground(getRowTextColor(row));
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
        }

        public void redisplay() {
            for (Prober prober : regionToLabel.values()) {
                prober.redisplay();
            }
        }

        public void refresh(boolean force) {
            for (Prober prober : regionToLabel.values()) {
                prober.refresh(force);
            }
        }
    }

    private final class AllocCellRenderer implements TableCellRenderer, Prober {

        private final Map<MaxMemoryRegion, InspectorLabel> regionToLabel = new HashMap<MaxMemoryRegion, InspectorLabel>();

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final MaxMemoryRegion memoryRegion = (MaxMemoryRegion) value;
            InspectorLabel label = regionToLabel.get(memoryRegion);
            if (label == null) {
                label = new MemoryRegionAllocationLabel(inspection(), memoryRegion, MemoryRegionsTable.this);
                regionToLabel.put(memoryRegion, label);
            }
            label.setForeground(getRowTextColor(row));
            label.setBackground(cellBackgroundColor(isSelected));
            return label;
        }

        public void redisplay() {
            for (Prober prober : regionToLabel.values()) {
                prober.redisplay();
            }
        }

        public void refresh(boolean force) {
            for (Prober prober : regionToLabel.values()) {
                prober.refresh(force);
            }
        }
    }

}
