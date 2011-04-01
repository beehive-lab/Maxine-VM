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

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.SaveSettingsListener;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;

/**
 * A tabbed container for {@link MethodInspector}s.  This container is managed
 * as a singleton view.
 *
 * @author Bernd Mathiske
 * @author Mick Jordan
 * @author Michael Van De Vanter
 */
public final class MethodInspectorContainer extends TabbedInspector<MethodInspector> {

    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.METHODS;
    private static final String SHORT_NAME = "Methods";
    private static final String LONG_NAME = "Methods Inspector";
    private static final String GEOMETRY_SETTINGS_KEY = "methodsInspectorGeometry";

    static final class MethodViewManager extends AbstractSingletonViewManager<MethodInspectorContainer> {

        protected MethodViewManager(final Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
            // Method view creation is managed by a listener that creates/highlights
            // method views following user focus.
            inspection.focus().addListener(MethodInspector.methodFocusListener(inspection));
        }

        public boolean isSupported() {
            return true;
        }

        public boolean isEnabled() {
            return true;
        }

        public MethodInspectorContainer activateView(Inspection inspection) {
            if (inspector == null) {
                inspector = new MethodInspectorContainer(inspection);
            }
            // Creating the container also starts the code focus listener for creating method viewers.
            // If we set the focus now, the newly created container will appear with the currently
            // actively method showing; otherwise the container would initially appear empty.
            final MaxThread thread = focus().thread();
            if (thread != null) {
                if (focus().codeLocation() != null) {
                    focus().setCodeLocation(focus().codeLocation());
                } else if (thread.ipLocation() != null) {
                    focus().setCodeLocation(thread.ipLocation());
                }
            }
            return inspector;
        }
    }

    // Will be non-null before any instances created.
    private static MethodViewManager viewManager = null;

    public static ViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new MethodViewManager(inspection);
        }
        return viewManager;
    }

    private final SaveSettingsListener saveSettingsListener = createGeometrySettingsListener(this, GEOMETRY_SETTINGS_KEY);

    private MethodInspectorContainer(Inspection inspection) {
        super(inspection, VIEW_KIND);
        Trace.begin(TRACE_VALUE,  tracePrefix() + " initializing");
        createFrame(false);
        Trace.end(TRACE_VALUE,  tracePrefix() + " initializing");
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return saveSettingsListener;
    }

    @Override
    public String getTextForTitle() {
        final MethodInspector methodInspector = getSelected();
        return methodInspector == null ? viewManager.shortName() : "Method: " + methodInspector.getToolTip();
    }

    @Override
    protected void refreshState(boolean force) {
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                MethodInspectorPreferences.globalPreferences(inspection()).showDialog();
            }
        };
    }

    @Override
    public InspectorAction getPrintAction() {
        return new InspectorAction(inspection(), "Print") {
            @Override
            public void procedure() {
                final MethodInspector methodInspector = MethodInspectorContainer.this.getSelected();
                if (methodInspector != null) {
                    methodInspector.print();
                }
            }
        };
    }

    @Override
    public void add(MethodInspector methodInspector) {
        final String longTitle = methodInspector.getToolTip();
        add(methodInspector, methodInspector.getTextForTitle(), longTitle);
        addCloseIconToTab(methodInspector, longTitle);
        methodInspector.highlight();
    }

    /**
     * Relocate the MethodInspector as necessary so that the upper left corner is visible.
     */
    public void makeVisible() {
        gui().moveToExposeDefaultMenu(this);
    }

}
