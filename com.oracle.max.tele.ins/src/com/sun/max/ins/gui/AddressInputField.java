/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.gui;

import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.unsafe.*;

/**
 * An editable {@link JTextField} that permits entering a positive integer number
 * in a specified range, using digits of a specified radix/base.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class AddressInputField extends JTextField {

    private static final int ADDRESSINPUTFIELDSIZE = 16;

    private final int radix;
    private Address value;

    private Address lowerBound = Address.zero();
    private Address upperBound = Address.fromLong(-1L);

    private int numberOfDigits;

    protected AddressInputField(Inspection inspection, final int radix, Address initialValue) {
        super(ADDRESSINPUTFIELDSIZE);
        this.radix = radix;
        this.value = initialValue;
        setFont(inspection.style().wordDataFont());
        updateView();
        addKeyListener(new KeyTypedListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                if (keyEvent.getKeyChar() == KeyEvent.VK_ENTER) {
                    returnPressed();
                } else if (Character.digit(keyEvent.getKeyChar(), radix) < 0 || getText().length() >= numberOfDigits) {
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

    public final Address value() {
        return value;
    }

    public final void setValue(Address value) {
        this.value = value;
        updateView();
    }

    public final void setRange(int lowerBound, int upperBound) {
        setRange(Address.fromInt(lowerBound), Address.fromInt(upperBound));
    }

    public final void attemptUpdate() {
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

    /**
     * Called each time the user enters a legitimate keystroke.
     *
     * @param value the address currently represented by the contents of the field.
     */
    protected abstract void update(Address value);

    /**
     * Called when the user hits Return; default action is nothing.
     */
    protected void returnPressed() {
    }

    private void updateView() {
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

    public abstract static class Decimal extends AddressInputField {
        public Decimal(Inspection inspection, Address initialValue) {
            super(inspection, 10, initialValue);
        }

    }

    public abstract static class Hex extends AddressInputField {
        public Hex(Inspection inspection, Address initialValue) {
            super(inspection, 16, initialValue);
        }

    }
}
