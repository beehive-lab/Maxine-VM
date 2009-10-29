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
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectorNameDisplay.*;
import com.sun.max.ins.method.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;

/**
 * Visual inspector and debugger for a Java method in the VM, able to display one or more kinds of code
 * associated with the method: target code, bytecode, and source.
 *
 * @author Michael Van De Vanter
 */
public class JavaMethodInspector extends MethodInspector {

    private final MethodInspectorPreferences methodInspectorPreferences;

    private final TeleClassMethodActor teleClassMethodActor;
    private final MethodCodeKind requestedCodeKind;

    /**
     * A particular compilation of the method, to which this Inspector is permanently bound, and which distinguishes
     * this Inspector uniquely from others that may be viewing the same method but bound to a different compilation.
     * Null when this Inspector is not bound to any compilation, in which case this Inspector is the unique (unbound)
     * inspector for the method.
     */
    private final TeleTargetMethod teleTargetMethod;

    /**
     * An Inspector for a Java Method associated with a specific compilation, and which association does not change
     * for the life of the inspector.
     *
     * @param inspection the {@link Inspection} of which this Inspector is part
     * @param parent the tabbed container for this Inspector
     * @param teleTargetMethod surrogate for the compilation of the method in the VM
     * @param codeKind request for a particular code view to be displayed initially
     */
    public JavaMethodInspector(Inspection inspection, MethodInspectorContainer parent, TeleTargetMethod teleTargetMethod, MethodCodeKind codeKind) {
        this(inspection, parent, teleTargetMethod, teleTargetMethod.getTeleClassMethodActor(), codeKind);
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

    private JavaMethodInspector(Inspection inspection, MethodInspectorContainer parent, TeleTargetMethod teleTargetMethod, TeleClassMethodActor teleClassMethodActor, MethodCodeKind requestedCodeKind) {
        super(inspection, parent);

        this.methodInspectorPreferences = MethodInspectorPreferences.globalPreferences(inspection);
        this.teleClassMethodActor = teleClassMethodActor;
        this.teleTargetMethod = teleTargetMethod;
        this.requestedCodeKind = requestedCodeKind;

        // enable choice if target code is present, even though this Inspector is not bound to a TargetMethod
        codeKindEnabled.put(MethodCodeKind.TARGET_CODE, teleTargetMethod != null || teleClassMethodActor.hasTargetMethod());
        // enable if bytecodes present
        codeKindEnabled.put(MethodCodeKind.BYTECODES, (teleClassMethodActor == null) ? false : teleClassMethodActor.hasCodeAttribute());
        // not implemented yet
        codeKindEnabled.put(MethodCodeKind.JAVA_SOURCE, false);

        final InspectionActions actions = inspection.actions();

        final InspectorFrameInterface frame = createTabFrame(parent);
        final InspectorMenu editMenu = frame.makeMenu(MenuKind.EDIT_MENU);

        final InspectorAction copyAction = actions.copyTargetMethodCodeToClipboard(teleTargetMethod, null);
        copyAction.setEnabled(teleTargetMethod != null);
        editMenu.add(copyAction);

        final InspectorMenu objectMenu = frame.makeMenu(MenuKind.OBJECT_MENU);
        objectMenu.add(actions.inspectObject(teleTargetMethod, "Target method: " + teleTargetMethod.classActorForType().simpleName()));
        objectMenu.add(actions.inspectObject(teleClassMethodActor, "Method: " + teleClassMethodActor.classActorForType().simpleName()));
        final TeleClassActor teleClassActor = teleClassMethodActor.getTeleHolder();
        objectMenu.add(actions.inspectObject(teleClassActor, "Holder: " + teleClassActor.classActorForType().simpleName()));
        objectMenu.add(actions.inspectSubstitutionSourceClassActorAction(teleClassMethodActor));
        objectMenu.add(actions.inspectTargetMethodCompilationsMenu(teleClassMethodActor, "Method compilations:"));
        objectMenu.add(defaultMenuItems(MenuKind.OBJECT_MENU));

        final InspectorMenu codeMenu = frame.makeMenu(MenuKind.CODE_MENU);
        codeMenu.add(actions.viewTargetMethodCodeMenu(teleClassMethodActor, "View method's compilations"));
        codeMenu.add(defaultMenuItems(MenuKind.CODE_MENU));

        final InspectorMenu debugMenu = frame.makeMenu(MenuKind.DEBUG_MENU);
        final InspectorMenu breakOnEntryMenu = new InspectorMenu("Break at this method entry");
        breakOnEntryMenu.add(actions.setTargetCodeBreakpointAtMethodEntry(teleTargetMethod, "Target code"));
        breakOnEntryMenu.add(actions.setBytecodeBreakpointAtMethodEntry(teleClassMethodActor, "Bytecode"));
        debugMenu.add(breakOnEntryMenu);
        final InspectorMenu breakAtLabelsMenu = new InspectorMenu("Break at this method labels");
        breakAtLabelsMenu.add(actions.setTargetCodeLabelBreakpoints(teleTargetMethod, "Add target code breakpoints"));
        breakAtLabelsMenu.add(actions.removeTargetCodeLabelBreakpoints(teleTargetMethod, "Remove target code breakpoints"));
        debugMenu.add(breakAtLabelsMenu);
        debugMenu.add(actions.debugInvokeMethod(teleClassMethodActor, "Invoke this method"));
        debugMenu.addSeparator();
        debugMenu.add(actions.genericBreakpointMenuItems());
        final JMenuItem viewBreakpointsMenuItem = new JMenuItem(actions().viewBreakpoints());
        viewBreakpointsMenuItem.setText("View Breakpoints");
        debugMenu.add(viewBreakpointsMenuItem);
        if (maxVM().watchpointsEnabled()) {
            debugMenu.add(actions.genericWatchpointMenuItems());
            final JMenuItem viewWatchpointsMenuItem = new JMenuItem(actions.viewWatchpoints());
            viewWatchpointsMenuItem.setText("View Watchpoints");
            debugMenu.add(viewWatchpointsMenuItem);
        }
    }

    @Override
    public TeleTargetRoutine teleTargetRoutine() {
        return teleTargetMethod;
    }

    @Override
    public TeleClassMethodActor teleClassMethodActor() {
        return teleClassMethodActor;
    }

    @Override
    public String getTextForTitle() {
        if (teleClassMethodActor == null || teleClassMethodActor.classMethodActor() == null) {
            return teleTargetMethod.description();
        }

        final ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
        final StringBuilder sb = new StringBuilder(50);
        sb.append(classMethodActor.holder().simpleName());
        sb.append(".");
        sb.append(classMethodActor.name.toString());
        sb.append(inspection().nameDisplay().methodCompilationID(teleTargetMethod));
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
        if (teleTargetMethod != null) {
            result =  inspection().nameDisplay().longName(teleTargetMethod);
        } else if (teleClassMethodActor != null) {
            result = inspection().nameDisplay().shortName(teleClassMethodActor, ReturnTypeSpecification.AS_PREFIX);
        }
        if (teleClassMethodActor != null && teleClassMethodActor.isSubstituted()) {
            result = result + inspection().nameDisplay().methodSubstitutionLongAnnotation(teleClassMethodActor);
        }
        return result;
    }

    /** Is it possible to display this source kind: code kind exists and the viewer is implemented. */
    private final Map<MethodCodeKind, Boolean> codeKindEnabled = new EnumMap<MethodCodeKind, Boolean>(MethodCodeKind.class);

    /** Code viewers being displayed in the inspector. */
    private final Map<MethodCodeKind, CodeViewer> codeViewers = new EnumMap<MethodCodeKind, CodeViewer>(MethodCodeKind.class);

    private JSplitPane splitPane;

    @Override
    public void createView() {
        // Create code viewers, either by explicit request or by defaults.
        if (requestedCodeKind != null && codeKindEnabled.get(requestedCodeKind)) {
            addCodeViewer(requestedCodeKind);
        }
        for (MethodCodeKind codeKind : MethodCodeKind.VALUES) {
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
                return new JTableTargetCodeViewer(inspection(), this, teleTargetMethod);
            case BYTECODES:
                return new JTableBytecodeViewer(inspection(), this, teleClassMethodActor, teleTargetMethod);
            case JAVA_SOURCE:
                FatalError.unimplemented();
                return null;
            default:
                ProgramError.unexpected("Unexpected MethodCodeKind");
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
    protected boolean refreshView(boolean force) {
        if (getJComponent().isShowing() || force) {
            if (teleClassMethodActor != null) {
                teleClassMethodActor.refreshView();
            }
            for (CodeViewer codeViewer : codeViewers.values()) {
                codeViewer.refresh(force);
            }
            super.refreshView(force);
        }
        return true;
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
    public void codeLocationFocusSet(TeleCodeLocation codeLocation, boolean interactiveForNative) {
        boolean haveSelection = false;
        for (CodeViewer codeViewer : codeViewers.values()) {
            if (codeViewer.updateCodeFocus(codeLocation)) {
                haveSelection = true;
            }
        }
        if (haveSelection && !isSelected()) {
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
            final JCheckBox[] checkBoxes = new JCheckBox[MethodCodeKind.VALUES.length()];

            final ItemListener itemListener = new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    final Object source = e.getItemSelectable();
                    for (MethodCodeKind codeKind : MethodCodeKind.VALUES) {
                        final JCheckBox checkBox = checkBoxes[codeKind.ordinal()];
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
            for (MethodCodeKind codeKind : MethodCodeKind.VALUES) {
                final boolean currentValue = codeViewers.containsKey(codeKind);
                final JCheckBox checkBox =
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
