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
        add(createClassMenu());
        add(createObjectMenu());
        add(createMemoryMenu());
        add(createMethodMenu());
        if (inspection().hasProcess()) {
            add(createDebugMenu());
        }
        add(createViewMenu());
        add(createJavaMenu());
        add(createTestMenu());
        add(createHelpMenu());
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

    private JMenu createInspectionMenu() {
        final JMenu menu = new JMenu("Inspector");
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

    private JMenu createClassMenu() {
        final JMenu menu = new JMenu("Class");
        menu.add(actions.inspectClassActorByName());
        menu.add(actions.inspectClassActorByHexId());
        menu.add(actions.inspectClassActorByDecimalId());
        return menu;
    }

    private JMenu createObjectMenu() {
        final JMenu menu = new JMenu("Object");
        menu.add(actions.inspectBootClassRegistry());
        menu.add(actions.inspectObject());
        menu.add(actions.inspectObjectByID());
        return menu;
    }

    private JMenu createMemoryMenu() {
        final JMenu menu = new JMenu("Memory");

        final JMenu wordsMenu = new JMenu("As words");
        wordsMenu.add(actions.inspectBootHeapMemoryWords());
        wordsMenu.add(actions.inspectBootCodeMemoryWords());
        wordsMenu.add(actions.inspectMemoryWords());
        menu.add(wordsMenu);

        final JMenu bytesMenu = new JMenu("As bytes");
        bytesMenu.add(actions.inspectBootHeapMemory());
        bytesMenu.add(actions.inspectBootCodeMemory());
        bytesMenu.add(actions.inspectMemory());
        menu.add(bytesMenu);
        return menu;
    }

    private JMenu createMethodMenu() {
        final JMenu menu = new JMenu("Method");
        menu.add(actions.inspectMethodActorByName());
        menu.add(actions.viewMethodCodeAtSelection());
        menu.add(actions.viewMethodCodeAtIP());
        menu.add(actions.viewMethodBytecodeByName());
        menu.add(actions.viewMethodTargetCodeByName());
        final JMenu sub = new JMenu("View boot image method code");
        sub.add(actions.viewRunMethodCodeInBootImage());
        sub.add(actions.viewThreadRunMethodCodeInBootImage());
        sub.add(actions.viewSchemeRunMethodCodeInBootImage());
        menu.add(sub);
        menu.add(actions.viewMethodCodeByAddress());
        menu.add(actions.viewNativeCodeByAddress());
        menu.add(actions.viewRuntimeStubByAddress());
        return menu;
    }

    private JMenu createDebugMenu() {
        final JMenu menu = new JMenu("Debug");
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
        final JMenuItem viewBreakpointsMenuItem = new JMenuItem(actions.viewBreakpoints());
        viewBreakpointsMenuItem.setText("View Breakpoints");
        menu.add(viewBreakpointsMenuItem);
        final JMenu methodEntryBreakpoints = new JMenu("Break at method entry");
        methodEntryBreakpoints.add(actions.setTargetCodeBreakpointAtMethodEntriesByName());
        methodEntryBreakpoints.add(actions.setBytecodeBreakpointAtMethodEntryByName());
        methodEntryBreakpoints.add(actions.setBytecodeBreakpointAtMethodEntryByKey());
        menu.add(methodEntryBreakpoints);
        menu.add(actions.setTargetCodeBreakpointAtObjectInitializer());
        menu.add(actions.removeAllBreakpoints());
        menu.addSeparator();
        menu.add(actions.toggleTargetCodeBreakpoint());
        menu.add(actions.setTargetCodeLabelBreakpoints());
        menu.add(actions.removeTargetCodeLabelBreakpoints());
        menu.add(actions.removeAllTargetCodeBreakpoints());
        menu.addSeparator();
        menu.add(actions.toggleBytecodeBreakpoint());
        menu.add(actions.removeAllBytecodeBreakpoints());
        if (maxVM().watchpointsEnabled()) {
            menu.addSeparator();
            final JMenuItem viewWatchpointsMenuItem = new JMenuItem(actions.viewWatchpoints());
            viewWatchpointsMenuItem.setText("View Watchpoints");
            menu.add(viewWatchpointsMenuItem);
            menu.add(actions.setWordWatchpoint());
        }

        return menu;
    }

    public JMenu createViewMenu() {
        final JMenu menu = new JMenu("View");
        menu.add(actions.viewBootImage());
        menu.add(actions.viewMemoryRegions());
        menu.add(actions.viewThreads());
        menu.add(actions.viewVmThreadLocals());
        menu.add(actions.viewRegisters());
        menu.add(actions.viewStack());
        menu.add(actions.viewMethodCode());
        menu.add(actions.viewBreakpoints());
        if (maxVM().watchpointsEnabled()) {
            menu.add(actions.viewWatchpoints());
        }
        return menu;
    }

    private JMenu createJavaMenu() {
        final JMenu menu = new JMenu("Java");
        menu.add(actions.setVMTraceLevel());
        menu.add(actions.setVMTraceThreshold());
        menu.addSeparator();
        menu.add(actions.inspectJavaFrameDescriptor());
        return menu;
    }

    private JMenu createTestMenu() {
        final JMenu menu = new JMenu("Test");
        menu.add(actions.viewFocus());
        menu.add(actions.listVMStateHistory());
        menu.add(actions.listCodeRegistry());
        menu.add(actions.listCodeRegistryToFile());
        return menu;
    }

    private JMenu createHelpMenu() {
        final JMenu menu = new JMenu("Help");
        menu.add(actions.about());
        return menu;
    }

}
