/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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

    public Point inspectorFrameDefaultLocation() {
        return inspectionFrameDefaultLocation;
    }
    public Dimension inspectorFrameMinSize() {
        return inspectionFrameMinSize;
    }
    public Dimension inspectorFramePrefSize() {
        return inspectionFramePrefSize;
    }

    public Rectangle threadsFrameDefaultBounds() {
        return new Rectangle(0, 0, 175, 150);
    }

    public Rectangle registersFrameDefaultBounds() {
        return new Rectangle(0, 150, 175, 600);
    }

    public Rectangle stackFrameDefaultBounds() {
        return new Rectangle(175, 0, 175, 750);
    }

    public Rectangle methodsFrameDefaultBounds() {
        return new Rectangle(350, 0, 600, 750);
    }

    public Rectangle breakpointsFrameDefaultBounds() {
        return new Rectangle(950, 0, 400, 150);
    }

    public Rectangle watchpointsFrameDefaultBounds() {
        return new Rectangle(100, 100, 450, 140);
    }

    public Rectangle threadLocalsFrameDefaultBounds() {
        return new Rectangle(950, 150, 400, 600);
    }

    public Rectangle memoryRegionsFrameDefaultBounds() {
        return new Rectangle(100, 100, 400, 200);
    }

    public Rectangle bootImageFrameDefaultBounds() {
        return new Rectangle(75, 0, 350, 725);
    }

    // Java Source Inspector frame
    private static final Point javaSourceFrameDefaultLocation = new Point(100, 100);
    private static final Dimension javaSourceFramePrefSize = new Dimension(500, 500);

    public Point javaSourceFrameDefaultLocation() {
        return javaSourceFrameDefaultLocation;
    }
    public Dimension javaSourceFramePrefSize() {
        return javaSourceFramePrefSize;
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
