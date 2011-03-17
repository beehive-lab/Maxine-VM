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
 * Preferences and policies for layout out inspection windows.
 *
 * @author Michael Van De Vanter
 */
public interface InspectorGeometry {

    // Main Inspector frame
    Point inspectorDefaultFrameLocation();
    Dimension inspectorMinFrameSize();
    Dimension inspectorPrefFrameSize();

    /**
     * @return default geometry for the {@link ThreadsInspector}.
     */
    Rectangle threadsDefaultFrameBounds();

    /**
     * @return default geometry for the {@link RegistersInspector}.
     */
    Rectangle registersDefaultFrameBounds();

    /**
     * @return default geometry for the {@link StackInspector}.
     */
    Rectangle stackDefaultFrameBounds();

    /**
     * @return default geometry for the {@link MethodInspector}.
     */
    Rectangle methodsDefaultFrameBounds();

    /**
     * @return default geometry for the {@link BreakpointsInspector}.
     */
    Rectangle breakpointsDefaultFrameBounds();

    /**
     * @return default geometry for the {@linkWatchpointsInspector}.
     */
    Rectangle watchpointsDefaultFrameBounds();

    /**
     * @return default geometry for the {@link ThreadLocalsInspector}.
     */
    Rectangle threadLocalsDefaultFrameBounds();

    /**
     * @return default geometry for the {@link MemoryRegionsInspector}.
     */
    Rectangle memoryRegionsDefaultFrameBounds();

    /**
     * @return default geometry for the {@link BootImageInspector}.
     */
    Rectangle bootImageDefaultFrameBounds();

    /**
     * @return default geometry for the {@link NotepadInspector}.
     */
    Rectangle notepadDefaultFrameBounds();

    // Java Source Inspector frame
    Point javaSourceDefaultFrameLocation();
    Dimension javaSourcePrefFrameSize();

    // Offset from mouse location for new frames
    int defaultNewFrameXOffset();
    int defaultNewFrameYOffset();
    int objectInspectorNewFrameDiagonalOffset();
}
