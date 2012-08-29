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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;


public class MarkBitsView extends AbstractView<MarkBitsView> {
    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.MARK_BITS_INFO;
    private static final String SHORT_NAME = "Mark Bits";
    private static final String LONG_NAME = "Mark Bits View";
    private static final String GEOMETRY_SETTINGS_KEY = "markBitsViewGeometry";

    public static final class MarkBitsViewManager extends AbstractSingletonViewManager<MarkBitsView> {
        protected MarkBitsViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        @Override
        protected MarkBitsView createView(Inspection inspection) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isSupported() {
            return vm().heap().markBitMap() != null;
        }
    }

    private static MarkBitsViewManager viewManager = null;

    public static MarkBitsViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new MarkBitsViewManager(inspection);
        }
        return viewManager;
    }

    private MarkBitsTable table;

    protected MarkBitsView(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        createFrame(true);
        Trace.end(TRACE_VALUE,  tracePrefix() + " initializing");
    }

    @Override
    protected void createViewContent() {
        table = new MarkBitsTable(inspection(), this);
        setContentPane(new InspectorScrollPane(inspection(), table));

        // Populate menu bar
        makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        final InspectorMenu editMenu = makeMenu(MenuKind.EDIT_MENU);
        editMenu.add(actions().setMarkBitAtAddress());
        editMenu.add(actions().setMarkBitAtIndex());
        editMenu.addSeparator();
        editMenu.add(actions().removeSelectedMarkBit());
        editMenu.add(actions().removeAllMarkBits());

        final InspectorMenu memoryMenu = makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(actions().viewSelectedMemoryWatchpointAction());
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));

        makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));
    }

    @Override
    protected void refreshState(boolean force) {
        table.refresh(force);
    }

    @Override
    public String getTextForTitle() {
        return viewManager.shortName();
    }

}
