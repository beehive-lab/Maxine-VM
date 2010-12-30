/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins;

import javax.swing.*;

import com.sun.max.ins.gui.*;

/**
 * A rudimentary "about" message for the Inspector Help menu.
 *
 * @author Michael Van De Vanter
 */
public final class AboutDialog extends SimpleDialog {

    private static final String aboutString =
        "The Maxine Inspector is a combined debugger and object browser tool for Maxine,\n" +
        "a research virtual machine written in and for the Java(TM) Programming Language.\n" +
        "\n" +
        "Maxine and the Maxine Inspector are produced by the Maxine Open Source Project,\n" +
        "which is backed by the Maxine Research Project at Oracle Sun Labs:\n" +
        "\n" +
        "     Doug Simon (Principal Investigator)\n" +
        "     Laurent Daynes\n" +
        "     Mick Jordan\n" +
        "     Michael Van De Vanter\n" +
        "\n" +
        "For more information please visit:\n" +
        "     http://kenai.com/projects/maxine\n" +
        "     http://research.sun.com/projects/maxine/\n";

    private static final JTextArea textArea = new JTextArea(aboutString);

    public static AboutDialog create(Inspection inspection) {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setColumns(50);
        textArea.setRows(17);
        final JScrollPane scrollPane = new InspectorScrollPane(inspection, textArea);
        return new AboutDialog(inspection, scrollPane);
    }

    public AboutDialog(Inspection inspection, JScrollPane scrollPane) {
        super(inspection, scrollPane, "About Maxine", true);
    }
}
