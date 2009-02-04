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
 * /**
 * A text field specialized for use in the Maxine Inspector.
 *
 * An editable JTextField that lets you enter a positive integer number
 * in a specified range, using digits of a specified radix/base.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class AddressInputField extends JTextField {

    public abstract void update(Address value);

    private final int _radix;
    private Address _value;

    public Address value() {
        return _value;
    }

    private Address _lowerBound = Address.zero();
    private Address _upperBound = Address.fromLong(-1L);

    private int _numberOfDigits;

    protected void updateView() {
        _numberOfDigits = _upperBound.toString(_radix).length();
        setPreferredSize(new Dimension(9 * _numberOfDigits, 25));
        setText(_value.toString(_radix));
    }

    private void setRange(Address lowerBound, Address upperBound) {
        _lowerBound = lowerBound;
        _upperBound = upperBound;
        if (_value.greaterThan(upperBound)) {
            _value = upperBound;
            updateView();
        } else if (_value.lessThan(lowerBound)) {
            _value = lowerBound;
            updateView();
        }
    }

    public void setRange(int lowerBound, int upperBound) {
        setRange(Address.fromInt(lowerBound), Address.fromInt(upperBound));
    }

    public void attemptUpdate() {
        String text = getText().trim();
        if (_radix == 16 && text.substring(0, 2).equalsIgnoreCase("0x")) {
            text = text.substring(2);
            setText(text);
        }
        final Address value = Address.parse(text, _radix);
        if (_lowerBound.lessEqual(value) && value.lessEqual(_upperBound)) {
            _value = value;
            update(value);
        } else {
            setText(_value.toString(_radix)); // revert
        }
    }

    private void textChanged() {
        String text = getText().trim();
        if (_radix == 16 && text.length() >= 2 && text.substring(0, 2).equalsIgnoreCase("0x")) {
            text = text.substring(2);
            setText(text);
        }
        final Address value = Address.parse(text, _radix);
        if (!value.equals(_value)) {
            attemptUpdate();
        }
    }

    protected AddressInputField(Inspection inspection, final int radix, Address initialValue) {
        _radix = radix;
        _value = initialValue;
        setFont(inspection.style().wordDataFont());
        updateView();
        addKeyListener(new KeyTypedListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                if (Character.digit(keyEvent.getKeyChar(), radix) < 0 || getText().length() >= _numberOfDigits) {
                    keyEvent.consume();
                }
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent focusEvent) {
                textChanged();
            }
        });
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                textChanged();
            }
        });
    }

    public abstract static class Decimal extends AddressInputField {
        public Decimal(Inspection inspection, Address initialValue) {
            super(inspection, 10, initialValue);
        }

        public Decimal(Inspection inspection) {
            this(inspection, Address.zero());
        }
    }

    public abstract static class Hex extends AddressInputField {
        public Hex(Inspection inspection, Address initialValue) {
            super(inspection, 16, initialValue);
        }

        public Hex(Inspection inspection) {
            this(inspection, Address.zero());
        }
    }
}
