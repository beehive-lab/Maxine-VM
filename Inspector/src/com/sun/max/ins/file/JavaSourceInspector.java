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
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;

/**
 * A very simple inspector for Java source code.
 *
 * @author Michael Van De Vanter
 */
public final class JavaSourceInspector extends FileInspector<JavaSourceInspector> {

    private static final int TRACE_VALUE = 2;
    private static final ViewKind VIEW_KIND = ViewKind.JAVA_SOURCE;
    private static final String SHORT_NAME = "Java Source";
    private static final String LONG_NAME = SHORT_NAME + " Inspector";

    private static JavaSourceViewManager viewManager;

    public static JavaSourceViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new JavaSourceViewManager(inspection);
        }
        return viewManager;
    }

    public static final class JavaSourceViewManager extends AbstractMultiViewManager<JavaSourceInspector> implements JavaSourceViewFactory {

        private static final Map<File, JavaSourceInspector> inspectors =
            new Hashtable<File, JavaSourceInspector>();

        protected JavaSourceViewManager(final Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
            Trace.begin(TRACE_VALUE, tracePrefix() + "creating");

            Trace.end(TRACE_VALUE, tracePrefix() + "creating");
        }

        @Override
        public void notifyViewClosing(Inspector inspector) {
            // TODO (mlvdv)  should be using generics here
            final JavaSourceInspector javaSourceInspector = (JavaSourceInspector) inspector;
            assert inspectors.remove(javaSourceInspector.file()) != null;
            super.notifyViewClosing(inspector);
        }

        public JavaSourceInspector makeView(ClassActor classActor, File sourceFile) {
            assert sourceFile != null;
            JavaSourceInspector inspector = inspectors.get(sourceFile);
            if (inspector == null) {
                inspector = new JavaSourceInspector(inspection(), sourceFile);
                inspectors.put(sourceFile, inspector);
            }
            return inspector;
        }
    }

    private JTextArea textArea;

    private JavaSourceInspector(Inspection inspection, File file) {
        super(inspection, file, VIEW_KIND);
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
        setContentPane(scrollPane);
        setGeometry(inspection().geometry().preferredFrameGeometry(ViewKind.JAVA_SOURCE));
    }

    @Override
    public void highlightLine(int lineNumber) {
        try {
            textArea.setCaretPosition(textArea.getLineStartOffset(lineNumber));
            textArea.moveCaretPosition(textArea.getLineEndOffset(lineNumber));
        } catch (BadLocationException badLocationException) {
        }
    }

    @Override
    public void inspectorClosing() {
        // Unsubscribe to view preferences, when we get them.
        super.inspectorClosing();
    }

    @Override
    protected void refreshState(boolean force) {
    }

}
