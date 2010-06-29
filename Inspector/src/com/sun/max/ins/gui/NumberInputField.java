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
import com.sun.max.vm.*;

/**
 * An editable {@link JTextField} that permits entering a positive integer number
 * in a specified range, using digits of a specified radix/base.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
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
        setFont(inspection.style().wordDataFont());
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
