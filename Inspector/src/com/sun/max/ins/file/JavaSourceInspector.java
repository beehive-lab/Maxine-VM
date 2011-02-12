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
package com.sun.max.ins.file;

import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;

/**
 * A very simple inspector for Java source code.
 *
 * @author Michael Van De Vanter
 */
public final class JavaSourceInspector extends FileInspector {

    private static final Map<File, JavaSourceInspector> inspectors =
        new Hashtable<File, JavaSourceInspector>();

    /**
     * Displays an inspector containing the source code for a Java class.
     */
    public static JavaSourceInspector make(Inspection inspection, ClassActor classActor, File sourceFile) {
        assert sourceFile != null;
        JavaSourceInspector inspector = inspectors.get(sourceFile);
        if (inspector == null) {
            inspector = new JavaSourceInspector(inspection, sourceFile);
            inspectors.put(sourceFile, inspector);
        }
        return inspector;
    }

    private JTextArea textArea;

    private JavaSourceInspector(Inspection inspection, File file) {
        super(inspection, file);
        final InspectorFrame frame = createFrame(true);
        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));
    }

    @Override
    public String getTextForTitle() {
        return file().getName();
    }

    @Override
    public void createView() {
        textArea = new JTextArea(readFile());
        textArea.setEditable(false);
        textArea.setFont(style().javaNameFont());
        textArea.setCaretPosition(0);

        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), textArea);
        scrollPane.setPreferredSize(inspection().geometry().javaSourceFramePrefSize());
        //frame().setLocation(geometry().javaSourceFrameDefaultLocation());
        setContentPane(scrollPane);
        gui().moveToMiddle(this);
    }

    @Override
    public void highlightLine(int lineNumber) {
        try {
            textArea.setCaretPosition(textArea.getLineStartOffset(lineNumber));
            textArea.moveCaretPosition(textArea.getLineEndOffset(lineNumber));
        } catch (BadLocationException badLocationException) {
        }
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing for " + getTitle());
        inspectors.remove(file());
        super.inspectorClosing();
    }

    @Override
    protected void refreshState(boolean force) {
    }

}
