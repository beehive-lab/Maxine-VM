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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class AddressInputDialog extends InspectorDialog {

    public abstract void entered(Address address);

    private final String actionName;

    private final AddressInputField.Hex addressInputField;

    private final class EnterAction extends InspectorAction {
        private EnterAction() {
            super(inspection(), actionName);
        }

        @Override
        protected void procedure() {
            try {
                addressInputField.attemptUpdate();
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage("Badly formed address: " + numberFormatException.getMessage());
            }
        }
    }

    /**
     * Validates a given address.
     *
     * @param address an address to validate
     * @return {@code null} is {@code address} is valid, an error message if not
     */
    public String validateInput(Address address) {
        return null;
    }

    public AddressInputDialog(Inspection inspection, Address initialAddress) {
        this(inspection, initialAddress, "Address", null);
    }

    public AddressInputDialog(Inspection inspection, Address initialAddress, String title) {
        this(inspection, initialAddress, title, null);
    }

    public AddressInputDialog(Inspection inspection, Address initialAddress, String title, String actionName) {
        super(inspection, title, true);
        if (actionName == null) {
            this.actionName = "OK";
        } else {
            this.actionName = actionName;
        }

        final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());

        final JPanel upperPanel = new InspectorPanel(inspection);
        upperPanel.add(new TextLabel(inspection(), "Address:    0x"));
        addressInputField = new AddressInputField.Hex(inspection, initialAddress) {
            @Override
            public void update(Address address) {
                final String errorMessage = validateInput(address);
                if (errorMessage == null) {
                    dispose();
                    entered(address);
                } else {
                    JOptionPane.showMessageDialog(dialogPanel, errorMessage, "Invalid Address", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        upperPanel.add(addressInputField);
        dialogPanel.add(upperPanel, BorderLayout.NORTH);

        final JPanel lowerPanel = new InspectorPanel(inspection);
        lowerPanel.add(new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        }));
        lowerPanel.add(new JButton(new EnterAction()));
        dialogPanel.add(lowerPanel, BorderLayout.SOUTH);

        setContentPane(dialogPanel);
        pack();
        inspection.gui().moveToMiddle(this);
        setVisible(true);
    }

}
