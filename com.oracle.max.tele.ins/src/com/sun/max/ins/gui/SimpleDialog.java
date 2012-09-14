/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import com.sun.max.ins.*;

/**
 * A simple Inspector dialog that shows a single component and has a single "Close" button.
 */
public class SimpleDialog extends InspectorDialog {

    static final Border border = BorderFactory.createLineBorder(Color.black);

    public SimpleDialog(final Inspection inspection, JComponent component, String frameTitle, boolean modal) {
        super(inspection, frameTitle, modal);

        final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());

        component.setBorder(border);

        final JPanel buttonsPanel = new InspectorPanel(inspection);
        buttonsPanel.add(new InspectorButton(inspection, new AbstractAction("Close") {
            public void actionPerformed(ActionEvent e) {
                inspection.settings().save();
                dispose();
            }
        }));
        dialogPanel.add(component, BorderLayout.CENTER);
        dialogPanel.add(buttonsPanel, BorderLayout.SOUTH);
        setContentPane(dialogPanel);
        pack();
        //inspection.gui().moveToMiddle(this);
        inspection.gui().setLocationRelativeToMouse(this, 5);
        setVisible(true);
    }
}
