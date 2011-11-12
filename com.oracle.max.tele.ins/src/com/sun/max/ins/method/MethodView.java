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
import static com.sun.max.tele.MaxProcessState.*;

import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.MethodViewContainer.MethodViewManager;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * A view that can present one or more code representations of a method. Method views are unique, keyed from
 * an instance of {@link TeleClassMethodActor}.
 * <p>
 * Views are managed via the container class {@link MethodViewContainer}, in which method views are displayed.
 * <p>
 * Instance view creation follows the user focus, and a static listener here drives the factory for method views.
 */
public abstract class MethodView<View_Kind extends MethodView> extends AbstractView<View_Kind> {

    private static final int TRACE_VALUE = 2;
    private static final ViewKind VIEW_KIND = ViewKind.METHOD_CODE;

    private static ViewFocusListener methodFocusListener;

    /**
     * Gets (singleton) listener that can be added to focus listeners, which causes the Method view factory
     * to create/highlight a view on the method in focus.
     *
     * @param inspection
     * @return
     */
    static ViewFocusListener methodFocusListener(final Inspection inspection) {
        if (methodFocusListener == null) {
            methodFocusListener = new InspectionFocusAdapter() {

                @Override
                public void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative) {
                    if (codeLocation != null && ViewKind.METHODS.viewManager().isActive()) {
                        try {
                            final MethodView methodView = MethodView.make(inspection, codeLocation, interactiveForNative);
                            if (methodView != null) {
                                methodView.setCodeLocationFocus();
                                methodView.highlightIfNotVisible();
                            }
                        } catch (MaxVMBusyException maxVMBusyException) {
                            inspection.announceVMBusyFailure("Can't view method");
                        }
                    }
                }
            };
        }
        return methodFocusListener;
    }

    /**
     * Makes a view displaying code for the method pointed to by the instructionPointer. Should always work for Java
     * methods. For external native methods, only works if the code block is already known to the Inspector or if the
     * user supplies some additional information at an optional prompt.
     *
     * @param address machine code location in the VM.
     * @param interactive Should user be prompted for additional address information in case the location is unknown
     *            native code.
     * @return A possibly new view, null if unable to view.
     * @throws MaxVMBusyException if creation of a new view fails because the VM is unavailable
     */
    private static MethodView make(final Inspection inspection, Address address, boolean interactive) throws MaxVMBusyException {
        MethodView methodView = null;
        final MaxCompilation compiledCode = inspection.vm().machineCode().findCompilation(address);
        if (compiledCode != null) {
            // Java method
            methodView = make(inspection, compiledCode, MethodCodeKind.MACHINE_CODE);
        } else {
            final MaxExternalCodeRoutine externalCode = inspection.vm().machineCode().findExternalCode(address);
            if (externalCode != null) {
                // Some other kind of known external machine code
                methodView = make(inspection, externalCode);
            } else if (interactive) {
                // Code location is not in a Java method or runtime stub and has not yet been viewed in a native routine.
                // Check any loaded native code maps and in the last resort
                // give the user a chance to guess at its length so we can register and view it
                NativeCodeLibraries.SymbolInfo nativeCodeInfo = NativeCodeLibraries.find(address);
                if (nativeCodeInfo != null) {
                    try {
                        final MaxExternalCodeRoutine nativeCode = inspection.vm().machineCode().registerExternalCode(nativeCodeInfo.base, nativeCodeInfo.length, nativeCodeInfo.qualName());
                        methodView = MethodView.make(inspection, nativeCode);
                    } catch (MaxInvalidAddressException e) {
                        inspection.gui().errorMessage("Unable to read memory at " + address.to0xHexString());
                        e.printStackTrace();
                    }
                } else {
                    final MutableInnerClassGlobal<MethodView> result = new MutableInnerClassGlobal<MethodView>();
                    final String defaultDescription = "Native code @0x" + address.toHexString();
                    new NativeLocationInputDialog(inspection, "Name unknown native code", address, MaxExternalCodeRoutine.DEFAULT_NATIVE_CODE_LENGTH, defaultDescription) {

                        @Override
                        public void entered(Address nativeAddress, long nBytes, String enteredName) {
                            try {
                                String name = enteredName;
                                if (name == null || name.equals("")) {
                                    name = defaultDescription;
                                }
                                final MaxExternalCodeRoutine externalCode = vm().machineCode().registerExternalCode(nativeAddress, nBytes, name);
                                result.setValue(MethodView.make(inspection, externalCode));
                                // inspection.focus().setCodeLocation(new TeleCodeLocation(inspection.vm(), nativeAddress));
                            } catch (IllegalArgumentException illegalArgumentException) {
                                inspection.gui().errorMessage("Specified external code range overlaps region already registered in Inpsector");
                            } catch (MaxVMBusyException maxVMBusyException) {
                                inspection.announceVMBusyFailure("View native code");
                            } catch (MaxInvalidAddressException e) {
                                inspection.gui().errorMessage("Unable to read memory at " + nativeAddress.to0xHexString());
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public boolean isValidSize(long nBytes) {
                            return nBytes > 0;
                        }
                    };
                    methodView = result.value();
                }
            }
        }
        return methodView;
    }

    private static final Map<MaxMachineCodeRoutine, MethodView> machineCodeToMethodView =
        new IdentityHashMap<MaxMachineCodeRoutine, MethodView>();

    private static final Map<TeleClassMethodActor, MethodView> teleClassMethodActorToMethodView =
        new IdentityHashMap<TeleClassMethodActor, MethodView>();

    /**
     * Makes a view displaying code for specified code location. Should always work for
     * Java methods. For native methods, only works if the code block is already known.
     *
     * @param codeLocation a code location
     * @return A possibly new view, null if unable to view.
     * @throws MaxVMBusyException if trying to visit a method not yet seen and the VM is unavailable
     */
    private static MethodView make(Inspection inspection, MaxCodeLocation codeLocation, boolean interactiveForNative) throws MaxVMBusyException {
        if (codeLocation.hasAddress()) {
            return make(inspection, codeLocation.address(), interactiveForNative);
        }
        if (codeLocation.hasTeleClassMethodActor()) {
            // TODO (mlvdv)  Select the specified bytecode position
            return make(inspection, codeLocation.teleClassMethodActor(), MethodCodeKind.BYTECODES);
        }
        // Has neither machine nor bytecode location specified.
        return null;
    }

    /**
     * Display a view for a Java method, showing the kind of code requested if available. If a view for the
     * method doesn't exist, create a new one and display the kind of code requested if available. if an view for
     * the method does exist, add a display of the kind of code requested if available.
     *
     * @return A possibly new view for the method.
     * @throws MaxVMBusyException if creation of a new method view fails because the VM is unavailable
     */
    private static JavaMethodView make(Inspection inspection, TeleClassMethodActor teleClassMethodActor, MethodCodeKind codeKind) throws MaxVMBusyException {
        JavaMethodView javaMethodView = null;
        // If there are compilations, then inspect in association with the most recent
        final MaxCompilation compiledCode = inspection.vm().machineCode().latestCompilation(teleClassMethodActor);
        if (compiledCode != null) {
            return make(inspection, compiledCode, codeKind);
        }
        final MethodView methodView = teleClassMethodActorToMethodView.get(teleClassMethodActor);
        if (methodView == null) {
            final MethodViewManager methodViewManager = (MethodViewManager) ViewKind.METHODS.viewManager();
            final MethodViewContainer container = methodViewManager.activateView();
            inspection.vm().acquireLegacyVMAccess();
            try {
                javaMethodView = new JavaMethodView(inspection, container, teleClassMethodActor, codeKind);
                container.add(javaMethodView);
                teleClassMethodActorToMethodView.put(teleClassMethodActor, javaMethodView);
            } finally {
                inspection.vm().releaseLegacyVMAccess();
            }

        } else {
            javaMethodView = (JavaMethodView) methodView;
        }
        return javaMethodView;
    }

    /**
     * Gets the {@link MethodView} associated with a specific compilation of a Java method in the VM,
     * creating a new one if necessary, and makes the requested code visible.
     *
     * @return a possibly new view with the specified code visible.
     * @throws MaxVMBusyException if can't create a new method view because the VM is unavailable
     */
    private static JavaMethodView make(Inspection inspection, MaxCompilation compiledCode, MethodCodeKind codeKind) throws MaxVMBusyException {
        JavaMethodView javaMethodView = null;

        // Is there already a view open that is bound to this compilation?
        MethodView methodView = machineCodeToMethodView.get(compiledCode);
        if (methodView == null) {
            // No existing view is bound to this compilation; see if there is a view for this method that is
            // unbound
            inspection.vm().acquireLegacyVMAccess();
            try {
                TeleClassMethodActor teleClassMethodActor = compiledCode.getTeleClassMethodActor();
                if (teleClassMethodActor != null) {
                    methodView = teleClassMethodActorToMethodView.get(teleClassMethodActor);
                }
                final MethodViewManager methodViewManager = (MethodViewManager) ViewKind.METHODS.viewManager();
                final MethodViewContainer container = methodViewManager.activateView();
                if (methodView == null) {
                    // No existing view exists for this method; create new one bound to this compilation
                    javaMethodView = new JavaMethodView(inspection, container, compiledCode, codeKind);
                } else {
                    // A view exists for the method, but not bound to any compilation; bind it to this compilation
                    // TODO (mlvdv) Temp patch; just create a new one in this case too.
                    javaMethodView = new JavaMethodView(inspection, container, compiledCode, codeKind);
                }
                if (javaMethodView != null) {
                    container.add(javaMethodView);
                    machineCodeToMethodView.put(compiledCode, javaMethodView);
                }
            } finally {
                inspection.vm().releaseLegacyVMAccess();
            }
        } else {
            // An existing view is bound to this method & compilation; ensure that it has the requested code view
            javaMethodView = (JavaMethodView) methodView;
            javaMethodView.viewCodeKind(codeKind);
        }
        return javaMethodView;
    }

    /**
     * @return A possibly new view for a block of native code in the VM already known to the Inspector.
     * @throws MaxVMBusyException if a new view cannot be created because the VM is unavailable
     */
    private static NativeMethodView make(Inspection inspection, MaxExternalCodeRoutine maxExternalCode) throws MaxVMBusyException {
        NativeMethodView nativeMethodView = null;
        MethodView methodView = machineCodeToMethodView.get(maxExternalCode);
        if (methodView == null) {
            inspection.vm().acquireLegacyVMAccess();
            try {
                final MethodViewManager methodViewManager = (MethodViewManager) ViewKind.METHODS.viewManager();
                final MethodViewContainer container = methodViewManager.activateView();
                nativeMethodView = new NativeMethodView(inspection, container, maxExternalCode);
                container.add(nativeMethodView);
                machineCodeToMethodView.put(maxExternalCode, nativeMethodView);
            } finally {
                inspection.vm().releaseLegacyVMAccess();
            }
        } else {
            nativeMethodView = (NativeMethodView) methodView;
        }
        return nativeMethodView;
    }

    private final MethodViewContainer container;

    protected MethodView(Inspection inspection, MethodViewContainer container) {
        super(inspection, VIEW_KIND, null);
        this.container = container;
    }

    @Override
    public InspectorFrame createTabFrame(TabbedView parent) {

        final InspectorFrame frame = super.createTabFrame(parent);

        // The default menu operates from the perspective of the parent container.
        frame.makeMenu(DEFAULT_MENU).add(defaultMenuItems(DEFAULT_MENU, parent));

        frame.makeMenu(EDIT_MENU);

        final InspectorMenu memoryMenu = frame.makeMenu(MEMORY_MENU);
        final MaxMachineCodeRoutine machineCode = machineCode();
        if (machineCode != null) {
            memoryMenu.add(views().memory().makeViewAction(machineCode.memoryRegion(), machineCode.entityName(), "View memory for machine code"));
        }
        memoryMenu.add(defaultMenuItems(MEMORY_MENU));
        memoryMenu.add(views().activateSingletonViewAction(ViewKind.ALLOCATIONS));

        frame.makeMenu(OBJECT_MENU);

        frame.makeMenu(CODE_MENU);

        frame.makeMenu(DEBUG_MENU);

        frame.makeMenu(VIEW_MENU).add(defaultMenuItems(VIEW_MENU));
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
        // TODO (mlvdv)  Data reading PATCH, there should be a more systematic way of handling this.
        if (vm().state().processState() != TERMINATED) {
            forceRefresh();
        }
    }

    public void close() {
        container.close(this);
    }

    public void closeOthers() {
        container.closeOthers(this);
    }

    /**
     * @return Local {@link MaxMachineCodeRoutine} for the method in the VM; null if not bound to compiled code yet.
     */
    public abstract MaxMachineCodeRoutine machineCode();

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
    public void viewClosing() {
        machineCodeToMethodView.remove(machineCode());
        teleClassMethodActorToMethodView.remove(teleClassMethodActor());
        super.viewClosing();
    }

}
