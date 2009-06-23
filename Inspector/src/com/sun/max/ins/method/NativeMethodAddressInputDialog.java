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

import java.awt.event.*;

import javax.swing.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.unsafe.*;

/**
 *
 * @author Mick Jordan
 *
 */
public abstract class NativeMethodAddressInputDialog extends InspectorDialog {

    private  AddressInputField.Decimal codeLengthInputField;
    private final AddressInputField.Hex addressInputField;
    private final JTextField titleInputField;

    private final Size initialCodeSize;
    private final String initialTitle;

    private Address codeStart;
    private Size codeSize;
    private String title;

    public abstract void entered(Address address, Size codeSize, String title);

    public NativeMethodAddressInputDialog(Inspection inspection, Address codeStart, Size initialCodeSize) {
        super(inspection, "Native Code Address", true);
        this.initialCodeSize = initialCodeSize;
        initialTitle =  defaultTitle(codeStart);

        final JPanel dialogPanel = new InspectorPanel(inspection, new SpringLayout());
        dialogPanel.add(new TextLabel(inspection, "Address:"));
        addressInputField = new AddressInputField.Hex(inspection, codeStart) {
            @Override
            public void update(Address address) {
                if (isValidInput(address)) {
                    NativeMethodAddressInputDialog.this.codeStart = address;
                }
            }
        };
        dialogPanel.add(addressInputField);
        dialogPanel.add(new TextLabel(inspection, "CodeLength:"));
        codeLengthInputField = new AddressInputField.Decimal(inspection, initialCodeSize) {
            @Override
            public void update(Address address) {
                if (isValidInput(address)) {
                    codeSize = Size.fromLong(address.toLong());
                }
            }
        };
        dialogPanel.add(codeLengthInputField);
        dialogPanel.add(new TextLabel(inspection, "Title:"));
        titleInputField = new JTextField(initialTitle);
        dialogPanel.add(titleInputField);

        dialogPanel.add(new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }));
        dialogPanel.add(new JButton(new EnterAction()));

        SpringUtilities.makeGrid(dialogPanel, 4, 2, 10, 10, 20, 20);
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
            title = titleInputField.getText();
            if (title.equals(initialTitle)) {
                title = defaultTitle(codeStart);
            }
            dispose();
            entered(codeStart, codeSize, title);
        }
    }

}
