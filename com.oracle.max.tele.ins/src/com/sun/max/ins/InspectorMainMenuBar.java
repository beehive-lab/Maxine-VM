/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;

import javax.swing.*;

import com.sun.max.ins.gui.*;
import com.sun.max.ins.view.InspectionViews.*;

/**
 * MenuBar for the Inspection; shows VM state with background color.
 */
public final class InspectorMainMenuBar extends InspectorMenuBar {

    private final InspectionActions actions;

    public InspectorMainMenuBar(InspectionActions actions) {
        super(actions.inspection());
        this.actions = actions;
        addMenus();
    }

    private void addMenus() {
        add(createInspectionMenu());
        add(createMemoryMenu());
        add(createObjectMenu());
        add(createCodeMenu());
        if (inspection().hasProcess()) {
            add(createDebugMenu());
        }
        add(createViewMenu());
        add(createJavaMenu());
        add(createTestMenu());
        add(createHelpMenu());
    }

    @Override
    public JMenu add(JMenu menu) {
        menu.setOpaque(false);
        super.add(menu);
        return menu;
    }

    @Override
    public void redisplay() {
    }

    @Override
    public void refresh(boolean force) {
    }

    /**
     * Change the appearance to reflect the current state of the VM.
     */
    public void setStateColor(Color color) {
        setBackground(color);
    }

    private InspectorMenu createInspectionMenu() {
        final InspectorMenu menu = new InspectorMenu("Inspector");
        if (!vm().isBootImageRelocated()) {
            menu.add(actions.relocateBootImage());
            menu.addSeparator();
        }
        menu.add(actions.setInspectorTraceLevel());
        menu.add(actions.changeInterpreterUseLevel());
        menu.add(actions.setTransportDebugLevel());
        menu.add(actions.runFileCommands());
        menu.add(actions.loadNativeCodeMapFromFile());
        menu.add(actions.updateClasspathTypes());
        menu.addSeparator();
        menu.add(actions.refreshAll());
        menu.addSeparator();
        menu.add(views().deactivateAllViewsAction());
        menu.addSeparator();
        menu.add(actions.preferences());
        menu.addSeparator();
        menu.add(actions.quit());
        return menu;
    }

    private InspectorMenu createObjectMenu() {
        final InspectorMenu menu = new InspectorMenu("Object");
        menu.add(actions.genericObjectMenuItems());
        return menu;
    }

    private InspectorMenu createMemoryMenu() {
        final InspectorMenu menu = new InspectorMenu("Memory");
        menu.add(actions.genericMemoryMenuItems());
        menu.add(views().activateSingletonViewAction(ViewKind.ALLOCATIONS));
        return menu;
    }

    private InspectorMenu createCodeMenu() {
        final InspectorMenu menu = new InspectorMenu("Code");
        menu.add(actions.genericCodeMenuItems());
        return menu;
    }

    private InspectorMenu createDebugMenu() {
        final InspectorMenu menu = new InspectorMenu("Debug");
        menu.add(actions.debugResume());
        menu.add(actions.debugSingleStep());
        menu.add(actions.debugStepOverWithBreakpoints());
        menu.add(actions.debugStepOver());
        menu.add(actions.debugReturnFromFrameWithBreakpoints());
        menu.add(actions.debugReturnFromFrame());
        menu.add(actions.debugRunToSelectedInstructionWithBreakpoints());
        menu.add(actions.debugRunToSelectedInstruction());
        menu.add(actions.debugRunToNextCallWithBreakpoints());
        menu.add(actions.debugRunToNextCall());
        menu.add(actions.debugPause());

        menu.addSeparator();
        menu.add(actions.genericBreakpointMenuItems());
        menu.add(views().activateSingletonViewAction(ViewKind.BREAKPOINTS));

        if (vm().watchpointManager() != null) {
            menu.add(actions.genericWatchpointMenuItems());
            menu.add(views().activateSingletonViewAction(ViewKind.WATCHPOINTS));
        }

        return menu;
    }

    public InspectorMenu createViewMenu() {
        final InspectorMenu menu = new InspectorMenu("View");
        menu.add(actions().genericViewMenuItems());
        return menu;
    }

    private InspectorMenu createJavaMenu() {
        final InspectorMenu menu = new InspectorMenu("Java");
        menu.add(actions.setVMTraceLevel());
        menu.add(actions.setVMTraceThreshold());
        return menu;
    }

    private InspectorMenu createTestMenu() {
        final InspectorMenu menu = new InspectorMenu("Test");
        menu.add(views().activateSingletonViewAction(ViewKind.USER_FOCUS));
        menu.add(actions.listVMStateHistory());
        menu.add(actions.listThreads());
        menu.add(actions.listStackFrames());
        menu.add(actions.listCodeRegistry());
        menu.add(actions.listCodeRegistryToFile());
        menu.add(actions.listBreakpoints());
        if (vm().watchpointManager() != null) {
            menu.add(actions.listWatchpoints());
        }
        menu.add(actions.listInspectableMethods());
        menu.add(actions.listSettings());
        return menu;
    }

    private InspectorMenu createHelpMenu() {
        final InspectorMenu menu = new InspectorMenu("Help");
        menu.add(actions.aboutSession("Session info"));
        menu.add(actions.aboutMaxine("Project info"));
        return menu;
    }

}
