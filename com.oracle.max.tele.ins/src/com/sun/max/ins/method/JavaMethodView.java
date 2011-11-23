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
package com.sun.max.ins.method;

import static com.sun.max.ins.gui.AbstractView.MenuKind.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectorNameDisplay.ReturnTypeSpecification;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.member.*;

/**
 * Visual view and debugger for a Java method and other routines in the VM, able to display one or more kinds of code
 * associated with the method: compiled code, bytecode, and source.
 */
public final class JavaMethodView extends MethodView<JavaMethodView> {

    private final int TRACE_VALUE = 1;

    private final MethodViewPreferences methodViewPreferences;

    /**
     * Shared check boxes to be used in all UI code view selection.
     */
    private final JCheckBoxMenuItem[] codeViewCheckBoxes = new JCheckBoxMenuItem[MethodCodeKind.values().length];

    private final TeleClassMethodActor teleClassMethodActor;

    /**
     * A particular compilation of the method, to which this view is permanently bound, and which distinguishes
     * this view uniquely from others that may be viewing the same method but bound to a different compilation.
     * Null when this view is not bound to any compilation, in which case this view is the unique (unbound)
     * view for the method.
     */
    private MaxCompilation compilation;

    /**
     * The generation count for the code in the VM, the last time this classed accessed any information.
     */
    private int vmCodeGeneration = -1;

    /**
     * The kinds of code views it is possible to create for this method.
     */
    private Set<MethodCodeKind> enabledCodeKinds = EnumSet.noneOf(MethodCodeKind.class);

    /**
     * The code viewer kinds that should be added the next time the whole view's content is created or reconstructed.
     * This is established for initial view creation.  If the view needs to reconstruct all its content, for
     * example if the underlying machine code is determined to have changed, then this is reset to the set of existing
     * views so that they will be restored correctly.
     */
    private Set<MethodCodeKind> requestedCodeKinds = EnumSet.noneOf(MethodCodeKind.class);

    /** Map: MethodCodeKind -> CodeViewer
     * The viewer for the code kind, if it exists, i.e. if it is being displayed in the view.
     *
     * The map forces the corresponding view preference check boxes for the kind to agree:  on if there is a viewer, off if there is not.
     */
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

    /**
     * Used when two code viewers are visible; we don't yet support three.
     */
    private JSplitPane splitPane;

    /**
     * A view for a Java Method associated with a specific compilation, and which association does not change
     * for the life of the view.
     *
     * @param inspection the {@link Inspection} of which this view is part
     * @param container the tabbed container for this view
     * @param compilation surrogate for the compilation in the VM
     * @param codeKind request for a particular code view to be displayed initially
     */
    public JavaMethodView(Inspection inspection, MethodViewContainer container, MaxCompilation compilation, MethodCodeKind codeKind) {
        this(inspection, container, compilation, compilation.getTeleClassMethodActor(), codeKind);
    }

    /**
     * Creates a view for a Java Method without association to any compilation, and which can thus view only bytecodes or
     * source code. If a user, within the context of this view, requests a view of an associated compilation, then
     * another existing view associated with the specified compilation must be located or a new one created; in
     * either case, the resulting view replaces this one.
     *
     * @param inspection the {@link Inspection} of which this view is part
     * @param container the tabbed container for this view
     * @param teleClassMethodActor surrogate for the specified Java method in the VM
     * @param codeKind requested kind of code view: either source code or bytecodes
     */
    public JavaMethodView(Inspection inspection, MethodViewContainer container, TeleClassMethodActor teleClassMethodActor, MethodCodeKind codeKind) {
        this(inspection, container, null, teleClassMethodActor, codeKind);
        assert codeKind != MethodCodeKind.MACHINE_CODE;
    }

