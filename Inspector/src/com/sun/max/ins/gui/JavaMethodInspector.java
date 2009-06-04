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
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.member.*;

/**
 * Visual inspector and debugger for a Java method in the VM, able to display one or more kinds of code
 * associated with the method: target code, bytecode, and source.
 *
 * @author Michael Van De Vanter
 */
public class JavaMethodInspector extends MethodInspector {

    private final MethodInspectorPreferences _methodInspectorPreferences;

    private final TeleClassMethodActor _teleClassMethodActor;
    private final MethodCodeKind _requestedCodeKind;

    /**
     * A particular compilation of the method, to which this Inspector is permanently bound, and which distinguishes
     * this Inspector uniquely from others that may be viewing the same method but bound to a different compilation.
     * Null when this Inspector is not bound to any compilation, in which case this Inspector is the unique (unbound)
     * inspector for the method.
     */
    private final TeleTargetMethod _teleTargetMethod;

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
        super(inspection, parent, teleTargetMethod, teleClassMethodActor);

        _methodInspectorPreferences = MethodInspectorPreferences.globalPreferences(inspection);
        _teleClassMethodActor = teleClassMethodActor;
        _teleTargetMethod = teleTargetMethod;
        _requestedCodeKind = requestedCodeKind;

        // enable choice if target code is present, even though this Inspector is not bound to a TargetMethod
        _codeKindEnabled.put(MethodCodeKind.TARGET_CODE, teleTargetMethod != null || teleClassMethodActor.hasTargetMethod());
        // enable if bytecodes present
        _codeKindEnabled.put(MethodCodeKind.BYTECODES, _teleClassMethodActor.hasCodeAttribute());
        // not implemented yet
        _codeKindEnabled.put(MethodCodeKind.JAVA_SOURCE, false);

        createFrame(null);

