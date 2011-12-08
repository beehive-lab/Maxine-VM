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
package com.sun.max.ins.view;

import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.ins.BootImageView.BootImageViewManager;
import com.sun.max.ins.InspectionSettings.AbstractSaveSettingsListener;
import com.sun.max.ins.InspectionSettings.SaveSettingsEvent;
import com.sun.max.ins.InspectionSettings.SaveSettingsListener;
import com.sun.max.ins.NotepadView.NotepadViewManager;
import com.sun.max.ins.UserFocusView.UserFocusViewManager;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.debug.BreakpointsView.BreakpointsViewManager;
import com.sun.max.ins.debug.MarkBitsView.*;
import com.sun.max.ins.debug.RegistersView.RegistersViewManager;
import com.sun.max.ins.debug.StackFrameView.StackFrameViewManager;
import com.sun.max.ins.debug.StackView.StackViewManager;
import com.sun.max.ins.debug.ThreadLocalsView.ThreadLocalsViewManager;
import com.sun.max.ins.debug.ThreadsView.ThreadsViewManager;
import com.sun.max.ins.debug.WatchpointsView.WatchpointsViewManager;
import com.sun.max.ins.file.*;
import com.sun.max.ins.file.JavaSourceView.JavaSourceViewManager;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.java.*;
import com.sun.max.ins.java.DebugInfoView.DebugInfoViewManager;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.memory.AllocationsView.AllocationsViewManager;
import com.sun.max.ins.memory.MemoryBytesView.MemoryBytesViewManager;
import com.sun.max.ins.memory.MemoryView.MemoryViewManager;
import com.sun.max.ins.method.*;
import com.sun.max.ins.method.MethodViewContainer.MethodViewManager;
import com.sun.max.ins.object.HeapRegionInfoView.HeapRegionInfoViewManager;
import com.sun.max.ins.object.JVMTILogView.*;
import com.sun.max.ins.object.*;
import com.sun.max.program.option.*;

/**
 * A generalized manager for views, some of which are singletons and some of which are not.
 */
public final class InspectionViews extends AbstractInspectionHolder {

    private static final int TRACE_VALUE = 1;

