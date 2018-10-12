/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;

/**
 *
 */
public final class AboutSessionDialog extends InspectorDialog {

    private static final String INDENT = "    ";
    private static final int indent = INDENT.length();


    private static final Border border = BorderFactory.createLineBorder(Color.black);


    private final JScrollPane scrollPane;
    private final JTextArea textArea;
    private final JRadioButton verboseRadioButton;

    public AboutSessionDialog(final Inspection inspection) {
        super(inspection, MaxineInspector.NAME + " session information", true);


        this.textArea = new JTextArea(20, 60);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);


        this.verboseRadioButton = new JRadioButton("Verbose");
        verboseRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });

        final JPanel buttonsPanel = new InspectorPanel(inspection);
        buttonsPanel.add(verboseRadioButton);
        buttonsPanel.add(new InspectorButton(inspection, new AbstractAction("Close") {
            public void actionPerformed(ActionEvent e) {
                inspection.settings().save();
                dispose();
            }
        }));

        scrollPane = new InspectorScrollPane(inspection, textArea);
        scrollPane.setBorder(border);

        final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());
        dialogPanel.add(scrollPane, BorderLayout.CENTER);
        dialogPanel.add(buttonsPanel, BorderLayout.SOUTH);
        setContentPane(dialogPanel);

        refresh();
        pack();
        inspection.gui().setLocationRelativeToMouse(this, 5);
        setVisible(true);
    }

    /**
     * Replaces contents of the text area.
     */
    private void refresh() {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream stream = new PrintStream(byteArrayOutputStream);
        final long lastModified = vm().bootImageFile().lastModified();
        final Date bootImageDate = lastModified == 0 ? null : new Date(lastModified);
        final boolean verbose = verboseRadioButton.isSelected();
        final MaxVMState state = vm().state();
        if (verbose) {
            stream.print(MaxineInspector.NAME + ":\n");
            stream.print(INDENT + MaxineInspector.description() + "\n");
            stream.print(INDENT + "Inspection mode: " + vm().inspectionMode().name() + ",  " + vm().inspectionMode().description() + "\n");
            stream.println();
            stream.print(vm().entityName() + ":\n");
            stream.print(INDENT + vm().getDescription() + "\n");
            stream.print(INDENT + "Process state: ");
            if (state == null) {
                stream.print("none\n");
            } else {
                stream.print(state.processState().label() + ", " + state.processState().description() + "\n");
            }
            stream.print(INDENT + "Boot image: " + vm().bootImageFile().getAbsolutePath().toString() + "\n");
            stream.print(INDENT + "Last modified: " + bootImageDate.toString() + "\n");
            stream.print(INDENT + "See also: View->Boot image info\n");
        } else {
            stream.print(MaxineInspector.NAME + ": Ver. " + MaxineInspector.VERSION_STRING + " mode=" + vm().inspectionMode().name() + "\n");
            stream.println();
            stream.print(vm().entityName() + ": Ver. " + vm().getVersion() + ", state=" + (state == null ? "none" : state.processState().toString() + "\n"));
            stream.print(INDENT + vm().bootImageFile().getAbsolutePath().toString() + "\n");
            stream.print(INDENT + "built: " + bootImageDate.toString() + "\n");
        }

        stream.println();
        final MaxAddressSpace addressSpace = vm().addressSpace();
        stream.print(addressSpace.entityName().toUpperCase() + ":\n");
        addressSpace.printSessionStats(stream, indent, verbose);

        stream.println();
        final MaxClasses classRegistry = vm().classes();
        stream.print(classRegistry.entityName().toString().toUpperCase() + ":\n");
        classRegistry.printSessionStats(stream, indent, verbose);

        stream.println();
        final MaxObjects objects = vm().objects();
        stream.print(objects.entityName().toString().toUpperCase() + ":\n");
        objects.printSessionStats(stream, indent, verbose);

        stream.println();
        final MaxMachineCode machineCode = vm().machineCode();
        stream.print(machineCode.entityName().toString().toUpperCase() + ":\n");
        machineCode.printSessionStats(stream, indent, verbose);

        stream.println();
        final MaxHeap heap = vm().heap();
        stream.print(heap.entityName().toString().toUpperCase() + ":\n");
        heap.printSessionStats(stream, indent, verbose);

        stream.println();
        final MaxCodeCache codeCache = vm().codeCache();
        stream.print(codeCache.entityName().toString().toUpperCase() + ":\n");
        codeCache.printSessionStats(stream, indent, verbose);

        stream.println();
        stream.print("SESSION STARTUP OPTIONS: \n");
        inspection().options().printValues(stream, indent, verbose);

        textArea.setText(byteArrayOutputStream.toString());
        textArea.setCaretPosition(0);
    }

}
