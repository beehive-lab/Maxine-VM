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
    private static final String OBJECT_INSPECTOR_PREFERENCE = "objectInspectorPrefs";

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
     * Creates global preferences for object inspectors.
     */
    private ObjectViewPreferences(Inspection inspection) {
        super(inspection, OBJECT_COLUMN_PREFERENCE, ObjectColumnKind.VALUES);
        final InspectionSettings settings = inspection.settings();
        final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener(OBJECT_INSPECTOR_PREFERENCE) {
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

        final JCheckBox showHeaderCheckBox =
            new InspectorCheckBox(inspection(), "Show Header", "Display object headers?", showHeader);
        showHeaderCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBox checkBox = (JCheckBox) e.getSource();
                setShowHeader(checkBox.isSelected());
            }
        });
        final JCheckBox hideNullArrayElementsCheckBox =
            new InspectorCheckBox(inspection(), "Hide null array elements", "Hide null elements in array displays?", hideNullArrayElements);
        hideNullArrayElementsCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBox checkBox = (JCheckBox) e.getSource();
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
