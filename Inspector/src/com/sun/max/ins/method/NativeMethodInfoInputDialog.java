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
package com.sun.max.ins.method;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.unsafe.*;

/**
 * Dialog for entering information about areas in native code that are generally
 * unknown to the VM and Inspector.
 *
 * @author Mick Jordan
 * @author Michael Van De Vanter
 */
public abstract class NativeMethodInfoInputDialog extends InspectorDialog {

    private  AddressInputField.Decimal codeLengthInputField;
    private final AddressInputField.Hex addressInputField;
    private final JTextField descriptionInputField;

    private final Size initialCodeSize;
    private final String initialTitle;

    private Address codeStart;
    private Size codeSize;
    private String description;

    public abstract void entered(Address address, Size codeSize, String title);

    public NativeMethodInfoInputDialog(Inspection inspection, Address codeStart, Size initialCodeSize, String title) {
        super(inspection, title == null ? "Native Code Description" : title, true);
        this.initialCodeSize = initialCodeSize;
        initialTitle =  defaultTitle(codeStart);

        final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());

        final JPanel fieldsPanel = new InspectorPanel(inspection, new SpringLayout());

        final JLabel addressFieldLabel = new JLabel("Address:", JLabel.TRAILING);
        fieldsPanel.add(addressFieldLabel);
        addressInputField = new AddressInputField.Hex(inspection, codeStart) {
            @Override
            public void update(Address address) {
                if (isValidInput(address)) {
                    NativeMethodInfoInputDialog.this.codeStart = address;
                }
            }
        };
        addressFieldLabel.setLabelFor(addressInputField);
        fieldsPanel.add(addressInputField);

        final JLabel lengthFieldLabel = new JLabel("CodeLength:", JLabel.TRAILING);
        fieldsPanel.add(lengthFieldLabel);
        codeLengthInputField = new AddressInputField.Decimal(inspection, initialCodeSize) {
            @Override
            public void update(Address address) {
                if (isValidInput(address)) {
                    codeSize = Size.fromLong(address.toLong());
                }
            }
        };
        codeLengthInputField.setRange(1, 999);
        lengthFieldLabel.setLabelFor(codeLengthInputField);
        fieldsPanel.add(codeLengthInputField);

        final JLabel descriptionFieldLabel = new JLabel("Description:", JLabel.TRAILING);
        fieldsPanel.add(descriptionFieldLabel);
        descriptionInputField = new JTextField(initialTitle);
        descriptionFieldLabel.setLabelFor(descriptionInputField);
        fieldsPanel.add(descriptionInputField);

        SpringUtilities.makeCompactGrid(fieldsPanel, 3, 2, 6, 6, 6, 6);

        dialogPanel.add(fieldsPanel, BorderLayout.NORTH);

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

    private String defaultTitle(Address address) {
        return "native code " + address;
    }

    public boolean isValidInput(Address address) {
        return true;
    }

    private final class EnterAction extends InspectorAction {
        private EnterAction() {
            super(inspection(), "OK");
        }

        @Override
        protected void procedure() {
            addressInputField.attemptUpdate();
            codeLengthInputField.attemptUpdate();
            description = descriptionInputField.getText();
            if (description.equals(initialTitle)) {
                description = defaultTitle(codeStart);
            }
            dispose();
            entered(codeStart, codeSize, description);
        }
    }

}
