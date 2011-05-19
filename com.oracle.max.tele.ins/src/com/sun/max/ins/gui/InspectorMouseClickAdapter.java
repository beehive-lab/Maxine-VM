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

import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.util.*;

/**
 * An implementation of {@link MouseListener} that only responds to single
 * mouse clicks, in which the abstract method {@link #procedure(MouseEvent)}
 * is called.
 *
 * @author Michael Van De Vanter
 */
public abstract class InspectorMouseClickAdapter implements MouseListener {

    private final Inspection inspection;

    /**
     * Creates a{@link MouseListener} that only responds to single
     * mouse clicks, in which the abstract method {@link #procedure(MouseEvent)}
     * is called.
     */
    protected InspectorMouseClickAdapter(Inspection inspection) {
        this.inspection = inspection;
    }

    public abstract void procedure(MouseEvent mouseEvent);

    public final void mouseClicked(MouseEvent mouseEvent) {
        try {
            procedure(mouseEvent);
        } catch (InspectorError inspectorError) {
            inspectorError.display(inspection);
        }
    }

    public final void mousePressed(MouseEvent mouseEvent) {
    }

    public final void mouseReleased(MouseEvent mouseEvent) {
    }

    public final void mouseEntered(MouseEvent mouseEvent) {
    }

    public final void mouseExited(MouseEvent mouseEvent) {
    }

}
