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

import com.sun.max.ins.*;

/**
 * A text label specialized for use in the VM Inspector.
 *
 * @author Michael Van De Vanter
 */
public class PlainLabel extends InspectorLabel {

    /**
     * Creates a new text label displaying the value of an integer.
     */
    public PlainLabel(Inspection inspection, int n) {
        this(inspection, Integer.toString(n));
    }

    /**
     * Creates a new text label displaying specified text.
     */
    public PlainLabel(Inspection inspection, String text, String toolTipText) {
        super(inspection, text, toolTipText);
        redisplay();
    }

    /**
     * Creates a new text label displaying specified text.
     */
    public PlainLabel(Inspection inspection, String text) {
        super(inspection, text);
        redisplay();
    }

    public void setValue(String text, String toolTipText) {
        setText(text);
        setToolTipText(toolTipText);
    }

    public void setValue(String text) {
        setValue(text, null);
    }

    public void setValue(int n, String toolTipText) {
        setValue(Integer.toString(n), toolTipText);
    }

    public void setValue(int n) {
        setValue(n, null);
    }

    public void refresh(boolean force) {
        // No data to refresh in ordinary cases.
    }

    public void redisplay() {
        setFont(style().defaultFont());
    }

}
