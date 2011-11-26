/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.gui;

import java.awt.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.view.*;
import com.sun.max.tele.*;

/**
 * A scroll pane specialized for use in the VM Inspector.
 * By default uses policies VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER
 */
public class InspectorScrollPane extends JScrollPane implements Prober, InspectionHolder {

    private final Inspection inspection;
    private final String tracePrefix;

    /**
     * Creates a new {@link JScrollPane} for use in the {@link Inspection}.
     * By default uses policies VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER.
     *
     * @param inspection
     * @param component the component to display in the scrollbar's viewport
     */
    public InspectorScrollPane(Inspection inspection, Component component) {
        super(component, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.inspection = inspection;
        this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
        // Ensure that any background that isn't covered with the component has the same background color
        if (getViewport() != null) {
            getViewport().setBackground(component.getBackground());
        }
    }

    public final Inspection inspection() {
        return inspection;
    }

    public final MaxVM vm() {
        return inspection.vm();
    }

    public final InspectorGUI gui() {
        return inspection.gui();
    }

    public final InspectionFocus focus() {
        return inspection.focus();
    }

    public final InspectionViews views() {
        return inspection.views();
    }

    public final InspectionActions actions() {
        return inspection.actions();
    }

    public final InspectionPreferences preference() {
        return inspection.preference();
    }

    public void redisplay() {
    }

    public void refresh(boolean force) {
    }

    /**
     * @return default prefix text for trace messages; identifies the class being traced.
     */
    protected String tracePrefix() {
        return tracePrefix;
    }
}
