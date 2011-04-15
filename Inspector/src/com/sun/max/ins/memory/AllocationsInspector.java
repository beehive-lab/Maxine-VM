/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.tele.MaxProcessState.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;

/**
 * A singleton inspector that displays a list of {@linkplain MaxMemoryRegion memory regions} that have been allocated in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class AllocationsInspector extends Inspector implements TableColumnViewPreferenceListener {

    private static final int TRACE_VALUE = 2;
    private static final ViewKind VIEW_KIND = ViewKind.ALLOCATIONS;
    private static final String SHORT_NAME = "Allocations";
    private static final String LONG_NAME = "Allocations Inspector";
    private static final String GEOMETRY_SETTINGS_KEY = "allocationsInspectorGeometry";

    private static final class AllocationsViewManager extends AbstractSingletonViewManager<AllocationsInspector> {

        protected AllocationsViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        public boolean isSupported() {
            return true;
        }

        public boolean isEnabled() {
            return true;
        }

        @Override
        protected AllocationsInspector createView(Inspection inspection) {
            return new AllocationsInspector(inspection);
        }

    }

    // Will be non-null before any instances created.
    private static AllocationsViewManager viewManager = null;

    public static ViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new AllocationsViewManager(inspection);
        }
        return viewManager;
    }

    // This is a singleton viewer, so only use a single level of view preferences.
    private final MemoryAllocationsViewPreferences viewPreferences;

    private InspectorPanel contentPane;

    private MemoryAllocationsTable table;

    private TableRowFilterToolBar filterToolBar = null;
    private JCheckBoxMenuItem showFilterCheckboxMenuItem;
    private int[] filterMatchingRows = null;

    private AllocationsInspector(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        Trace.begin(1, tracePrefix() + "initializing");
        viewPreferences = MemoryAllocationsViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);
        showFilterCheckboxMenuItem = new InspectorCheckBox(inspection, "Filter view", "Show Filter Field", false);
        showFilterCheckboxMenuItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBoxMenuItem checkBoxMenuItem = (JCheckBoxMenuItem) e.getSource();
                if (checkBoxMenuItem.isSelected()) {
                    openFilter();
                } else {
                    closeFilter();
                }
            }
        });
        final InspectorFrame frame = createFrame(true);

        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(actions().inspectSelectedMemoryRegion());
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));

        final InspectorMenuItems defaultViewMenuItems = defaultMenuItems(MenuKind.VIEW_MENU);
        final InspectorMenu viewMenu = frame.makeMenu(MenuKind.VIEW_MENU);
        viewMenu.add(showFilterCheckboxMenuItem);
        viewMenu.addSeparator();
        viewMenu.add(defaultViewMenuItems);

        Trace.end(1, tracePrefix() + "initializing");
    }

    @Override
    public String getTextForTitle() {
        String title = viewManager.shortName();
        if (!inspection().hasProcess()) {
            title += ": " + inspection().nameDisplay().noProcessShortText();
        }
        return title;
    }

    @Override
    protected InspectorTable getTable() {
        return table;
    }

    @Override
    protected void createView() {
        if (vm().state().processState() == TERMINATED) {
            table = null;
            contentPane = new InspectorPanel(inspection());
        } else {
            table = new MemoryAllocationsTable(inspection(), viewPreferences);
            final InspectorScrollPane memoryAllocationsScrollPane = new InspectorScrollPane(inspection(), table);
            contentPane = new InspectorPanel(inspection(), new BorderLayout());
            contentPane.add(memoryAllocationsScrollPane, BorderLayout.CENTER);
        }
        setContentPane(contentPane);
    }

    @Override
    protected void refreshState(boolean force) {
        if (inspection().hasProcess()) {
            table.refresh(force);
        }
        if (filterToolBar != null) {
            filterToolBar.refresh(force);
        }
    }

    @Override
    public void memoryRegionFocusChanged(MaxMemoryRegion oldMemoryRegion, MaxMemoryRegion memoryRegion) {
        if (table != null) {
            table.updateFocusSelection();
        }
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
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<MemoryAllocationsColumnKind>(inspection(), viewManager.shortName() + " View Options", viewPreferences);
            }
        };
    }

    @Override
    public InspectorAction getPrintAction() {
        return getDefaultPrintAction();
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    private final RowMatchListener rowMatchListener = new RowMatchListener() {

        public void setSearchResult(int[] result) {
            filterMatchingRows = result;
            table.setDisplayedRows(filterMatchingRows);
            System.out.println("Match=" + Arrays.toString(filterMatchingRows));
        }

        public void closeRequested() {
            closeFilter();
            showFilterCheckboxMenuItem.setState(false);
        }
    };

    private void openFilter() {
        if (filterToolBar == null) {
            filterToolBar = new TableRowFilterToolBar(inspection(), rowMatchListener, table);
            contentPane.add(filterToolBar, BorderLayout.NORTH);
            pack();
            filterToolBar.getFocus();
        }
    }

    private void closeFilter() {
        if (filterToolBar != null) {
            contentPane.remove(filterToolBar);
            table.setDisplayedRows(null);
            pack();
            filterToolBar = null;
            filterMatchingRows = null;
        }
    }

    @Override
    public void vmProcessTerminated() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

}