        // Assemble menu: override the standard frame menu by starting with an empty one and adding
        // view-specific commands.
        _classMethodMenuItems = new ClassMethodMenuItems(inspection(), _teleClassMethodActor);
        frame().menu().add(_classMethodMenuItems);
        if (teleTargetMethod != null) {
            _targetMethodMenuItems = new TargetMethodMenuItems(inspection(), teleTargetMethod);
            frame().menu().add(_targetMethodMenuItems);
        } else {
            _targetMethodMenuItems = null;
        }


    }

    @Override
    public TeleTargetRoutine teleTargetRoutine() {
        return _teleTargetMethod;
    }

    @Override
    public String getTextForTitle() {
        final ClassMethodActor classMethodActor = _teleClassMethodActor.classMethodActor();
        final StringBuilder sb = new StringBuilder(50);
        sb.append(classMethodActor.holder().simpleName());
        sb.append(".");
        sb.append(classMethodActor.name().toString());
        sb.append(inspection().nameDisplay().methodCompilationID(_teleTargetMethod));
        sb.append(inspection().nameDisplay().methodSubstitutionShortAnnotation(_teleClassMethodActor));
        return sb.toString();
        //return classMethodActor.holder().simpleName() + "." + classMethodActor.name().toString() + inspection().nameDisplay().methodCompilationID(_teleTargetMethod);
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            protected void procedure() {
                new JavaMethodInspectorViewOptionsDialog(inspection());
            }
        };
    }
    @Override
    public String getToolTip() {
        String result;
        if (_teleTargetMethod != null) {
            result =  inspection().nameDisplay().shortName(_teleTargetMethod, ReturnTypeSpecification.AS_PREFIX);
        } else {
            result = inspection().nameDisplay().shortName(_teleClassMethodActor, ReturnTypeSpecification.AS_PREFIX);
        }
        if (_teleClassMethodActor.isSubstituted()) {
            result = result + inspection().nameDisplay().methodSubstitutionLongAnnotation(_teleClassMethodActor);
        }
        return result;
    }

    /** Is it possible to display this source kind: code kind exists and the viewer is implemented. */
    private final Map<MethodCodeKind, Boolean> _codeKindEnabled = new EnumMap<MethodCodeKind, Boolean>(MethodCodeKind.class);

    /** Code viewers being displayed in the inspector. */
    private final Map<MethodCodeKind, CodeViewer> _codeViewers = new EnumMap<MethodCodeKind, CodeViewer>(MethodCodeKind.class);

    private JSplitPane _splitPane;
    private final ClassMethodMenuItems _classMethodMenuItems;
    private final TargetMethodMenuItems _targetMethodMenuItems;

    @Override
    public void createView(long epoch) {
        // Create code viewers, either by explicit request or by defaults.
        if (_requestedCodeKind != null && _codeKindEnabled.get(_requestedCodeKind)) {
            addCodeViewer(_requestedCodeKind);
        }
        for (MethodCodeKind codeKind : MethodCodeKind.VALUES) {
            if (_codeKindEnabled.get(codeKind) && _methodInspectorPreferences.isVisible(codeKind)) {
                if (!_codeViewers.containsKey(codeKind)) {
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
                return new JTableTargetCodeViewer(inspection(), this, _teleTargetMethod);
            case BYTECODES:
                return new JTableBytecodeViewer(inspection(), this, _teleClassMethodActor, _teleTargetMethod);
            case JAVA_SOURCE:
                Problem.unimplemented();
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
        if (!_codeViewers.containsKey(kind) && _codeKindEnabled.get(kind)) {
            addCodeViewer(kind);
        }
    }

    private void addCodeViewer(MethodCodeKind kind) {
        if (kind != null && !_codeViewers.containsKey(kind)) {
            final CodeViewer newViewer = codeViewerFactory(kind);
            if (newViewer != null) {
                // this is awkward, doesn't work if add an inspector that we already have
                assert !_codeViewers.containsKey(kind);
                // final InspectorFrame newInspectorFrame = newInspector;
                // final Component newComponent = (Component) newInspectorFrame;
                if (codeViewerCount() == 0) {
                    frame().getContentPane().add(newViewer);
                    frame().pack();
                    frame().invalidate();
                    frame().repaint();
                } else if (codeViewerCount() == 1) {
                    final CodeViewer oldInspector = firstViewer();
                    // final Component oldComponent = (Component) oldInspector.frame();
                    frame().getContentPane().remove(oldInspector);
                    if (oldInspector.codeKind().ordinal() < newViewer.codeKind().ordinal()) {
                        _splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, oldInspector, newViewer);
                    } else {
                        _splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, newViewer, oldInspector);
                    }
                    _splitPane.setOneTouchExpandable(true);
                    _splitPane.setResizeWeight(0.5);
                    _splitPane.setBackground(style().defaultBackgroundColor());
                    frame().getContentPane().add(_splitPane);
                    frame().pack();
                    frame().invalidate();
                    frame().repaint();
                }
                _codeViewers.put(kind, newViewer);
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
            Component keepComponent = _splitPane.getLeftComponent();
            if (keepComponent == deleteComponent) {
                keepComponent = _splitPane.getRightComponent();
            }
            frame().getContentPane().remove(_splitPane);
            frame().getContentPane().add(keepComponent);
            _codeViewers.remove(viewer.codeKind());
            frame().pack();
            frame().repaint();
        }
    }

    @Override
    protected void refreshView(long epoch, boolean force) {
        if (isShowing() || force) {
            _teleClassMethodActor.refreshView();
            if (_classMethodMenuItems != null) {
                _classMethodMenuItems.refresh(epoch, force);
            }
            for (CodeViewer codeViewer : _codeViewers.values()) {
                codeViewer.refresh(epoch, force);
            }
            if (_targetMethodMenuItems != null) {
                _targetMethodMenuItems.refresh(epoch, force);
            }
            super.refreshView(epoch, force);
        }
    }

    public void viewConfigurationChanged(long epoch) {
        // TODO (mlvdv) fix method display update when view configurations change, patched now
        for (CodeViewer codeViewer : _codeViewers.values()) {
            codeViewer.redisplay();
        }
        // Reconstruct doesnt' work now for code views
        // reconstructView();
    }

    private int codeViewerCount() {
        return _codeViewers.size();
    }

    private CodeViewer firstViewer() {
        final Iterator<CodeViewer> iterator = _codeViewers.values().iterator();
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
        for (CodeViewer codeViewer : _codeViewers.values()) {
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
    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
        for (CodeViewer codeViewer : _codeViewers.values()) {
            codeViewer.updateThreadFocus(teleNativeThread);
        }
    }

    @Override
    public void print() {
        final String textForTitle = getTextForTitle();
        if (codeViewerCount() == 1) {
            firstViewer().print(textForTitle);
        } else {
            for (CodeViewer codeViewer : _codeViewers.values()) {
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
                                if (!_codeViewers.containsKey(codeKind)) {
                                    addCodeViewer(codeKind);
                                }
                            } else if (_codeViewers.containsKey(codeKind)) {
                                closeCodeViewer(_codeViewers.get(codeKind));
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
                final boolean currentValue = _codeViewers.containsKey(codeKind);
                final JCheckBox checkBox =
                    new InspectorCheckBox(inspection(), codeKind.toString(), toolTipText, currentValue);
                checkBox.addItemListener(itemListener);
                checkBoxes[codeKind.ordinal()] = checkBox;
                content.add(checkBox);
            }
            add(content, BorderLayout.WEST);
        }
    }


    private final class JavaMethodInspectorViewOptionsDialog extends InspectorDialog {
        JavaMethodInspectorViewOptionsDialog(Inspection inspection) {
            super(inspection, "Java Method Inspector View Options", false);

            final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());

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

            final JPanel buttons = new InspectorPanel(inspection);
            buttons.add(new JButton(new InspectorAction(inspection, "Close") {
                @Override
                protected void procedure() {
                    dispose();
                }
            }));

            dialogPanel.add(prefPanel, BorderLayout.CENTER);
            dialogPanel.add(buttons, BorderLayout.SOUTH);
            setContentPane(dialogPanel);
            pack();
            inspection.gui().moveToMiddle(this);
            setVisible(true);
        }
    }
}
