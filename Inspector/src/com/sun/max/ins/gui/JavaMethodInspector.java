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
 * Visual inspector and debugger for a Java method in the {@link TeleVM}, able to display one or more kinds of code
 * associated with the method: target code, bytecode, and source.
 *
 * @author Michael Van De Vanter
 */
public class JavaMethodInspector extends MethodInspector {

    private final TeleClassMethodActor _teleClassMethodActor;
    private final CodeKind _requestedCodeKind;

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
     * @param teleTargetMethod surrogate for the compilation of the method in the {@link TeleVM}
     * @param codeKind request for a particular code view to be displayed initially
     */
    public JavaMethodInspector(Inspection inspection, MethodInspectorContainer parent, TeleTargetMethod teleTargetMethod, CodeKind codeKind) {
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
     * @param teleClassMethodActor surrogate for the specified Java method in the {@link TeleVM}
     * @param codeKind requested kind of code view: either source code or bytecodes
     */
    public JavaMethodInspector(Inspection inspection, MethodInspectorContainer parent, TeleClassMethodActor teleClassMethodActor, CodeKind codeKind) {
        this(inspection, parent, null, teleClassMethodActor, codeKind);
        assert codeKind != CodeKind.TARGET_CODE;
    }

    private JavaMethodInspector(Inspection inspection, MethodInspectorContainer parent, TeleTargetMethod teleTargetMethod, TeleClassMethodActor teleClassMethodActor, CodeKind requestedCodeKind) {
        super(inspection, parent, teleTargetMethod, teleClassMethodActor);
        _teleClassMethodActor = teleClassMethodActor;
        _teleTargetMethod = teleTargetMethod;
        _requestedCodeKind = requestedCodeKind;

        // enable choice if target code is present, even though this Inspector is not bound to a TargetMethod
        _codeKindEnabled.put(CodeKind.TARGET_CODE, teleTargetMethod != null || teleClassMethodActor.hasTargetMethod());
        // enable if bytecodes present
        _codeKindEnabled.put(CodeKind.BYTECODES, _teleClassMethodActor.hasCodeAttribute());
        // not implemented yet
        _codeKindEnabled.put(CodeKind.JAVA_SOURCE, false);

        // Assemble menu: override the standard frame menu by starting with an empty one and adding
        // view-specific commands.
        final InspectorMenu menu = new InspectorMenu();
        _menuCheckBoxes = new JCheckBoxMenuItem[CodeKind.VALUES.length()];
        for (CodeKind codeKind : CodeKind.VALUES) {
            final JCheckBoxMenuItem menuCheckBox = new JCheckBoxMenuItem(" Display " + codeKind);
            menuCheckBox.addActionListener(_menuCheckBoxActionListener);
            _menuCheckBoxes[codeKind.ordinal()] = menuCheckBox;
            menu.add(menuCheckBox);
        }

        menu.add(new InspectorAction(inspection(), "Method Code Display Prefs") {
            @Override
            public void procedure() {
                globalPreferences(inspection()).showDialog();
            }
        });
        _classMethodMenuItems = new ClassMethodMenuItems(inspection(), _teleClassMethodActor);
        menu.add(_classMethodMenuItems);
        if (teleTargetMethod != null) {
            _targetMethodMenuItems = new TargetMethodMenuItems(inspection(), teleTargetMethod);
            menu.add(_targetMethodMenuItems);
        } else {
            _targetMethodMenuItems = null;
        }

        createFrame(menu);
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
    private final Map<CodeKind, Boolean> _codeKindEnabled = new EnumMap<CodeKind, Boolean>(CodeKind.class);

    /** Code viewers being displayed in the inspector. */
    private final Map<CodeKind, CodeViewer> _codeViewers = new EnumMap<CodeKind, CodeViewer>(CodeKind.class);

    private JSplitPane _splitPane;
    private JCheckBoxMenuItem[] _menuCheckBoxes;
    private final ClassMethodMenuItems _classMethodMenuItems;
    private final TargetMethodMenuItems _targetMethodMenuItems;

    @Override
    public void createView(long epoch) {
        // Create code viewers, either by explicit request or by defaults.
        if (_requestedCodeKind != null && _codeKindEnabled.get(_requestedCodeKind)) {
            addCodeViewer(_requestedCodeKind);
        }
        for (CodeKind codeKind : CodeKind.VALUES) {
            if (_codeKindEnabled.get(codeKind) && globalPreferences(inspection()).isVisible(codeKind)) {
                if (!_codeViewers.containsKey(codeKind)) {
                    addCodeViewer(codeKind);
                }
            }
        }
        if (codeViewerCount() == 0) {
            addCodeViewer(CodeKind.TARGET_CODE);
        }
        refreshMenu();
    }

    private void refreshMenu() {
        for (CodeKind codeKind : CodeKind.VALUES) {
            _menuCheckBoxes[codeKind.ordinal()].setSelected(_codeViewers.containsKey(codeKind));
            _menuCheckBoxes[codeKind.ordinal()].setEnabled(_codeKindEnabled.get(codeKind));
        }
    }

    private CodeViewer codeViewerFactory(CodeKind codeKind) {
        switch (codeKind) {
            case TARGET_CODE:
                return new JTableTargetCodeViewer(inspection(), this, _teleTargetMethod);
            case BYTECODES:
                return new JTableBytecodeViewer(inspection(), this, _teleClassMethodActor, _teleTargetMethod);
            case JAVA_SOURCE:
                Problem.unimplemented();
                return null;
            default:
                ProgramError.unexpected("Unexpected CodeKind");
        }
        return null;
    }

    /**
     * Adds a code view to this inspector, if possible.
     */
    public void viewCodeKind(CodeKind kind) {
        if (!_codeViewers.containsKey(kind) && _codeKindEnabled.get(kind)) {
            addCodeViewer(kind);
        }
    }

    private void addCodeViewer(CodeKind kind) {
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
                refreshMenu();
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
            refreshMenu();
        }
    }

    private ActionListener _menuCheckBoxActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent actionEvent) {
            final Object source = actionEvent.getSource();
            if (source instanceof JCheckBoxMenuItem) {
                for (CodeKind codeKind : CodeKind.VALUES) {
                    final JCheckBoxMenuItem checkBox = _menuCheckBoxes[codeKind.ordinal()];
                    if (source == checkBox) {
                        globalPreferences(inspection()).setIsVisible(codeKind, checkBox.isSelected());
                        if (checkBox.isSelected()) {
                            if (!_codeViewers.containsKey(codeKind)) {
                                addCodeViewer(codeKind);
                            }
                        } else if (_codeViewers.containsKey(codeKind)) {
                            closeCodeViewer(_codeViewers.get(codeKind));
                        }
                        return;
                    }
                }
            }
        }
    };

    @Override
    public void refreshView(long epoch, boolean force) {
        if (isShowing() || force) {
            _teleClassMethodActor.refreshView();
            if (_classMethodMenuItems != null) {
                _classMethodMenuItems.refresh(epoch, force);
            }
            refreshMenu();
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
}
