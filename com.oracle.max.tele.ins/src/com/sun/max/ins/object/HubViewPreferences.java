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
package com.sun.max.ins.object;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.option.*;

public final class HubViewPreferences {

    private static final String SHOW_FIELDS_PREFERENCE = "showFields";
    private static final String SHOW_VTABLES_PREFERENCE = "showVTables";
    private static final String SHOW_ITABLES_PREFERENCE = "showITables";
    private static final String SHOW_MTABLES_PREFERENCE = "showMTables";
    private static final String SHOW_REFERENCE_MAPS_PREFERENCE = "showReferenceMaps";

    private static HubViewPreferences globalPreferences;

    static HubViewPreferences globalHubPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new HubViewPreferences(inspection);
        }
        return globalPreferences;
    }

    /**
     * @return a GUI panel suitable for setting global preferences for this kind of view.
     */
    public static JPanel globalPreferencesPanel(Inspection inspection) {
        return globalHubPreferences(inspection).getPanel();
    }

    private final Inspection inspection;
    boolean showFields;
    boolean showVTables;
    boolean showITables;
    boolean showMTables;
    boolean showRefMaps;

    HubViewPreferences(Inspection inspection) {
        this.inspection = inspection;
        final InspectionSettings settings = inspection.settings();
        final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener("hubViewPrefs") {
            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                saveSettingsEvent.save(SHOW_FIELDS_PREFERENCE, showFields);
                saveSettingsEvent.save(SHOW_VTABLES_PREFERENCE, showVTables);
                saveSettingsEvent.save(SHOW_ITABLES_PREFERENCE, showITables);
                saveSettingsEvent.save(SHOW_MTABLES_PREFERENCE, showMTables);
                saveSettingsEvent.save(SHOW_REFERENCE_MAPS_PREFERENCE,  showRefMaps);
            }
        };
        settings.addSaveSettingsListener(saveSettingsListener);

        showFields = settings.get(saveSettingsListener, SHOW_FIELDS_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);
        showVTables = settings.get(saveSettingsListener, SHOW_VTABLES_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
        showITables = settings.get(saveSettingsListener, SHOW_ITABLES_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
        showMTables = settings.get(saveSettingsListener, SHOW_MTABLES_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
        showRefMaps = settings.get(saveSettingsListener, SHOW_REFERENCE_MAPS_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
    }

    /**
     * @return a GUI panel for setting preferences
     */
    private JPanel getPanel() {
        final InspectorCheckBox alwaysShowFieldsCheckBox =
            new InspectorCheckBox(inspection, "Fields", "Should new Object Views initially display the fields in a Hub?", showFields);
        final InspectorCheckBox alwaysShowVTablesCheckBox =
            new InspectorCheckBox(inspection, "vTables", "Should new Object Views initially display the vTables in a Hub?", showVTables);
        final InspectorCheckBox alwaysShowITablesCheckBox =
            new InspectorCheckBox(inspection, "iTables", "Should new Object Views initially display the iTables in a Hub?", showITables);
        final InspectorCheckBox alwaysShowMTablesCheckBox =
            new InspectorCheckBox(inspection, "mTables", "Should new Object Views initially display the mTables in a Hub?", showMTables);
        final InspectorCheckBox alwaysShowRefMapsCheckBox =
            new InspectorCheckBox(inspection, "Reference Maps", "Should new Object Views initially display the reference maps in a Hub?", showRefMaps);

        final ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final Object source = e.getItemSelectable();
                if (source == alwaysShowFieldsCheckBox) {
                    showFields = alwaysShowFieldsCheckBox.isSelected();
                } else if (source == alwaysShowVTablesCheckBox) {
                    showVTables = alwaysShowVTablesCheckBox.isSelected();
                } else if (source == alwaysShowITablesCheckBox) {
                    showITables = alwaysShowITablesCheckBox.isSelected();
                } else if (source == alwaysShowMTablesCheckBox) {
                    showMTables = alwaysShowMTablesCheckBox.isSelected();
                } else if (source == alwaysShowRefMapsCheckBox) {
                    showRefMaps = alwaysShowRefMapsCheckBox.isSelected();
                }
                inspection.settings().save();
            }
        };
        alwaysShowFieldsCheckBox.addItemListener(itemListener);
        alwaysShowVTablesCheckBox.addItemListener(itemListener);
        alwaysShowITablesCheckBox.addItemListener(itemListener);
        alwaysShowMTablesCheckBox.addItemListener(itemListener);
        alwaysShowRefMapsCheckBox.addItemListener(itemListener);

        final JPanel contentPanel = new InspectorPanel(inspection);
        contentPanel.add(new TextLabel(inspection, "View Options:  "));
        contentPanel.add(alwaysShowFieldsCheckBox);
        contentPanel.add(alwaysShowVTablesCheckBox);
        contentPanel.add(alwaysShowITablesCheckBox);
        contentPanel.add(alwaysShowMTablesCheckBox);
        contentPanel.add(alwaysShowRefMapsCheckBox);

        final JPanel panel = new InspectorPanel(inspection, new BorderLayout());
        panel.add(contentPanel, BorderLayout.WEST);

        return panel;
    }

    void showDialog() {
        new SimpleDialog(inspection, getPanel(), "Hub View Preferences", false);
    }
}
