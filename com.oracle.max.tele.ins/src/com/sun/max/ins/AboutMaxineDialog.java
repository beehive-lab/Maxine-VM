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
package com.sun.max.ins;

import javax.swing.*;

import com.sun.max.ins.gui.*;
import com.sun.max.vm.*;

/**
 * A rudimentary message describing the Maxine project.
 *
 * @author Michael Van De Vanter
 */
public final class AboutMaxineDialog extends SimpleDialog {

    private static final String aboutString =
        MaxineInspector.NAME + " Ver. " + MaxineInspector.VERSION_STRING + "\n" +
        "A combined debugger and object browser tool for the " + MaxineVM.NAME + ", an Open Source\n" +
        "research virtual machine written in and for the Java(TM) Programming Language.\n" +
        "\n" +
        "The Maxine Project at Oracle Labs (<" + MaxineVM.HOME_URL + ">):\n" +
        "     Doug Simon, Principal Investigator\n" +
        "     Laurent Daynes\n" +
        "     Michael Haupt\n" +
        "     Mick Jordan\n" +
        "     Michael Van De Vanter\n" +
        "     Christian Wimmer\n" +
        "     Thomas Wuerthinger\n" +
        "\n";

    private static final JTextArea textArea = new JTextArea(aboutString);

    public static AboutMaxineDialog create(Inspection inspection) {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setColumns(50);
        textArea.setRows(17);
        final JScrollPane scrollPane = new InspectorScrollPane(inspection, textArea);
        return new AboutMaxineDialog(inspection, scrollPane);
    }

    public AboutMaxineDialog(Inspection inspection, JScrollPane scrollPane) {
        super(inspection, scrollPane, "Maxine project information", true);
    }
}
