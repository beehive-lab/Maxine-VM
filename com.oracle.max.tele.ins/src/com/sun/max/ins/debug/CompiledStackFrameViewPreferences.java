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
    private static final String STACK_FRAME_COLUMN_PREFERENCE = "stackFrameViewColumn";

    // Prefix for all other preferences in view
    private static final String STACK_FRAME_VIEW_PREFERENCE = "stackFrameViewPrefs";

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
        super(inspection, STACK_FRAME_COLUMN_PREFERENCE, CompiledStackFrameColumnKind.values());
        final InspectionSettings settings = inspection.settings();
        final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener(STACK_FRAME_VIEW_PREFERENCE) {
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

        final InspectorCheckBox biasCheckBox = new InspectorCheckBox(inspection(), "Bias Offsets", "Display stack slot offsets with platform-specific bias", biasSlotOffsets);
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
