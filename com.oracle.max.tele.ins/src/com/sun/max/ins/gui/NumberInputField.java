/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.*;

/**
 * An editable {@link JTextField} that permits entering a positive integer number
 * in a specified range, using digits of a specified radix/base.
 */
public abstract class NumberInputField extends JTextField {

    /**
     * Default JTextField size of number input field.
     */
    private static final int NUMBERINPUTFIELDDEFAULTSIZE = 8;

    /**
     * The value of the number input field.
     */
    private long value;

    /**
     * Abstract update method.
     * @param value
     */
    public abstract void update(long value);

    public long value() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
        updateView();
    }

    protected void updateView() {
        setText(String.valueOf(value));
        update(value);
    }

    public void attemptUpdate() {
        String text = getText().trim();
        if (text.isEmpty()) {
            return;
        }

        if (text.startsWith("0x")) {
            try {
                this.value = Integer.valueOf(text.substring(2), 16);
            } catch (NumberFormatException ne) {
                Log.println("HEX PARSE ERROR " + text);
                value = 0;
            }
        } else {
            try {
                value = Integer.parseInt(text);
            } catch (NumberFormatException ne) {
                Log.println("INT PARSE ERROR" + text);
                value = 0;
            }
        }
        update(value);

    }

    private void textChanged() {
        String text = getText().trim();
        if (text.isEmpty()) {
            return;
        }
        attemptUpdate();
    }

    public NumberInputField(Inspection inspection, long initialValue) {
        super(NUMBERINPUTFIELDDEFAULTSIZE);
        this.value = initialValue;
        setFont(inspection.style().defaultWordDataFont());
        setValue(initialValue);
        addKeyListener(new KeyTypedListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
//                if (Character.digit(keyEvent.getKeyChar(), radix) < 0) {
//                    keyEvent.consume();
//                }
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
}
