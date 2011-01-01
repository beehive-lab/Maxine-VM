/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.method;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.unsafe.*;

/**
 * Dialog for entering information about areas in VM memory that are generally
 * unknown to the VM and Inspector:  starting location, size (optional), and
 * a description useful for debugging.
 * <br>
 * The dialog works in two modes:  one that includes address, size, and description
 * fields and one that includes just address and description.
 *
 * @author Mick Jordan
 * @author Michael Van De Vanter
 */
public abstract class NativeLocationInputDialog extends InspectorDialog {

    private AddressInputField.Hex addressInputField;
    private AddressInputField.Decimal sizeInputField;
    private JTextField descriptionInputField;

    // Most recently entered and validated value in the address field; null if current entry is not valid.
    private Address address;

    // Most recently entered and validated value in the size field; null if current entry is not valid.
    private Size size;
    // Should the dialog display a size field?
    private final boolean getSizeInput;

    // Most recently entered value in the description field.
    private String description;

    /**
     * When the dialog was created with a size parameter, this method gets called with the
     * results when the dialog is complete and if all field values are valid.
     *
     * @param address valid address entered
     * @param size valid size entered
     * @param title description entered
     */
    public void entered(Address address, Size size, String description) {
    }

    /**
     * When the dialog was created without a size parameter, this method gets called with the
     * results when the dialog is complete and if all field values are valid.
     *
     * @param address valid address entered
     * @param title description entered
     */
    public void entered(Address address, String description) {
    }

    /**
     * Creates a dialog for entering information about a region of otherwise unknown memory in the VM.
     * Calls {@link #entered(Address, Size, String)} when completed with valid values.
     * <br>
     * Override {@link #isValidAddress(Address)} for additional validation on the address field.
     * <br>
     * Override {@link #isValidSize(Size)} for additional validation on the size field.
     * <br>
     * @param inspection
     * @param frameTitle optional title to appear in the window frame of the dialog
     * @param address initial value for the address input field
     * @param size initial value for the size input field
     * @param description initial value for the description input field
     */
    public NativeLocationInputDialog(Inspection inspection, String frameTitle, Address address, Size size, String description) {
        super(inspection, frameTitle == null ? "Native Location Description" : frameTitle, true);
        this.address = address == null ? Address.zero() : address;
        this.size = size == null ? Size.zero() : size;
        this.getSizeInput = true;
        this.description = description;
        createDialog();
    }

    /**
     * Creates a dialog for entering information about a region of otherwise unknown memory in the VM.
     * Calls {@link #entered(Address, String)} when completed with valid values.
     * <br>
     * Override {@link #isValidAddress(Address)} for additional validation on the address field.
     * <br>
     * @param inspection
     * @param frameTitle optional title to appear in the window frame of the dialog
     * @param address initial value for the address input field
     * @param description initial value for the description input field
     */
    public NativeLocationInputDialog(Inspection inspection, String frameTitle, Address address, String description) {
        super(inspection, frameTitle == null ? "Native Location Description" : frameTitle, true);
        this.address = address == null ? Address.zero() : address;
        this.size = null;
        this.getSizeInput = false;
        this.description = description;
        createDialog();
    }

    private void createDialog() {

        final JPanel dialogPanel = new InspectorPanel(inspection(), new BorderLayout());

        final JPanel fieldsPanel = new InspectorPanel(inspection(), new SpringLayout());

        final JLabel addressFieldLabel = new JLabel("Address:", JLabel.TRAILING);
        fieldsPanel.add(addressFieldLabel);
        addressInputField = new AddressInputField.Hex(inspection(), address) {
            @Override
            public void update(Address address) {
                if (isValidAddress(address)) {
                    NativeLocationInputDialog.this.address = address;
                } else {
                    NativeLocationInputDialog.this.address = null;
                }
            }
        };
        addressFieldLabel.setLabelFor(addressInputField);
        fieldsPanel.add(addressInputField);

        if (getSizeInput) {
            final JLabel sizeFieldLabel = new JLabel("Size:", JLabel.TRAILING);
            fieldsPanel.add(sizeFieldLabel);
            sizeInputField = new AddressInputField.Decimal(inspection(), size) {

                @Override
                public void update(Address address) {
                    final Size size = Size.fromLong(address.toLong());
                    if (isValidSize(size)) {
                        NativeLocationInputDialog.this.size = size;
                    } else {
                        NativeLocationInputDialog.this.size = null;
                    }
                }
            };
            sizeInputField.setRange(1, 999);
            sizeFieldLabel.setLabelFor(sizeInputField);
            fieldsPanel.add(sizeInputField);
        }
        final JLabel descriptionFieldLabel = new JLabel("Description:", JLabel.TRAILING);
        fieldsPanel.add(descriptionFieldLabel);
        descriptionInputField = new JTextField(description);
        descriptionFieldLabel.setLabelFor(descriptionInputField);
        fieldsPanel.add(descriptionInputField);

        final int rows = getSizeInput ? 3 : 2;
        final int cols = 2;
        SpringUtilities.makeCompactGrid(fieldsPanel, rows, cols, 6, 6, 6, 6);

        dialogPanel.add(fieldsPanel, BorderLayout.NORTH);

        final JPanel buttonPanel = new InspectorPanel(inspection());
        buttonPanel.add(new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        }));
        buttonPanel.add(new JButton(new EnterAction()));

        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(dialogPanel);
        pack();
        inspection().gui().moveToMiddle(this);
        setVisible(true);
    }

    private String defaultTitle(Address address) {
        return "native  " + address;
    }

    protected boolean isValidAddress(Address address) {
        return true;
    }

    protected boolean isValidSize(Size size) {
        return true;
    }

    private final class EnterAction extends InspectorAction {
        private EnterAction() {
            super(inspection(), "OK");
        }

        @Override
        protected void procedure() {
            addressInputField.attemptUpdate();
            if (NativeLocationInputDialog.this.address == null) {
                gui().errorMessage("Invalid address");
                return;
            }
            if (getSizeInput) {
                sizeInputField.attemptUpdate();
                if (NativeLocationInputDialog.this.size == null) {
                    gui().errorMessage("Invalid size");
                    return;
                }
            }
            description = descriptionInputField.getText();
            dispose();
            if (getSizeInput) {
                entered(address, size, description);
            } else {
                entered(address, description);
            }
        }
    }

}
