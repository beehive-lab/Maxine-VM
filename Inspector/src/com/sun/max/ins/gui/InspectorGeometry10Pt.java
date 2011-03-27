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

/**
 * Standard choices and policies for inspection window layout, tiled for use with 10 pt. font.
 *
 * @author Michael Van De Vanter
 */
public class InspectorGeometry10Pt implements InspectorGeometry {

    // Main Inspection frame
    private static final Point inspectionFrameDefaultLocation = new Point(100, 100);
    private static final Dimension inspectionFrameMinSize = new Dimension(100, 100);
    private static final Dimension inspectionFramePrefSize = new Dimension(1365, 810);

    public Point inspectorDefaultFrameLocation() {
        return inspectionFrameDefaultLocation;
    }
    public Dimension inspectorMinFrameSize() {
        return inspectionFrameMinSize;
    }
    public Dimension inspectorPrefFrameSize() {
        return inspectionFramePrefSize;
    }

    public Rectangle threadsDefaultFrameGeometry() {
        return new Rectangle(0, 0, 175, 150);
    }

    public Rectangle registersDefaultFrameGeometry() {
        return new Rectangle(0, 150, 175, 600);
    }

    public Rectangle stackDefaultFrameGeometry() {
        return new Rectangle(175, 0, 175, 250);
    }

    public Rectangle stackFrameDefaultFrameGeometry() {
        return new Rectangle(175, 250, 175, 500);
    }

    public Rectangle methodsDefaultFrameGeometry() {
        return new Rectangle(350, 0, 600, 750);
    }

    public Rectangle breakpointsDefaultFrameGeometry() {
        return new Rectangle(950, 0, 400, 150);
    }

    public Rectangle watchpointsDefaultFrameGeometry() {
        return new Rectangle(100, 100, 450, 140);
    }

    public Rectangle threadLocalsDefaultFrameGeometry() {
        return new Rectangle(950, 150, 400, 600);
    }

    public Rectangle memoryRegionsDefaultFrameGeometry() {
        return new Rectangle(100, 100, 400, 200);
    }

    public Rectangle bootImageDefaultFrameGeometry() {
        return new Rectangle(75, 0, 350, 725);
    }

    public Rectangle notepadDefaultFrameGeometry() {
        return new Rectangle(150, 150, 200, 200);
    }

    // Java Source Inspector frame
    private static final Point javaSourceFrameDefaultLocation = new Point(100, 100);
    private static final Dimension javaSourceFramePrefSize = new Dimension(500, 500);

    public Point javaSourceDefaultFrameLocation() {
        return javaSourceFrameDefaultLocation;
    }
    public Dimension javaSourcePrefFrameSize() {
        return javaSourceFramePrefSize;
    }

    // Offset from mouse location for new frames
    public static final int newFrameDiagonalOffset = 5;
    public int newFrameDiagonalOffset() {
        return newFrameDiagonalOffset;
    }

}
