/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * A very simple view for Java source code.
 */
public final class JavaSourceView extends FileView<JavaSourceView> {

    private static final int TRACE_VALUE = 2;
    private static final ViewKind VIEW_KIND = ViewKind.JAVA_SOURCE;
    private static final String SHORT_NAME = "Java Source";
    private static final String LONG_NAME = SHORT_NAME + " View";

    private static JavaSourceViewManager viewManager;

    public static JavaSourceViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new JavaSourceViewManager(inspection);
        }
        return viewManager;
    }

    public static final class JavaSourceViewManager extends AbstractMultiViewManager<JavaSourceView> implements JavaSourceViewFactory {

        private static final Map<File, JavaSourceView> views = new Hashtable<File, JavaSourceView>();

        protected JavaSourceViewManager(final Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
            Trace.begin(TRACE_VALUE, tracePrefix() + "creating");

            Trace.end(TRACE_VALUE, tracePrefix() + "creating");
        }

        public JavaSourceView makeView(ClassActor classActor, File sourceFile) {
            assert sourceFile != null;
            JavaSourceView view = views.get(sourceFile);
            if (view == null) {
                view = new JavaSourceView(inspection(), sourceFile);
                views.put(sourceFile, view);
                view.addViewEventListener(new ViewEventListener() {

                    @Override
                    public void viewClosing(InspectorView view) {
                        final JavaSourceView javaSourceView = (JavaSourceView) view;
                        assert views.remove(javaSourceView.file()) != null;
                    }
                });
            }
            return view;
        }
    }

    private JTextArea textArea;

    private JavaSourceView(Inspection inspection, File file) {
        super(inspection, file, VIEW_KIND);
        createFrame(true);
    }

    @Override
    public String getTextForTitle() {
        return file().getName();
    }

    @Override
    public void createViewContent() {
        textArea = new JTextArea(readFile());
        textArea.setEditable(false);
        textArea.setFont(preference().style().javaNameFont());
        textArea.setCaretPosition(0);

        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), textArea);
        setContentPane(scrollPane);
        setGeometry(preference().geometry().preferredFrameGeometry(ViewKind.JAVA_SOURCE));

        // Populate menu bar
        makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));
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
    public void viewClosing() {
        // Unsubscribe to view preferences, when we get them.
        super.viewClosing();
    }

    @Override
    protected void refreshState(boolean force) {
    }

}
