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
 * A dialog that permits entering of a hex-specified memory address.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class AddressInputDialog extends InspectorDialog {

    /**
     * Action that attempts to close the dialog; fails if input value not valid.
     */
    private final class EnterAction extends InspectorAction {
        private EnterAction() {
            super(inspection(), actionButtonTitle);
        }

        @Override
        protected void procedure() {
            try {
                addressInputField.attemptUpdate();
                if (AddressInputDialog.this.address != null) {
                    dispose();
                    entered(AddressInputDialog.this.address);
                }
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage("Badly formed address: " + numberFormatException.getMessage());
            }
        }
    }

    private final InspectorAction enterAction;
    private final AddressInputField.Hex addressInputField;
    private final String actionButtonTitle;

    // Most recently entered/updated address from the input field, if valid; null if not valid.
    private Address address;

    /**
     * Creates and displays an interactive dialog that allows entering of a hex-specified memory address.
     *
     * @param inspection
     * @param initialAddress default value for the address
     */
    public AddressInputDialog(Inspection inspection, Address initialAddress) {
        this(inspection, initialAddress, "Address", null);
    }

    /**
     * Creates and displays an interactive dialog that allows entering of a hex-specified memory address.
     *
     * @param inspection
     * @param initialAddress default value for the address
     * @param frameTitle optional title to appear in the window frame of the dialog
     */
    public AddressInputDialog(Inspection inspection, Address initialAddress, String frameTitle) {
        this(inspection, initialAddress, frameTitle, null);
    }

    /**
     * Creates and displays an interactive dialog that allows entering of a hex-specified memory address.
     *
     * @param inspection
     * @param address default value for the address
     * @param frameTitle optional title to appear in the window frame of the dialog
     * @param actionButtonTitle  optional text to appear on the button that triggers action; if null, button text will be "OK".
     */
    public AddressInputDialog(Inspection inspection, Address address, String frameTitle, String actionButtonTitle) {
        super(inspection, frameTitle, true);
        this.address = address;
        if (actionButtonTitle == null) {
            this.actionButtonTitle = "OK";
        } else {
            this.actionButtonTitle = actionButtonTitle;
        }
        this.enterAction = new EnterAction();

        final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());

        final JPanel fieldPanel = new InspectorPanel(inspection);
        fieldPanel.add(new TextLabel(inspection(), "Address:    0x"));
        addressInputField = new AddressInputField.Hex(inspection, address) {

            @Override
            public void update(Address address) {
                final String errorMessage = validateInput(address);
                if (errorMessage == null) {
                    AddressInputDialog.this.address = address;
                } else {
                    AddressInputDialog.this.address = null;
                    JOptionPane.showMessageDialog(dialogPanel, errorMessage, "Invalid Address", JOptionPane.ERROR_MESSAGE);
                }
            }

            @Override
            public void returnPressed() {
                enterAction.perform();
            }
        };
        fieldPanel.add(addressInputField);
        dialogPanel.add(fieldPanel, BorderLayout.NORTH);

        final JPanel buttonPanel = new InspectorPanel(inspection);
        buttonPanel.add(new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        }));
        buttonPanel.add(new JButton(enterAction));
        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(dialogPanel);
        pack();
        inspection.gui().moveToMiddle(this);
        setVisible(true);
    }

    /**
     * Notifies subclasses that the dialog is closing with a valid address entered.
     *
     * @param address valid address entered.
     */
    protected abstract void entered(Address address);

    /**
     * Subclasses override to validate an entered address, above and beyond being a valid hex number.
     *
     * @param address an address to validate
     * @return {@code null} is {@code address} is valid, an error message if not
     */
    protected String validateInput(Address address) {
        return null;
    }

}
