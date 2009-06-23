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
package com.sun.max.ins;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.gui.*;

/**
 * A rudimentary "about" message for the Inspector Help menu.
 *
 * @author Michael Van De Vanter
 */
public final class AboutDialog extends InspectorDialog {

    private static final String aboutString =
        "The Maxine Inspector is a combined debugger and object browser tool for Maxine,\n" +
        "a research virtual machine written in and for the Java(TM) Programming Language.\n" +
        "\n" +
        "Maxine and the Maxine Inspector are produced by the Maxine Open Source Project,\n" +
        "which is backed by the Maxine Research Project at Sun Labs,\n" +
        "the research organization of Sun Microsystems, Inc.:\n" +
        "\n" +
        "     Doug Simon (Principal Investigator)\n" +
        "     Laurent Daynes\n" +
        "     Ben Titzer\n" +
        "     Michael Van De Vanter\n" +
        "\n" +
        "For more information please visit:\n" +
        "     http://kenai.com/projects/maxine\n" +
        "     http://research.sun.com/projects/maxine/\n";


    private static final JTextArea textArea = new JTextArea(aboutString);

    public AboutDialog(Inspection inspection) {
        super(inspection, "About Maxine", true);

        final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setColumns(50);
        textArea.setRows(17);
        final JScrollPane scrollPane = new InspectorScrollPane(inspection, textArea);
        dialogPanel.add(scrollPane, BorderLayout.CENTER);
        dialogPanel.add(new JButton(new AbstractAction("Close") {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }), BorderLayout.SOUTH);
        setContentPane(dialogPanel);
        pack();
        inspection.gui().moveToMiddle(this);
        setVisible(true);
    }
}
