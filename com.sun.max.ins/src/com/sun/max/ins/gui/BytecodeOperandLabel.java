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
 *  A label for presenting a general, unchanging Bytecodes operand.
 */
public final class BytecodeOperandLabel extends InspectorLabel {

    private final String toolTipText;

    /**
     * Creates a label suitable for displaying a bytecode operand field.
     *
     * @param inspection
     * @param text the text to display in the label
     * @param toolTipText the text to display in the tooltip
     */
    public BytecodeOperandLabel(Inspection inspection, String text, String toolTipText) {
        super(inspection, text);
        this.toolTipText = toolTipText == null ? "" : toolTipText;
        redisplay();
    }

    public BytecodeOperandLabel(Inspection inspection, String text) {
        this(inspection, text, null);
    }

    public BytecodeOperandLabel(Inspection inspection, int n) {
        this(inspection, Integer.toString(n), intTo0xHex(n));
    }

    public void refresh(boolean force) {
        // no remote data to refresh
    }

    public void redisplay() {
        setWrappedToolTipHtmlText(toolTipText);
        setFont(preference().style().bytecodeOperandFont());
    }

}
