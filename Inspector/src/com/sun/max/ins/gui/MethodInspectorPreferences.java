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
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.method.*;
import com.sun.max.program.option.*;


public class MethodInspectorPreferences extends AbstractInspectionHolder {

    private static MethodInspectorPreferences globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing method code
     */
    public static MethodInspectorPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new MethodInspectorPreferences(inspection);
        }
        return globalPreferences;
    }

    /**
     * @return a GUI panel suitable for setting global preferences for this kind of view.
     */
    public static JPanel globalPreferencesPanel(Inspection inspection) {
        return globalPreferences(inspection).getPanel();
    }

    /**
     * A predicate specifying which kinds of code are to be displayed in a method inspector.
     */
    private final Map<MethodCodeKind, Boolean> visibleCodeKinds = new EnumMap<MethodCodeKind, Boolean>(MethodCodeKind.class);

    public MethodInspectorPreferences(Inspection inspection) {
        super(inspection);
        final InspectionSettings settings = inspection.settings();
        final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener("methodInspectorPrefs") {
            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                for (Map.Entry<MethodCodeKind, Boolean> entry : visibleCodeKinds.entrySet()) {
                    saveSettingsEvent.save(entry.getKey().name().toLowerCase(), entry.getValue());
                }
            }
        };
        settings.addSaveSettingsListener(saveSettingsListener);
        for (MethodCodeKind codeKind : MethodCodeKind.VALUES) {
            final boolean defaultVisibility = codeKind.defaultVisibility();
            final String name = codeKind.name().toLowerCase();
            final Boolean value = settings.get(saveSettingsListener, name, OptionTypes.BOOLEAN_TYPE, defaultVisibility);
            visibleCodeKinds.put(codeKind, value);
        }
    }

    /**
     * Determines if this preferences object indicates that a {@linkplain CodeViewer code viewer} should be created
     * for a given code kind.
     */
    public boolean isVisible(MethodCodeKind codeKind) {
        return visibleCodeKinds.get(codeKind);
    }

    /**
     * @return a GUI panel for setting these preferences
     */
    public JPanel getPanel() {
        final JCheckBox[] checkBoxes = new JCheckBox[MethodCodeKind.VALUES.length()];

        final ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final Object source = e.getItemSelectable();
                for (MethodCodeKind codeKind : MethodCodeKind.VALUES) {
                    final JCheckBox checkBox = checkBoxes[codeKind.ordinal()];
                    if (source == checkBox) {
                        visibleCodeKinds.put(codeKind, checkBox.isSelected());
                        inspection().settings().save();
                        break;
                    }
                }
            }
        };
        final JPanel content = new InspectorPanel(inspection());
        content.add(new TextLabel(inspection(), "View:  "));
        final String toolTipText = "Should new Method inspectors initially display this code, when available?";
        for (MethodCodeKind codeKind : MethodCodeKind.VALUES) {
            final boolean currentValue = visibleCodeKinds.get(codeKind);
            final JCheckBox checkBox =
                new InspectorCheckBox(inspection(), codeKind.toString(), toolTipText, currentValue);
            checkBox.addItemListener(itemListener);
            checkBoxes[codeKind.ordinal()] = checkBox;
            content.add(checkBox);
        }
        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        panel.add(content, BorderLayout.WEST);
        return panel;
    }

    public void showDialog() {
        new MethodCodeDisplayPreferencesDialog(inspection());
    }

    private final class MethodCodeDisplayPreferencesDialog extends InspectorDialog {
        MethodCodeDisplayPreferencesDialog(Inspection inspection) {
            super(inspection, "Method Code Display Preferences", false);

            final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());

            final JPanel prefPanel = new InspectorPanel(inspection, new SpringLayout());

            final Border border = BorderFactory.createLineBorder(Color.black);

            final JPanel prefslLabelPanel = new InspectorPanel(inspection, new BorderLayout());
            prefslLabelPanel.setBorder(border);
            prefslLabelPanel.add(new TextLabel(inspection, "Preferences"), BorderLayout.WEST);
            prefPanel.add(prefslLabelPanel);

            final JPanel keyBindingsPanel = getPanel();
            keyBindingsPanel.setBorder(border);
            prefPanel.add(keyBindingsPanel);

            SpringUtilities.makeCompactGrid(prefPanel, 2);

            final JPanel buttons = new InspectorPanel(inspection);
            buttons.add(new JButton(new InspectorAction(inspection, "Close") {
                @Override
                protected void procedure() {
                    dispose();
                }
            }));

            dialogPanel.add(prefPanel, BorderLayout.CENTER);
            dialogPanel.add(buttons, BorderLayout.SOUTH);
            setContentPane(dialogPanel);
            pack();
            inspection.gui().setLocationRelativeToMouse(this, 5);
            setVisible(true);
        }
    }
}
