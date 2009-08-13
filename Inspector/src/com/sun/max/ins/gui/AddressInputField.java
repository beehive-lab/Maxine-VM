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

import com.sun.max.ins.*;
import com.sun.max.unsafe.*;

/**
 * An editable JTextField that lets you enter a positive integer number
 * in a specified range, using digits of a specified radix/base.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class AddressInputField extends JTextField {

    public abstract void update(Address value);

    private final int radix;
    private Address value;

    public Address value() {
        return value;
    }

    public void setValue(Address value) {
        this.value = value;
        updateView();
    }

    private Address lowerBound = Address.zero();
    private Address upperBound = Address.fromLong(-1L);

    private int numberOfDigits;

    protected void updateView() {
        numberOfDigits = upperBound.toUnsignedString(radix).length();
        final String unsignedString = value.toUnsignedString(radix);
        setText(unsignedString);
    }

    private void setRange(Address lowerBound, Address upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        if (value.greaterThan(upperBound)) {
            value = upperBound;
        } else if (value.lessThan(lowerBound)) {
            value = lowerBound;
        }
        updateView();
    }

    public void setRange(int lowerBound, int upperBound) {
        setRange(Address.fromInt(lowerBound), Address.fromInt(upperBound));
    }

    public void attemptUpdate() {
        String text = getText().trim();
        if (text.isEmpty()) {
            return;
        }
        if (radix == 16 && text.length() > 2 && text.substring(0, 2).equalsIgnoreCase("0x")) {
            text = text.substring(2);
            setText(text);
        }
        final Address value = Address.parse(text, radix);
        if (lowerBound.lessEqual(value) && value.lessEqual(upperBound)) {
            this.value = value;
            update(value);
        } else {
            setText(this.value.toUnsignedString(radix)); // revert
        }
    }

    private void textChanged() {
        String text = getText().trim();
        if (radix == 16 && text.length() >= 2 && text.substring(0, 2).equalsIgnoreCase("0x")) {
            text = text.substring(2);
            setText(text);
        }
        final Address value = Address.parse(text, radix);
        if (!value.equals(this.value)) {
            attemptUpdate();
        }
    }

    protected AddressInputField(Inspection inspection, final int radix, Address initialValue) {
        this.radix = radix;
        this.value = initialValue;
        setFont(inspection.style().wordDataFont());
        updateView();
        addKeyListener(new KeyTypedListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                if (Character.digit(keyEvent.getKeyChar(), radix) < 0 || getText().length() >= numberOfDigits) {
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
