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
 * Preferences and policies for layout out inspector windows.
 *
 * @author Michael Van De Vanter
 */
public interface InspectorGeometry {

    // Main Inspector frame
    Point inspectorFrameDefaultLocation();
    Dimension inspectorFrameMinSize();
    Dimension inspectorFramePrefSize();

    // Threads Inspection frame
    Point threadsFrameDefaultLocation();
    Dimension threadsFramePrefSize();

    // Registers Inspection frame
    Point registersFrameDefaultLocation();
    Dimension registersFramePrefSize();

    // Stacks Inspection frame
    Point stackFrameDefaultLocation();
    Dimension stackFramePrefSize();

    // Thread locals Inspection frame
    Dimension threadLocalsFramePrefSize();
    Point threadLocalsFrameDefaultLocation();

    // Methods Inspection (container) frame
    Point methodsFrameDefaultLocation();
    Dimension methodsFramePrefSize();

    // Breakpoints Inspector frame
    Point breakpointsFrameDefaultLocation();
    Dimension breakpointsFramePrefSize();

    // Java Source Inspector frame
    Point javaSourceFrameDefaultLocation();
    Dimension javaSourceFramePrefSize();

    // Offset from mouse location for new frames
    int defaultNewFrameXOffset();
    int defaultNewFrameYOffset();
    int objectInspectorNewFrameDiagonalOffset();
}
