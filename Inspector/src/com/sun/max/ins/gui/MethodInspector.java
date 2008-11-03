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

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.memory.MemoryInspector.*;
import com.sun.max.ins.memory.MemoryWordInspector.*;
import com.sun.max.ins.method.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * An inspector that can present one or more code representations of a method. MethodInspectors are unique, keyed from
 * an instance of {@link TeleClassMethodActor}.
 *
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public abstract class MethodInspector extends UniqueInspector<MethodInspector> implements MemoryInspectable, MemoryWordInspectable {

    private static final int TRACE_VALUE = 2;

    private static Manager _manager;

    /**
     * Manages inspection of methods in the {@link TeleVM}, even when the tabbed view does not exist.
     * Has no visible presence or direct user interaction at this time.
     *
     * @author Michael Van De Vanter
     */
    public static final class Manager extends InspectionHolder {

        private Manager(Inspection inspection) {
            super(inspection);
        }

        public static void make(Inspection inspection) {
            if (_manager == null) {
                _manager = new Manager(inspection);
                inspection.focus().addListener(new InspectionFocusAdapter() {

                    @Override
                    public void codeLocationFocusSet(TeleCodeLocation teleCodeLocation, boolean interactiveForNative) {
                        final MethodInspector methodInspector = MethodInspector.make(_manager.inspection(), teleCodeLocation, interactiveForNative);
                        if (methodInspector != null) {
                            // Ensure that a newly created MethodInspector will have the focus set;
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    methodInspector.setCodeLocationFocus();
                                }
                            });
                        }
                    }
                });
            }
        }

    }

    /**
     * Makes an inspector displaying code for the method pointed to by the instructionPointer. Should always work for
     * Java methods. For native methods, only works if the code block is already known to the inspector or if the user
     * supplies some additional information at an optional prompt.
     *
     * @param address Target code location in the {@link TeleVM}.
     * @param interactive Should user be prompted for additional address information in case the location is unknown
     *            native code.
     * @return A possibly new inspector, null if unable to view.
     */
    private static MethodInspector make(final Inspection inspection, Address address, boolean interactive) {
        MethodInspector methodInspector = null;
        final TeleTargetRoutine teleTargetRoutine = inspection.teleVM().teleCodeRegistry().get(TeleTargetRoutine.class, address);
        if (teleTargetRoutine != null) {
            if (teleTargetRoutine instanceof TeleNativeTargetRoutine) {
                inspection.focus().setCodeLocation(new TeleCodeLocation(inspection.teleVM(), address), true);
            } else if (teleTargetRoutine instanceof TeleTargetMethod) {
                methodInspector = make(inspection, (TeleTargetMethod) teleTargetRoutine, CodeKind.TARGET_CODE);
            } else if (teleTargetRoutine instanceof TeleRuntimeStub) {
                methodInspector = make(inspection, teleTargetRoutine);
            } else {
                assert teleTargetRoutine == null : "unknown type of TeleTargetRoutine: " + teleTargetRoutine.getClass();
            }
        } else {
            final TeleTargetMethod teleTargetMethod = TeleTargetMethod.make(inspection.teleVM(), address);
            if (teleTargetMethod != null) {
                // Java method
                methodInspector = make(inspection, teleTargetMethod, CodeKind.TARGET_CODE);
                // inspection.focus().setCodeLocation(new TeleCodeLocation(inspection.teleVM(), address));
            } else {
                final TeleRuntimeStub teleRuntimeStub = TeleRuntimeStub.make(inspection.teleVM(), address);
                if (teleRuntimeStub != null) {
                    methodInspector = make(inspection, teleRuntimeStub);
                    methodInspector.highlight();
                } else {
                    if (interactive) {
                        // Code location is not in a Java method and has not yet been viewed in a native routine.
                        final MutableInnerClassGlobal<MethodInspector> result = new MutableInnerClassGlobal<MethodInspector>();
                        new NativeMethodAddressInputDialog(inspection, address, TeleNativeTargetRoutine.DEFAULT_NATIVE_CODE_LENGTH) {
                            @Override
                            public void entered(Address nativeAddress, Size codeSize, String title) {
                                try {
                                    final TeleNativeTargetRoutine teleNativeTargetRoutine = TeleNativeTargetRoutine.create(inspection.teleVM(), nativeAddress, codeSize);
                                    result.setValue(MethodInspector.make(inspection, teleNativeTargetRoutine));
                                    // inspection.focus().setCodeLocation(new TeleCodeLocation(inspection.teleVM(), nativeAddress));
                                    result.value().highlight();
                                } catch (IllegalArgumentException illegalArgumentException) {
                                    inspection.errorMessage("Specified native code range overlaps region already registered in Inpsector");
                                }
                            }
                        };
                        methodInspector = result.value();
                    }
                }
            }
        }
        return methodInspector;
    }

    /**
     * Makes an inspector displaying code for specified code location. Should always work for
     * Java methods. For native methods, only works if the code block is already known.
     *
     * @param teleCodeLocation a code location
     * @return A possibly new inspector, null if unable to view.
     */
    public static MethodInspector make(Inspection inspection, TeleCodeLocation teleCodeLocation, boolean interactiveForNative) {
        if (teleCodeLocation.hasTargetCodeLocation()) {
            return make(inspection, teleCodeLocation.targetCodeInstructionAddresss(), interactiveForNative);
        }
        if (teleCodeLocation.hasBytecodeLocation()) {
            // TODO (mlvdv)  Select the specified bytecode position
            return make(inspection, teleCodeLocation.teleBytecodeLocation().teleClassMethodActor(), CodeKind.BYTECODES);
        }
        // Has neither target nor bytecode location specified.
        return null;
    }

    /**
     * Display an inspector for a Java method, showing the kind of code requested if available. If an inspector for the
     * method doesn't exist, create a new one and display the kind of code requested if available. if an inspector for
     * the method does exist, add a display of the kind of code requested if available.
     *
     * @return A possibly new inspector for the method.
     */
    private static JavaMethodInspector make(Inspection inspection, TeleClassMethodActor teleClassMethodActor, CodeKind codeKind) {
        JavaMethodInspector javaMethodInspector = null;
        // If there are compilations, then inspect in association with the most recent
        final TeleTargetMethod teleTargetMethod = teleClassMethodActor.getCurrentJavaTargetMethod();
        if (teleTargetMethod != null) {
            return make(inspection, teleTargetMethod, codeKind);
        }
        final UniqueInspector.Key<? extends MethodInspector> key = UniqueInspector.Key.create(JavaMethodInspector.class, teleClassMethodActor);
        final MethodInspector methodInspector = UniqueInspector.find(inspection, key);
        if (methodInspector == null) {
            final MethodInspectorContainer parent = MethodInspectorContainer.make(inspection);
            javaMethodInspector = new JavaMethodInspector(inspection, parent, teleClassMethodActor, codeKind);
            parent.add(javaMethodInspector);
        } else {
            javaMethodInspector = (JavaMethodInspector) methodInspector;
        }
        return javaMethodInspector;
    }

    /**
     * @return a possibly new {@link MethodInspector} associated with a specific compilation of a Java method in the
     *         {@link TeleVM}, and with the requested code view visible.
     */
    private static JavaMethodInspector make(Inspection inspection, TeleTargetMethod teleTargetMethod, CodeKind codeKind) {
        JavaMethodInspector javaMethodInspector = null;
        final UniqueInspector.Key<? extends MethodInspector> targetMethodKey = UniqueInspector.Key.create(JavaMethodInspector.class, teleTargetMethod);
        // Is there already an inspection open that is bound to this compilation?
        MethodInspector methodInspector = UniqueInspector.find(inspection, targetMethodKey);
        if (methodInspector == null) {
            // No existing inspector is bound to this compilation; see if there is an inspector for this method that is
            // unbound
            final UniqueInspector.Key<? extends MethodInspector> classMethodActorKey = UniqueInspector.Key.create(JavaMethodInspector.class, teleTargetMethod.getTeleClassMethodActor());
            methodInspector = UniqueInspector.find(inspection, classMethodActorKey);
            final MethodInspectorContainer parent = MethodInspectorContainer.make(inspection);
            if (methodInspector == null) {
                // No existing inspector exists for this method; create new one bound to this compilation
                javaMethodInspector = new JavaMethodInspector(inspection, parent, teleTargetMethod, codeKind);
            } else {
                // An inspector exists for the method, but not bound to any compilation; bind it to this compilation
                // TODO (mlvdv) Temp patch; just create a new one in this case too.
                javaMethodInspector = new JavaMethodInspector(inspection, parent, teleTargetMethod, codeKind);
            }
            parent.add(javaMethodInspector);
        } else {
            // An existing inspector is bound to this method & compilation; ensure that it has the requested code view
            javaMethodInspector = (JavaMethodInspector) methodInspector;
            javaMethodInspector.viewCodeKind(codeKind);
        }
        return javaMethodInspector;
    }

    /**
     * @return A possibly new inspector for a block of native code in the {@link TeleVM} already known to the inspector.
     */
    private static NativeMethodInspector make(Inspection inspection, TeleTargetRoutine teleTargetRoutine) {
        NativeMethodInspector nativeMethodInspector = null;
        final UniqueInspector.Key<? extends MethodInspector> key = UniqueInspector.Key.create(NativeMethodInspector.class, teleTargetRoutine.teleRoutine());
        final MethodInspector methodInspector = UniqueInspector.find(inspection, key);
        if (methodInspector == null) {
            final MethodInspectorContainer parent = MethodInspectorContainer.make(inspection);
            nativeMethodInspector = new NativeMethodInspector(inspection, parent, teleTargetRoutine);
            parent.add(nativeMethodInspector);
        } else {
            nativeMethodInspector = (NativeMethodInspector) methodInspector;
        }
        return nativeMethodInspector;
    }

    /**
     * Constants denoting the kinds of code that can be inspected.
     */
    public enum CodeKind {
        TARGET_CODE("Target Code", true),
        BYTECODES("Bytecodes", false),
        JAVA_SOURCE("Java Source", false);

        private final String _label;
        private final boolean _defaultVisibility;

        private CodeKind(String label, boolean defaultVisibility) {
            _label = label;
            _defaultVisibility = defaultVisibility;
        }

        /**
         * Determines if it the display of this source kind is implemented.
         *
         * TODO (mlvdv) This is a hack until source code viewing is implemented
         */
        public boolean isImplemented() {
            return this != JAVA_SOURCE;
        }

        public String label() {
            return _label;
        }

        @Override
        public String toString() {
            return _label;
        }

        /**
         * Determines if this kind should be visible by default in new inspectors.
         */
        public boolean defaultVisibility() {
            return _defaultVisibility;
        }

        public static final IndexedSequence<CodeKind> VALUES = new ArraySequence<CodeKind>(values());
    }

    // Preference

    /**
     * Encapsulates the user-settable preferences governing various aspects of how (or what) code is displayed.
     */
    public static class Preferences {
        /**
         * A predicate specifying which kinds of code are to be displayed in a method inspector.
         */
        private final Map<CodeKind, Boolean> _visibleCodeKinds = new EnumMap<CodeKind, Boolean>(CodeKind.class);
        private final Inspection _inspection;
        Preferences(Inspection inspection) {
            _inspection = inspection;
            final InspectionSettings settings = inspection.settings();
            final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener("methodInspectorPrefs", null) {
                public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                    for (Map.Entry<CodeKind, Boolean> entry : _visibleCodeKinds.entrySet()) {
                        saveSettingsEvent.save(entry.getKey().name().toLowerCase(), entry.getValue());
                    }
                }
            };
            settings.addSaveSettingsListener(saveSettingsListener);
            for (CodeKind codeKind : CodeKind.VALUES) {
                final boolean defaultVisibility = codeKind.defaultVisibility();
                _visibleCodeKinds.put(codeKind, settings.get(saveSettingsListener, codeKind.name().toLowerCase(), OptionTypes.BOOLEAN_TYPE, defaultVisibility));
            }
        }

        /**
         * Determines if this preferences object indicates that a {@linkplain CodeViewer code viewer} should be created
         * for a given code kind.
         */
        public boolean isVisible(CodeKind codeKind) {
            return _visibleCodeKinds.get(codeKind);
        }

        /**
         * Updates this preferences object to indicate whether a {@linkplain CodeViewer code viewer} should be created
         * for a given code kind.
         */
        public void setIsVisible(CodeKind codeKind, boolean visible) {
            final boolean needToSave = _visibleCodeKinds.get(codeKind) != visible;
            _visibleCodeKinds.put(codeKind, visible);
            if (needToSave) {
                _inspection.settings().save();
            }
        }

        /**
         * @return a GUI panel for setting these preferences
         */
        public JPanel getPanel() {
            final JCheckBox[] checkBoxes = new JCheckBox[CodeKind.VALUES.length()];

            final Preferences preferences = globalPreferences(_inspection);
            final ItemListener itemListener = new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    final Object source = e.getItemSelectable();
                    for (CodeKind codeKind : CodeKind.VALUES) {
                        final JCheckBox checkBox = checkBoxes[codeKind.ordinal()];
                        if (source == checkBox) {
                            preferences.setIsVisible(codeKind, checkBox.isSelected());
                            return;
                        }
                    }
                }
            };
            final JPanel content = new JPanel();
            content.add(new TextLabel(_inspection, "View:  "));
            for (CodeKind codeKind : CodeKind.VALUES) {
                final JCheckBox checkBox = new JCheckBox(codeKind.toString());
                checkBox.setOpaque(true);
                checkBox.setBackground(_inspection.style().defaultBackgroundColor());
                checkBox.setToolTipText("Should new Method inspectors initially display this code, when available?");
                checkBox.setEnabled(codeKind.isImplemented());
                checkBox.setSelected(preferences.isVisible(codeKind));
                checkBox.addItemListener(itemListener);
                checkBoxes[codeKind.ordinal()] = checkBox;
                content.add(checkBox);
            }
            final JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(true);
            panel.setBackground(_inspection.style().defaultBackgroundColor());
            panel.add(content, BorderLayout.WEST);
            return panel;
        }

        public void showDialog() {
            new Dialog(_inspection);
        }

        private final class Dialog extends InspectorDialog {
            Dialog(Inspection inspection) {
                super(inspection, "Method Code Display Preferences", false);

                final JPanel dialogPanel = new JPanel();
                dialogPanel.setLayout(new BorderLayout());
                dialogPanel.setOpaque(true);
                dialogPanel.setBackground(style().defaultBackgroundColor());

                final JPanel buttons = new JPanel();
                buttons.setOpaque(true);
                buttons.setBackground(style().defaultBackgroundColor());
                buttons.add(new JButton(new InspectorAction(inspection(), "Close") {
                    @Override
                    protected void procedure() {
                        dispose();
                    }
                }));

                dialogPanel.add(getPanel(), BorderLayout.NORTH);
                dialogPanel.add(buttons, BorderLayout.SOUTH);

                setContentPane(dialogPanel);
                pack();
                inspection().moveToMiddle(this);
                setVisible(true);
            }
        }
    }

    private static Preferences _globalPreferences;

    public static synchronized Preferences globalPreferences(Inspection inspection) {
        if (_globalPreferences == null) {
            _globalPreferences = new Preferences(inspection);
        }
        return _globalPreferences;
    }

    private final MethodInspectorContainer _parent;

    protected MethodInspectorContainer parent() {
        return _parent;
    }

    public MethodInspector(Inspection inspection, MethodInspectorContainer parent, TeleTargetMethod teleTargetMethod, TeleRoutine teleRoutine) {
        super(inspection, parent.residence(), teleTargetMethod, teleRoutine);
        _parent = parent;
    }

    @Override
    public void createFrame(InspectorMenu baseMenu) {
        InspectorMenu menu = baseMenu;
        if (menu == null) {
            // Ensure that the default frame menu is not used; this shouldn't really be an Inspector at all.
            menu = new InspectorMenu();
        }
        super.createFrame(menu);
        frame().menu().addSeparator();
        frame().menu().add(new InspectorAction(inspection(), "Refresh") {
            @Override
            public void procedure() {
                refreshView(true);
                Trace.line(TRACE_VALUE, "Refreshing method view: " + getTitle());
            }
        });
        frame().menu().add(new InspectorAction(inspection(), "Close all other tabs") {
            @Override
            public void procedure() {
                closeOthers();
            }
        });
        frame().replaceFrameCloseAction(new InspectorAction(inspection(), "Close Method Inspector") {
            @Override
            public void procedure() {
                close();
            }
        });
    }

    /**
     * Updates the code selection to agree with the current focus.
     */
    private void setCodeLocationFocus() {
        codeLocationFocusSet(inspection().focus().codeLocation(), false);
    }

    @Override
    public void breakpointSetChanged() {
        refreshView(true);
    }

    public void close() {
        _parent.close(this);
    }

    public void closeOthers() {
        _parent.closeOthers(this);
    }

    @Override
    public void moveToFront() {
        if (parent() != null) {
            parent().setSelected(this);
        } else {
            super.moveToFront();
        }
    }

    @Override
    public void setSelected() {
        if (parent() != null) {
            parent().setSelected(this);
        } else {
            super.moveToFront();
        }
    }

    @Override
    public boolean isSelected() {
        if (parent() != null) {
            return parent().isSelected(this);
        }
        return false;
    }

    @Override
    public synchronized void setResidence(Residence residence) {
        final Residence current = residence();
        super.setResidence(residence);
        if (current != residence) {
            if (residence == Residence.INTERNAL) {
                // coming back from EXTERNAL, need to redock
                if (parent() != null) {
                    parent().add(this);
                }
                moveToFront();
            } else if (residence == Residence.EXTERNAL) {
                frame().setTitle(getTitle());
            }
        }
    }

    /**
     * @return Local {@link TeleTargetRoutine} for the method in the {@link TeleVM}; null if not bound to target code yet.
     */
    public abstract TeleTargetRoutine teleTargetRoutine();

    /**
     * @return Text suitable for a tool tip.
     */
    public abstract String getToolTip();

    /**
     * @param codeViewer Code view that should be closed and removed from the visual inspection; if this is the only
     *            view in the method inspection, then dispose of the method inspection as well.
     */
    public abstract void closeCodeViewer(CodeViewer codeViewer);

    public void makeMemoryInspector() {
        if (teleTargetRoutine() != null) {
            MemoryInspector.create(inspection(), _parent.residence(), teleTargetRoutine().targetCodeRegion().start(), teleTargetRoutine().targetCodeRegion().size().toInt(), 1, 8);
        }
    }

    public InspectorAction getMemoryInspectorAction() {
        return new InspectorAction(inspection(), "Inspect Memory") {

            @Override
            protected void procedure() {
                makeMemoryInspector();
            }
        };
    }

    public void makeMemoryWordInspector() {
        if (teleTargetRoutine() != null) {
            MemoryWordInspector.create(inspection(), _parent.residence(), teleTargetRoutine().targetCodeRegion().start(), teleTargetRoutine().targetCodeRegion().size().toInt());
        }
    }

    public InspectorAction getMemoryWordInspectorAction() {
        return new InspectorAction(inspection(), "Inspect Memory Words") {

            @Override
            protected void procedure() {
                makeMemoryWordInspector();
            }
        };
    }
}
