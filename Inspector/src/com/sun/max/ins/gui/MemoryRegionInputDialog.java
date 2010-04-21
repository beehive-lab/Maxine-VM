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
 * A dialog that permits entering of a hex-specified VM memory address.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class MemoryRegionInputDialog extends InspectorDialog {

    private final AddressInputField.Hex addressInputField;
    private final String actionButtonTitle;
    private final NumberInputField sizeField;

    // Most recently entered/updated address from the input field, if valid; null if not valid.
    //private MemoryRegion memoryRegion;
    private Address address = null;
    private Size size = null;

    /**
     * Notifies subclasses that the dialog is closing with a valid address entered.
     *
     * @param address valid address entered.
     */
    public abstract void entered(Address address, Size size);

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
                sizeField.attemptUpdate();
                if (MemoryRegionInputDialog.this.address != null && MemoryRegionInputDialog.this.size != null) {
                    dispose();
                    entered(MemoryRegionInputDialog.this.address, MemoryRegionInputDialog.this.size);
                }
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage("Badly formed address: " + numberFormatException.getMessage());
            }
        }
    }

    /**
     * Subclasses override to validate an entered address, above and beyond being a valid hex number.
     *
     * @param address an address to validate
     * @return {@code null} is {@code address} is valid, an error message if not
     */
    protected String validateInput(Address address) {
        return null;
    }

    /**
     * Creates and displays an interactive dialog that allows entering of a hex-specified memory address.
     *
     * @param inspection
     * @param initialAddress default value for the address
     */
    public MemoryRegionInputDialog(Inspection inspection, Address initialAddress) {
        this(inspection, initialAddress, "Address", null);
    }

    /**
     * Creates and displays an interactive dialog that allows entering of a hex-specified memory address.
     *
     * @param inspection
     * @param initialAddress default value for the address
     * @param frameTitle optional title to appear in the window frame of the dialog
     */
    public MemoryRegionInputDialog(Inspection inspection, Address initialAddress, String frameTitle) {
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
    public MemoryRegionInputDialog(Inspection inspection, Address address, String frameTitle, String actionButtonTitle) {
        super(inspection, frameTitle, true);
        this.address = address;
        if (actionButtonTitle == null) {
            this.actionButtonTitle = "OK";
        } else {
            this.actionButtonTitle = actionButtonTitle;
        }

        final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());

        final JPanel fieldPanel = new InspectorPanel(inspection);
        fieldPanel.add(new TextLabel(inspection(), "Address:    0x"));
        addressInputField = new AddressInputField.Hex(inspection, address) {
            @Override
            public void update(Address address) {
                final String errorMessage = validateInput(address);
                if (errorMessage == null) {
                    MemoryRegionInputDialog.this.address = address;
                } else {
                    MemoryRegionInputDialog.this.address = null;
                    JOptionPane.showMessageDialog(dialogPanel, errorMessage, "Invalid Address", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        sizeField = new NumberInputField(inspection, 8) {
            @Override
            public void update(long value) {
                if (value == 0) {
                    MemoryRegionInputDialog.this.size = null;
                    JOptionPane.showMessageDialog(dialogPanel, "Insert valid size in dec or hex (0x...).", "Invalid Size", JOptionPane.ERROR_MESSAGE);
                } else {
                    MemoryRegionInputDialog.this.size = Size.fromLong(value);
                }
            }
        };

        fieldPanel.add(addressInputField);
        fieldPanel.add(new TextLabel(inspection(), "    Size:    "));
        fieldPanel.add(sizeField);

        dialogPanel.add(fieldPanel, BorderLayout.NORTH);

        final JPanel buttonPanel = new InspectorPanel(inspection);
        buttonPanel.add(new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        }));
        buttonPanel.add(new JButton(new EnterAction()));
        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(dialogPanel);
        pack();
        inspection.gui().moveToMiddle(this);
        setVisible(true);
    }

}
