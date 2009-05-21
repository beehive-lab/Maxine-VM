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


public class ObjectInspectorPreferences extends AbstractInspectionHolder {

    private static ObjectInspectorPreferences _globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing heap objects
     */
    public static ObjectInspectorPreferences globalPreferences(Inspection inspection) {
        if (_globalPreferences == null) {
            _globalPreferences = new ObjectInspectorPreferences(inspection);
        }
        return _globalPreferences;
    }

    private static final String SHOW_HEADER_PREFERENCE = "showHeader";
    private static final String SHOW_ADDRESSES_PREFERENCE = "showAddresses";
    private static final String SHOW_OFFSETS_PREFERENCE = "showOffsets";
    private static final String SHOW_FIELD_TYPES_PREFERENCE = "showFieldTypes";
    private static final String SHOW_MEMORY_REGIONS_PREFERENCE = "showMemoryRegions";
    private static final String HIDE_NULL_ARRAY_ELEMENTS_PREFERENCE = "hideNullArrayElements";

    private boolean _showHeader;

    public boolean showHeader() {
        return _showHeader;
    }

    private boolean _showAddresses;

    public boolean showAddresses() {
        return _showAddresses;
    }

    private boolean _showOffsets;

    public boolean showOffsets() {
        return _showOffsets;
    }

    private boolean _showFieldTypes;

    public boolean showFieldTypes() {
        return _showFieldTypes;
    }

    private boolean _showMemoryRegions;

    public boolean showMemoryRegions() {
        return _showMemoryRegions;
    }

    private boolean _hideNullArrayElements;

    public boolean hideNullArrayElements() {
        return _hideNullArrayElements;
    }

    public ObjectInspectorPreferences(Inspection inspection) {
        super(inspection);
        final InspectionSettings settings = inspection.settings();
        final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener("objectInspectorPrefs") {
            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                saveSettingsEvent.save(SHOW_HEADER_PREFERENCE, _showHeader);
                saveSettingsEvent.save(SHOW_ADDRESSES_PREFERENCE, _showAddresses);
                saveSettingsEvent.save(SHOW_OFFSETS_PREFERENCE, _showOffsets);
                saveSettingsEvent.save(SHOW_FIELD_TYPES_PREFERENCE, _showFieldTypes);
                saveSettingsEvent.save(SHOW_MEMORY_REGIONS_PREFERENCE,  _showMemoryRegions);
                saveSettingsEvent.save(HIDE_NULL_ARRAY_ELEMENTS_PREFERENCE,  _hideNullArrayElements);
            }
        };
        settings.addSaveSettingsListener(saveSettingsListener);

        _showHeader = settings.get(saveSettingsListener, SHOW_HEADER_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);
        _showAddresses = settings.get(saveSettingsListener, SHOW_ADDRESSES_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
        _showOffsets = settings.get(saveSettingsListener, SHOW_OFFSETS_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);
        _showFieldTypes = settings.get(saveSettingsListener, SHOW_FIELD_TYPES_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);
        _showMemoryRegions = settings.get(saveSettingsListener, SHOW_MEMORY_REGIONS_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
        _hideNullArrayElements = settings.get(saveSettingsListener, HIDE_NULL_ARRAY_ELEMENTS_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
    }

    /**
     * @return a GUI panel for setting preferences
     */
    public JPanel getPanel() {
        final JCheckBox alwaysShowHeaderCheckBox =
            new InspectorCheckBox(inspection(), "Header", "Should new Object Inspectors initially display the header?", _showHeader);
        final JCheckBox alwaysShowAddressesCheckBox =
            new InspectorCheckBox(inspection(), "Addresses", "Should new Object Inspectors initially display addresses?", _showAddresses);
        final JCheckBox alwaysShowOffsetsCheckBox =
            new InspectorCheckBox(inspection(), "Offsets", "Should new Object Inspectors initially display offsets?", _showOffsets);
        final JCheckBox alwaysShowTupleTypeCheckBox =
            new InspectorCheckBox(inspection(), "Type", "Should new Object Inspectors initially display types?", _showFieldTypes);
        final JCheckBox alwaysShowMemoryRegionCheckBox =
            new InspectorCheckBox(inspection(), "Region", "Should new Object Inspectors initially display memory regions?", _showMemoryRegions);
        final JCheckBox hideNullArrayElementsCheckBox =
            new InspectorCheckBox(inspection(), "Hide null array elements", "Should new Object Inspectors initially hide null elements in arrays?", _hideNullArrayElements);

        final ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final Object source = e.getItemSelectable();
                if (source == alwaysShowHeaderCheckBox) {
                    _showHeader = alwaysShowHeaderCheckBox.isSelected();
                } else if (source == alwaysShowAddressesCheckBox) {
                    _showAddresses = alwaysShowAddressesCheckBox.isSelected();
                } else if (source == alwaysShowOffsetsCheckBox) {
                    _showOffsets = alwaysShowOffsetsCheckBox.isSelected();
                } else if (source == alwaysShowTupleTypeCheckBox) {
                    _showFieldTypes = alwaysShowTupleTypeCheckBox.isSelected();
                } else if (source == alwaysShowMemoryRegionCheckBox) {
                    _showMemoryRegions = alwaysShowMemoryRegionCheckBox.isSelected();
                } else if (source == hideNullArrayElementsCheckBox) {
                    _hideNullArrayElements = hideNullArrayElementsCheckBox.isSelected();
                }
                inspection().settings().save();
            }
        };
        alwaysShowHeaderCheckBox.addItemListener(itemListener);
        alwaysShowAddressesCheckBox.addItemListener(itemListener);
        alwaysShowOffsetsCheckBox.addItemListener(itemListener);
        alwaysShowTupleTypeCheckBox.addItemListener(itemListener);
        alwaysShowMemoryRegionCheckBox.addItemListener(itemListener);
        hideNullArrayElementsCheckBox.addItemListener(itemListener);

        final JPanel upperContentPanel = new InspectorPanel(inspection());
        upperContentPanel.add(new TextLabel(inspection(), "View:  "));
        upperContentPanel.add(alwaysShowHeaderCheckBox);
        upperContentPanel.add(alwaysShowAddressesCheckBox);
        upperContentPanel.add(alwaysShowOffsetsCheckBox);
        upperContentPanel.add(alwaysShowTupleTypeCheckBox);
        upperContentPanel.add(alwaysShowMemoryRegionCheckBox);

        final JPanel upperPanel = new InspectorPanel(inspection(), new BorderLayout());
        upperPanel.add(upperContentPanel, BorderLayout.WEST);

        final JPanel lowerContentPanel = new InspectorPanel(inspection());
        lowerContentPanel.add(new TextLabel(inspection(), "Options:  "));
        lowerContentPanel.add(hideNullArrayElementsCheckBox);

        final JPanel lowerPanel = new InspectorPanel(inspection(), new BorderLayout());
        lowerPanel.add(lowerContentPanel, BorderLayout.WEST);

        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        panel.add(upperPanel, BorderLayout.NORTH);
        panel.add(lowerPanel, BorderLayout.SOUTH);

        return panel;
    }

}
