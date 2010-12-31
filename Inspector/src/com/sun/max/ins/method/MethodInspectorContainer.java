/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;

/**
 * A tabbed container for {@link MethodInspector}s.
 *
 * @author Bernd Mathiske
 * @author Mick Jordan
 * @author Michael Van De Vanter
 */
public final class MethodInspectorContainer extends TabbedInspector<MethodInspector> {

    private static MethodInspectorContainer methodInspectorContainer = null;

    /**
     * Return the singleton MethodInspectorContainer, creating it if necessary.
     */
    public static MethodInspectorContainer make(Inspection inspection) {
        if (methodInspectorContainer == null) {
            methodInspectorContainer = new MethodInspectorContainer(inspection);
        }
        return methodInspectorContainer;
    }

    private MethodInspectorContainer(Inspection inspection) {
        super(inspection, "methodsInspector");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return  inspection().geometry().methodsFrameDefaultBounds();
    }

    @Override
    public String getTextForTitle() {
        final MethodInspector methodInspector = getSelected();
        return methodInspector == null ? "Methods" : "Method: " + methodInspector.getToolTip();
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

    @Override
    public void inspectorClosing() {
        methodInspectorContainer = null;
        super.inspectorClosing();
    }

}
