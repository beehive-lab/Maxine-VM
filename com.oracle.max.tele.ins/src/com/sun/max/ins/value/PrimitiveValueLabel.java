/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.value;

import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 *
 * A textual display label associated with a primitive value to be read from the VM.
 */
public class PrimitiveValueLabel extends ValueLabel {

    private final Kind kind;
    private String displayText;

    private boolean textDisplayMode = true;

    public PrimitiveValueLabel(Inspection inspection, Kind kind) {
        super(inspection, null);
        this.kind = kind;
        initializeValue();
        redisplay();
        addMouseListener(new InspectorMouseClickAdapter(inspection()) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                switch (inspection().gui().getButton(mouseEvent)) {
                    case MouseEvent.BUTTON1: {
                        break;
                    }
                    case MouseEvent.BUTTON2: {
                        cycleDisplay();
                        break;
                    }
                    case MouseEvent.BUTTON3: {
                        final InspectorPopupMenu menu = new InspectorPopupMenu();
                        menu.add(actions().copyValue(value(), "Copy value to clipboard"));
                        menu.add(new InspectorAction(inspection(), "Cycle display (Middle-Button)") {

                            @Override
                            protected void procedure() {
                                cycleDisplay();
                            }

                        });
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        });
    }

    public void redisplay() {
        setModeFont();
        updateText();
    }

    @Override
    public void updateText() {
        final Value value = value();
        assert value != null;
        setModeFont();

        try {
            final String asString = value.toString();


            if (kind == Kind.BOOLEAN) {
                final int asInt = value.toInt();
                final String asHex = intTo0xHex(asInt);
                if (textDisplayMode) {
                    displayText = asString;
                } else {
                    displayText = asHex;
                }
                setText(displayText);
                setWrappedToolTipHtmlText("boolean '" + asString + "' <br>as int: " + Integer.toString(asInt) + ", " + asHex);

            } else if (kind == Kind.BYTE) {
                final short asShort = value.toShort();
                final String asHex = intTo0xHex(asShort);
                if (textDisplayMode) {
                    displayText = asString;
                } else {
                    displayText = asHex;
                }
                setText(displayText);
                setWrappedToolTipHtmlText("byte '" + asString + "' <br>as int: " + Short.toString(asShort) + ", " + asHex);

            } else if (kind == Kind.CHAR) {
                final short asShort = value.toShort();
                final String asHex = intTo0xHex(asShort);
                if (textDisplayMode) {
                    displayText = "'" + asString + "'";
                } else {
                    displayText = asHex;
                }
                setText(displayText);
                setWrappedToolTipHtmlText("char '" + asString + "' <br>as int: " + Short.toString(asShort) + ", " + asHex);

            } else if (kind == Kind.DOUBLE) {
                final long asLong = value.toLong();
                final String asHex = longTo0xHex(asLong);
                if (textDisplayMode) {
                    displayText = asString;
                } else {
                    displayText = asHex;
                }
                setText(displayText);
                setWrappedToolTipHtmlText("double '" + asString + "' <br>as int: " + Long.toString(asLong) + ", " + asHex);

            } else if (kind == Kind.FLOAT) {
                final long asLong = value.toLong();
                final String asHex = longTo0xHex(asLong);
                if (textDisplayMode) {
                    displayText = asString;
                } else {
                    displayText = asHex;
                }
                setText(displayText);
                setWrappedToolTipHtmlText("float '" + asString + "' <br>as int: " + Long.toString(asLong) + ", " + asHex);

            } else if (kind == Kind.INT) {
                final int asInt = value.toInt();
                final String asHex = intTo0xHex(asInt);
                if (textDisplayMode) {
                    displayText = asString;
                } else {
                    displayText = asHex;
                }
                setText(displayText);
                setWrappedToolTipHtmlText("int '" + asString + "' <br>as int: " + Integer.toString(asInt) + ", " + asHex);

            } else if (kind == Kind.LONG) {
                final long asLong = value.toLong();
                final String asHex = longTo0xHex(asLong);
                if (textDisplayMode) {
                    displayText = asString;
                } else {
                    displayText = asHex;
                }
                setText(displayText);
                setWrappedToolTipHtmlText("long '" + asString + "' <br>as int: " + Long.toString(asLong) + ", " + asHex);

            } else if (kind == Kind.SHORT) {
                final short asShort = value.toShort();
                final String asHex = intTo0xHex(asShort);
                if (textDisplayMode) {
                    displayText = "'" + asString + "'";
                } else {
                    displayText = asHex;
                }
                setText(displayText);
                setWrappedToolTipHtmlText("short '" + asString + "' <br>as int: " + Short.toString(asShort) + ", " + asHex);
            } else {
                setText(asString);
                setWrappedToolTipHtmlText(asString);
            }
            if (value().kind().width.numberOfBytes == vm().platform().nBytesInWord() &&
                            vm().memoryIO().isZappedValue(value())) {
                setText(inspection().nameDisplay().zappedDataShortText());
                setToolTipText(inspection().nameDisplay().zappedDataLongText());
            }
        } catch (IllegalArgumentException e) {
            setText(inspection().nameDisplay().unavailableDataShortText());
            setToolTipText(inspection().nameDisplay().unavailableDataLongText());
        }
    }

    private void setModeFont() {
        if (textDisplayMode) {
            setFont(preference().style().primitiveDataFont());
        } else {
            setFont(preference().style().hexDataFont());
        }
    }

    private void cycleDisplay() {
        textDisplayMode = !textDisplayMode;
        updateText();
    }

}
