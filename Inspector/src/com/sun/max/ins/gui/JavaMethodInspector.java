/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectorNameDisplay.ReturnTypeSpecification;
import com.sun.max.ins.method.*;
import com.sun.max.ins.util.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.member.*;

/**
 * Visual inspector and debugger for a Java method and other routines in the VM, able to display one or more kinds of code
 * associated with the method: compiled code, bytecode, and source.
 *
 * @author Michael Van De Vanter
 */
public class JavaMethodInspector extends MethodInspector {

    private final MethodInspectorPreferences methodInspectorPreferences;

    /**
     * Shared check boxes to be used in all UI code view selection.
     */
    private final JCheckBoxMenuItem[] codeViewCheckBoxes = new JCheckBoxMenuItem[MethodCodeKind.values().length];

    private final TeleClassMethodActor teleClassMethodActor;
    private final MethodCodeKind requestedCodeKind;

    /**
     * A particular compilation of the method, to which this Inspector is permanently bound, and which distinguishes
     * this Inspector uniquely from others that may be viewing the same method but bound to a different compilation.
     * Null when this Inspector is not bound to any compilation, in which case this Inspector is the unique (unbound)
     * inspector for the method.
     */
    private final MaxCompiledCode compiledCode;

    /**
     * An Inspector for a Java Method associated with a specific compilation, and which association does not change
     * for the life of the inspector.
     *
     * @param inspection the {@link Inspection} of which this Inspector is part
     * @param parent the tabbed container for this Inspector
     * @param compiledCode surrogate for the compilation in the VM
     * @param codeKind request for a particular code view to be displayed initially
     */
    public JavaMethodInspector(Inspection inspection, MethodInspectorContainer parent, MaxCompiledCode compiledCode, MethodCodeKind codeKind) {
        this(inspection, parent, compiledCode, compiledCode.getTeleClassMethodActor(), codeKind);
    }

    /**
     * Creates an Inspector for a Java Method without association to any compilation, and which can thus view only bytecodes or
     * source code. If a user, within the context of this Inspector, requests a view of an associated compilation, then
     * another existing Inspector associated with the specified compilation must be located or a new one created; in
     * either case, the resulting Inspector replaces this one.
     *
     * @param inspection the {@link Inspection} of which this Inspector is part
     * @param parent the tabbed container for this Inspector
     * @param teleClassMethodActor surrogate for the specified Java method in the VM
     * @param codeKind requested kind of code view: either source code or bytecodes
     */
    public JavaMethodInspector(Inspection inspection, MethodInspectorContainer parent, TeleClassMethodActor teleClassMethodActor, MethodCodeKind codeKind) {
        this(inspection, parent, null, teleClassMethodActor, codeKind);
        assert codeKind != MethodCodeKind.TARGET_CODE;
    }

