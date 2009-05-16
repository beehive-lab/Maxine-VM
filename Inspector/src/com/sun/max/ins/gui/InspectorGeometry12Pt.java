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
 * Standard choices and policies for layout, tiled for use with 12 pt. font.
 *
 * @author Michael Van De Vanter
 */
public class InspectorGeometry12Pt implements InspectorGeometry {

    // Main Inspection frame
    private static final Point _inspectionFrameDefaultLocation = new Point(100, 100);
    private static final Dimension _inspectionFrameMinSize = new Dimension(100, 100);
    private static final Dimension _inspectionFramePrefSize = new Dimension(1615, 960);

    public Point inspectorFrameDefaultLocation() {
        return _inspectionFrameDefaultLocation;
    }
    public Dimension inspectorFrameMinSize() {
        return _inspectionFrameMinSize;
    }
    public Dimension inspectorFramePrefSize() {
        return _inspectionFramePrefSize;
    }

    public Rectangle threadsFrameDefaultBounds() {
        return new Rectangle(0, 0, 225, 170);
    }

    public Rectangle registersFrameDefaultBounds() {
        return new Rectangle(0, 170, 225, 730);
    }

    public Rectangle stackFrameDefaultBounds() {
        return new Rectangle(225, 0, 225, 900);
    }

    public Rectangle methodsFrameDefaultBounds() {
        return new Rectangle(450, 0, 700, 900);
    }

    public Rectangle breakpointsFrameDefaultBounds() {
        return new Rectangle(1150, 0, 450, 170);
    }

    public Rectangle threadLocalsFrameDefaultBounds() {
        return new Rectangle(1150, 170, 450, 730);
    }

    public Rectangle memoryRegionsFrameDefaultBounds() {
        return new Rectangle(100, 100, 450, 250);
    }

    public Rectangle bootImageFrameDefaultBounds() {
        return new Rectangle(100, 0, 390, 900);
    }

    // Java Source Inspector frame
    private static final Point _javaSourceFrameDefaultLocation = new Point(1270, 0);
    private static final Dimension _javaSourceFramePrefSize = new Dimension(605, 400);

    public Point javaSourceFrameDefaultLocation() {
        return _javaSourceFrameDefaultLocation;
    }
    public Dimension javaSourceFramePrefSize() {
        return _javaSourceFramePrefSize;
    }

    // Offset from mouse location for new frames
    public static final int _defaultNewFrameXOffset = 50;
    public static final int _defaultNewFrameYOffset = 50;
    public static final int _objectInspectorNewFrameDiagonalOffset = 5;
    public int defaultNewFrameXOffset() {
        return _defaultNewFrameXOffset;
    }
    public int defaultNewFrameYOffset() {
        return _defaultNewFrameYOffset;
    }
    public int objectInspectorNewFrameDiagonalOffset() {
        return _objectInspectorNewFrameDiagonalOffset;
    }

}