    private JavaMethodView(Inspection inspection, MethodViewContainer container, MaxCompilation compilation, TeleClassMethodActor teleClassMethodActor, MethodCodeKind requestedCodeKind) {
        super(inspection, container);

        this.methodViewPreferences = MethodViewPreferences.globalPreferences(inspection);
        this.teleClassMethodActor = teleClassMethodActor;
        this.compilation = compilation;
        this.vmCodeGeneration = compilation != null ? compilation.vmCodeGeneration() : 0;

        // Determine which code viewers it is possible to present for this method.
        // This doesn't change.
        if (compilation != null || teleClassMethodActor.compilationCount() > 0) {
            enabledCodeKinds.add(MethodCodeKind.MACHINE_CODE);
        }
        if (teleClassMethodActor != null && teleClassMethodActor.hasCodeAttribute()) {
            enabledCodeKinds.add(MethodCodeKind.BYTECODES);
        }
        if (false) {
            enabledCodeKinds.add(MethodCodeKind.JAVA_SOURCE);
        }

        // Determine which code viewers to present at creation, starting with the originating request
        if (requestedCodeKind != null && enabledCodeKinds.contains(requestedCodeKind)) {
            requestedCodeKinds.add(requestedCodeKind);
        }
        // Now check for other requested views based on preference settings.
        for (MethodCodeKind codeKind : MethodCodeKind.values()) {
            if (enabledCodeKinds.contains(codeKind) && methodViewPreferences.isVisible(codeKind)) {
                requestedCodeKinds.add(codeKind);
            }
        }
        // If all else fails, revert to lowest level
        if (requestedCodeKinds.isEmpty()) {
            requestedCodeKinds.add(MethodCodeKind.MACHINE_CODE);
        }

        // Create shared check boxes that will track and help control what views are visible.
        // Invariant: checkbox selected iff the code kind is in {@link #codeViewers}.
        for (final MethodCodeKind codeKind : MethodCodeKind.values()) {
            // The check box settings can either be changed by user action on the check box
            // itself, or by other actions that add/remove code viewers.  There are no code
            // viewers present at this point in the construction of the view.
            final boolean currentValue = false;
            final String toolTipText = "Display this kind of source for the Java method?";
            final JCheckBoxMenuItem checkBox = new InspectorCheckBox(inspection(), codeKind.toString(), toolTipText, currentValue);
            checkBox.setEnabled(enabledCodeKinds.contains(codeKind));
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

        final InspectorFrame frame = createTabFrame(container);

        final InspectorMenu editMenu = frame.makeMenu(EDIT_MENU);
        final InspectorMenu objectMenu = frame.makeMenu(OBJECT_MENU);
        final InspectorMenu codeMenu = frame.makeMenu(CODE_MENU);
        final InspectorMenu debugMenu = frame.makeMenu(DEBUG_MENU);
        final InspectorMenu breakOnEntryMenu = new InspectorMenu("Break at this method entry");
        final InspectorMenu breakAtLabelsMenu = new InspectorMenu("Break at this method labels");

        if (compilation != null) {
            final InspectorAction copyAction = actions().copyCompiledCodeToClipboard(compilation, null);
            copyAction.setEnabled(true);
            editMenu.add(copyAction);
            objectMenu.add(views().objects().makeViewAction(compilation.representation(), "Compiled method: " + compilation.classActorForObjectType().simpleName()));
        }

        if (teleClassMethodActor != null) {
            final InspectorAction copyAction = actions().copyBytecodeToClipboard(teleClassMethodActor, null);
            copyAction.setEnabled(true);
            editMenu.add(copyAction);
            objectMenu.add(views().objects().makeViewAction(teleClassMethodActor, "Method: " + teleClassMethodActor.classActorForObjectType().simpleName()));
            final TeleClassActor teleClassActor = teleClassMethodActor.getTeleHolder();
            objectMenu.add(views().objects().makeViewAction(teleClassActor, "Holder: " + teleClassActor.classActorForObjectType().simpleName()));
            objectMenu.add(actions().viewSubstitutionSourceClassActorAction(teleClassMethodActor));
            objectMenu.add(actions().inspectMethodCompilationsMenu(teleClassMethodActor, "Method compilations:"));
            objectMenu.add(defaultMenuItems(OBJECT_MENU));
        }
        for (final MethodCodeKind codeKind : MethodCodeKind.values()) {
            codeMenu.add(codeViewCheckBoxes[codeKind.ordinal()]);
        }
        if (teleClassMethodActor != null) {
            codeMenu.add(actions().viewMethodCompilationsMenu(teleClassMethodActor, "View method's compilations"));
        }
        codeMenu.add(defaultMenuItems(CODE_MENU));

        if (compilation != null) {
            breakOnEntryMenu.add(actions().setMachineCodeBreakpointAtEntry(compilation, "Machine code"));
        }
        if (teleClassMethodActor != null) {
            breakOnEntryMenu.add(actions().setBytecodeBreakpointAtMethodEntry(teleClassMethodActor, "Bytecodes"));
        }
        debugMenu.add(breakOnEntryMenu);
        if (compilation != null) {
            breakAtLabelsMenu.add(actions().setMachineCodeLabelBreakpoints(compilation, "Add machine code breakpoints"));
            breakAtLabelsMenu.add(actions().removeMachineCodeLabelBreakpoints(compilation, "Remove machine code breakpoints"));
        }
        debugMenu.add(breakAtLabelsMenu);
        if (teleClassMethodActor != null) {
            debugMenu.add(actions().debugInvokeMethod(teleClassMethodActor, "Invoke this method"));
        }
        debugMenu.addSeparator();
        debugMenu.add(actions().genericBreakpointMenuItems());
        debugMenu.add(views().activateSingletonViewAction(ViewKind.BREAKPOINTS));
        if (vm().watchpointManager() != null) {
            debugMenu.add(actions().genericWatchpointMenuItems());
            debugMenu.add(views().activateSingletonViewAction(ViewKind.WATCHPOINTS));
        }
    }

    @Override
    public void createViewContent() {
        if (!codeViewers.isEmpty()) {
            // Code viewers already exist, so we must be reconstructing the whole view.
            // Remember which views were visible
            requestedCodeKinds.clear();
            requestedCodeKinds.addAll(codeViewers.keySet());

            // Now get rid of the code viewers; this is awkward because of the way we alternate
            // between a single viewer in the content pane and a split pane that contains two viewers.
            // Some day we may support three.
            while (codeViewers.size() > 1) {
                closeCodeViewer(firstViewer());
            }
            getContentPane().remove(firstViewer());
            codeViewers.clear();
        }
        // Create requested code viewers
        for (MethodCodeKind codeKind : requestedCodeKinds) {
            addCodeViewer(codeKind);
        }
    }

    @Override
    protected void refreshState(boolean force) {
        if (compilation != null && compilation.vmCodeGeneration() > vmCodeGeneration) {
            reconstructView();
            vmCodeGeneration = compilation.vmCodeGeneration();
            Trace.line(TRACE_VALUE, tracePrefix() + "Updated after code change in method " + getToolTip());

        } else if (getJComponent().isShowing() || force) {
            for (CodeViewer codeViewer : codeViewers.values()) {
                codeViewer.refresh(force);
            }
        }
    }

    @Override
    public String getTextForTitle() {
        if (teleClassMethodActor == null || teleClassMethodActor.classMethodActor() == null) {
            return compilation.entityName();
        }

        final ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
        final StringBuilder sb = new StringBuilder(50);
        sb.append(classMethodActor.holder().simpleName());
        sb.append(".");
        sb.append(classMethodActor.name.toString());
        sb.append(inspection().nameDisplay().shortMethodCompilationID(compilation));
        sb.append(inspection().nameDisplay().methodSubstitutionShortAnnotation(teleClassMethodActor));
        return sb.toString();
        //return classMethodActor.holder().simpleName() + "." + classMethodActor.name().toString() + inspection().nameDisplay().methodCompilationID(_teleTargetMethod);
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
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            protected void procedure() {
                showViewOptionsDialog(inspection());
            }
        };
    }

