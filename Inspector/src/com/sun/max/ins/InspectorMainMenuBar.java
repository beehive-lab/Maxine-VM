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
package com.sun.max.ins;

import java.awt.*;

import javax.swing.*;

import com.sun.max.ins.gui.*;
import com.sun.max.tele.object.*;

/**
 * MenuBar for the Inspection; shows VM state with background color.
 *
 * @author Michael Van De Vanter
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
        if (!maxVM().isBootImageRelocated()) {
            menu.add(actions.relocateBootImage());
            menu.addSeparator();
        }
        menu.add(actions.setInspectorTraceLevel());
        menu.add(actions.changeInterpreterUseLevel());
        menu.add(actions.setTransportDebugLevel());
        menu.add(actions.runFileCommands());
        menu.add(actions.updateClasspathTypes());
        menu.addSeparator();
        menu.add(actions.refreshAll());
        menu.addSeparator();
        menu.add(actions.closeAllViews());
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
        final JMenuItem viewMemoryRegionsMenuItem = new JMenuItem(actions().viewMemoryRegions());
        viewMemoryRegionsMenuItem.setText("View Memory Regions");
        menu.add(viewMemoryRegionsMenuItem);
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
        final JMenuItem viewBreakpointsMenuItem = new JMenuItem(actions().viewBreakpoints());
        viewBreakpointsMenuItem.setText("View Breakpoints");
        menu.add(viewBreakpointsMenuItem);

        if (maxVM().watchpointsEnabled()) {

            menu.add(actions.genericWatchpointMenuItems());
            final JMenuItem viewWatchpointsMenuItem = new JMenuItem(actions.viewWatchpoints());
            viewWatchpointsMenuItem.setText("View Watchpoints");
            menu.add(viewWatchpointsMenuItem);
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
        menu.addSeparator();
        menu.add(actions.inspectJavaFrameDescriptor());
        return menu;
    }

    private InspectorMenu createTestMenu() {
        final InspectorMenu menu = new InspectorMenu("Test");
        menu.add(actions.viewFocus());
        menu.add(actions.listVMStateHistory());
        menu.add(actions.listCodeRegistry());
        menu.add(actions.listCodeRegistryToFile());
        menu.add(actions.listBreakpoints());

        final InspectorMenu testBreakMenu = new InspectorMenu("Break at");
        final TeleClassMethodActor teleClassMethodActor = actions.inspection().maxVM().teleMethods().InspectableCodeInfo_compilationFinished.teleClassMethodActor();
        final TeleTargetMethod javaTargetMethod = teleClassMethodActor.getJavaTargetMethod(0);
        testBreakMenu.add(actions.setTargetCodeBreakpointAtMethodEntry(javaTargetMethod, inspection().nameDisplay().shortName(javaTargetMethod)));
        menu.add(testBreakMenu);
        return menu;
    }

    private InspectorMenu createHelpMenu() {
        final InspectorMenu menu = new InspectorMenu("Help");
        menu.add(actions.about());
        return menu;
    }

}
