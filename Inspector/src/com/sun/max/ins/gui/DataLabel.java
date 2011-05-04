/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.datatransfer.*;
import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.*;

/**
 * A selectable, lightweight, label for basic kinds of data
 * about which little is known.
 *
 * @author Michael Van De Vanter
 */
public abstract class DataLabel extends InspectorLabel {

    protected DataLabel(Inspection inspection, String text) {
        super(inspection, text);
    }

    protected DataLabel(Inspection inspection, String text, String toolTipText) {
        this(inspection, text);
        setToolTipText(toolTipText);
    }

    public void refresh(boolean force) {
        // Values don't change unless explicitly set
    }

    public void redisplay() {
        // Default styles
        setFont(style().primitiveDataFont());
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
            super(inspection, Byte.toString(b), "byte: " + intTo0xHex(Byte.valueOf(b).intValue()));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().decimalDataFont());
        }
    }

    /**
     * A label that displays an unchanging byte value in hex; a ToolTip shows the value in decimal.
     */
    public static final class ByteAsHex extends DataLabel {
        public ByteAsHex(Inspection inspection, byte b) {
            super(inspection, intTo0xHex(Byte.valueOf(b).intValue()), "byte:  " + Byte.toString(b));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().hexDataFont());
        }
    }

    /**
     * A label that displays a changeable array of bytes in a bracketed string of hex pairs.
     */
    public static class ByteArrayAsHex extends DataLabel {

        private byte[] bytes;

        public ByteArrayAsHex(Inspection inspection, byte[] bytes) {
            super(inspection, "");
            this.bytes = bytes;
            redisplay();
        }

        @Override
        public void redisplay() {
            setFont(style().hexDataFont());
            updateText();
        }

        public void setValue(byte[] bytes) {
            this.bytes = bytes;
            updateText();
        }

        private void updateText() {
            final String byteString = bytesToByteString(bytes);
            setText(byteString);
            setWrappedToolTipText(byteString);
        }
    }

    /**
     * A label that displays a changeable array of bytes as 8 bit characters.
     */
    public static class ByteArrayAsUnicode extends DataLabel {

        private byte[] bytes;

        public ByteArrayAsUnicode(Inspection inspection, byte[] bytes) {
            super(inspection, "");
            this.bytes = bytes;
            redisplay();
        }

        @Override
        public void redisplay() {
            setFont(style().hexDataFont());
            updateText();
        }

        protected void setValue(byte[] bytes) {
            this.bytes = bytes;
            updateText();
        }

        private void updateText() {
            if (bytes != null && bytes.length > 0) {
                final StringBuilder result = new StringBuilder(100);
                String prefix = "[";
                for (int i = 0; i < bytes.length / 2; i++) {
                    result.append(prefix);
                    final int index = 2 * i;
                    final char ch = (char) ((bytes[index + 1] * 256) + bytes[index]);
                    result.append(Character.toString(ch));
                    prefix = " ";
                }
                result.append("]");
                final String labelText = result.toString();
                setText(labelText);
                setWrappedToolTipText(labelText);
            }
        }
    }

    /**
     * A label that displays a changeable array of bytes as 8 bit characters.
     */
    public static class ByteArrayAsChar extends DataLabel {

        private byte[] bytes;

        public ByteArrayAsChar(Inspection inspection, byte[] bytes) {
            super(inspection, "");
            this.bytes = bytes;
            redisplay();
        }

        @Override
        public void redisplay() {
            setFont(style().hexDataFont());
            updateText();
        }

        protected void setValue(byte[] bytes) {
            this.bytes = bytes;
            updateText();
        }

        private void updateText() {
            if (bytes != null && bytes.length > 0) {
                final StringBuilder result = new StringBuilder(100);
                String prefix = "[";
                for (byte b : bytes) {
                    result.append(prefix);
                    final char ch = (char) b;
                    result.append(Character.toString(ch));
                    prefix = " ";
                }
                result.append("]");
                final String labelText = result.toString();
                setText(labelText);
                setWrappedToolTipText(labelText);
            }
        }
    }

    /**
     * A label that displays an unchanging short value in decimal;  a ToolTip displays the value in hex.
     */
    public static final class ShortAsDecimal extends DataLabel {
        ShortAsDecimal(Inspection inspection, short n) {
            super(inspection, Short.toString(n), "short: " + intTo0xHex(Short.valueOf(n).intValue()));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().decimalDataFont());
        }
    }

    /**
     * A label that displays an unchanging char value as a textual character; a ToolTip displays the value in decimal and hex.
     */
    public static final class CharAsText extends DataLabel {
        CharAsText(Inspection inspection, char c) {
            super(inspection, "'" + c + "'");
            final int n = Character.getNumericValue(c);
            setToolTipText("char:  " + Integer.toString(n) + ", " + intTo0xHex(n));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().charDataFont());
        }
    }

    /**
     * A label that displays the decimal value of an unchanging char; a ToolTip displays the character and hex value.
     */
    public static final class CharAsDecimal extends DataLabel {
        public CharAsDecimal(Inspection inspection, char c) {
            super(inspection, Integer.toString(Character.getNumericValue(c)));
            setToolTipText("char:  '" + c + "', " + intTo0xHex(Character.getNumericValue(c)));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().decimalDataFont());
        }
    }

    /**
     * A label that displays the decimal value of an integer; a ToolTip displays the value in hex.
     */
    public static class IntAsDecimal extends DataLabel {

        private int n;

        public IntAsDecimal(Inspection inspection, int n) {
            super(inspection, "");
            this.n = n;
            updateText();
            redisplay();
        }

        public IntAsDecimal(Inspection inspection) {
            this(inspection, 0);
        }

        @Override
        public void redisplay() {
            setFont(style().decimalDataFont());
        }

        public void setValue(int n) {
            this.n = n;
            updateText();
        }

        private void updateText() {
            setText(Integer.toString(n));
            setWrappedToolTipText("int: " + intTo0xHex(n));
        }
    }

    /**
     * A label that displays the hex value of an  int; a ToolTip displays the value in decimal.
     */
    public static class IntAsHex extends DataLabel {

        private int n;

        public IntAsHex(Inspection inspection, int n) {
            super(inspection, "");
            this.n = n;
            updateText();
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().hexDataFont());
        }

        protected void setValue(int n) {
            this.n = n;
            updateText();
        }

        private void updateText() {
            setText(intTo0xHex(n));
            setToolTipText("int:  " + Integer.toString(n));
        }

    }

    public static class FloatAsText extends DataLabel {

        private float f;

        public FloatAsText(Inspection inspection, float f) {
            super(inspection, Float.toString(f), intTo0xHex(Float.floatToIntBits(f)));
            this.f = f;
            updateText();
            redisplay();
        }

        @Override
        public void redisplay() {
            // TODO: define a font for floats
            setFont(style().hexDataFont());
        }

        protected void updateText() {
            final String labelText = Float.toString(f);
            setText(labelText);
            setWrappedToolTipText(labelText);
        }

        protected void setValue(float f) {
            this.f = f;
            updateText();
        }
    }

    /**
     * A label that displays the decimal value of a long; a ToolTip displays the value in hex.
     */
    public static class LongAsDecimal extends DataLabel {
        long n;
        public LongAsDecimal(Inspection inspection, long n) {
            super(inspection, "");
            this.n = n;
            updateText();
            redisplay();
        }
        public LongAsDecimal(Inspection inspection) {
            this(inspection, 0);
        }
        @Override
        public void redisplay() {
            setFont(style().decimalDataFont());
        }
        public void setValue(long n) {
            this.n = n;
            updateText();
        }
        private void updateText() {
            setText(Long.toString(n));
            setWrappedToolTipText("long: " + longTo0xHex(n));
        }
    }

    /**
     * A label that displays the hex value of an unchanging long; a ToolTip displays the value in decimal.
     */
    public static final class LongAsHex extends DataLabel {
        public LongAsHex(Inspection inspection, long n) {
            super(inspection, longTo0xHex(n), "long:  " + Long.toString(n));
            redisplay();
        }
        @Override
        public void redisplay() {
            setFont(style().hexDataFont());
        }
    }

    public static class DoubleAsText extends DataLabel {
        private double f;

        public DoubleAsText(Inspection inspection, double f) {
            super(inspection, "", "");
            this.f = f;
            updateText();
            redisplay();
        }

        @Override
        public void redisplay() {
            // TODO: define a font for doubles
            setFont(style().hexDataFont());
        }

        private void updateText() {
            final String labelText = Double.toString(f);
            setText(labelText);
            setWrappedToolTipText(labelText);
        }

        protected void setValue(double f) {
            this.f = f;
            updateText();
        }
    }

    /**
     * A label that displays a memory address in hex; if a base address
     * is specified, then a ToolTip displays the offset from the base.
     */
    public static class AddressAsHex extends DataLabel {
        protected Address address;
        private final Address origin;

        public AddressAsHex(Inspection inspection, Address address) {
            this(inspection, address, null);
        }

        public AddressAsHex(Inspection inspection, Address addr, Address origin) {
            super(inspection, addr.toHexString());
            this.address = addr;
            this.origin = origin;
            enableDragSource();
            addMouseListener(new InspectorMouseClickAdapter(inspection()) {
                @Override
                public void procedure(final MouseEvent mouseEvent) {
                    switch (inspection().gui().getButton(mouseEvent)) {
                        case MouseEvent.BUTTON3: {
                            final InspectorPopupMenu menu = new InspectorPopupMenu("Address");
                            menu.add(actions().copyWord(address, "Copy address to clipboard"));
                            menu.add(actions().inspectMemory(address));
                            if (vm().watchpointManager() != null) {
                                menu.add(actions().setWordWatchpoint(address, null));
                            }
                            menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                            break;
                        }
                        case MouseEvent.BUTTON2: {
                            changeBiasState();
                            break;
                        }

                    }
                }
            });
            redisplay();
        }

        protected AddressAsHex(Inspection inspection, Address address, Address origin, InspectorMouseClickAdapter mouseListener) {
            super(inspection, address.toHexString());
            this.address = address;
            this.origin = origin;
            enableDragSource();
            addMouseListener(mouseListener);
            redisplay();
        }

        @Override
        public Transferable getTransferable() {
            return new InspectorTransferable.AddressTransferable(inspection(), address);
        }

        @Override
        public void redisplay() {
            setFont(style().hexDataFont());
            updateText();
        }

        protected void setValue(Address address) {
            this.address = address;
            updateText();
        }

        protected void changeBiasState() {
        }

        protected String toolTipText() {
            if (origin == null) {
                return null;
            }
            final long position = address.minus(origin).toLong();
            return "AsPosition: " + position + ", " +  longTo0xHex(position);
        }

        private void updateText() {
            setText(address.toHexString());
            setToolTipText(toolTipText());
        }
    }

    public static class BiasedStackAddressAsHex extends AddressAsHex {
        boolean biased;
        final StackBias bias;

        public BiasedStackAddressAsHex(Inspection inspection, Address address, StackBias bias) {
            super(inspection, address, null);
            this.bias = bias;
            biased = true;
        }

        public BiasedStackAddressAsHex(Inspection inspection, StackBias bias) {
            this(inspection, Address.zero(), bias);
        }

        private boolean useBias() {
            return bias != null && !bias.equals(StackBias.NONE);
        }

        @Override
        protected void changeBiasState() {
            if (!useBias()) {
                return;
            }
            if (biased) {
                biased = false;
                setValue(bias.unbias(address.asPointer()));
            } else {
                biased = true;
                setValue(bias.bias(address.asPointer()));
            }
        }

        @Override
        protected String toolTipText() {
            if (useBias()) {
                return biased ? "Biased" : "Unbiased";
            }
            return null;
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

    public static final class Percent extends DataLabel {

        private long numerator;
        private long denominator;

        public Percent(Inspection inspection, long numerator, long denominator) {
            super(inspection, null);
            assert denominator != 0;
            this.numerator = numerator;
            this.denominator = denominator;
            redisplay();
        }

        @Override
        public void redisplay() {
            setFont(style().decimalDataFont());
            updateText();
        }

        public void setValue(int numerator, int denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
            updateText();
        }

        private void updateText() {
            final long percent = 100 * numerator / denominator;
            setText(Long.toString(percent) + "%");
            setToolTipText(Long.toString(numerator) + " /  " + denominator);
        }
    }

}
