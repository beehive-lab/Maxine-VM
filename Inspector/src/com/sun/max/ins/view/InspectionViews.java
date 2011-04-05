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
import com.sun.max.ins.InspectionSettings.AbstractSaveSettingsListener;
import com.sun.max.ins.InspectionSettings.SaveSettingsEvent;
import com.sun.max.ins.InspectionSettings.SaveSettingsListener;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.method.*;
import com.sun.max.program.option.*;

/**
 * A generalized manager for views in the inspector, some of which are singletons and some of which are not.
 * <p>
 * So far, only the machinery for singletons has been developed.
 *
 * @author Michael Van De Vanter
 *
 */
public final class InspectionViews extends AbstractInspectionHolder {

    private static final int TRACE_VALUE = 1;

    /**
     * The kinds of VM inspection views that can be activated (made visible).
     *
     * @author Michael Van De Vanter
     */
    public enum ViewKind {
        ALLOCATIONS(true, false, "Regions of memory allocated by the VM") {

            @Override
            public ViewManager viewManager() {
                final ViewManager viewManager = AllocationsInspector.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        BOOT_IMAGE(true, false, "Selected parameters in the VM's boot image") {

            @Override
            public ViewManager viewManager() {
                final ViewManager viewManager = BootImageInspector.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        BREAKPOINTS(true, false, "Breakpoints that currently exist for VM code") {

            @Override
            public ViewManager viewManager() {
                final ViewManager viewManager = BreakpointsInspector.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        FRAME(true, true, "Stack frame contents in the VM for the currently frame") {

            @Override
            public ViewManager viewManager() {
                final ViewManager viewManager = FrameInspector.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        FRAME_DESCRIPTOR(false, false, "The details of a Java frame descriptor"), NOTEPAD(true, false, "Notepad for keeping user notes") {

            @Override
            public ViewManager viewManager() {
                final ViewManager viewManager = NotepadInspector.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        JAVA_SOURCE(false, false, "The contents of a Java source file"),
        MEMORY(false, false, "The contents of a region of VM memory, expressed as words"),
        MEMORY_BYTES(false, false, "The contents of a region of VM memory, expressed as bytes"),
        METHODS(true, true, "Container for multiple disassembled methods from the VM") {

            @Override
            public ViewManager viewManager() {
                final ViewManager viewManager = MethodInspectorContainer.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        METHOD_CODE(false, false, "Disassembled code from a single method in the VM"),
        OBJECT(false, false, "The contents of a region of VM memory interpreted as an object representation"),
        REGISTERS(true, true, "Register contents in the VM for the currently selected thread") {

            @Override
            public ViewManager viewManager() {
                final ViewManager viewManager = RegistersInspector.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        STACK(true, true, "Stack contents in the VM for the currently selected thread") {

            @Override
            public ViewManager viewManager() {
                final ViewManager viewManager = StackInspector.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        THREADS(true, true, "Threads that currently exist in the VM") {

            @Override
            public ViewManager viewManager() {
                final ViewManager viewManager = ThreadsInspector.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        THREAD_LOCALS(true, false, "Thread locals in the VM for the currently selected thread") {

            @Override
            public ViewManager viewManager() {
                final ViewManager viewManager = ThreadLocalsInspector.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        USER_FOCUS(true, false, "The current state of Inspector's user focus") {

            @Override
            public ViewManager viewManager() {
                final ViewManager viewManager = UserFocusInspector.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        },
        WATCHPOINTS(true, true, "Watchpoints that currently exist for VM memory") {

            @Override
            public ViewManager viewManager() {
                final ViewManager viewManager = WatchpointsInspector.makeViewManager(inspection);
                assert viewManager.viewKind() == this;
                return viewManager;
            }
        };

        private static Inspection inspection;
        private static ViewKind[] singletonViewKinds;
        static {
            // Rely on the language property that statics are executed
            // after all initializations are complete.
            final List<ViewKind> singletonKinds = new ArrayList<ViewKind>();
            for (ViewKind kind : ViewKind.values()) {
                if (kind.isSingleton) {
                    singletonKinds.add(kind);
                }
            }
            singletonViewKinds = singletonKinds.toArray(new ViewKind[0]);
        }

        private final boolean isSingleton;
        private final boolean activeByDefault;
        private final String description;
        private final String key;

        /**
         * There's no need for more than one {@link InspectorAction} for activating singleton views. It gets created
         * lazily and stored as a property of the enum.
         */
        private InspectorAction activateViewAction = null;

        private ViewKind(boolean isSingleton, boolean activeByDefault, String description) {
            this.isSingleton = isSingleton;
            this.activeByDefault = activeByDefault;
            this.description = description;
            this.key = this.name().toLowerCase();
        }

        public ViewManager viewManager() {
            return null;
        }
    }

    final SaveSettingsListener saveSettingsListener;

    public InspectionViews(Inspection inspection) {
        super(inspection);
        ViewKind.inspection = inspection;
        for (ViewKind kind : ViewKind.singletonViewKinds) {
            kind.activateViewAction = new ActivateSingletonViewAction(kind);
        }
        saveSettingsListener = new AbstractSaveSettingsListener("inspectionViewActive") {

            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                for (ViewKind kind : ViewKind.singletonViewKinds) {
                    final SingletonViewManager singletonViewManager = (SingletonViewManager) kind.viewManager();
                    saveSettingsEvent.save(kind.key, singletonViewManager.isActive());
                }
            }
        };
        inspection.settings().addSaveSettingsListener(saveSettingsListener);
    }

    /**
     * Create all the views that should be present at the beginning of a session.
     */
    public void activateInitialViews() {
        final InspectionSettings settings = inspection().settings();
        for (ViewKind kind : ViewKind.singletonViewKinds) {
            if (kind.viewManager().isSupported() && settings.get(saveSettingsListener, kind.key, OptionTypes.BOOLEAN_TYPE, kind.activeByDefault)) {
                final SingletonViewManager singletonViewManager = (SingletonViewManager) kind.viewManager();
                singletonViewManager.activateView(inspection());
            }
        }
    }

    /**
     * Gets the action that will activate a singleton view.
     *
     * @param kind the kind of view to be activated, must be a singleton.
     * @return the action for activating
     */
    public InspectorAction activateSingletonViewAction(ViewKind kind) {
        return kind.activateViewAction;
    }

    /**
     * Action: makes visible and highlights a singleton view.
     */
    private final class ActivateSingletonViewAction extends InspectorAction {

        private final ViewKind kind;

        public ActivateSingletonViewAction(ViewKind kind) {
            super(inspection(), "View " + kind.viewManager().shortName());
            assert kind.isSingleton;
            this.kind = kind;
        }

        @Override
        protected void procedure() {
            final SingletonViewManager singletonViewManager = (SingletonViewManager) kind.viewManager();
            singletonViewManager.activateView(inspection()).highlight();
            inspection().settings().save();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(kind.viewManager().isEnabled());
        }
    }

}
