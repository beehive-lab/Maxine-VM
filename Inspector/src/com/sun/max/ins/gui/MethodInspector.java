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

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.method.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
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
public abstract class MethodInspector extends Inspector {

    private static final int TRACE_VALUE = 2;

    private static Manager manager;

    /**
     * Manages inspection of methods in the VM, even when the tabbed view does not exist.
     * Has no visible presence or direct user interaction at this time.
     *
     * @author Michael Van De Vanter
     */
    public static final class Manager extends AbstractInspectionHolder {

        private Manager(Inspection inspection) {
            super(inspection);
        }

        public static void make(Inspection inspection) {
            if (manager == null) {
                manager = new Manager(inspection);
                inspection.focus().addListener(new InspectionFocusAdapter() {

                    @Override
                    public void codeLocationFocusSet(TeleCodeLocation teleCodeLocation, boolean interactiveForNative) {
                        final MethodInspector methodInspector = MethodInspector.make(manager.inspection(), teleCodeLocation, interactiveForNative);
                        if (methodInspector != null) {
                            // Ensure that a newly created MethodInspector will have the focus set;
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    methodInspector.setCodeLocationFocus();
                                    // Highlight the inspector if it is not the selected one (this happens when the inspector already existed).
                                    methodInspector.highlightIfNotVisible();
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
     * @param address Target code location in the VM.
     * @param interactive Should user be prompted for additional address information in case the location is unknown
     *            native code.
     * @return A possibly new inspector, null if unable to view.
     */
    private static MethodInspector make(final Inspection inspection, Address address, boolean interactive) {
        MethodInspector methodInspector = null;
        final TeleTargetMethod teleTargetMethod = inspection.maxVM().makeTeleTargetMethod(address);
        if (teleTargetMethod != null) {
            // Java method
            methodInspector = make(inspection, teleTargetMethod, MethodCodeKind.TARGET_CODE);
        } else {
            final TeleTargetRoutine teleTargetRoutine = inspection.maxVM().findTeleTargetRoutine(TeleTargetRoutine.class, address);
            if (teleTargetRoutine != null) {
                // Some other kind of known target code
                methodInspector = make(inspection, teleTargetRoutine);
            } else if (interactive) {
                // Code location is not in a Java method or runtime stub and has not yet been viewed in a native routine.
                // Give the user a chance to guess at its length so we can register and view it
                final MutableInnerClassGlobal<MethodInspector> result = new MutableInnerClassGlobal<MethodInspector>();
                final String defaultDescription = "Native code @0x" + address.toHexString();
                new NativeLocationInputDialog(inspection, "Describe unknown native code", address, TeleNativeTargetRoutine.DEFAULT_NATIVE_CODE_LENGTH, defaultDescription) {
                    @Override
                    public void entered(Address nativeAddress, Size codeSize, String enteredDescription) {
                        try {
                            String description = enteredDescription;
                            if (description == null || description.equals("")) {
                                description = "Native code @0x" + nativeAddress.toHexString();
                            }
                            final TeleNativeTargetRoutine teleNativeTargetRoutine = maxVM().createTeleNativeTargetRoutine(nativeAddress, codeSize, description);
                            result.setValue(MethodInspector.make(inspection, teleNativeTargetRoutine));
                            // inspection.focus().setCodeLocation(new TeleCodeLocation(inspection.teleVM(), nativeAddress));
                        } catch (IllegalArgumentException illegalArgumentException) {
                            inspection.gui().errorMessage("Specified native code range overlaps region already registered in Inpsector");
                        }
                    }
                    @Override
                    public boolean isValidSize(Size size) {
                        return size.greaterThan(0);
                    }
                };
                methodInspector = result.value();
            }
        }
        return methodInspector;
    }

    private static final VariableMapping<TeleTargetRoutine, MethodInspector> teleTargetRoutineToMethodInspector = new IdentityHashMapping<TeleTargetRoutine, MethodInspector>();
    private static final VariableMapping<TeleClassMethodActor, MethodInspector> teleClassMethodActorToMethodInspector = new IdentityHashMapping<TeleClassMethodActor, MethodInspector>();


    /**
     * Makes an inspector displaying code for specified code location. Should always work for
     * Java methods. For native methods, only works if the code block is already known.
     *
     * @param teleCodeLocation a code location
     * @return A possibly new inspector, null if unable to view.
     */
    private static MethodInspector make(Inspection inspection, TeleCodeLocation teleCodeLocation, boolean interactiveForNative) {
        if (teleCodeLocation.hasTargetCodeLocation()) {
            return make(inspection, teleCodeLocation.targetCodeInstructionAddress(), interactiveForNative);
        }
        if (teleCodeLocation.hasBytecodeLocation()) {
            // TODO (mlvdv)  Select the specified bytecode position
            return make(inspection, teleCodeLocation.teleBytecodeLocation().teleClassMethodActor(), MethodCodeKind.BYTECODES);
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
    private static JavaMethodInspector make(Inspection inspection, TeleClassMethodActor teleClassMethodActor, MethodCodeKind codeKind) {
        JavaMethodInspector javaMethodInspector = null;
        // If there are compilations, then inspect in association with the most recent
        final TeleTargetMethod teleTargetMethod = teleClassMethodActor.getCurrentJavaTargetMethod();
        if (teleTargetMethod != null) {
            return make(inspection, teleTargetMethod, codeKind);
        }

        // TODO (mlvdv)  Clean up
        //final UniqueInspector.Key<? extends MethodInspector> key = UniqueInspector.Key.create(JavaMethodInspector.class, teleClassMethodActor);
        //final MethodInspector methodInspector = UniqueInspector.find(inspection, key);

        final MethodInspector methodInspector = teleClassMethodActorToMethodInspector.get(teleClassMethodActor);
        if (methodInspector == null) {
            final MethodInspectorContainer parent = MethodInspectorContainer.make(inspection);
            javaMethodInspector = new JavaMethodInspector(inspection, parent, teleClassMethodActor, codeKind);
            parent.add(javaMethodInspector);
            teleClassMethodActorToMethodInspector.put(teleClassMethodActor, javaMethodInspector);

        } else {
            javaMethodInspector = (JavaMethodInspector) methodInspector;
        }
        return javaMethodInspector;
    }

    /**
     * @return a possibly new {@link MethodInspector} associated with a specific compilation of a Java method in the
     *         VM, and with the requested code view visible.
     */
    private static JavaMethodInspector make(Inspection inspection, TeleTargetMethod teleTargetMethod, MethodCodeKind codeKind) {
        JavaMethodInspector javaMethodInspector = null;


        // TODO (mlvdv) Clean up
        //final UniqueInspector.Key<? extends MethodInspector> targetMethodKey = UniqueInspector.Key.create(JavaMethodInspector.class, teleTargetMethod);

        //MethodInspector methodInspector = UniqueInspector.find(inspection, targetMethodKey);

        // Is there already an inspection open that is bound to this compilation?
        MethodInspector methodInspector = teleTargetRoutineToMethodInspector.get(teleTargetMethod);
        if (methodInspector == null) {
            // No existing inspector is bound to this compilation; see if there is an inspector for this method that is
            // unbound

            // TODO (mlvdv) Clean up
            //UniqueInspector.Key<? extends MethodInspector> classMethodActorKey = null;
            TeleClassMethodActor teleClassMethodActor = teleTargetMethod.getTeleClassMethodActor();
            if (teleClassMethodActor != null) {
                //classMethodActorKey = UniqueInspector.Key.create(JavaMethodInspector.class, teleTargetMethod.getTeleClassMethodActor());
                //methodInspector = UniqueInspector.find(inspection, classMethodActorKey);
                methodInspector = teleClassMethodActorToMethodInspector.get(teleClassMethodActor);

            }
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
            teleTargetRoutineToMethodInspector.put(teleTargetMethod, javaMethodInspector);
        } else {
            // An existing inspector is bound to this method & compilation; ensure that it has the requested code view
            javaMethodInspector = (JavaMethodInspector) methodInspector;
            javaMethodInspector.viewCodeKind(codeKind);
        }
        return javaMethodInspector;
    }

    /**
     * @return A possibly new inspector for a block of native code in the VM already known to the inspector.
     */
    private static NativeMethodInspector make(Inspection inspection, TeleTargetRoutine teleTargetRoutine) {
        NativeMethodInspector nativeMethodInspector = null;

        // TODO (mlvdv) Clean up
        //final UniqueInspector.Key<? extends MethodInspector> key = UniqueInspector.Key.create(NativeMethodInspector.class, teleTargetRoutine.teleRoutine());
        //final MethodInspector methodInspector = UniqueInspector.find(inspection, key);

        MethodInspector methodInspector = teleTargetRoutineToMethodInspector.get(teleTargetRoutine);

        if (methodInspector == null) {
            final MethodInspectorContainer parent = MethodInspectorContainer.make(inspection);
            nativeMethodInspector = new NativeMethodInspector(inspection, parent, teleTargetRoutine);
            parent.add(nativeMethodInspector);
            teleTargetRoutineToMethodInspector.put(teleTargetRoutine, nativeMethodInspector);
        } else {
            nativeMethodInspector = (NativeMethodInspector) methodInspector;
        }
        return nativeMethodInspector;
    }

    private final MethodInspectorContainer parent;

    protected MethodInspector(Inspection inspection, MethodInspectorContainer parent) {
        super(inspection);
        this.parent = parent;
    }

    @Override
    public InspectorFrame createTabFrame(TabbedInspector parent) {

        final InspectorFrame frame = super.createTabFrame(parent);

        frame.makeMenu(MenuKind.EDIT_MENU);

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(actions().inspectTargetRegionMemoryWords(teleTargetRoutine()));
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        final JMenuItem viewMemoryRegionsMenuItem = new JMenuItem(actions().viewMemoryRegions());
        viewMemoryRegionsMenuItem.setText("View Memory Regions");
        memoryMenu.add(viewMemoryRegionsMenuItem);

        frame.makeMenu(MenuKind.OBJECT_MENU);

        frame.makeMenu(MenuKind.CODE_MENU);

        frame.makeMenu(MenuKind.DEBUG_MENU);

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));
        return frame;
    }


    /**
     * Updates the code selection to agree with the current focus.
     */
    private void setCodeLocationFocus() {
        codeLocationFocusSet(inspection().focus().codeLocation(), false);
    }

    @Override
    public void breakpointStateChanged() {
        // TODO (mlvdv)  Data reading PATCH
        if (maxVMState().processState() != ProcessState.TERMINATED) {
            refreshView(true);
        }
    }

    public void close() {
        parent.close(this);
    }

    public void closeOthers() {
        parent.closeOthers(this);
    }

    @Override
    public void moveToFront() {
        if (parent != null) {
            parent.setSelected(this);
        } else {
            super.moveToFront();
        }
    }

    @Override
    public void setSelected() {
        if (parent != null) {
            parent.setSelected(this);
        } else {
            super.moveToFront();
        }
    }

    @Override
    public boolean isSelected() {
        if (parent != null) {
            return parent.isSelected(this);
        }
        return false;
    }

    /**
     * @return Local {@link TeleTargetRoutine} for the method in the VM; null if not bound to target code yet.
     */
    public abstract TeleTargetRoutine teleTargetRoutine();

    /**
     * @return Java method information; null if not known to be associated with a Java method.
     */
    public abstract TeleClassMethodActor teleClassMethodActor();

    /**
     * @return Text suitable for a tool tip.
     */
    public abstract String getToolTip();

    /**
     * Prints the content of the method display.
     */
    public abstract void print();

    /**
     * @param codeViewer Code view that should be closed and removed from the visual inspection; if this is the only
     *            view in the method inspection, then dispose of the method inspection as well.
     */
    public abstract void closeCodeViewer(CodeViewer codeViewer);

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing for " + getTitle());
        teleTargetRoutineToMethodInspector.remove(teleTargetRoutine());
        teleClassMethodActorToMethodInspector.remove(teleClassMethodActor());
        super.inspectorClosing();
    }

}
