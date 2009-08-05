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
import com.sun.max.ins.debug.JavaStackFramePanel.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.option.*;


/**
 * Persistent preferences for viewing stack information in the VM.
 *
 * @author Michael Van De Vanter
 */
public class StackViewPreferences extends AbstractInspectionHolder {

    private static final String STACK_INSPECTION_SETTINGS_NAME = "stackInspectorPrefs";
    private static final String SLOT_NAME_DISPLAY_MODE_PREFERENCE = "slotNameDisplay";

    private static SlotNameDisplayMode slotNameDisplayMode = SlotNameDisplayMode.NAME;

    private static final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener(STACK_INSPECTION_SETTINGS_NAME) {

        @Override
        public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
            saveSettingsEvent.save(SLOT_NAME_DISPLAY_MODE_PREFERENCE, slotNameDisplayMode.name());
        }
    };

    StackViewPreferences(Inspection inspection) {
        super(inspection);
        final InspectionSettings settings = inspection.settings();
        settings.addSaveSettingsListener(saveSettingsListener);
        slotNameDisplayMode = settings.get(saveSettingsListener, SLOT_NAME_DISPLAY_MODE_PREFERENCE, new OptionTypes.EnumType<SlotNameDisplayMode>(SlotNameDisplayMode.class), SlotNameDisplayMode.NAME);
    }

    public void setSlotNameDisplayMode(SlotNameDisplayMode slotNameDisplayMode) {
        StackViewPreferences.slotNameDisplayMode = slotNameDisplayMode;
    }

    public SlotNameDisplayMode slotNameDisplayMode() {
        return slotNameDisplayMode;
    }

    /**
     * @return a GUI panel for setting these preferences
     */
    public JPanel getPanel() {
        final JRadioButton[] radioButtons = new JRadioButton[JavaStackFramePanel.SlotNameDisplayMode.VALUES.length()];
        final ButtonGroup group = new ButtonGroup();

        final ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JRadioButton button = (JRadioButton) e.getSource();
                if (button.isSelected()) {
                    for (int index = 0; index < radioButtons.length; index++) {
                        if (radioButtons[index] == button) {
                            setSlotNameDisplayMode(JavaStackFramePanel.SlotNameDisplayMode.VALUES.get(index));
                            inspection().settings().save();
                            break;
                        }
                    }
                }
            }
        };
        final JPanel content = new InspectorPanel(inspection());
        content.add(new TextLabel(inspection(), "Identify frame slots by:  "));

        for (SlotNameDisplayMode mode : SlotNameDisplayMode.VALUES) {
            final JRadioButton radioButton = new InspectorRadioButton(inspection(), mode.label(), mode.description());
            radioButton.setSelected(mode == slotNameDisplayMode);
            radioButton.addItemListener(itemListener);
            radioButtons[mode.ordinal()] = radioButton;
            group.add(radioButton);
            content.add(radioButton);
        }
        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        panel.add(content, BorderLayout.WEST);
        return panel;
    }


}