    private JavaMethodInspector(Inspection inspection, MethodInspectorContainer parent, MaxCompiledCode compiledCode, TeleClassMethodActor teleClassMethodActor, MethodCodeKind requestedCodeKind) {
        super(inspection, parent);

        this.methodInspectorPreferences = MethodInspectorPreferences.globalPreferences(inspection);
        this.teleClassMethodActor = teleClassMethodActor;
        this.compiledCode = compiledCode;
        this.requestedCodeKind = requestedCodeKind;

        // enable choice if target code is present, even though this Inspector is not bound to a TargetMethod
        codeKindEnabled.put(MethodCodeKind.TARGET_CODE, compiledCode != null || teleClassMethodActor.hasTargetMethod());
        // enable if bytecodes present
        codeKindEnabled.put(MethodCodeKind.BYTECODES, (teleClassMethodActor == null) ? false : teleClassMethodActor.hasCodeAttribute());
        // not implemented yet
        codeKindEnabled.put(MethodCodeKind.JAVA_SOURCE, false);

        // Create shared check boxes that will track and help control what views are visible.
        // Invariant: checkbox selected iff the code kind is in {@link #codeViewers}.
        for (final MethodCodeKind codeKind : MethodCodeKind.values()) {
            // The check box settings can either be changed by user action on the check box
            // itself, or by other actions that add/remove code viewers.  There are no code
            // viewers present at this point in the construction of the inspector.
            final boolean currentValue = false;
            final String toolTipText = "Display this kind of source for the Java method?";
            final JCheckBoxMenuItem checkBox = new InspectorCheckBox(inspection(), codeKind.toString(), toolTipText, currentValue);
            checkBox.setEnabled(codeKindEnabled.get(codeKind));
            checkBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent itemEvent) {
                    // Catch check box events where the user wants to open/close a code viewer
                    if (checkBox.isSelected()) {
                        if (!codeViewers.containsKey(codeKind)) {
                            addCodeViewer(codeKind);
                        }
                    } else if (codeViewers.containsKey(codeKind)) {
                        closeCodeViewer(codeViewers.get(codeKind));
                    }
                }
            });
            codeViewCheckBoxes[codeKind.ordinal()] = checkBox;
        }

        final InspectorFrame frame = createTabFrame(parent);
        final InspectorMenu editMenu = frame.makeMenu(MenuKind.EDIT_MENU);
        final InspectorMenu objectMenu = frame.makeMenu(MenuKind.OBJECT_MENU);
        final InspectorMenu codeMenu = frame.makeMenu(MenuKind.CODE_MENU);
        final InspectorMenu debugMenu = frame.makeMenu(MenuKind.DEBUG_MENU);
        final InspectorMenu breakOnEntryMenu = new InspectorMenu("Break at this method entry");
        final InspectorMenu breakAtLabelsMenu = new InspectorMenu("Break at this method labels");

        if (compiledCode != null) {
            final InspectorAction copyAction = actions().copyCompiledCodeToClipboard(compiledCode, null);
            copyAction.setEnabled(true);
            editMenu.add(copyAction);
            objectMenu.add(actions().inspectObject(compiledCode.teleTargetMethod(), "Compiled method: " + compiledCode.classActorForObjectType().simpleName()));
        }

        if (teleClassMethodActor != null) {
            objectMenu.add(actions().inspectObject(teleClassMethodActor, "Method: " + teleClassMethodActor.classActorForObjectType().simpleName()));
            final TeleClassActor teleClassActor = teleClassMethodActor.getTeleHolder();
            objectMenu.add(actions().inspectObject(teleClassActor, "Holder: " + teleClassActor.classActorForObjectType().simpleName()));
            objectMenu.add(actions().inspectSubstitutionSourceClassActorAction(teleClassMethodActor));
            objectMenu.add(actions().inspectTargetMethodCompilationsMenu(teleClassMethodActor, "Method compilations:"));
            objectMenu.add(defaultMenuItems(MenuKind.OBJECT_MENU));
        }
        for (final MethodCodeKind codeKind : MethodCodeKind.values()) {
            codeMenu.add(codeViewCheckBoxes[codeKind.ordinal()]);
        }
        if (teleClassMethodActor != null) {
            codeMenu.add(actions().viewTargetMethodCodeMenu(teleClassMethodActor, "View method's compilations"));
        }
        codeMenu.add(defaultMenuItems(MenuKind.CODE_MENU));

        if (compiledCode != null) {
            breakOnEntryMenu.add(actions().setMachineCodeBreakpointAtEntry(compiledCode, "Target code"));
        }
        if (teleClassMethodActor != null) {
            breakOnEntryMenu.add(actions().setBytecodeBreakpointAtMethodEntry(teleClassMethodActor, "Bytecodes"));
        }
        debugMenu.add(breakOnEntryMenu);
        if (compiledCode != null) {
            breakAtLabelsMenu.add(actions().setMachineCodeLabelBreakpoints(compiledCode, "Add target code breakpoints"));
            breakAtLabelsMenu.add(actions().removeMachineCodeLabelBreakpoints(compiledCode, "Remove target code breakpoints"));
        }
        debugMenu.add(breakAtLabelsMenu);
        if (teleClassMethodActor != null) {
            debugMenu.add(actions().debugInvokeMethod(teleClassMethodActor, "Invoke this method"));
        }
        debugMenu.addSeparator();
        debugMenu.add(actions().genericBreakpointMenuItems());
        final JMenuItem viewBreakpointsMenuItem = new JMenuItem(actions().viewBreakpoints());
        viewBreakpointsMenuItem.setText("View Breakpoints");
        debugMenu.add(viewBreakpointsMenuItem);
        if (vm().watchpointManager() != null) {
            debugMenu.add(actions().genericWatchpointMenuItems());
            final JMenuItem viewWatchpointsMenuItem = new JMenuItem(actions().viewWatchpoints());
            viewWatchpointsMenuItem.setText("View Watchpoints");
            debugMenu.add(viewWatchpointsMenuItem);
        }
    }

    @Override
    public MaxCompiledCode machineCode() {
        return compiledCode;
    }

    @Override
    public TeleClassMethodActor teleClassMethodActor() {
        return teleClassMethodActor;
    }

    @Override
    public String getTextForTitle() {
        if (teleClassMethodActor == null || teleClassMethodActor.classMethodActor() == null) {
            return compiledCode.entityName();
        }

        final ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
        final StringBuilder sb = new StringBuilder(50);
        sb.append(classMethodActor.holder().simpleName());
        sb.append(".");
        sb.append(classMethodActor.name.toString());
        sb.append(inspection().nameDisplay().methodCompilationID(compiledCode));
        sb.append(inspection().nameDisplay().methodSubstitutionShortAnnotation(teleClassMethodActor));
        return sb.toString();
        //return classMethodActor.holder().simpleName() + "." + classMethodActor.name().toString() + inspection().nameDisplay().methodCompilationID(_teleTargetMethod);
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            protected void procedure() {
                showViewOptionsDialog(inspection());
            }
        };
    }
    @Override
    public String getToolTip() {
        String result = "";
        if (compiledCode != null) {
            result =  inspection().nameDisplay().longName(compiledCode);
        } else if (teleClassMethodActor != null) {
            result = inspection().nameDisplay().shortName(teleClassMethodActor, ReturnTypeSpecification.AS_PREFIX);
            if (teleClassMethodActor.isSubstituted()) {
                result = result + inspection().nameDisplay().methodSubstitutionLongAnnotation(teleClassMethodActor);
            }
        }
        return result;
    }

    /** Is it possible to display this source kind: code kind exists and the viewer is implemented. */
    private final Map<MethodCodeKind, Boolean> codeKindEnabled = new EnumMap<MethodCodeKind, Boolean>(MethodCodeKind.class);

    /** Code viewers being displayed in the inspector. */
    private final Map<MethodCodeKind, CodeViewer> codeViewers = new EnumMap<MethodCodeKind, CodeViewer>(MethodCodeKind.class) {
        @Override
        public CodeViewer put(MethodCodeKind kind, CodeViewer value) {
            final CodeViewer old = super.put(kind, value);
            codeViewCheckBoxes[kind.ordinal()].setSelected(true);
            return old;
        }

        @Override
        public CodeViewer remove(Object key) {
            final MethodCodeKind kind = (MethodCodeKind) key;
            final CodeViewer old = super.remove(kind);
            codeViewCheckBoxes[kind.ordinal()].setSelected(false);
            return old;
        }

    };

    private JSplitPane splitPane;

    @Override
    public void createView() {
        // Create code viewers, either by explicit request or by defaults.
        if (requestedCodeKind != null && codeKindEnabled.get(requestedCodeKind)) {
            addCodeViewer(requestedCodeKind);
        }
        for (MethodCodeKind codeKind : MethodCodeKind.values()) {
            if (codeKindEnabled.get(codeKind) && methodInspectorPreferences.isVisible(codeKind)) {
                if (!codeViewers.containsKey(codeKind)) {
                    addCodeViewer(codeKind);
                }
            }
        }
        if (codeViewerCount() == 0) {
            addCodeViewer(MethodCodeKind.TARGET_CODE);
        }
    }

    private CodeViewer codeViewerFactory(MethodCodeKind codeKind) {
        switch (codeKind) {
            case TARGET_CODE:
                return new JTableTargetCodeViewer(inspection(), this, compiledCode);
            case BYTECODES:
                return new JTableBytecodeViewer(inspection(), this, teleClassMethodActor, compiledCode);
            case JAVA_SOURCE:
                InspectorError.unimplemented();
                return null;
            default:
                InspectorError.unexpected("Unexpected MethodCodeKind");
        }
        return null;
    }

    /**
     * Adds a code view to this inspector, if possible.
     */
    public void viewCodeKind(MethodCodeKind kind) {
        if (!codeViewers.containsKey(kind) && codeKindEnabled.get(kind)) {
            addCodeViewer(kind);
        }
    }

    private void addCodeViewer(MethodCodeKind kind) {
        if (kind != null && !codeViewers.containsKey(kind)) {
            final CodeViewer newViewer = codeViewerFactory(kind);
            if (newViewer != null) {
                // this is awkward, doesn't work if add an inspector that we already have
                assert !codeViewers.containsKey(kind);
                // final InspectorFrame newInspectorFrame = newInspector;
                // final Component newComponent = (Component) newInspectorFrame;
                if (codeViewerCount() == 0) {
                    getContentPane().add(newViewer);
                    pack();
                } else if (codeViewerCount() == 1) {
                    final CodeViewer oldInspector = firstViewer();
                    // final Component oldComponent = (Component) oldInspector.frame();
                    getContentPane().remove(oldInspector);
                    if (oldInspector.codeKind().ordinal() < newViewer.codeKind().ordinal()) {
                        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, oldInspector, newViewer);
                    } else {
                        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, newViewer, oldInspector);
                    }
                    splitPane.setOneTouchExpandable(true);
                    splitPane.setResizeWeight(0.5);
                    getContentPane().add(splitPane);
                    pack();
                }
                codeViewers.put(kind, newViewer);
            }
        }
    }

    // simplified awkward model for now: there can only be 1 or 2 code inspectors
    @Override
    public void closeCodeViewer(CodeViewer viewer) {
        if (codeViewerCount() == 1) {
            // last code inspector; nuke the whole MethodInspector
            close();
        } else if (codeViewerCount() == 2) {
            final Component deleteComponent = viewer;
            Component keepComponent = splitPane.getLeftComponent();
            if (keepComponent == deleteComponent) {
                keepComponent = splitPane.getRightComponent();
            }
            Container contentPane = getContentPane();
            contentPane.remove(splitPane);
            contentPane.add(keepComponent);
            codeViewers.remove(viewer.codeKind());
            pack();
        }
    }

    @Override
    protected void refreshView(boolean force) {
        if (getJComponent().isShowing() || force) {
            if (teleClassMethodActor != null) {
                teleClassMethodActor.refreshView();
            }
            for (CodeViewer codeViewer : codeViewers.values()) {
                codeViewer.refresh(force);
            }
            super.refreshView(force);
        }
    }

    public void viewConfigurationChanged() {
        // TODO (mlvdv) fix method display update when view configurations change, patched now
        for (CodeViewer codeViewer : codeViewers.values()) {
            codeViewer.redisplay();
        }
        // Reconstruct doesn't work now for code views
        // reconstructView();
    }

    private int codeViewerCount() {
        return codeViewers.size();
    }

    private CodeViewer firstViewer() {
        final Iterator<CodeViewer> iterator = codeViewers.values().iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    /**
     * Global code selection has been set, though possibly unchanged; update all viewers.
     */
    @Override
    public void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative) {
        boolean haveSelection = false;
        for (CodeViewer codeViewer : codeViewers.values()) {
            if (codeViewer.updateCodeFocus(codeLocation)) {
                haveSelection = true;
            }
        }
        if (haveSelection && !isVisible()) {
            highlight();
        }
    }

    /**
     * Global thread selection has been set, though possibly unchanged; update all viewers.
     */
    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        for (CodeViewer codeViewer : codeViewers.values()) {
            codeViewer.updateThreadFocus(thread);
        }
    }

    @Override
    public void print() {
        final String textForTitle = getTextForTitle();
        if (codeViewerCount() == 1) {
            firstViewer().print(textForTitle);
        } else {
            for (CodeViewer codeViewer : codeViewers.values()) {
                if (gui().yesNoDialog("Print " + codeViewer.codeViewerKindName() + "?")) {
                    codeViewer.print(textForTitle);
                }
            }
        }
    }

    private final class ViewOptionsPanel extends InspectorPanel {

        public ViewOptionsPanel(Inspection inspection) {
            super(inspection, new BorderLayout());
            final InspectorCheckBox[] checkBoxes = new InspectorCheckBox[MethodCodeKind.values().length];

            final ItemListener itemListener = new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    final Object source = e.getItemSelectable();
                    for (MethodCodeKind codeKind : MethodCodeKind.values()) {
                        final InspectorCheckBox checkBox = checkBoxes[codeKind.ordinal()];
                        if (source == checkBox) {
                            if (checkBox.isSelected()) {
                                if (!codeViewers.containsKey(codeKind)) {
                                    addCodeViewer(codeKind);
                                }
                            } else if (codeViewers.containsKey(codeKind)) {
                                closeCodeViewer(codeViewers.get(codeKind));
                            }
                            break;
                        }
                    }
                }
            };
            final JPanel content = new InspectorPanel(inspection());
            content.add(new TextLabel(inspection(), "View:  "));
            final String toolTipText = "Should new Method inspectors initially display this code, when available?";
            for (MethodCodeKind codeKind : MethodCodeKind.values()) {
                final boolean currentValue = codeViewers.containsKey(codeKind);
                final InspectorCheckBox checkBox =
                    new InspectorCheckBox(inspection(), codeKind.toString(), toolTipText, currentValue);
                checkBox.addItemListener(itemListener);
                checkBoxes[codeKind.ordinal()] = checkBox;
                content.add(checkBox);
            }
            add(content, BorderLayout.WEST);
        }
    }

    private void showViewOptionsDialog(Inspection inspection) {

        final JPanel prefPanel = new InspectorPanel(inspection, new SpringLayout());

        final Border border = BorderFactory.createLineBorder(Color.black);

        final JPanel thisLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        thisLabelPanel.setBorder(border);
        thisLabelPanel.add(new TextLabel(inspection, "This Method"), BorderLayout.WEST);
        prefPanel.add(thisLabelPanel);

        final JPanel thisOptionsPanel = new ViewOptionsPanel(inspection);
        thisOptionsPanel.setBorder(border);
        prefPanel.add(thisOptionsPanel);

        final JPanel prefslLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        prefslLabelPanel.setBorder(border);
        prefslLabelPanel.add(new TextLabel(inspection, "Preferences"), BorderLayout.WEST);
        prefPanel.add(prefslLabelPanel);

        final JPanel prefsOptionsPanel = MethodInspectorPreferences.globalPreferences(inspection).getPanel();
        prefsOptionsPanel.setBorder(border);
        prefPanel.add(prefsOptionsPanel);

        SpringUtilities.makeCompactGrid(prefPanel, 2);

        new SimpleDialog(inspection, prefPanel, "Java Method Inspector View Options", true);
    }

}
