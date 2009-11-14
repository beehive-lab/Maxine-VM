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
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import com.sun.max.ins.*;

/**
 * A simple Inspector dialog that shows a single component and has a single "Close" button.
 * @author Michael Van De Vanter
 *
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
