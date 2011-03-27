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

/**
 * Standard choices and policies for inspection window layout, tiled for use with 12 pt. font.
 *
 * @author Michael Van De Vanter
 */
public class InspectorGeometry12Pt implements InspectorGeometry {

    // Main Inspection frame
    private static final Point inspectionFrameDefaultLocation = new Point(100, 100);
    private static final Dimension inspectionFrameMinSize = new Dimension(100, 100);
    private static final Dimension inspectionFramePrefSize = new Dimension(1615, 960);

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
        return new Rectangle(0, 0, 225, 170);
    }

    public Rectangle registersDefaultFrameGeometry() {
        return new Rectangle(0, 170, 225, 730);
    }

    public Rectangle stackDefaultFrameGeometry() {
        return new Rectangle(225, 0, 225, 300);
    }

    public Rectangle stackFrameDefaultFrameGeometry() {
        return new Rectangle(225, 300, 225, 600);
    }

    public Rectangle methodsDefaultFrameGeometry() {
        return new Rectangle(450, 0, 700, 900);
    }

    public Rectangle breakpointsDefaultFrameGeometry() {
        return new Rectangle(1150, 0, 450, 170);
    }

    public Rectangle watchpointsDefaultFrameGeometry() {
        return new Rectangle(100, 100, 575, 150);
    }

    public Rectangle threadLocalsDefaultFrameGeometry() {
        return new Rectangle(1150, 170, 450, 730);
    }

    public Rectangle memoryRegionsDefaultFrameGeometry() {
        return new Rectangle(100, 100, 450, 250);
    }

    public Rectangle bootImageDefaultFrameGeometry() {
        return new Rectangle(100, 0, 390, 900);
    }

    public Rectangle notepadDefaultFrameGeometry() {
        return new Rectangle(200, 200, 200, 200);
    }

    // Java Source Inspector frame
    private static final Point javaSourceFrameDefaultLocation = new Point(1270, 0);
    private static final Dimension javaSourceFramePrefSize = new Dimension(605, 400);

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
