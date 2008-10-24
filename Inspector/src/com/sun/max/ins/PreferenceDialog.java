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
import javax.swing.border.*;

import com.sun.max.gui.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.*;
import com.sun.max.ins.object.*;


/**
 * A rudimentary Preferences dialog; leveraging {@link JPanel}s provided by various {@link Inspector}s.
 *
 * @author Michael Van De Vanter
 */
public class PreferenceDialog extends InspectorDialog {

    public PreferenceDialog(final Inspection inspection) {
        super(inspection, "Inspector Preferences", true);

        final JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BorderLayout());
        dialogPanel.setOpaque(true);
        dialogPanel.setBackground(style().defaultBackgroundColor());

        final JPanel prefPanel = new JPanel();
        prefPanel.setLayout(new SpringLayout());

        final Border border = BorderFactory.createLineBorder(Color.black);

        final JPanel generalLabelPanel = new JPanel(new BorderLayout());
        generalLabelPanel.setBorder(border);
        generalLabelPanel.add(new TextLabel(inspection, "General"), BorderLayout.WEST);
        prefPanel.add(generalLabelPanel);

        final JPanel keyBindingsPanel = inspection().preferences().getPanel();
        keyBindingsPanel.setBorder(border);
        prefPanel.add(keyBindingsPanel);

        final JPanel objectLabelPanel = new JPanel(new BorderLayout());
        objectLabelPanel.setBorder(border);
        objectLabelPanel.add(new TextLabel(inspection, "Objects"), BorderLayout.WEST);
        prefPanel.add(objectLabelPanel);

        final JPanel objectInspectorPanel = ObjectInspector.globalPreferences(inspection).getPanel();
        objectInspectorPanel.setBorder(border);
        prefPanel.add(objectInspectorPanel);

        final JPanel methodLabelPanel = new JPanel(new BorderLayout());
        methodLabelPanel.setBorder(border);
        methodLabelPanel.add(new TextLabel(inspection, "Methods"), BorderLayout.WEST);
        prefPanel.add(methodLabelPanel);

        final JPanel methodInspectorPanel = MethodInspector.globalPreferences(inspection).getPanel();
        methodInspectorPanel.setBorder(border);
        prefPanel.add(methodInspectorPanel);

        final JPanel targetCodeLabelPanel = new JPanel(new BorderLayout());
        targetCodeLabelPanel.setBorder(border);
        targetCodeLabelPanel.add(new TextLabel(inspection, "Target Code"), BorderLayout.WEST);
        prefPanel.add(targetCodeLabelPanel);

        final JPanel targetCodeInspectorPanel = JTableTargetCodeViewer.globalPreferences(inspection).getPanel();
        targetCodeInspectorPanel.setBorder(border);
        prefPanel.add(targetCodeInspectorPanel);

        final JPanel bytecodeLabelPanel = new JPanel(new BorderLayout());
        bytecodeLabelPanel.setBorder(border);
        bytecodeLabelPanel.add(new TextLabel(inspection, "Bytecode"), BorderLayout.WEST);
        prefPanel.add(bytecodeLabelPanel);

        final JPanel bytecodeInspectorPanel = JTableBytecodeViewer.globalPreferences(inspection).getPanel();
        bytecodeInspectorPanel.setBorder(border);
        prefPanel.add(bytecodeInspectorPanel);

        SpringUtilities.makeCompactGrid(prefPanel, 2);

        final JPanel buttonsPanel = new JPanel();
        buttonsPanel.setOpaque(true);
        buttonsPanel.setBackground(style().defaultBackgroundColor());
        buttonsPanel.add(new JButton(new AbstractAction("Close") {
            public void actionPerformed(ActionEvent e) {
                inspection.settings().save();
                dispose();
            }
        }));
        dialogPanel.add(prefPanel, BorderLayout.CENTER);
        dialogPanel.add(buttonsPanel, BorderLayout.SOUTH);
        setContentPane(dialogPanel);
        pack();
        inspection().moveToMiddle(this);
        setVisible(true);
    }

}
