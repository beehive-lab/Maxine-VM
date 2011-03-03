/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * A set of layout parameters for testing GUI code.
 *
 * @author Michael Van De Vanter
 */
public class TestInspectorGeometry implements InspectorGeometry {

    // Main Inspector Frame
    private static final Point inspectionFrameDefaultLocation = new Point(100, 100);
    private static final Dimension inspectionFrameMinSize = new Dimension(100, 100);
    private static final Dimension inspectionFramePrefSize = new Dimension(1625, 1040);

    public Point inspectorFrameDefaultLocation() {
        return inspectionFrameDefaultLocation;
    }
    public Dimension inspectorFrameMinSize() {
        return inspectionFrameMinSize;
    }
    public Dimension inspectorFramePrefSize() {
        return inspectionFramePrefSize;
    }

    private static final Point testFrameDefaultLocation = new Point(100, 100);
    private static final Dimension testFramePrefSize = new Dimension(300, 300);
    private static final Rectangle testFrameDefaultBounds = new Rectangle(100, 100, 300, 300);

    public  Rectangle threadsFrameDefaultBounds() {
        return testFrameDefaultBounds;
    }

    public Rectangle registersFrameDefaultBounds() {
        return  testFrameDefaultBounds;
    }

    public Rectangle stackFrameDefaultBounds() {
        return testFrameDefaultBounds;
    }

    public Rectangle methodsFrameDefaultBounds() {
        return testFrameDefaultBounds;
    }

    public Rectangle breakpointsFrameDefaultBounds() {
        return testFrameDefaultBounds;
    }

    public Rectangle watchpointsFrameDefaultBounds() {
        return testFrameDefaultBounds;
    }

    public Rectangle threadLocalsFrameDefaultBounds() {
        return testFrameDefaultBounds;
    }

    public Rectangle memoryRegionsFrameDefaultBounds() {
        return testFrameDefaultBounds;
    }

    public Rectangle bootImageFrameDefaultBounds() {
        return testFrameDefaultBounds;
    }

    public Rectangle notepadFrameDefaultBounds() {
        return testFrameDefaultBounds;
    }

    // Java Source Inspector frame
    public Point javaSourceFrameDefaultLocation() {
        return testFrameDefaultLocation;
    }
    public Dimension javaSourceFramePrefSize() {
        return testFramePrefSize;
    }

    // Offset from mouse location for new frames
    public static final int defaultNewFrameXOffset = 50;
    public static final int defaultNewFrameYOffset = 50;
    public static final int objectInspectorNewFrameDiagonalOffset = 5;
    public int defaultNewFrameXOffset() {
        return defaultNewFrameXOffset;
    }
    public int defaultNewFrameYOffset() {
        return defaultNewFrameYOffset;
    }
    public int objectInspectorNewFrameDiagonalOffset() {
        return objectInspectorNewFrameDiagonalOffset;
    }

}
