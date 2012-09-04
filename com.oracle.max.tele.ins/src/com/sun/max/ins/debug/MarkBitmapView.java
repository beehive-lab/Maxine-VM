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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.TableColumnViewPreferenceListener;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;

public class MarkBitmapView extends AbstractView<MarkBitmapView> implements TableColumnViewPreferenceListener {
    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.MARK_BITMAP;
    private static final String SHORT_NAME = "Mark Bitmap";
    private static final String LONG_NAME = "Mark Bitmap View";
    private static final String GEOMETRY_SETTINGS_KEY = "markBitmapViewGeometry";

    public static final class MarkBitmapViewManager extends AbstractSingletonViewManager<MarkBitmapView> {
        protected MarkBitmapViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
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

    private InspectorAction viewBitmapMemoryAction;
    private InspectorAction viewBitmapDataAction;
    private MarkBitmapAction setMarkBitAtIndexAction;
    private MarkBitmapAction setMarkBitAtAddressAction;
    private MarkBitmapAction removeSelectedMarkBitAction;
    private MarkBitmapAction removeAllMarkBitsAction;

    private MaxMarkBitmap markBitmap;
    private MaxObject markBitmapData = null;
    private MemoryColoringTable table;

    private final InspectorPanel nullPanel;

    // This is a singleton viewer, so only use a single level of view preferences.
    private final MarkBitmapViewPreferences viewPreferences;

    protected MarkBitmapView(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);

        viewPreferences = MarkBitmapViewPreferences.globalPreferences(inspection());
        viewPreferences.addListener(this);
        this.viewBitmapMemoryAction = new ViewBitmapMemoryAction(inspection);
        this.viewBitmapDataAction = new ViewBitmapDataAction(inspection);
        this.setMarkBitAtIndexAction = new SetMarkBitAtIndexAction(inspection);
        this.setMarkBitAtAddressAction = new SetMarkBitAtAddressAction(inspection);
        this.removeSelectedMarkBitAction = new RemoveSelectedMarkBitAction(inspection);
        this.removeAllMarkBitsAction = new RemoveAllMarkBitsAction(inspection);

        nullPanel = new InspectorPanel(inspection(), new BorderLayout());
        nullPanel.add(new PlainLabel(inspection(), inspection().nameDisplay().unavailableDataShortText()), BorderLayout.PAGE_START);

        markBitmap = vm().heap().markBitMap();

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
        if (markBitmap == null) {
            setContentPane(nullPanel);
        } else {
            markBitmapData = markBitmap.representation();
            table = new MemoryColoringTable(inspection(), this, markBitmap, viewPreferences);
            setContentPane(new InspectorScrollPane(inspection(), table));
        }

        // Populate menu bar
        makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu editMenu = makeMenu(MenuKind.EDIT_MENU);
        editMenu.add(setMarkBitAtAddressAction);
        editMenu.add(setMarkBitAtIndexAction);
        editMenu.addSeparator();
        editMenu.add(removeSelectedMarkBitAction);
        editMenu.add(removeAllMarkBitsAction);

        final InspectorMenu memoryMenu = makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(viewBitmapMemoryAction);
        memoryMenu.add(actions().viewSelectedMemoryWatchpointAction());
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));

        final InspectorMenu objectMenu = makeMenu(MenuKind.OBJECT_MENU);
        objectMenu.add(viewBitmapDataAction);
        objectMenu.add(defaultMenuItems(MenuKind.OBJECT_MENU));

        makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        refreshAllActions(true);
    }

    @Override
    protected void refreshState(boolean force) {
        if (markBitmap == null && vm().heap().markBitMap() != null) {
            // Has been allocated since we last check;  assume it won't change now.
            markBitmap = vm().heap().markBitMap();
            markBitmapData = markBitmap.representation();
            table = new MemoryColoringTable(inspection(), this, markBitmap, viewPreferences);
            setContentPane(new InspectorScrollPane(inspection(), table));
        }
        refreshAllActions(force);
                    // table.refresh(force);
    }

    private void refreshAllActions(boolean force) {
        viewBitmapMemoryAction.refresh(force);
        viewBitmapDataAction.refresh(force);
        setMarkBitAtIndexAction.refresh(force);
        setMarkBitAtAddressAction.refresh(force);
        removeSelectedMarkBitAction.refresh(force);
        removeAllMarkBitsAction.refresh(force);
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
            final MaxEntityMemoryRegion<MaxMarkBitmap> memoryRegion = markBitmap.memoryRegion();
            if (memoryRegion.isAllocated()) {
                views().memory().makeView(memoryRegion, null);
            }
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(markBitmap != null && markBitmap.memoryRegion().isAllocated());
        }
    }


    private abstract class MarkBitmapAction extends InspectorAction {

        public MarkBitmapAction(Inspection inspection, String title) {
            super(inspection, title);
            refresh(true);
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(markBitmap != null);
        }
    }

    private final class SetMarkBitAtIndexAction extends MarkBitmapAction {

        private static final String TITLE = "SetMarkBitAtIndex";

        public SetMarkBitAtIndexAction(Inspection inspection) {
            super(inspection, TITLE);
        }

        @Override
        protected void procedure() {
            gui().errorMessage(TITLE + " not implemented yet");
        }
    }

    private final class SetMarkBitAtAddressAction extends MarkBitmapAction {

        private static final String TITLE = "SetMarkBitAtAddress";

        public SetMarkBitAtAddressAction(Inspection inspection) {
            super(inspection, TITLE);
        }

        @Override
        protected void procedure() {
            gui().errorMessage(TITLE + " not implemented yet");
        }
    }

    private final class RemoveSelectedMarkBitAction extends MarkBitmapAction {

        private static final String TITLE = "RemoveSelectedMarkBit";

        public RemoveSelectedMarkBitAction(Inspection inspection) {
            super(inspection, TITLE);
        }

        @Override
        protected void procedure() {
            gui().errorMessage(TITLE + " not implemented yet");
        }
    }

    private final class RemoveAllMarkBitsAction extends MarkBitmapAction {

        private static final String TITLE = "RemoveAllMarkBits";

        public RemoveAllMarkBitsAction(Inspection inspection) {
            super(inspection, TITLE);
        }

        @Override
        protected void procedure() {
            gui().errorMessage(TITLE + " not implemented yet");
        }
    }

}
