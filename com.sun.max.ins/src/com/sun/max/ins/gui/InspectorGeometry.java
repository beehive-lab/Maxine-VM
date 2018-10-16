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
package com.sun.max.ins.gui;

import java.awt.*;

import com.sun.max.ins.view.InspectionViews.ViewKind;

/**
 * Preferences and policies for layout out inspection windows.
 */
public interface InspectorGeometry {

    // Main Inspector frame
    Point inspectorDefaultFrameLocation();
    Dimension inspectorMinFrameSize();
    Dimension inspectorPrefFrameSize();

    /**
     * Gets the default geometry for a view, null if none specified.
     *
     * @param viewKind the kind of view
     * @return the default geometry specified for the kind
     */
    Rectangle preferredFrameGeometry(ViewKind viewKind);

    // Offset from mouse location for new frames
    int newFrameDiagonalOffset();
}
