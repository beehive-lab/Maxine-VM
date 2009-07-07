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
public class BytecodeViewerPreferences  extends TableColumnVisibilityPreferences<BytecodeColumnKind> {


    private static BytecodeViewerPreferences globalPreferences;

    public static BytecodeViewerPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new BytecodeViewerPreferences(inspection);
        }
        return globalPreferences;
    }

    private static final String OPERAND_DISPLAY_MODE_PREFERENCE = "operandDisplayMode";

    private PoolConstantLabel.Mode operandDisplayMode;

    public BytecodeViewerPreferences(Inspection inspection) {
        super(inspection, "bytecodeInspectorPrefs", BytecodeColumnKind.class, BytecodeColumnKind.VALUES);
        final OptionTypes.EnumType<PoolConstantLabel.Mode> optionType = new OptionTypes.EnumType<PoolConstantLabel.Mode>(PoolConstantLabel.Mode.class);
        operandDisplayMode = inspection.settings().get(saveSettingsListener, OPERAND_DISPLAY_MODE_PREFERENCE, optionType, PoolConstantLabel.Mode.JAVAP);
    }

    public BytecodeViewerPreferences(BytecodeViewerPreferences otherPreferences) {
        super(otherPreferences);
        operandDisplayMode = otherPreferences.operandDisplayMode;
    }

    @Override
    protected boolean canBeMadeInvisible(BytecodeColumnKind columnType) {
        return columnType.canBeMadeInvisible();
    }

    @Override
    protected boolean defaultVisibility(BytecodeColumnKind columnType) {
        return columnType.defaultVisibility();
    }

    @Override
    protected String label(BytecodeColumnKind columnType) {
        return columnType.label();
    }

    public PoolConstantLabel.Mode operandDisplayMode() {
        return operandDisplayMode;
    }

    public void setOperandDisplayMode(PoolConstantLabel.Mode mode) {
        final boolean needToSave = mode != operandDisplayMode;
        operandDisplayMode = mode;
        if (needToSave) {
            inspection().settings().save();
        }
    }

    @Override
    protected void saveSettings(SaveSettingsEvent saveSettingsEvent) {
        super.saveSettings(saveSettingsEvent);
        saveSettingsEvent.save(OPERAND_DISPLAY_MODE_PREFERENCE, operandDisplayMode.name());
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
        panel.add(operandStylePanel, BorderLayout.SOUTH);
        return panel;
    }

}
