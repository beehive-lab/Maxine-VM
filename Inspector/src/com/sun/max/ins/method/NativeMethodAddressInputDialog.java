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

    private  AddressInputField.Decimal _codeLengthInputField;
    private final AddressInputField.Hex _addressInputField;
    private final JTextField _titleInputField;

    private final Size _initialCodeSize;
    private final String _initialTitle;

    private Address _codeStart;
    private Size _codeSize;
    private String _title;

    public abstract void entered(Address address, Size codeSize, String title);

    public NativeMethodAddressInputDialog(Inspection inspection, Address codeStart, Size initialCodeSize) {
        super(inspection, "Native Code Address", true);
        _initialCodeSize = initialCodeSize;
        _initialTitle =  defaultTitle(codeStart);

        final JPanel dialogPanel = new InspectorPanel(inspection, new SpringLayout());
        dialogPanel.add(new TextLabel(inspection, "Address:"));
        _addressInputField = new AddressInputField.Hex(inspection, codeStart) {
            @Override
            public void update(Address address) {
                if (isValidInput(address)) {
                    _codeStart = address;
                }
            }
        };
        dialogPanel.add(_addressInputField);
        dialogPanel.add(new TextLabel(inspection, "CodeLength:"));
        _codeLengthInputField = new AddressInputField.Decimal(inspection, _initialCodeSize) {
            @Override
            public void update(Address address) {
                if (isValidInput(address)) {
                    _codeSize = Size.fromLong(address.toLong());
                }
            }
        };
        dialogPanel.add(_codeLengthInputField);
        dialogPanel.add(new TextLabel(inspection, "Title:"));
        _titleInputField = new JTextField(_initialTitle);
        dialogPanel.add(_titleInputField);

        dialogPanel.add(new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }));
        dialogPanel.add(new JButton(new EnterAction()));

        SpringUtilities.makeGrid(dialogPanel, 4, 2, 10, 10, 20, 20);
        setContentPane(dialogPanel);
        pack();
        inspection.moveToMiddle(this);
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
            _addressInputField.attemptUpdate();
            _codeLengthInputField.attemptUpdate();
            _title = _titleInputField.getText();
            if (_title.equals(_initialTitle)) {
                _title = defaultTitle(_codeStart);
            }
            dispose();
            entered(_codeStart, _codeSize, _title);
        }
    }

}
