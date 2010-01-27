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
package com.sun.max.ins.debug;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.option.*;

/**
 * Persistent preferences for table-based viewing of compiled stack frames in the VM.
 *
 * @author Michael Van De Vanter
  */
public final class CompiledStackFrameViewPreferences extends TableColumnVisibilityPreferences<CompiledStackFrameColumnKind> {

    private static CompiledStackFrameViewPreferences globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing a table describing a StackFrame.
     */
    static CompiledStackFrameViewPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new CompiledStackFrameViewPreferences(inspection);
        }
        return globalPreferences;
    }

    // Prefix for all persistent column preferences in view
    private static final String STACK_FRAME_COLUMN_PREFERENCE = "javaStackFrameViewColumn";

    // Prefix for all other preferences in view
    private static final String STACK_INSPECTOR_PREFERENCE = "javaStackInspectorPrefs";

    // Names of other preferences in view
    private static final String BIAS_SLOT_OFFSETS_PREFERENCE = "biasSlotOffsets";

    // Default value of offset biasing when no other information present.
    private static final boolean DEFAULT_BIAS_SLOT_OFFSETS_PREFERENCE = false;

    /**
     * @return a GUI panel suitable for setting global preferences for this kind of view.
     */
    public static JPanel globalPreferencesPanel(Inspection inspection) {
        return globalPreferences(inspection).getPanel();
    }

    private boolean biasSlotOffsets = DEFAULT_BIAS_SLOT_OFFSETS_PREFERENCE;

    /**
     * Creates a persistent, global set of preferences for view preferences.
     */
    private CompiledStackFrameViewPreferences(Inspection inspection) {
        super(inspection, STACK_FRAME_COLUMN_PREFERENCE, CompiledStackFrameColumnKind.VALUES);
        final InspectionSettings settings = inspection.settings();
        final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener(STACK_INSPECTOR_PREFERENCE) {
            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                saveSettingsEvent.save(BIAS_SLOT_OFFSETS_PREFERENCE, biasSlotOffsets);
            }
        };
        settings.addSaveSettingsListener(saveSettingsListener);

        biasSlotOffsets = settings.get(saveSettingsListener, BIAS_SLOT_OFFSETS_PREFERENCE, OptionTypes.BOOLEAN_TYPE, DEFAULT_BIAS_SLOT_OFFSETS_PREFERENCE);
    }

    public boolean biasSlotOffsets() {
        return biasSlotOffsets;
    }

    @Override
    public JPanel getPanel() {

        final JCheckBox biasCheckBox = new InspectorCheckBox(inspection(), "Bias Offsets", "Display stack slot offsets with platform-specific bias", biasSlotOffsets);
        biasCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBox checkBox = (JCheckBox) e.getSource();
                biasSlotOffsets = checkBox.isSelected();
            }
        });

        final JPanel panel2 = new InspectorPanel(inspection(), new BorderLayout());

        final JPanel stackPanel = new InspectorPanel(inspection());
        stackPanel.add(new TextLabel(inspection(), "Stack display options:  "));
        stackPanel.add(biasCheckBox);
        panel2.add(stackPanel, BorderLayout.WEST);

        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        panel.add(super.getPanel(), BorderLayout.NORTH);
        panel.add(panel2, BorderLayout.SOUTH);
        return panel;
    }

}
