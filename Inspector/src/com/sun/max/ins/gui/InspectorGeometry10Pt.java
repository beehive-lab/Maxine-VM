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
import java.util.*;

import com.sun.max.ins.view.InspectionViews.*;

/**
 * Standard choices and policies for inspection window layout, tiled for use with 10 pt. font.
 *
 * @author Michael Van De Vanter
 */
public class InspectorGeometry10Pt implements InspectorGeometry {

    private final Map<ViewKind, Rectangle> preferredFrameGeometry = new HashMap<ViewKind, Rectangle>();

    // Main Inspection frame
    private static final Point inspectionFrameDefaultLocation = new Point(100, 100);
    private static final Dimension inspectionFrameMinSize = new Dimension(100, 100);
    private static final Dimension inspectionFramePrefSize = new Dimension(1365, 810);

    public InspectorGeometry10Pt() {
        preferredFrameGeometry.put(ViewKind.ALLOCATIONS, new Rectangle(100, 100, 400, 200));
        preferredFrameGeometry.put(ViewKind.BOOT_IMAGE, new Rectangle(75, 0, 350, 700));
        preferredFrameGeometry.put(ViewKind.BREAKPOINTS, new Rectangle(950, 0, 400, 150));
        preferredFrameGeometry.put(ViewKind.FRAME, new Rectangle(1175, 450, 275, 450));
        preferredFrameGeometry.put(ViewKind.JAVA_SOURCE, new Rectangle(100, 100, 500, 500));
        preferredFrameGeometry.put(ViewKind.METHODS, new Rectangle(275, 0, 800, 800));
        preferredFrameGeometry.put(ViewKind.NOTEPAD, new Rectangle(150, 150, 300, 300));
        preferredFrameGeometry.put(ViewKind.REGISTERS, new Rectangle(1175, 0, 275, 350));
        preferredFrameGeometry.put(ViewKind.STACK, new Rectangle(0, 200, 275, 600));
        preferredFrameGeometry.put(ViewKind.THREADS, new Rectangle(0, 0, 275, 200));
        preferredFrameGeometry.put(ViewKind.THREAD_LOCALS, new Rectangle(950, 150, 400, 600));
        preferredFrameGeometry.put(ViewKind.WATCHPOINTS, new Rectangle(100, 100, 450, 140));
    }

    public Point inspectorDefaultFrameLocation() {
        return inspectionFrameDefaultLocation;
    }
    public Dimension inspectorMinFrameSize() {
        return inspectionFrameMinSize;
    }
    public Dimension inspectorPrefFrameSize() {
        return inspectionFramePrefSize;
    }

    public Rectangle preferredFrameGeometry(ViewKind viewKind) {
        return preferredFrameGeometry.get(viewKind);
    }

    // Offset from mouse location for new frames
    public static final int newFrameDiagonalOffset = 5;
    public int newFrameDiagonalOffset() {
        return newFrameDiagonalOffset;
    }

}
