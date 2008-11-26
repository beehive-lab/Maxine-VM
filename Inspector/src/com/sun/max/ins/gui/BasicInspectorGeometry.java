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
 * Standard choices and policies for layout.
 *
 * @author Michael Van De Vanter
 */
public class BasicInspectorGeometry implements InspectorGeometry {

    // Main Inspection frame
    private static final Point _inspectionFrameDefaultLocation = new Point(100, 100);
    private static final Dimension _inspectionFrameMinSize = new Dimension(100, 100);
    private static final Dimension _inspectionFramePrefSize = new Dimension(1625, 1040);

    public Point inspectorFrameDefaultLocation() {
        return _inspectionFrameDefaultLocation;
    }
    public Dimension inspectorFrameMinSize() {
        return _inspectionFrameMinSize;
    }
    public Dimension inspectorFramePrefSize() {
        return _inspectionFramePrefSize;
    }

    // Thread Inspection (container) frame
    private static final Point _threadsFrameDefaultLocation = new Point(0, 0);
    private static final Dimension _threadsFramePrefSize = new Dimension(225, 170);
    public Point threadsFrameDefaultLocation() {
        return _threadsFrameDefaultLocation;
    }
    public Dimension threadsFramePrefSize() {
        return _threadsFramePrefSize;
    }

    // Register Inspection (container) frame
    private static final Point _registersFrameDefaultLocation = new Point(0, 200);
    private static final Dimension _registersFramePrefSize = new Dimension(225, 800);
    public Point registersFrameDefaultLocation() {
        return _registersFrameDefaultLocation;
    }
    public Dimension registersFramePrefSize() {
        return _registersFramePrefSize;
    }

    // Stacks Inspection (container) frame
    private static final Point _stacksFrameDefaultLocation = new Point(235, 0);
    private static final Dimension _stacksFramePrefSize = new Dimension(250, 1000);
    public Point stacksFrameDefaultLocation() {
        return _stacksFrameDefaultLocation;
    }
    public Dimension stacksFramePrefSize() {
        return _stacksFramePrefSize;
    }

    // Target method Inspector (container) frame
    private static final Point _methodsFrameDefaultLocation = new Point(490, 0);
    private static final Dimension _methodsFramePrefSize = new Dimension(600, 1000);

    public Point methodsFrameDefaultLocation() {
        return _methodsFrameDefaultLocation;
    }
    public Dimension methodsFramePrefSize() {
        return _methodsFramePrefSize;
    }

    // Breakpoint Inspector frame
    private static final Point _breakpointsFrameDefaultLocation = new Point(1095, 0);
    private static final Dimension _breakpointsFramePrefSize = new Dimension(500, 250);

    public Point breakpointsFrameDefaultLocation() {
        return _breakpointsFrameDefaultLocation;
    }
    public Dimension breakpointsFramePrefSize() {
        return _breakpointsFramePrefSize;
    }

    // Thread Locals Inspection (container) frame
    private static final Point _threadLocalsFrameDefaultLocation = new Point(1095, 280);
    private static final Dimension _threadLocalsFramePrefSize = new Dimension(500, 750);
    public Point threadLocalsFrameDefaultLocation() {
        return _threadLocalsFrameDefaultLocation;
    }
    public Dimension threadLocalsFramePrefSize() {
        return _threadLocalsFramePrefSize;
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
