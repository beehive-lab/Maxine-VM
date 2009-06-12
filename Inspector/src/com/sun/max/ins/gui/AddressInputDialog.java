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

import java.awt.event.*;

import javax.swing.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class AddressInputDialog extends InspectorDialog {

    public abstract void entered(Address address);

    private final String _actionName;

    private final AddressInputField.Hex _addressInputField;

    private final class EnterAction extends InspectorAction {
        private EnterAction() {
            super(inspection(), _actionName);
        }

        @Override
        protected void procedure() {
            try {
                _addressInputField.attemptUpdate();
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage("Badly formed address: " + numberFormatException.getMessage());
            }
        }
    }

    public boolean isValidInput(Address address) {
        return true;
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
            _actionName = "OK";
        } else {
            _actionName = actionName;
        }

        final JPanel dialogPanel = new InspectorPanel(inspection, new SpringLayout());
        dialogPanel.add(new TextLabel(inspection(), "Address:"));
        _addressInputField = new AddressInputField.Hex(inspection, initialAddress) {
            @Override
            public void update(Address address) {
                if (isValidInput(address)) {
                    dispose();
                    entered(address);
                }
            }
        };
        dialogPanel.add(_addressInputField);

        dialogPanel.add(new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        }));
        dialogPanel.add(new JButton(new EnterAction()));

        SpringUtilities.makeGrid(dialogPanel, 2, 2, 10, 10, 20, 20);
        setContentPane(dialogPanel);
        pack();
        inspection.gui().moveToMiddle(this);
        setVisible(true);
    }

}
