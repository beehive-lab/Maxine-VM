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

import com.sun.max.ins.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;

/**
 * A selectable, lightweight, label for basic kinds of data
 * that we don't know much about.
 *
 * @author Michael Van De Vanter
 */
// TODO (mlvdv) - review new DataLabel, and integrate with other label usage, in particular WordValueLabel
public abstract class DataLabel extends InspectorLabel {

    protected DataLabel(Inspection inspection, String text) {
        super(inspection, text);
    }

    protected DataLabel(Inspection inspection, String text, String toolTipText) {
        this(inspection, text);
        setToolTipText(toolTipText);
    }

    public final void refresh(long epoch, boolean force) {
        // Values don't change unless explicitly set
    }

    public void redisplay() {
        // Default styles
        setFont(style().primitiveDataFont());
        setForeground(style().primitiveDataColor());
        setBackground(style().primitiveDataBackgroundColor());
    }

    /**
     * A label that displays an unchanging boolean value as "true" or "false".
     */
    public static final class BooleanAsText extends DataLabel {
        public BooleanAsText(Inspection inspection, boolean b) {
            super(inspection, b ? "true" : "false");
            redisplay();
        }
    }

    /**
     * A label that displays an unchanging byte value in decimal; a ToolTip shows the value in hex.
     */
    public static final class ByteAsDecimal extends DataLabel {
        public ByteAsDecimal(Inspection inspection, byte b) {
            super(inspection, Byte.toString(b), "byte: 0x" + Integer.toHexString(Byte.valueOf(b).intValue()));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().decimalDataFont());
            setForeground(style().decimalDataColor());
            setBackground(style().decimalDataBackgroundColor());
        }
    }

    /**
     * A label that displays an unchanging byte value in hex; a ToolTip shows the value in decimal.
     */
    public static final class ByteAsHex extends DataLabel {
        public ByteAsHex(Inspection inspection, byte b) {
            super(inspection, "0x" + Integer.toHexString(Byte.valueOf(b).intValue()), "byte:  " + Byte.toString(b));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().hexDataFont());
            setForeground(style().hexDataColor());
            setBackground(style().hexDataBackgroundColor());
        }
    }

    /**
     * A label that displays a changeable array of bytes in a bracketed string of hex pairs.
     */
    public static class ByteArrayAsHex extends DataLabel {

        private byte[] _bytes;

        public ByteArrayAsHex(Inspection inspection, byte[] bytes) {
            super(inspection, "");
            _bytes = bytes;
            redisplay();
        }

        @Override
        public void redisplay() {
            setFont(style().hexDataFont());
            setForeground(style().hexDataColor());
            setBackground(style().hexDataBackgroundColor());
            updateText();
        }

        public void setValue(byte[] bytes) {
            _bytes = bytes;
            updateText();
        }

        private void updateText() {
            if (_bytes != null) {
                final StringBuilder result = new StringBuilder(100);
                String prefix = "[";
                for (byte b : _bytes) {
                    result.append(prefix);
                    result.append(String.format("%02X", b));
                    prefix = " ";
                }
                result.append("]");
                setText(result.toString());
            }
        }
    }

    /**
     * A label that displays an unchanging short value in decimal;  a ToolTip displays the value in hex.
     */
    public static final class ShortAsDecimal extends DataLabel {
        ShortAsDecimal(Inspection inspection, short n) {
            super(inspection, Short.toString(n), "short: 0x" + Integer.toHexString(Short.valueOf(n).intValue()));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().decimalDataFont());
            setForeground(style().decimalDataColor());
            setBackground(style().decimalDataBackgroundColor());
        }
    }

    /**
     * A label that displays an unchanging char value as a textual character; a ToolTip displays the value in decimal and hex.
     */
    public static final class CharAsText extends DataLabel {
        CharAsText(Inspection inspection, char c) {
            super(inspection, "'" + c + "'");
            final int n = Character.getNumericValue(c);
            setToolTipText("char:  " + Integer.toString(n) + ", 0x" + Integer.toHexString(n));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().charDataFont());
            setForeground(style().charDataColor());
            setBackground(style().primitiveDataBackgroundColor());
        }
    }

    /**
     * A label that displays the decimal value of an unchanging char; a ToolTip displays the character and hex value.
     */
    public static final class CharAsDecimal extends DataLabel {
        public CharAsDecimal(Inspection inspection, char c) {
            super(inspection, Integer.toString(Character.getNumericValue(c)));
            setToolTipText("char:  '" + c + "', 0x" + Integer.toHexString(Character.getNumericValue(c)));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().decimalDataFont());
            setForeground(style().decimalDataColor());
            setBackground(style().decimalDataBackgroundColor());
        }
    }

    /**
     * A label that displays the decimal value of an unchanging int; a ToolTip displays the value in hex.
     */
    public static final class IntAsDecimal extends DataLabel {
        public IntAsDecimal(Inspection inspection, int n) {
            super(inspection, Integer.toString(n), "int: 0x" + Integer.toHexString(n));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().decimalDataFont());
            setForeground(style().decimalDataColor());
            setBackground(style().decimalDataBackgroundColor());
        }
    }

    /**
     * A label that displays the hex value of an unchanging int; a ToolTip displays the value in decimal.
     */
    public static final class IntAsHex extends DataLabel {
        public IntAsHex(Inspection inspection, int n) {
            super(inspection, "0x" + Integer.toHexString(n), "int:  " + Integer.toString(n));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().hexDataFont());
            setForeground(style().hexDataColor());
            setBackground(style().hexDataBackgroundColor());
        }
    }

    // TODO (mlvdv) implement FloatAsText
    public static final class FloatAsText extends DataLabel {
        public FloatAsText(Inspection inspection, float f) {
            super(inspection, "");
            Problem.unimplemented("FloatAsText");
        }
    }

    /**
     * A label that displays the decimal value of an unchanging long; a ToolTip displays the value in hex.
     */
    public static final class LongAsDecimal extends DataLabel {
        public LongAsDecimal(Inspection inspection, long n) {
            super(inspection, Long.toString(n), "int: 0x" + Long.toHexString(n));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().decimalDataFont());
            setForeground(style().decimalDataColor());
            setBackground(style().decimalDataBackgroundColor());
        }
    }

    /**
     * A label that displays the hex value of an unchanging long; a ToolTip displays the value in decimal.
     */
    public static final class LongAsHex extends DataLabel {
        public LongAsHex(Inspection inspection, long n) {
            super(inspection, "0x" + Long.toHexString(n), "long:  " + Long.toString(n));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().hexDataFont());
            setForeground(style().hexDataColor());
            setBackground(style().hexDataBackgroundColor());
        }
    }

    // TODO (mlvdv) implement DoubleAsText
    public static final class DoubleAsText extends DataLabel {
        public DoubleAsText(Inspection inspection, double f) {
            super(inspection, "");
            Problem.unimplemented("DoubleAsText");
        }
    }

    /**
     * A label that displays a memory address in hex; if a base address
     * is specified, then a ToolTip displays the offset from the base.
     */
    public static final class AddressAsHex extends DataLabel {

        private Address _address;
        private final Address _origin;

        public AddressAsHex(Inspection inspection, Address address) {
            this(inspection, address, null);
        }

        public AddressAsHex(Inspection inspection, Address address, Address origin) {
            super(inspection, address.toHexString());
            _address = address;
            _origin = origin;
            addMouseListener(new InspectorMouseClickAdapter(inspection()) {
                @Override
                public void procedure(final MouseEvent mouseEvent) {
                    switch (MaxineInspector.mouseButtonWithModifiers(mouseEvent)) {
                        case MouseEvent.BUTTON3: {
                            final InspectorMenu menu = new InspectorMenu();
                            menu.add(inspection().inspectionMenus().getCopyWordAction(_address, "Copy address to clipboard"));
                            menu.add(inspection().inspectionMenus().getInspectMemoryAction(_address));
                            menu.add(inspection().inspectionMenus().getInspectMemoryWordsAction(_address));
                            menu.popupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                        }
                    }
                }
            });
            redisplay();
        }

        @Override
        public void redisplay() {
            setFont(style().hexDataFont());
            setForeground(style().hexDataColor());
            setBackground(style().hexDataBackgroundColor());
            updateText();
        }

        public void setValue(Address address) {
            _address = address;
            updateText();
        }

        private void updateText() {
            setText(_address.toHexString());
            if (_origin == null) {
                setToolTipText(null);
            } else {
                final int position = _address.minus(_origin).toInt();
                setToolTipText("AsPosition: " + position + ", " +  "0x" + Integer.toHexString(position));
            }
        }

    }

    /**
     * A label that displays the textual name of an Enum value; a ToolTip displays
     * both the class name and the ordinal of the value.
     */
    public static final class EnumAsText extends DataLabel {
        public EnumAsText(Inspection inspection, Enum e) {
            super(inspection, e.getClass().getSimpleName() + "." + e.name(), "Enum:  " + e.getClass().getName() + "." + e.name() + " ord=" + e.ordinal());
            redisplay();
        }
    }

}
