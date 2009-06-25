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
package com.sun.max.ins.object;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;


/**
 * A factory class that creates scrollable pane components, each of which displays a string representation of some value in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class StringPane extends InspectorScrollPane {

    public static StringPane createStringPane(ObjectInspector objectInspector, StringSource stringSource) {
        return new StringPane(objectInspector.inspection(), new JTextArea(), stringSource);
    }

    public static interface StringSource {
        String fetchString();
    }

    private final StringSource stringSource;
    private String stringValue;
    private final JTextArea textArea;

    private StringPane(Inspection inspection, JTextArea textArea, StringSource stringSource) {
        super(inspection, textArea);
        this.stringSource = stringSource;
        this.stringValue = stringSource.fetchString();
        this.textArea = textArea;
        this.textArea.append(stringValue);
        this.textArea.setEditable(false);
        refresh(true);
    }

    @Override
    public void redisplay() {
        textArea.setFont(style().defaultTextFont());
    }

    private MaxVMState lastRefreshedState = null;

    @Override
    public void refresh(boolean force) {
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            final String newString = stringSource.fetchString();
            if (newString != stringValue) {
                stringValue = newString;
                textArea.selectAll();
                textArea.replaceSelection(stringValue);
            }
        }
    }

}
