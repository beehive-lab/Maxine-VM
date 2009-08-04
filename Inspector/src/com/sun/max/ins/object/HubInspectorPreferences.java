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
package com.sun.max.ins.object;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.option.*;


public final class HubInspectorPreferences {

    private static final String SHOW_FIELDS_PREFERENCE = "showFields";
    private static final String SHOW_VTABLES_PREFERENCE = "showVTables";
    private static final String SHOW_ITABLES_PREFERENCE = "showITables";
    private static final String SHOW_MTABLES_PREFERENCE = "showMTables";
    private static final String SHOW_REFERENCE_MAPS_PREFERENCE = "showReferenceMaps";

    private static HubInspectorPreferences globalPreferences;

    static HubInspectorPreferences globalHubPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new HubInspectorPreferences(inspection);
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

    HubInspectorPreferences(Inspection inspection) {
        this.inspection = inspection;
        final InspectionSettings settings = inspection.settings();
        final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener("hubInspectorPrefs") {
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
            new InspectorCheckBox(inspection, "Fields", "Should new Object Inspectors initially display the fields in a Hub?", showFields);
        final InspectorCheckBox alwaysShowVTablesCheckBox =
            new InspectorCheckBox(inspection, "vTables", "Should new Object Inspectors initially display the vTables in a Hub?", showVTables);
        final InspectorCheckBox alwaysShowITablesCheckBox =
            new InspectorCheckBox(inspection, "iTables", "Should new Object Inspectors initially display the iTables in a Hub?", showITables);
        final InspectorCheckBox alwaysShowMTablesCheckBox =
            new InspectorCheckBox(inspection, "mTables", "Should new Object Inspectors initially display the mTables in a Hub?", showMTables);
        final InspectorCheckBox alwaysShowRefMapsCheckBox =
            new InspectorCheckBox(inspection, "Reference Maps", "Should new Object Inspectors initially display the reference maps in a Hub?", showRefMaps);

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
        new HubInspectorPreferencesDialog(inspection);
    }

    private final class HubInspectorPreferencesDialog extends InspectorDialog {

        HubInspectorPreferencesDialog(Inspection inspection) {
            super(inspection, "Hub Inspector Preferences", false);

            final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());
            final JPanel buttons = new InspectorPanel(inspection);
            buttons.add(new JButton(new InspectorAction(inspection, "Close") {
                @Override
                protected void procedure() {
                    dispose();
                }
            }));

            dialogPanel.add(getPanel(), BorderLayout.NORTH);
            dialogPanel.add(buttons, BorderLayout.SOUTH);

            setContentPane(dialogPanel);
            pack();
            inspection.gui().moveToMiddle(this);
            setVisible(true);
        }
    }
}