    /**
     * The kinds of VM inspection views that can be activated (made visible).
     *
     */
    public enum ViewKind {
        ALLOCATIONS(true, false, "Regions of memory allocated by the VM") {

            @Override
            public AllocationsViewManager viewManager() {
                final AllocationsViewManager viewManager = AllocationsView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        BOOT_IMAGE(true, false, "Selected parameters in the VM's boot image") {

            @Override
            public BootImageViewManager viewManager() {
                final BootImageViewManager viewManager = BootImageView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        BREAKPOINTS(true, false, "Breakpoints that currently exist for VM code") {

            @Override
            public BreakpointsViewManager viewManager() {
                final BreakpointsViewManager viewManager = BreakpointsView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        DEBUG_INFO(true, false, "Debugging details at the currently selected position in compiled code") {

            @Override
            public DebugInfoViewManager viewManager() {
                final DebugInfoViewManager viewManager = DebugInfoView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        HEAP_REGION_INFO(false, false, "The Heap Region Info for a memory location") {
            @Override
            public HeapRegionInfoViewManager viewManager() {
                final HeapRegionInfoViewManager viewManager = HeapRegionInfoView.viewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        MARK_BITS_INFO(true, false, "Mark Bits for selected heap addresses") {
            @Override
            public MarkBitsViewManager viewManager() {
                final MarkBitsViewManager viewManager = MarkBitsView.makeViewManager(inspection);

                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        JAVA_SOURCE(false, false, "The contents of a Java source file") {

            @Override
            public JavaSourceViewManager viewManager() {
                final JavaSourceViewManager viewManager = JavaSourceView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        MEMORY(false, false, "The contents of a region of VM memory, expressed as words") {

            @Override
            public MemoryViewManager viewManager() {
                final MemoryViewManager viewManager = MemoryView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        MEMORY_BYTES(false, false, "The contents of a region of VM memory, expressed as bytes") {

            @Override
            public MemoryBytesViewManager viewManager() {
                final MemoryBytesViewManager viewManager = MemoryBytesView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        METHODS(true, true, "Container for multiple disassembled methods from the VM") {

            @Override
            public MethodViewManager viewManager() {
                final MethodViewManager viewManager = MethodViewContainer.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        // View management for methods is handled by the container
        METHOD_CODE(false, false, "Disassembled code from a single method in the VM"),
        NOTEPAD(true, false, "Notepad for keeping user notes") {

            @Override
            public NotepadViewManager viewManager() {
                final NotepadViewManager viewManager = NotepadView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        OBJECT(false, false, "The contents of a region of VM memory interpreted as an object representation") {
            @Override
            public ObjectViewManager viewManager() {
                final ObjectViewManager viewManager = ObjectView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        REGISTERS(true, true, "Register contents in the VM for the currently selected thread") {

            @Override
            public RegistersViewManager viewManager() {
                final RegistersViewManager viewManager = RegistersView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        STACK(true, true, "Stack contents in the VM for the currently selected thread") {

            @Override
            public StackViewManager viewManager() {
                final StackViewManager viewManager = StackView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        STACK_FRAME(true, true, "Stack frame contents in the VM for the currently frame") {

            @Override
            public StackFrameViewManager viewManager() {
                final StackFrameViewManager viewManager = StackFrameView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        THREADS(true, true, "Threads that currently exist in the VM") {

            @Override
            public ThreadsViewManager viewManager() {
                final ThreadsViewManager viewManager = ThreadsView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        THREAD_LOCALS(true, false, "Thread locals in the VM for the currently selected thread") {

            @Override
            public ThreadLocalsViewManager viewManager() {
                final ThreadLocalsViewManager viewManager = ThreadLocalsView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        USER_FOCUS(true, false, "The current state of session's user focus") {

            @Override
            public UserFocusViewManager viewManager() {
                final UserFocusViewManager viewManager = UserFocusView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        WATCHPOINTS(true, false, "Watchpoints that currently exist for VM memory") {

            @Override
            public WatchpointsViewManager viewManager() {
                final WatchpointsViewManager viewManager = WatchpointsView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        JVMTILOG(true, false, "JVMTI Log View") {

            @Override
            public JVMTILogViewManager viewManager() {
                final JVMTILogViewManager viewManager = JVMTILogView.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        };

        private static Inspection inspection;
        private static ViewKind[] singletonViewKinds;
        private static ViewKind[] multiViewKinds;
        static {
            // Rely on the language property that statics are executed
            // after all initializations are complete.
            final List<ViewKind> singletonKinds = new ArrayList<ViewKind>();
            final List<ViewKind> multiKinds = new ArrayList<ViewKind>();
            for (ViewKind kind : ViewKind.values()) {
                if (kind.isSingleton) {
                    singletonKinds.add(kind);
                } else {
                    multiKinds.add(kind);
                }
            }
            singletonViewKinds = singletonKinds.toArray(new ViewKind[0]);
            multiViewKinds = multiKinds.toArray(new ViewKind[0]);
        }

        private final boolean isSingleton;
        private final boolean activeByDefault;
        private final String description;
        private final String key;

        private ViewKind(boolean isSingleton, boolean activeByDefault, String description) {
            this.isSingleton = isSingleton;
            this.activeByDefault = activeByDefault;
            this.description = description;
            this.key = this.name().toLowerCase();
        }

        public ViewManager<? extends AbstractView> viewManager() {
            return null;
        }

        private InspectorAction deactivateAllAction(AbstractView exceptInspector) {
            final ViewManager< ? extends AbstractView> viewManager = viewManager();
            return viewManager == null ? null : viewManager.deactivateAllAction(exceptInspector);
        }
    }

    final SaveSettingsListener saveSettingsListener;
    final InspectorAction resetViewsToDefaultAction;
    final InspectorAction deactivateAllAction;

    public InspectionViews(Inspection inspection) {
        super(inspection);
        ViewKind.inspection = inspection;
        saveSettingsListener = new AbstractSaveSettingsListener("viewActive") {

            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                for (ViewKind kind : ViewKind.singletonViewKinds) {
                    final SingletonViewManager singletonViewManager = (SingletonViewManager) kind.viewManager();
                    if (singletonViewManager.isSupported()) {
                        saveSettingsEvent.save(kind.key, singletonViewManager.isActive());
                    }
                }
            }
        };
        inspection.settings().addSaveSettingsListener(saveSettingsListener);
        resetViewsToDefaultAction = new InspectorAction(inspection, "Reset to standard view set") {

            @Override
            protected void procedure() {
                // The standard view set has no multi view instances; dispose all
                for (AbstractView view : activeViews()) {
                    final ViewKind kind = view.viewManager().viewKind();
                    if (!kind.isSingleton) {
                        view.dispose();
                    }
                }
                for (ViewKind kind : ViewKind.singletonViewKinds) {
                    final SingletonViewManager viewManager = (SingletonViewManager) kind.viewManager();
                    if (kind.activeByDefault) {
                        gui().restoreDefaultGeometry(viewManager.activateView());
                    } else if (viewManager.isActive()) {
                        viewManager.deactivateView();
                    }
                }
            }
        };
        deactivateAllAction = new InspectorAction(inspection, "Close all views") {

            @Override
            protected void procedure() {
                for (AbstractView view : activeViews()) {
                    view.dispose();
                }
            }
        };
    }

    /**
     * @return all active views
     */
    public List<AbstractView> activeViews() {
        final List<AbstractView> views = new ArrayList<AbstractView>();
        for (ViewKind kind : ViewKind.values()) {
            final ViewManager<? extends AbstractView> viewManager = kind.viewManager();
            if (viewManager != null) {
                views.addAll(viewManager.activeViews());
            }
        }
        return views;
    }

    /**
     * @return all active views of a particular kind
     */
    public List<? extends AbstractView> activeViews(ViewKind kind) {
        final ViewManager<? extends AbstractView> viewManager = kind.viewManager();
        if (viewManager != null) {
            return viewManager.activeViews();
        }
        return Collections.emptyList();
    }

    /**
     * Create all the views that should be present at the beginning of a session.
     */
    public void activateInitialViews() {
        final InspectionSettings settings = inspection().settings();
        for (ViewKind kind : ViewKind.singletonViewKinds) {
            if (kind.viewManager().isSupported() && settings.get(saveSettingsListener, kind.key, OptionTypes.BOOLEAN_TYPE, kind.activeByDefault)) {
                final SingletonViewManager singletonViewManager = (SingletonViewManager) kind.viewManager();
                singletonViewManager.activateView();
            }
        }
        for (ViewKind kind : ViewKind.multiViewKinds) {
            // Initialize any view managers that might need it.
            kind.viewManager();
        }
    }

    /**
     * @return access to view creation for memory views.
     */
    public MemoryViewFactory memory() {
        return (MemoryViewFactory) ViewKind.MEMORY.viewManager();
    }

    /**
     * @return access to view creation for memory bytes views.
     */
    public MemoryBytesViewFactory memoryBytes() {
        return (MemoryBytesViewFactory) ViewKind.MEMORY_BYTES.viewManager();
    }

    /**
     * @return access to view creation for objects in the VM.
     */
    public ObjectViewFactory objects() {
        return (ObjectViewFactory) ViewKind.OBJECT.viewManager();
    }

    /**
     * Activates a singleton view. It is an error to call this on a
     * view kind that is not a singleton.
     *
     * @param kind the kind of view, must be a singleton.
     * @return a new view
     */
    public AbstractView activateSingletonView(ViewKind kind) {
        assert kind.isSingleton;
        final ViewManager< ? extends AbstractView> viewManager = kind.viewManager();
        if (viewManager != null) {
            SingletonViewManager singletonViewManager = (SingletonViewManager) viewManager;
            return singletonViewManager.activateView();
        }
        return null;
    }

    /**
     * Gets the action that will activate a singleton view.
     *
     * @param kind the kind of view to be activated, must be a singleton.
     * @return the action for activating the view, null if not a singleton
     */
    public InspectorAction activateSingletonViewAction(ViewKind kind) {
        if (kind.isSingleton) {
            final ViewManager< ? extends AbstractView> viewManager = kind.viewManager();
            if (viewManager != null) {
                SingletonViewManager singletonViewManager = (SingletonViewManager) viewManager;
                return singletonViewManager.activateSingletonViewAction();
            }
        }
        return null;
    }

    public Object resetViewsToDefaultAction() {
        return resetViewsToDefaultAction;
    }

    /**
     * Gets the action that will deactivate all active views.
     *
     * @return the action for deactivating views
     */
    public InspectorAction deactivateAllViewsAction() {
        return deactivateAllAction;
    }

    /**
     * Gets the action that will deactivate all views of a particular kind.
     *
     * @param kind the kind of views to be deactivated
     * @return the action for deactivating
     */
    public InspectorAction deactivateAllViewsAction(ViewKind kind) {
        return kind.deactivateAllAction(null);
    }


    /**
     * Gets the action that will deactivate all active views with one exception.
     *
     * @param exceptInspector the one view that should not be deactivated
     * @return the action for deactivating
     */
    public InspectorAction deactivateOtherViewsAction(final AbstractView exceptInspector) {
        return new InspectorAction(inspection(), "Close other views") {

            @Override
            protected void procedure() {
                for (AbstractView view : activeViews()) {
                    if (view != exceptInspector) {
                        view.dispose();
                    }
                }
            }

        };
    }

    /**
     * Gets the action that will deactivate all views of a particular kind
     * with one exception.
     *
     * @param kind the kind of views to be deactivated
     * @param exceptInspector the one view that should not be deactivated
     * @return the action for deactivating
     */
    public InspectorAction deactivateOtherViewsAction(ViewKind kind, AbstractView exceptInspector) {
        return kind.deactivateAllAction(exceptInspector);
    }


}
