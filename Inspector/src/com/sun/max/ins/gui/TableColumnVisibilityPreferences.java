/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import com.sun.cri.ci.*;
import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.debug.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;

/**
 * Encapsulates preferences about which columns should be visible in a table-based viewer.
 * There are three modes of use:
 * <ul>
 *  <li>1.  Global, persistent preferences.</li>
 *  <li>2.  Singleton instance-based preferences, where the set of persistent choices is the only set.</li>
 *  <li>3.  Instance-based preferences, which default to a global preference, but which can be overridden
 *       with preferences that do not persist.</li>
 *  </ul>
 *  <br>
 *  There are two ways to be notified of a change, which can be applied to either a global or per-instance set:
 *  <ol>
 *  <li>Register a {@link TableColumnViewPreferenceListener}, which reports that there has been some change.</li>
 *  <li>Override {@link #setIsVisible(ColumnKind, boolean)} and take measures, either before or after the super method has
 *  updated the internal state of the preferences.</li>
 *  </ol>
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class TableColumnVisibilityPreferences<ColumnKind_Type extends ColumnKind> implements InspectionHolder {

    public interface TableColumnViewPreferenceListener {
        void tableColumnViewPreferencesChanged();
    }

    private final Inspection inspection;
    private final String tracePrefix;

    /**
     * A predicate specifying which columns are to be displayed in the table-based viewer.
     */
    private final Map<ColumnKind_Type, Boolean> visibleColumns;

    /**
     * If non-null, this preferences object should persist any changes via the inspection's {@linkplain InspectionSettings settings}.
     */
    protected final SaveSettingsListener saveSettingsListener;

    private final Set<TableColumnViewPreferenceListener> tableColumnViewPreferenceListeners = CiUtil.newIdentityHashSet();

    private final ColumnKind_Type[] columnTypeValues;

    /**
     * Mode 1:  Global, persistent preferences.
     * Mode 2:  Singleton: Global and instance-preferences are identical.
     *
     * Creates an object for storing the preferences about which columns should be visible in a table-based viewer.
     * The returned object persists itself via the inspection's {@linkplain InspectionSettings settings}. That is, this
     * constructor should be used to create a set of preferences that are applied as the default for all the
     * viewers of a specific type.
     *
     * @param inspection the inspection context
     * @param name the name under which these preferences should be persisted in the inspection's settings
     * @param columnTypeValues the constants defined by {@code columnTypeClass}
     */
    protected TableColumnVisibilityPreferences(Inspection inspection,
                    String name,
                    ColumnKind_Type[] columnTypeValues) {
        this.inspection = inspection;
        this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
        this.visibleColumns = new HashMap<ColumnKind_Type, Boolean>();
        this.columnTypeValues = columnTypeValues;
        final InspectionSettings settings = inspection.settings();
        saveSettingsListener = new AbstractSaveSettingsListener(name) {
            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                TableColumnVisibilityPreferences.this.saveSettings(saveSettingsEvent);
            }
        };
        settings.addSaveSettingsListener(saveSettingsListener);

        for (ColumnKind_Type columnType : columnTypeValues) {
            final boolean defaultVisibility = defaultVisibility(columnType);
            final Boolean visible = settings.get(saveSettingsListener, getPreferenceName(columnType), OptionTypes.BOOLEAN_TYPE, defaultVisibility);
            visibleColumns.put(columnType, visible);
        }
    }

    /**
     * @return the textual name under which the visibility of the column type will be made persistent.
     */
    private String getPreferenceName(ColumnKind_Type columnType) {
        return columnType.name().toLowerCase();
    }

    /**
     * Mode 3: Instance-based preferences
     *
     * Creates an object for storing the preferences about which columns should be visible in a table-based viewer.
     * The returned object does not persist itself via the inspection's {@linkplain InspectionSettings settings}. That is, this
     * constructor should be used to create a set of preferences specific to an individual instance of a viewer.
     *
     * @param defaultPreferences the preferences from which the returned object should get its default values
     */
    public TableColumnVisibilityPreferences(TableColumnVisibilityPreferences<ColumnKind_Type> defaultPreferences) {
        inspection = defaultPreferences.inspection;
        this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
        saveSettingsListener = null;
        columnTypeValues = defaultPreferences.columnTypeValues;
        visibleColumns = new HashMap<ColumnKind_Type, Boolean>(defaultPreferences.visibleColumns);
    }

    public final Inspection inspection() {
        return inspection;
    }

    public final MaxVM vm() {
        return inspection.vm();
    }

    public final InspectorGUI gui() {
        return inspection.gui();
    }

    public final InspectorStyle style() {
        return inspection.style();
    }

    public final InspectionFocus focus() {
        return inspection.focus();
    }

    public final InspectionActions actions() {
        return inspection.actions();
    }

    /**
     * Adds a listener for view update when a preference changes.
     */
    public final void addListener(TableColumnViewPreferenceListener listener) {
        tableColumnViewPreferenceListeners.add(listener);
    }

    /**
     * Removes a listener for view update when a preference changes.
     */
    public final void removeListener(TableColumnViewPreferenceListener listener) {
        tableColumnViewPreferenceListeners.remove(listener);
    }

    protected void saveSettings(SaveSettingsEvent saveSettingsEvent) {
        for (Map.Entry<ColumnKind_Type, Boolean> entry : visibleColumns.entrySet()) {
            saveSettingsEvent.save(getPreferenceName(entry.getKey()), entry.getValue());
        }
    }

    /**
     * Determines if a given column type is visible by default (i.e. before the user has a chance to modify these
     * preferences).
     *
     * @param columnType denotes a column in a table-base viewer
     */
    protected boolean defaultVisibility(ColumnKind_Type columnType) {
        return columnType.defaultVisibility();
    }

    /**
     * Determines if a given column type is allowed to be invisible.
     *
     * @param columnType denotes a column in a table-base viewer
     */
    protected boolean canBeMadeInvisible(ColumnKind_Type columnType) {
        return columnType.canBeMadeInvisible();
    }

    /**
     * Gets the label to be used for a given column type.
     *
     * @param columnType denotes a column in a table-base viewer
     */
    protected String label(ColumnKind_Type columnType) {
        return columnType.label();
    }

    /**
     * Updates this preferences object to indicate whether a given column type should be made visible;
     * notifies any change listeners.
     */
    protected void setIsVisible(ColumnKind_Type columnType, boolean flag) {
        visibleColumns.put(columnType, flag);
        if (saveSettingsListener != null) {
            inspection.settings().save();
        }
        notifyChangeListeners();
    }

    /**
     * Announces to all registered listeners that column view preferences, or perhaps preferences managed by subclasses have changed.
     */
    private void notifyChangeListeners() {
        for (TableColumnViewPreferenceListener listener : tableColumnViewPreferenceListeners) {
            listener.tableColumnViewPreferencesChanged();
        }
    }

    /**
     * Determines if this preferences object indicates that a given column type should be made visible.
     */
    public boolean isVisible(ColumnKind_Type columnType) {
        return visibleColumns.get(columnType);
    }

    /**
     * Gets a panel that has controls for specifying which columns should be visible.
     */
    public JPanel getPanel() {
        final InspectorCheckBox[] checkBoxes = new InspectorCheckBox[columnTypeValues.length];

        final ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final Object source = e.getItemSelectable();
                for (ColumnKind_Type columnType : columnTypeValues) {
                    final InspectorCheckBox checkBox = checkBoxes[columnType.ordinal()];
                    if (source == checkBox) {
                        if (checkBox.isSelected() != isVisible(columnType)) {
                            setIsVisible(columnType, checkBox.isSelected());
                        }
                        return;
                    }
                }
            }
        };
        final JPanel content = new InspectorPanel(inspection);
        content.add(new TextLabel(inspection, "View Columns:  "));
        for (ColumnKind_Type columnType : columnTypeValues) {
            if (canBeMadeInvisible(columnType)) {
                final InspectorCheckBox checkBox =
                    new InspectorCheckBox(inspection, label(columnType), saveSettingsListener != null ? "Display column in all views?" : "Display column in this view?", isVisible(columnType));
                checkBox.addItemListener(itemListener);
                checkBoxes[columnType.ordinal()] = checkBox;
                content.add(checkBox);
            }
        }
        final JPanel panel = new InspectorPanel(inspection, new BorderLayout());
        panel.add(content, BorderLayout.WEST);
        return panel;
    }

    /**
     * A dialog that allows the user to specify preferences about column visibility.
     */
    public static final class ColumnPreferencesDialog<ColumnKind_Type extends ColumnKind> extends InspectorDialog {

        /**
         * Create a dialog that allows the user to specify both global preferences as well as preferences for an individual viewer.
         * @param inspection
         * @param title
         * @param instancePreferences
         * @param globalPreferences
         */
        public ColumnPreferencesDialog(Inspection inspection,
                        String title,
                        TableColumnVisibilityPreferences<ColumnKind_Type> instancePreferences,
                        TableColumnVisibilityPreferences<ColumnKind_Type> globalPreferences) {
            super(inspection, title, true);

            final JPanel contentPanel = new InspectorPanel(inspection, new BorderLayout());

            final JPanel prefPanel = new InspectorPanel(inspection, new SpringLayout());

            final Border border = BorderFactory.createLineBorder(Color.black);

            final JPanel thisViewLabelPanel = new InspectorPanel(inspection, new BorderLayout());
            thisViewLabelPanel.setBorder(border);
            thisViewLabelPanel.add(new TextLabel(inspection, "This View"), BorderLayout.WEST);
            prefPanel.add(thisViewLabelPanel);

            final JPanel viewOptionPanel = instancePreferences.getPanel();
            viewOptionPanel.setBorder(border);
            prefPanel.add(viewOptionPanel);

            final JPanel preferencesLabelPanel = new InspectorPanel(inspection, new BorderLayout());
            preferencesLabelPanel.setBorder(border);
            preferencesLabelPanel.add(new TextLabel(inspection, "Prefs"), BorderLayout.WEST);
            prefPanel.add(preferencesLabelPanel);

            final JPanel preferencesPanel = globalPreferences.getPanel();
            preferencesPanel.setBorder(border);
            prefPanel.add(preferencesPanel);

            SpringUtilities.makeCompactGrid(prefPanel, 2);

            final JPanel buttonsPanel = new InspectorPanel(inspection);
            buttonsPanel.add(new JButton(new InspectorAction(inspection, "Close") {
                @Override
                protected void procedure() {
                    dispose();
                }
            }));

            contentPanel.add(prefPanel, BorderLayout.CENTER);
            contentPanel.add(buttonsPanel, BorderLayout.SOUTH);

            setContentPane(contentPanel);
            pack();
            inspection.gui().setLocationRelativeToMouse(this, 5);
            setVisible(true);
        }

        /**
         * Create a dialog that allows the user to specify preferences for an individual viewer.
         * @param inspection
         * @param title
         * @param instancePreferences
         */
        public ColumnPreferencesDialog(Inspection inspection,
                        String title,
                        TableColumnVisibilityPreferences<ColumnKind_Type> instancePreferences) {
            super(inspection, title, true);

            final JPanel contentPanel = new InspectorPanel(inspection, new BorderLayout());

            final JPanel viewOptionPanel = instancePreferences.getPanel();
            viewOptionPanel.setBorder(BorderFactory.createLineBorder(Color.black));

            final JPanel buttonsPanel = new InspectorPanel(inspection);
            buttonsPanel.add(new JButton(new InspectorAction(inspection, "Close") {
                @Override
                protected void procedure() {
                    dispose();
                }
            }));

            contentPanel.add(viewOptionPanel, BorderLayout.CENTER);
            contentPanel.add(buttonsPanel, BorderLayout.SOUTH);

            setContentPane(contentPanel);
            pack();
            inspection.gui().setLocationRelativeToMouse(this, 5);
            setVisible(true);
        }
    }

}
