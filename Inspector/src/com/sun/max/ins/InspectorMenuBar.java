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

import javax.swing.*;

/**
 * MenuBar for the Inspection.
 *
 * @author Michael Van De Vanter
 */
public final class InspectorMenuBar extends InspectionHolder {

    // Note (mlvdv); this could all be made static.

    public static JMenuBar create(InspectionActions actions) {
        final InspectorMenuBar inspectionMenuBar = new InspectorMenuBar(actions);
        return inspectionMenuBar.create();
    }

    private final InspectionActions _actions;

    private InspectorMenuBar(InspectionActions actions) {
        super(actions.inspection());
        _actions = actions;
    }

    private JMenuBar create() {
        final JMenuBar menuBar = new JMenuBar();
        menuBar.add(createInspectionMenu());
        menuBar.add(createClassMenu());
        menuBar.add(createObjectMenu());
        menuBar.add(createMemoryMenu());
        menuBar.add(createMethodMenu());
        if (_actions.inspection().hasProcess()) {
            menuBar.add(createDebugMenu());
        }
        menuBar.add(createViewMenu());
        menuBar.add(createJavaMenu());
        menuBar.add(createTestMenu());
        menuBar.add(createHelpMenu());
        return menuBar;
    }

    private JMenu createInspectionMenu() {
        final JMenu inspectionMenu = new JMenu("Inspector");
        if (MaxineInspector.suspendingBeforeRelocating()) {
            inspectionMenu.add(_actions.relocateBootImage());
            inspectionMenu.addSeparator();
        }
        inspectionMenu.add(_actions.setInspectorTraceLevel());
        inspectionMenu.add(_actions.changeInterpreterUseLevel());
        inspectionMenu.add(_actions.setTransportDebugLevel());
        inspectionMenu.add(_actions.runFileCommands());
        inspectionMenu.add(_actions.updateClasspathTypes());
        inspectionMenu.addSeparator();
        inspectionMenu.add(_actions.refreshAll());
        inspectionMenu.addSeparator();
        inspectionMenu.add(_actions.closeAll());
        inspectionMenu.addSeparator();
        inspectionMenu.add(_actions.preferences());
        inspectionMenu.addSeparator();
        inspectionMenu.add(_actions.quit());
        return inspectionMenu;
    }

    private JMenu createClassMenu() {
        final JMenu menu = new JMenu("Class");
        menu.add(_actions.inspectClassActorByName());
        menu.add(_actions.inspectClassActorByHexId());
        menu.add(_actions.inspectClassActorByDecimalId());
        return menu;
    }

    private JMenu createObjectMenu() {
        final JMenu menu = new JMenu("Object");
        menu.add(_actions.inspectBootClassRegistry());
        menu.add(_actions.inspectObject());
        menu.add(_actions.inspectObjectByID());
        return menu;
    }

    private JMenu createMemoryMenu() {
        final JMenu menu = new JMenu("Memory");

        final JMenu wordsMenu = new JMenu("As words");
        wordsMenu.add(_actions.inspectBootHeapMemoryWords());
        wordsMenu.add(_actions.inspectBootCodeMemoryWords());
        wordsMenu.add(_actions.inspectMemoryWords());
        menu.add(wordsMenu);

        final JMenu bytesMenu = new JMenu("As bytes");
        bytesMenu.add(_actions.inspectBootHeapMemory());
        bytesMenu.add(_actions.inspectBootCodeMemory());
        bytesMenu.add(_actions.inspectMemory());
        menu.add(bytesMenu);
        return menu;
    }

    private JMenu createMethodMenu() {
        final JMenu menu = new JMenu("Method");
        menu.add(_actions.inspectMethodActorByName());
        menu.add(_actions.viewMethodCodeAtSelection());
        menu.add(_actions.viewMethodCodeAtIP());
        menu.add(_actions.viewMethodBytecodeByName());
        menu.add(_actions.viewMethodTargetCodeByName());
        final JMenu sub = new JMenu("View boot image method code");
        sub.add(_actions.viewRunMethodCodeInBootImage());
        sub.add(_actions.viewThreadRunMethodCodeInBootImage());
        sub.add(_actions.viewSchemeRunMethodCodeInBootImage());
        menu.add(sub);
        menu.add(_actions.viewMethodCodeByAddress());
        menu.add(_actions.viewNativeCodeByAddress());
        return menu;
    }

    private JMenu createDebugMenu() {
        final JMenu menu = new JMenu("Debug");
        menu.add(_actions.debugResume());
        menu.add(_actions.debugSingleStep());
        menu.add(_actions.debugStepOverWithBreakpoints());
        menu.add(_actions.debugStepOver());
        menu.add(_actions.debugReturnFromFrameWithBreakpoints());
        menu.add(_actions.debugReturnFromFrame());
        menu.add(_actions.debugRunToInstructionWithBreakpoints());
        menu.add(_actions.debugRunToInstruction());
        menu.addSeparator();
        menu.add(_actions.toggleTargetCodeBreakpoint());
        menu.add(_actions.setTargetCodeLabelBreakpoints());
        menu.add(_actions.removeTargetCodeLabelBreakpoints());
        final JMenu methodEntryBreakpoints = new JMenu("Break at method entry");
        methodEntryBreakpoints.add(_actions.setTargetCodeBreakpointAtMethodEntriesByName());
        methodEntryBreakpoints.add(_actions.setBytecodeBreakpointAtMethodEntryByName());
        methodEntryBreakpoints.add(_actions.setBytecodeBreakpointAtMethodEntryByKey());
        menu.add(methodEntryBreakpoints);
        menu.add(_actions.removeAllTargetCodeBreakpoints());
        menu.addSeparator();
        menu.add(_actions.viewBreakpoints());
        menu.addSeparator();
        menu.add(_actions.toggleBytecodeBreakpoint());
        menu.add(_actions.debugPause());
        return menu;
    }

    private JMenu createViewMenu() {
        final JMenu menu = new JMenu("View");
        menu.add(_actions.viewBootImage());
        if (_actions.inspection().hasProcess()) {
            menu.add(_actions.viewMemoryRegions());
            menu.add(_actions.viewThreads());
            menu.add(_actions.viewRegisters());
            menu.add(_actions.viewStack());
            menu.add(_actions.viewMethodCode());
            menu.add(_actions.viewBreakpoints());
        }
        return menu;
    }

    private JMenu createJavaMenu() {
        final JMenu menu = new JMenu("Java");
        menu.add(_actions.setVMTraceLevel());
        menu.add(_actions.setVMTraceThreshold());
        menu.addSeparator();
        menu.add(_actions.inspectJavaFrameDescriptor());
        return menu;
    }

    private JMenu createTestMenu() {
        final JMenu menu = new JMenu("Test");
        menu.add(_actions.listCodeRegistry());
        menu.add(_actions.listCodeRegistryToFile());
        menu.add(_actions.disassembleAll());
        return menu;
    }

    private JMenu createHelpMenu() {
        final JMenu menu = new JMenu("Help");
        menu.add(_actions.about());
        return menu;
    }

}
