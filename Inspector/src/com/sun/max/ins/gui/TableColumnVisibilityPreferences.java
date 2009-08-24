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

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;

/**
 * Encapsulates preferences about which columns should be visible in a table-based viewer.
 * There are three modes of use:
 * <ul>
 *  <li>1.  Global, persistent preferences.</li>
 *  <li>2.  Instance-based preferences, which default to a global preference, but which can be overridden
 *       with preferences that do not persist.</li>
 *  <li>3.  Singleton instance-based preferences, where overrides are treated as persistent global choices.</li>
 *  </ul>
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class TableColumnVisibilityPreferences<Column_Type extends Enum<Column_Type>> implements InspectionHolder {

    public interface TableColumnViewPreferenceListener {
        void tableColumnViewPreferencesChanged();
    }

    /**
     * A predicate specifying which columns are to be displayed in the table-based viewer.
     */
    private final Map<Column_Type, Boolean> visibleColumns;

    /**
     * If non-null, this preferences object should persist any changes via the inspection's {@linkplain InspectionSettings settings}.
     */
    protected final SaveSettingsListener saveSettingsListener;

    private final IdentityHashSet<TableColumnViewPreferenceListener> tableColumnViewPreferenceListeners =
        new IdentityHashSet<TableColumnViewPreferenceListener>();

    private final Inspection inspection;

    private final IndexedSequence<Column_Type> columnTypeValues;
    private final Class<Column_Type> columnTypeClass;

    /**
     * Mode 1:  Global, persistent preferences.
     *
     * Creates an object for storing the preferences about which columns should be visible in a table-based viewer.
     * The returned object persists itself via the inspection's {@linkplain InspectionSettings settings}. That is, this
     * constructor should be used to create a set of preferences that are applied as the default for all the
     * viewers of a specific type.
     *
     * @param inspection the inspection context
     * @param name the name under which these preferences should be persisted in the inspection's settings
     * @param columnTypeClass the {@link Enum} class defining the constants describing the columns in the table-based view
     * @param columnTypeValues the constants defined by {@code columnTypeClass}
     */
    protected TableColumnVisibilityPreferences(Inspection inspection,
                    String name,
                    Class<Column_Type> columnTypeClass,
                    IndexedSequence<Column_Type> columnTypeValues) {
        this.inspection = inspection;
        this.visibleColumns = new EnumMap<Column_Type, Boolean>(columnTypeClass);
        this.columnTypeValues = columnTypeValues;
        this.columnTypeClass = columnTypeClass;
        final InspectionSettings settings = inspection.settings();
        saveSettingsListener = new AbstractSaveSettingsListener(name) {
            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                TableColumnVisibilityPreferences.this.saveSettings(saveSettingsEvent);
            }
        };
        settings.addSaveSettingsListener(saveSettingsListener);

        for (Column_Type columnType : columnTypeValues) {
            final boolean defaultVisibility = defaultVisibility(columnType);
            final Boolean visible = settings.get(saveSettingsListener, columnType.name().toLowerCase(), OptionTypes.BOOLEAN_TYPE, defaultVisibility);
            visibleColumns.put(columnType, visible);
        }
    }

    /**
     * Creates an object for storing the preferences about which columns should be visible in a table-based viewer.
     * The returned object does not persist itself via the inspection's {@linkplain InspectionSettings settings}. That is, this
     * constructor should be used to create a set of preferences specific to an individual instance of a viewer.
     *
     * @param defaultPreferences the preferences from which the returned object should get its default values
     * @param singletonInstance when true, the instance preferences are shared with the global, so there are no per-instance variations.
     */
    public TableColumnVisibilityPreferences(TableColumnVisibilityPreferences<Column_Type> defaultPreferences, boolean singletonInstance) {
        inspection = defaultPreferences.inspection;
        saveSettingsListener = null;
        columnTypeClass = defaultPreferences.columnTypeClass;
        columnTypeValues = defaultPreferences.columnTypeValues;
        if (singletonInstance) {
            visibleColumns = defaultPreferences.visibleColumns;
        } else {
            visibleColumns = new EnumMap<Column_Type, Boolean>(defaultPreferences.visibleColumns);
        }
    }

    public TableColumnVisibilityPreferences(TableColumnVisibilityPreferences<Column_Type> defaultPreferences) {
        this (defaultPreferences, false);
    }

    public final Inspection inspection() {
        return inspection;
    }

    public final MaxVM maxVM() {
        return inspection.maxVM();
    }

    public final MaxVMState maxVMState() {
        return inspection.maxVM().maxVMState();
    }

    public InspectorGUI gui() {
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
    public void addListener(TableColumnViewPreferenceListener listener) {
        tableColumnViewPreferenceListeners.add(listener);
    }

    /**
     * Removes a listener for view update when a preference changes.
     */
    public void removeListener(TableColumnViewPreferenceListener listener) {
        tableColumnViewPreferenceListeners.remove(listener);
    }

    protected void saveSettings(SaveSettingsEvent saveSettingsEvent) {
        for (Map.Entry<Column_Type, Boolean> entry : visibleColumns.entrySet()) {
            saveSettingsEvent.save(entry.getKey().name().toLowerCase(), entry.getValue());
        }
    }

    /**
     * Determines if a given column type is visible by default (i.e. before the user has a chance to modify these
     * preferences).
     *
     * @param columnType denotes a column in a table-base viewer
     */
    protected abstract boolean defaultVisibility(Column_Type columnType);

    /**
     * Determines if a given column type is allowed to be invisible.
     *
     * @param columnType denotes a column in a table-base viewer
     */
    protected abstract boolean canBeMadeInvisible(Column_Type columnType);

    /**
     * Gets the label to be used for a given column type.
     *
     * @param columnType denotes a column in a table-base viewer
     */
    protected abstract String label(Column_Type columnType);

    /**
     * Updates this preferences object to indicate whether a given column type should be made visible;
     * notifies any change listeners.
     */
    protected void setIsVisible(Column_Type columnType, boolean flag) {
        visibleColumns.put(columnType, flag);
        if (saveSettingsListener != null) {
            inspection.settings().save();
        }
        for (TableColumnViewPreferenceListener listener : tableColumnViewPreferenceListeners) {
            listener.tableColumnViewPreferencesChanged();
        }
    }

    /**
     * Determines if this preferences object indicates that a given column type should be made visible.
     */
    public boolean isVisible(Column_Type columnType) {
        return visibleColumns.get(columnType);
    }

    /**
     * Gets a panel that has controls for specifying which columns should be visible.
     */
    public JPanel getPanel() {
        final JCheckBox[] checkBoxes = new JCheckBox[columnTypeValues.length()];

        final ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final Object source = e.getItemSelectable();
                for (Column_Type columnType : columnTypeValues) {
                    final JCheckBox checkBox = checkBoxes[columnType.ordinal()];
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
        for (Column_Type columnType : columnTypeValues) {
            if (canBeMadeInvisible(columnType)) {
                final JCheckBox checkBox =
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
    public static final class ColumnPreferencesDialog<Column_Type extends Enum<Column_Type>> extends InspectorDialog {

        /**
         * Create a dialog that allows the user to specify both global preferences as well as preferences for an individual viewer.
         * @param inspection
         * @param title
         * @param instancePreferences
         * @param globalPreferences
         */
        public ColumnPreferencesDialog(Inspection inspection,
                        String title,
                        TableColumnVisibilityPreferences<Column_Type> instancePreferences,
                        TableColumnVisibilityPreferences<Column_Type> globalPreferences) {
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
                        TableColumnVisibilityPreferences<Column_Type> instancePreferences) {
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
