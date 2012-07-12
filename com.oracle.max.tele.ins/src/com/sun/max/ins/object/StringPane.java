/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.object;

import javax.swing.*;

import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;

/**
 * A factory class that creates scrollable pane components, each of which displays a string representation of some value in the VM.
 */
public final class StringPane extends InspectorScrollPane {

    public static StringPane createStringPane(ObjectView objectView, StringSource stringSource) {
        return new StringPane(objectView, new JTextArea(), stringSource);
    }

    public interface StringSource {
        String fetchString();
    }

    private final ObjectView objectView;
    private final StringSource stringSource;
    private final JTextArea textArea;

    private StringPane(ObjectView objectView, JTextArea textArea, StringSource stringSource) {
        super(objectView.inspection(), textArea);
        this.objectView = objectView;
        this.stringSource = stringSource;
        this.textArea = textArea;
        this.textArea.append(stringSource.fetchString());
        this.textArea.setEditable(false);
        refresh(true);
    }

    @Override
    public void redisplay() {
        textArea.setFont(preference().style().defaultFont());
    }

    private MaxVMState lastRefreshedState = null;

    @Override
    public void refresh(boolean force) {
        super.refresh(force);
        if (vm().state().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = vm().state();
            setBackground(objectView.viewBackgroundColor());
            textArea.setBackground(objectView.viewBackgroundColor());
            textArea.selectAll();
            String newText;
            try {
                newText = stringSource.fetchString();
            } catch (Throwable e) {
                newText = inspection().nameDisplay().unavailableDataLongText();
            }
            textArea.replaceSelection(newText);
        }
    }

}