    @Override
    public MaxCompilation compilation() {
        return compilation;
    }

    @Override
    public TeleClassMethodActor teleClassMethodActor() {
        return teleClassMethodActor;
    }

    @Override
    public String getToolTip() {
        String result = "";
        if (compilation != null) {
            result =  inspection().nameDisplay().longName(compilation);
        } else if (teleClassMethodActor != null) {
            result = inspection().nameDisplay().shortName(teleClassMethodActor, ReturnTypeSpecification.AS_PREFIX);
            if (teleClassMethodActor.isSubstituted()) {
                result = result + inspection().nameDisplay().methodSubstitutionLongAnnotation(teleClassMethodActor);
            }
        }
        return result;
    }

    // simplified awkward model for now: there can only be 1 or 2 code viewers
    @Override
    public void closeCodeViewer(CodeViewer viewer) {
        if (codeViewers.size() == 1) {
            // only code view; nuke the whole Method viewer
            close();
        } else if (codeViewers.size() == 2) {
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
    public void print() {
        final String textForTitle = getTextForTitle();
        if (codeViewers.size() == 1) {
            firstViewer().print(textForTitle);
        } else {
            for (CodeViewer codeViewer : codeViewers.values()) {
                if (gui().yesNoDialog("Print " + codeViewer.codeViewerKindName() + "?")) {
                    codeViewer.print(textForTitle);
                }
            }
        }
    }

    /**
     * Adds a specified code view to this view, if possible.
     */
    public void viewCodeKind(MethodCodeKind kind) {
        if (!codeViewers.containsKey(kind) && enabledCodeKinds.contains(kind)) {
            addCodeViewer(kind);
        }
    }

    private CodeViewer codeViewerFactory(MethodCodeKind codeKind) {
        switch (codeKind) {
            case MACHINE_CODE:
                if (compilation == null && teleClassMethodActor != null) {
                    try {
                        compilation = vm().machineCode().latestCompilation(teleClassMethodActor);
                    } catch (MaxVMBusyException e) {
                        InspectorError.unexpected("Can't create machine code view");
                    }
                }
                return compilation == null ? null : new JTableMachineCodeViewer(inspection(), this, compilation);
            case BYTECODES:
                return new JTableBytecodeViewer(inspection(), this, teleClassMethodActor, compilation);
            case JAVA_SOURCE:
                InspectorError.unimplemented();
                return null;
            default:
                InspectorError.unexpected("Unexpected MethodCodeKind");
        }
        return null;
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
            final String toolTipText = "Should new Method views initially display this code, when available?";
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

    private void addCodeViewer(MethodCodeKind kind) {
        if (kind != null && !codeViewers.containsKey(kind)) {
            final CodeViewer newViewer = codeViewerFactory(kind);
            if (newViewer != null) {
                // this is awkward, doesn't work if add a view that we already have
                assert !codeViewers.containsKey(kind);
                // final InspectorFrame newInspectorFrame = newInspector;
                // final Component newComponent = (Component) newInspectorFrame;
                if (codeViewers.size() == 0) {
                    getContentPane().add(newViewer);
                    pack();
                } else if (codeViewers.size() == 1) {
                    final CodeViewer oldViewer = firstViewer();
                    // final Component oldComponent = (Component) oldInspector.frame();
                    getContentPane().remove(oldViewer);
                    if (oldViewer.codeKind().ordinal() < newViewer.codeKind().ordinal()) {
                        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, oldViewer, newViewer);
                    } else {
                        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, newViewer, oldViewer);
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

    private CodeViewer firstViewer() {
        final Iterator<CodeViewer> iterator = codeViewers.values().iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
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

        final JPanel prefsOptionsPanel = MethodViewPreferences.globalPreferences(inspection).getPanel();
        prefsOptionsPanel.setBorder(border);
        prefPanel.add(prefsOptionsPanel);

        SpringUtilities.makeCompactGrid(prefPanel, 2);

        new SimpleDialog(inspection, prefPanel, "Java Method View Options", true);
    }

}
