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

import static com.sun.max.tele.MaxProcessState.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.util.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxMarkBitmap.MarkColor;
import com.sun.max.unsafe.*;

public class MarkBitmapView extends AbstractView<MarkBitmapView> implements TableColumnViewPreferenceListener {
    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.MARK_BITMAP;
    private static final String SHORT_NAME = "Mark Bitmap";
    private static final String LONG_NAME = "Mark Bitmap View";
    private static final String GEOMETRY_SETTINGS_KEY = "markBitmapViewGeometry";

    public static final class MarkBitmapViewManager extends AbstractSingletonViewManager<MarkBitmapView> {
        protected MarkBitmapViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
            focus().addListener(new InspectionFocusAdapter() {

                @Override
                public void markBitIndexFocusChanged(int oldHeapMarkBit, int heapMarkBit) {
                    if (heapMarkBit >= 0) {
                        final MarkBitmapView view = MarkBitmapViewManager.this.activateView();
                        view.scrollToRowCentered(heapMarkBit);
                    }
                }
            });
        }

        @Override
        protected MarkBitmapView createView(Inspection inspection) {
            return new MarkBitmapView(inspection);
        }

        @Override
        public boolean isSupported() {
            return vm().heap().hasMarkBitmap();
        }
    }

    private static MarkBitmapViewManager viewManager = null;

    public static MarkBitmapViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new MarkBitmapViewManager(inspection);
        }
        return viewManager;
    }

    public static enum ViewMode {
        SET_BIT("Set Mark Bit", "Scroll to next/previous set mark bit",
            "Scroll to previous set mark bit", "Scroll to next set mark bit"),
        BLACK("Black Mark", "Scroll to next/previous object marked BLACK",
            "Scroll to previous object marked BLACK", "Scroll to next object marked BLACK"),
        GRAY("Gray Mark", "Scroll to next/previous object marked GRAY",
            "Scroll to previous object marked GRAY", "Scroll to next object marked GRAY"),
        WHITE("White Mark", "Scroll to next/previous object marked WHITE",
            "Scroll to previous object marked WHITE", "Scroll to next object marked WHITE"),
        INVALID("Invalid Mark", "Scroll to next/previous INVALID object mark",
            "Scroll to previous INVALID object mark", "Scroll to next INVALID object mark");

        private final String label;
        private final String description;
        private final String previousToolTip;
        private final String nextToolTip;

        /**
         * @param label the label that identifies the mode
         * @param description description of the mode
         * @param previousToolTip description of the move backwards action in this mode
         * @param nextToolTip description of the move forward action in this mode
         */
        private ViewMode(String label, String description, String previousToolTip, String nextToolTip) {
            this.label = label;
            this.description = description;
            this.previousToolTip = previousToolTip;
            this.nextToolTip = nextToolTip;
        }

        public String label() {
            return label;
        }

        public String description() {
            return description;
        }

        public String previousToolTip() {
            return previousToolTip;
        }

        public String nextToolTip() {
            return nextToolTip;
        }

    }

    private InspectorAction viewBitmapMemoryAction;
    private InspectorAction viewBitmapDataAction;

    private MaxMarkBitmap markBitmap = null;
    private MaxObject markBitmapData = null;
    private MemoryColoringTable table;
    private InspectorScrollPane scrollPane;
    private JToolBar toolBar;
    private final InspectorComboBox viewModeComboBox;  // TODO (mlvdv) generic in Java 7
    private final JLabel viewModeComboBoxRenderer;  // Holds current view mode, even across view reconstructions.
    private final InspectorButton previousButton;
    private final InspectorButton nextButton;
    private final InspectorButton prefsButton;

    // This is a singleton viewer, so only use a single level of view preferences.
    private final MarkBitmapViewPreferences viewPreferences;

    protected MarkBitmapView(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);

        viewPreferences = MarkBitmapViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);

        // The combo box holds the current view mode
        viewModeComboBox = new InspectorComboBox(inspection, ViewMode.values());
        viewModeComboBox.setSelectedItem(ViewMode.SET_BIT);
        // Add the listener after the initial selection is set; we're not ready for an update yet.
        viewModeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateViewMode();
            }
        });
        viewModeComboBoxRenderer = new JLabel();
        // TODO (mlvdv) can't change to the generic form here until we drop support for jdk6 and earlier
        viewModeComboBox.setRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final ViewMode viewMode = (ViewMode) value;
                viewModeComboBoxRenderer.setText(viewMode.label());
                return viewModeComboBoxRenderer;
            }
        });

        previousButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveBack();
            }
        });
        final InspectorStyle style = preference().style();
        previousButton.setIcon(style.navigationBackIcon());

        nextButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveForward();
            }
        });
        nextButton.setIcon(style.navigationForwardIcon());

        prefsButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                new SimpleDialog(inspection(), viewPreferences.getPanel(), "View Preferences", true);
            }
        });
        prefsButton.setText(null);
        prefsButton.setToolTipText("Column view options");
        prefsButton.setIcon(style.generalPreferencesIcon());

        viewBitmapMemoryAction = new ViewBitmapMemoryAction(inspection);
        viewBitmapDataAction = new ViewBitmapDataAction(inspection);
        refreshAllActions(true);

        createFrame(true);

        Trace.end(TRACE_VALUE,  tracePrefix() + " initializing");
    }

    @Override
    public String getTextForTitle() {
        return viewManager.shortName();
    }

    @Override
    protected InspectorTable getTable() {
        return table;
    }

    @Override
    protected void createViewContent() {
        // Populate menu bar
        makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu memoryMenu = makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(viewBitmapMemoryAction);
        memoryMenu.add(actions().viewSelectedMemoryWatchpointAction());
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));

        final InspectorMenu objectMenu = makeMenu(MenuKind.OBJECT_MENU);
        objectMenu.add(viewBitmapDataAction);
        objectMenu.add(defaultMenuItems(MenuKind.OBJECT_MENU));

        makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        if (vm().heap().markBitMap() == null) {
            createNullContent();
        } else {
            createTableContent();
        }
    }

    @Override
    protected void refreshState(boolean force) {
        if (table == null && vm().heap().markBitMap() != null) {
            reconstructView();
        }
        if (table != null) {
            table.refresh(force);
        }
        refreshAllActions(force);
    }

    /**
     * Creates a placeholder content pane, to be used until we discover that the mark bitmap has been allocated.
     */
    private void createNullContent() {
        final InspectorPanel nullPanel = new InspectorPanel(inspection(), new BorderLayout());
        nullPanel.add(new PlainLabel(inspection(), "<Mark Bitmap not yet allocated>"), BorderLayout.PAGE_START);
        setContentPane(nullPanel);
    }

    /**
     * Creates a pane that displays and permits interaction with the mark bitmap, assumes that the bitmap has been allocated in VM memory.
     */
    private void createTableContent() {
        markBitmap = vm().heap().markBitMap();
        assert markBitmap != null;
        markBitmapData = markBitmap.representation();
        table = new MemoryColoringTable(inspection(), this, markBitmap, viewPreferences);
        final InspectorPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        toolBar = new InspectorToolBar(inspection());
        toolBar.setBorder(preference().style().defaultPaneBorder());
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(previousButton);
        toolBar.add(viewModeComboBox);
        toolBar.add(nextButton);
        toolBar.add(prefsButton);
        toolBar.add(Box.createHorizontalGlue());
        panel.add(toolBar, BorderLayout.NORTH);

        scrollPane = new InspectorScrollPane(inspection(), table);
        panel.add(scrollPane, BorderLayout.CENTER);
        setContentPane(panel);
        // Force everything into consistency with the current view mode.
        updateViewMode();
    }

    /**
     * Gets current view mode.
     */
    private ViewMode viewMode() {
        return (ViewMode) viewModeComboBox.getSelectedItem();
    }

    /**
     * Sets current view mode and updates related state.
     */
    private void setViewMode(ViewMode viewMode) {
        viewModeComboBox.setSelectedItem(viewMode);
        updateViewMode();
    }

    /**
     * Sets the current view parameters to the default state.
     */
    private void clearViewMode() {
        setViewMode(ViewMode.SET_BIT);
    }

    /**
     * Updates state related to current view mode.
     */
    private void updateViewMode() {
        previousButton.setToolTipText(viewMode().previousToolTip());
        nextButton.setToolTipText(viewMode().nextToolTip());
        setTitle();
    }


    private void refreshAllActions(boolean force) {
        viewBitmapMemoryAction.refresh(force);
        viewBitmapDataAction.refresh(force);
    }

    private int firstVisibleRow() {
        return table.rowAtPoint(new Point(0, scrollPane.getViewport().getViewRect().y));
    }

    private int lastVisibleRow() {
        final Rectangle visible = scrollPane.getViewport().getViewRect();
        return table.rowAtPoint(new Point(0, visible.y + visible.height - 10));
    }

    private boolean rowIsVible(int row) {
        return firstVisibleRow() <= row && row <= lastVisibleRow();
    }

    private void scrollToRowCentered(int row) {
        final int nRows = lastVisibleRow() - firstVisibleRow();
        table.scrollToRows(row - nRows / 3, row + 2 * nRows / 3);
    }

    /**
     * Modal navigation; the kind of move depends on the currently selected view mode.
     */
    private void moveForward() {
        int startIndex = table.getSelectedRow();
        if (!rowIsVible(startIndex)) {
            startIndex = firstVisibleRow();
        }
        int goalIndex = -1;
        switch (viewMode()) {
            case SET_BIT:
                goalIndex = markBitmap.nextSetBitAfter(startIndex);
                break;
            case BLACK:
                goalIndex = markBitmap.nextMarkAfter(startIndex, MarkColor.MARK_BLACK);
                break;
            case GRAY:
                goalIndex = markBitmap.nextMarkAfter(startIndex, MarkColor.MARK_GRAY);
                break;
            case WHITE:
                goalIndex = markBitmap.nextMarkAfter(startIndex, MarkColor.MARK_WHITE);
                break;
            case INVALID:
                goalIndex = markBitmap.nextMarkAfter(startIndex, MarkColor.MARK_INVALID);
                break;
            default:
                InspectorError.unknownCase();
        }
        if (goalIndex < 0) {
            flash(3);
        } else {
            focus().setAddress(markBitmap.heapAddress(goalIndex));
            scrollToRowCentered(goalIndex);
        }
    }

    /**
     * Modal navigation; the kind of move depends on the currently selected view mode.
     */
    private void moveBack() {
        int startIndex = table.getSelectedRow();
        if (!rowIsVible(startIndex)) {
            startIndex = lastVisibleRow();
        }
        int goalIndex = -1;
        switch (viewMode()) {
            case SET_BIT:
                goalIndex = markBitmap.previousSetBitBefore(startIndex);
                break;
            case BLACK:
                goalIndex = markBitmap.previousMarkBefore(startIndex, MarkColor.MARK_BLACK);
                break;
            case GRAY:
                goalIndex = markBitmap.previousMarkBefore(startIndex, MarkColor.MARK_GRAY);
                break;
            case WHITE:
                goalIndex = markBitmap.previousMarkBefore(startIndex, MarkColor.MARK_WHITE);
                break;
            case INVALID:
                goalIndex = markBitmap.previousMarkBefore(startIndex, MarkColor.MARK_INVALID);
                break;
            default:
                InspectorError.unknownCase();
        }
        if (goalIndex < 0) {
            flash(3);
        } else {
            focus().setAddress(markBitmap.heapAddress(goalIndex));
            scrollToRowCentered(goalIndex);
        }
    }

    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        // Memory view displays are sensitive to the current thread selection (for register values)
        forceRefresh();
    }

    @Override
    public void addressFocusChanged(Address oldAddress, Address newAddress) {
        forceRefresh();
    }

    @Override
    public void watchpointSetChanged() {
        if (vm().state().processState() != TERMINATED) {
            forceRefresh();
        }
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<MarkBitmapColumnKind>(inspection(), viewManager.shortName() + " View Options", viewPreferences);
            }
        };
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    private final class ViewBitmapDataAction extends InspectorAction {

        private static final String TITLE = "View Bitmap Data as Array";

        public ViewBitmapDataAction(Inspection inspection) {
            super(inspection, TITLE);
            refresh(true);
        }

        @Override
        protected void procedure() {
            if (markBitmapData != null) {
                focus().setHeapObject(markBitmapData);
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(markBitmapData != null);
        }
    }

    private final class ViewBitmapMemoryAction extends InspectorAction {

        private static final String TITLE = "View Bitmap Memory";

        public ViewBitmapMemoryAction(Inspection inspection) {
            super(inspection, TITLE);
            refresh(true);
        }

        @Override
        protected void procedure() {
            final MaxEntityMemoryRegion<MaxMarkBitmap> memoryRegion = vm().heap().markBitMap().memoryRegion();
            if (memoryRegion.isAllocated()) {
                views().memory().makeView(memoryRegion, null);
            }
        }

        @Override
        public void refresh(boolean force) {
            final MaxMarkBitmap markBitMap = vm().heap().markBitMap();
            setEnabled(markBitMap != null && markBitMap.memoryRegion().isAllocated());
        }
    }

}
