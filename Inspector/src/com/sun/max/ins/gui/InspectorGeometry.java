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
    Rectangle threadsDefaultFrameGeometry();

    /**
     * @return default geometry for the {@link RegistersInspector}.
     */
    Rectangle registersDefaultFrameGeometry();

    /**
     * @return default geometry for the {@link StackInspector}.
     */
    Rectangle stackDefaultFrameGeometry();

    /**
     * @return default geometry for the {@link StackFrameInspector}.
     */
    Rectangle stackFrameDefaultFrameGeometry();

    /**
     * @return default geometry for the {@link MethodInspector}.
     */
    Rectangle methodsDefaultFrameGeometry();

    /**
     * @return default geometry for the {@link BreakpointsInspector}.
     */
    Rectangle breakpointsDefaultFrameGeometry();

    /**
     * @return default geometry for the {@linkWatchpointsInspector}.
     */
    Rectangle watchpointsDefaultFrameGeometry();

    /**
     * @return default geometry for the {@link ThreadLocalsInspector}.
     */
    Rectangle threadLocalsDefaultFrameGeometry();

    /**
     * @return default geometry for the {@link MemoryRegionsInspector}.
     */
    Rectangle memoryRegionsDefaultFrameGeometry();

    /**
     * @return default geometry for the {@link BootImageInspector}.
     */
    Rectangle bootImageDefaultFrameGeometry();

    /**
     * @return default geometry for the {@link NotepadInspector}.
     */
    Rectangle notepadDefaultFrameGeometry();

    // Java Source Inspector frame
    Point javaSourceDefaultFrameLocation();
    Dimension javaSourcePrefFrameSize();

    // Offset from mouse location for new frames
    int newFrameDiagonalOffset();
}
