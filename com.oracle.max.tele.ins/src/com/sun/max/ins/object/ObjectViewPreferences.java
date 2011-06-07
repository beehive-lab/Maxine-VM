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

/**
 * Persistent preferences for viewing heap objects in the VM.
 *
 * @author Michael Van De Vanter
  */
public class ObjectViewPreferences extends TableColumnVisibilityPreferences<ObjectColumnKind> {

    private static ObjectViewPreferences globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing heap objects
     */
    static ObjectViewPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new ObjectViewPreferences(inspection);
        }
        return globalPreferences;
    }

    // Prefix for all persistent column preferences in view
    private static final String OBJECT_COLUMN_PREFERENCE = "objectViewColumn";

    // Prefix for all other preferences in view
    private static final String OBJECT_VIEW_PREFERENCE = "objectViewPrefs";

    // Names of other preferences in view
    private static final String OBJECT_SHOW_HEADER_PREFERENCE = "showHeader";
    private static final String OBJECT_HIDE_NULL_ARRAY_ELEMENTS_PREFERENCE = "hideNullArrayElements";

    /**
     * @return a GUI panel suitable for setting global preferences for this kind of view.
     */
    public static JPanel globalPreferencesPanel(Inspection inspection) {
        return globalPreferences(inspection).getPanel();
    }

    private boolean showHeader = true;
    private boolean hideNullArrayElements = false;

    /**
     * Creates global preferences for object views.
     */
    private ObjectViewPreferences(Inspection inspection) {
        super(inspection, OBJECT_COLUMN_PREFERENCE, ObjectColumnKind.values());
        final InspectionSettings settings = inspection.settings();
        final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener(OBJECT_VIEW_PREFERENCE) {
            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                saveSettingsEvent.save(OBJECT_SHOW_HEADER_PREFERENCE, showHeader);
                saveSettingsEvent.save(OBJECT_HIDE_NULL_ARRAY_ELEMENTS_PREFERENCE,  hideNullArrayElements);
            }
        };
        settings.addSaveSettingsListener(saveSettingsListener);
        showHeader = settings.get(saveSettingsListener, OBJECT_SHOW_HEADER_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);
        hideNullArrayElements = settings.get(saveSettingsListener, OBJECT_HIDE_NULL_ARRAY_ELEMENTS_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
    }

    /**
     * A per-instance set of view preferences, initialized to the global preferences.
     * @param defaultPreferences the global defaults for this kind of view
     */
    public ObjectViewPreferences(ObjectViewPreferences globalPreferences) {
        super(globalPreferences);
        showHeader = globalPreferences.showHeader;
        hideNullArrayElements  =  globalPreferences.hideNullArrayElements;
    }

    /**
     * @return whether the object's header field(s) should be displayed.
     */
    public boolean showHeader() {
        return showHeader;
    }

    /**
     * Sets preference to new value; subclass and override to receive notification.
     * @param showHeader should object headers be displayed
     */
    protected void setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
    }

    /**
     * @return whether null elements in arrays should be displayed.
     */
    public boolean hideNullArrayElements() {
        return hideNullArrayElements;
    }

    /**
     * Sets preference to new value; subclass and override to receive notification.
     * @param showHeader should object headers be displayed
     */
    protected void setHideNullArrayElements(boolean hideNullArrayElements) {
        this.hideNullArrayElements = hideNullArrayElements;
    }

    @Override
    public JPanel getPanel() {

        final InspectorCheckBox showHeaderCheckBox =
            new InspectorCheckBox(inspection(), "Show Header", "Display object headers?", showHeader);
        showHeaderCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final InspectorCheckBox checkBox = (InspectorCheckBox) e.getSource();
                setShowHeader(checkBox.isSelected());
            }
        });
        final InspectorCheckBox hideNullArrayElementsCheckBox =
            new InspectorCheckBox(inspection(), "Hide null array elements", "Hide null elements in array displays?", hideNullArrayElements);
        hideNullArrayElementsCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final InspectorCheckBox checkBox = (InspectorCheckBox) e.getSource();
                setHideNullArrayElements(checkBox.isSelected());
            }
        });

        final JPanel panel2 = new InspectorPanel(inspection(), new BorderLayout());

        final JPanel optionsPanel = new InspectorPanel(inspection());
        optionsPanel.add(new TextLabel(inspection(), "View Options:  "));
        optionsPanel.add(showHeaderCheckBox);
        optionsPanel.add(hideNullArrayElementsCheckBox);
        panel2.add(optionsPanel, BorderLayout.WEST);

        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        panel.add(super.getPanel(), BorderLayout.NORTH);
        panel.add(panel2, BorderLayout.SOUTH);
        return panel;
    }
}
