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
package com.sun.max.ins.method;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.constant.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.option.*;

/**
 * Persistent preferences for viewing disassembled bytecodes in the VM.
 *
 * @author Michael Van De Vanter
  */
public class BytecodeViewPreferences  extends TableColumnVisibilityPreferences<BytecodeColumnKind> {

    private static BytecodeViewPreferences globalPreferences;

    public static BytecodeViewPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new BytecodeViewPreferences(inspection);
        }
        return globalPreferences;
    }

    // Prefix for all persistent column preferences in view
    private static final String BYTECODE_COLUMN_PREFERENCE = "bytecodeViewColumn";

    // Prefix for all other preferences in view
    private static final String BYTECODE_VIEWER_PREFERENCE = "bytecodeViewerPrefs";

    // Names of other preferences in view
    private static final String BYTECODE_OPERAND_DISPLAY_MODE = "operandDisplayMode";

    private PoolConstantLabel.Mode operandDisplayMode;

    /**
     * Creates the global, persistent set of preferences, initializing from stored values if available.
     */
    private BytecodeViewPreferences(Inspection inspection) {
        super(inspection, BYTECODE_COLUMN_PREFERENCE, BytecodeColumnKind.values());
        final InspectionSettings settings = inspection.settings();
        final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener(BYTECODE_VIEWER_PREFERENCE) {
            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                saveSettingsEvent.save(BYTECODE_OPERAND_DISPLAY_MODE, operandDisplayMode.name());
            }
        };
        settings.addSaveSettingsListener(saveSettingsListener);
        final OptionTypes.EnumType<PoolConstantLabel.Mode> optionType = new OptionTypes.EnumType<PoolConstantLabel.Mode>(PoolConstantLabel.Mode.class);
        operandDisplayMode = inspection.settings().get(saveSettingsListener, BYTECODE_OPERAND_DISPLAY_MODE, optionType, PoolConstantLabel.Mode.JAVAP);
    }

    /**
     * Creates a non-persistent set of preferences by cloning another set of preferences (i.e. the globally persistent set).
     */
    public BytecodeViewPreferences(BytecodeViewPreferences otherPreferences) {
        super(otherPreferences);
        operandDisplayMode = otherPreferences.operandDisplayMode;
    }

    public PoolConstantLabel.Mode operandDisplayMode() {
        return operandDisplayMode;
    }

    protected void setOperandDisplayMode(PoolConstantLabel.Mode mode) {
        final boolean needToSave = mode != operandDisplayMode;
        operandDisplayMode = mode;
        if (needToSave) {
            inspection().settings().save();
        }
    }

    @Override
    public JPanel getPanel() {
        final JRadioButton javapButton = new InspectorRadioButton(inspection(), "javap style", "Display bytecode operands in a style similar to the 'javap' tool and the JVM spec book");
        final JRadioButton terseButton = new InspectorRadioButton(inspection(), "terse style", "Display bytecode operands in a terse style");
        final ButtonGroup group = new ButtonGroup();
        group.add(javapButton);
        group.add(terseButton);

        javapButton.setSelected(operandDisplayMode == PoolConstantLabel.Mode.JAVAP);
        terseButton.setSelected(operandDisplayMode == PoolConstantLabel.Mode.TERSE);

        final ActionListener styleActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (javapButton.isSelected()) {
                    setOperandDisplayMode(PoolConstantLabel.Mode.JAVAP);
                } else if (terseButton.isSelected()) {
                    setOperandDisplayMode(PoolConstantLabel.Mode.TERSE);
                }
            }
        };
        javapButton.addActionListener(styleActionListener);
        terseButton.addActionListener(styleActionListener);

        final JPanel panel2 = new InspectorPanel(inspection(), new BorderLayout());

        final JPanel operandStylePanel = new InspectorPanel(inspection());
        operandStylePanel.add(new TextLabel(inspection(), "Operand Style:  "));
        operandStylePanel.add(javapButton);
        operandStylePanel.add(terseButton);
        panel2.add(operandStylePanel, BorderLayout.WEST);

        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        panel.add(super.getPanel(), BorderLayout.NORTH);
        panel.add(panel2, BorderLayout.SOUTH);
        return panel;
    }

}
