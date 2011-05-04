/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.ins.java;

import com.sun.cri.ci.*;
import com.sun.max.ins.*;
import com.sun.max.ins.view.*;
import com.sun.max.tele.*;


/**
 * Methods for creating views on bytecode frames.
 *
 * @author Michael Van De Vanter
 */
public interface BytecodeFramesViewFactory extends InspectionViewFactory<BytecodeFramesInspector> {

    /**
     * Creates a view on a target Java frame descriptor.
     *
     * @param bytecodeFrames the java bytecode frames at a given location
     * @param compiledCode
     * @return
     */
    BytecodeFramesInspector makeView(CiFrame bytecodeFrames, MaxCompiledCode compiledCode);

    /**
     * Gets an action that makes view of the bytecode frames available at
     * the currently selected code location; disabled if there are no
     * frames available at the currently selected code location.
     *
     * @return an action that creates a bytecode frame view
     */
    InspectorAction makeViewAction();
}
