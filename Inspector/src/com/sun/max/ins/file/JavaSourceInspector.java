/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.ins.file;

import java.io.*;

import javax.swing.*;
import javax.swing.text.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.vm.actor.holder.*;

/**
 * Inspector for Java source code.
 *
 * @author Michael Van De Vanter
 *
 */
public final class JavaSourceInspector  extends FileInspector {

    private JTextArea textArea;

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
        refreshView(true);
        gui().moveToMiddle(this);
    }

    private JavaSourceInspector(Inspection inspection, File file) {
        super(inspection, file);
        final InspectorFrame frame = createFrame(true);
        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));
    }

    /**
     * Displays an inspector containing the source code for a Java class.
     */
    public static JavaSourceInspector make(Inspection inspection, ClassActor classActor, File sourceFile) {
        assert sourceFile != null;
        JavaSourceInspector javaSourceInspector = null;
        final UniqueInspector.Key<JavaSourceInspector> key = UniqueInspector.Key.create(JavaSourceInspector.class, sourceFile);
        javaSourceInspector = UniqueInspector.find(inspection, key);
        if (javaSourceInspector == null) {
            javaSourceInspector = new JavaSourceInspector(inspection, sourceFile);
        }
        return javaSourceInspector;
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

}
