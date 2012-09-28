/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;

/**
 * A tabbed container for {@link MethodView}s.  This container is managed
 * as a singleton view.
 */
public final class MethodViewContainer extends TabbedView<MethodViewContainer> {

    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.METHODS;
    private static final String SHORT_NAME = "Methods";
    private static final String LONG_NAME = "Methods View";
    private static final String GEOMETRY_SETTINGS_KEY = "methodContainerGeometry";

    public static final class MethodViewManager extends AbstractSingletonViewManager<MethodViewContainer> {

        protected MethodViewManager(final Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
            // Method view creation is managed by a listener that creates/highlights
            // method views following user focus.
            inspection.focus().addListener(MethodView.methodFocusListener(inspection));
        }

        @Override
        protected MethodViewContainer createView(Inspection inspection) {
            final MethodViewContainer methodViewContainer = new MethodViewContainer(inspection);
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
            return methodViewContainer;
        }

    }

    // Will be non-null before any instances created.
    private static MethodViewManager viewManager = null;

    public static MethodViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new MethodViewManager(inspection);
        }
        return viewManager;
    }

    final Class<MethodView> methodViewType = null;

    private MethodViewContainer(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        Trace.begin(TRACE_VALUE,  tracePrefix() + " initializing");
        createFrame(false);
        Trace.end(TRACE_VALUE,  tracePrefix() + " initializing");
    }

    @Override
    public String getTextForTitle() {
        final InspectorView view = getSelected();
        final MethodView methodView = Utils.cast(methodViewType, view);
        return methodView == null ? viewManager.shortName() : "Method: " + methodView.getToolTip();
    }

    @Override
    protected void refreshState(boolean force) {
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                MethodViewPreferences.globalPreferences(inspection()).showDialog();
            }
        };
    }

    @Override
    public InspectorAction getPrintAction() {
        return new InspectorAction(inspection(), "Print") {
            @Override
            public void procedure() {
                final InspectorView view = MethodViewContainer.this.getSelected();
                final MethodView methodView = Utils.cast(methodViewType, view);
                if (methodView != null) {
                    methodView.print();
                }
            }
        };
    }

    @Override
    public void add(AbstractView view) {
        final MethodView methodView = Utils.cast(methodViewType, view);
        final String longTitle = methodView.getToolTip();
        add(methodView, methodView.getTextForTitle(), longTitle);
        addCloseIconToTab(methodView, longTitle);
        methodView.highlight();
    }

    /**
     * Relocate the Method view as necessary so that the upper left corner is visible.
     */
    public void makeVisible() {
        gui().moveToExposeDefaultMenu(this);
    }

    @Override
    public void vmProcessTerminated() {
        for (InspectorView view : this) {
            view.dispose();
        }
    }

}
