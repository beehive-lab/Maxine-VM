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
