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
package com.sun.max.ins.memory;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.MemoryWordsInspector.*;
import com.sun.max.program.option.*;


/**
 * Persistent preferences for viewing sequences of memory words in the VM.
 *
 * @author Michael Van De Vanter
  */
public class MemoryWordsViewPreferences extends TableColumnVisibilityPreferences<MemoryWordsColumnKind> {

    private static MemoryWordsViewPreferences globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing these tables..
     */
    public static MemoryWordsViewPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new MemoryWordsViewPreferences(inspection);
        }
        return globalPreferences;
    }

    private static final String MEMORY_WORDS_MODE_PREFERENCE = "memoryWordsNavigationMode";

    private NavMode navigationMode;


    private MemoryWordsViewPreferences(Inspection inspection) {
        super(inspection, "memoryWordsViewPrefs", MemoryWordsColumnKind.class, MemoryWordsColumnKind.VALUES);
        final OptionTypes.EnumType<NavMode> optionType = new OptionTypes.EnumType<NavMode>(NavMode.class);
        navigationMode = inspection.settings().get(saveSettingsListener, MEMORY_WORDS_MODE_PREFERENCE, optionType, NavMode.WORD);
    }

    /**
     * A per-instance set of view preferences, initialized to the global preferences.
     * @param defaultPreferences the global defaults for this kind of view
     */
    public MemoryWordsViewPreferences(MemoryWordsViewPreferences globalPreferences) {
        super(globalPreferences);
        navigationMode = globalPreferences.navigationMode;
    }


    @Override
    protected boolean canBeMadeInvisible(MemoryWordsColumnKind columnType) {
        return columnType.canBeMadeInvisible();
    }

    @Override
    protected boolean defaultVisibility(MemoryWordsColumnKind columnType) {
        return columnType.defaultVisibility();
    }

    @Override
    protected String label(MemoryWordsColumnKind columnType) {
        return columnType.label();
    }

    public NavMode navigationMode() {
        return navigationMode;
    }

    public void setNavigationMode(NavMode navigationMode) {
        this.navigationMode = navigationMode;
        inspection().settings().save();
    }

    @Override
    protected void saveSettings(SaveSettingsEvent saveSettingsEvent) {
        super.saveSettings(saveSettingsEvent);
        saveSettingsEvent.save(MEMORY_WORDS_MODE_PREFERENCE, navigationMode.name());
    }

    @Override
    public JPanel getPanel() {
        final JRadioButton[] radioButtons = new JRadioButton[NavMode.VALUES.length()];
        final ButtonGroup group = new ButtonGroup();

        final ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JRadioButton button = (JRadioButton) e.getSource();
                if (button.isSelected()) {
                    for (int index = 0; index < radioButtons.length; index++) {
                        if (radioButtons[index] == button) {
                            setNavigationMode(NavMode.VALUES.get(index));
                            inspection().settings().save();
                            break;
                        }
                    }
                }
            }
        };


        final JPanel optionsPane = new InspectorPanel(inspection());
        final TextLabel modeLabel = new TextLabel(inspection(), "Navigate by:  ");
        modeLabel.setToolTipText("Configures the behavior of navigation instructions");
        optionsPane.add(modeLabel);

        for (NavMode mode : NavMode.VALUES) {
            final JRadioButton radioButton = new InspectorRadioButton(inspection(), mode.label(), mode.description());
            radioButton.setSelected(mode == navigationMode);
            radioButton.addItemListener(itemListener);
            radioButtons[mode.ordinal()] = radioButton;
            group.add(radioButton);
            optionsPane.add(radioButton);
        }


        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        panel.add(super.getPanel(), BorderLayout.NORTH);
        panel.add(optionsPane, BorderLayout.SOUTH);
        return panel;
    }


}
