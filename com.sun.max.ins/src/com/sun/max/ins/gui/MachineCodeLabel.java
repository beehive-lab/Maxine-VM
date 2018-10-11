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
 * A bold label for displaying machine code from the VM.
 */
public class MachineCodeLabel extends InspectorLabel {

    /**
     * Creates an opaque textual label suitable for a program element name, to
     * be displayed in bold.
     * @param inspection
     * @param text
     */
    public MachineCodeLabel(Inspection inspection, String text) {
        super(inspection, text);
        setOpaque(true);
        redisplay();
    }

    public final void refresh(boolean force) {
        // no remote data to refresh.
    }

    /**
     * Sets the displayed text of the label, displayed in bold, as well as tool tip text.
     *
     * @param text the text for the label
     * @param toolTipText tool tip text for the label
     */
    public void setValue(String text, String toolTipText) {
        setText(text);
        setWrappedToolTipHtmlText(htmlify(toolTipText));
    }

    public final void redisplay() {
        setFont(preference().style().defaultBoldFont());
    }

}
