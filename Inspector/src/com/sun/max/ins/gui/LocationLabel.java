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
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * A selectable, lightweight label for displaying memory
 * locations in the VM with different interpretations.
 *
 * @author Michael Van De Vanter
 */
public abstract class LocationLabel extends InspectorLabel {

    protected int value;
    protected Address origin;
    private MaxVMState lastRefreshedState = null;

    /**
     * @return a menu containing actions suitable for a generic memory location.
     */
    protected InspectorPopupMenu createLocationMenu() {
        final InspectorPopupMenu menu = new InspectorPopupMenu("Location");
        final Address address = origin.plus(value);
        menu.add(inspection().actions().copyWord(address, null));
        menu.add(inspection().actions().inspectMemoryWords(address));
        return menu;
    }

    /**
     * Resets the text associated with the label.
     */
    protected abstract void updateText();

    protected LocationLabel(Inspection inspection, int value, Address origin) {
        super(inspection, null);
        this.value = value;
        this.origin = origin;
        if (origin != null) {
            addMouseListener(new InspectorMouseClickAdapter(inspection) {
                @Override
                public void procedure(final MouseEvent mouseEvent) {
                    switch (inspection().gui().getButton(mouseEvent)) {
                        case MouseEvent.BUTTON3: {
                            createLocationMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
            });
        }
        enableDragSource();
    }

    public final void refresh(boolean force) {
        if (vm().state().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = vm().state();
            updateText();
        }
    }

    @Override
    public Transferable getTransferable() {
        if (origin == null) {
            return null;
        }
        final Address address = origin.plus(value);
        return new InspectorTransferable.AddressTransferable(inspection(), address);
    }

    public final void setValue(int value) {
        this.value = value;
        updateText();
    }

    public void setValue(int value, Address origin) {
        this.value = value;
        this.origin = origin;
        updateText();
    }

    /**
     * A label that displays, in hex, an address relative to an origin.
     * A right-button menu is available with some useful commands.
     */
    public static class AsAddressWithByteOffset extends LocationLabel {

        public AsAddressWithByteOffset(Inspection inspection, int offset, Address origin) {
            super(inspection, offset, origin);
            redisplay();
        }

        public AsAddressWithByteOffset(Inspection inspection) {
            this(inspection, 0, Address.zero());
        }

        public final void redisplay() {
            setFont(style().hexDataFont());
            updateText();
        }

        @Override
        protected final void updateText() {
            final Address address = origin.plus(value);
            setText(address.toHexString());
            setWrappedToolTipText(address.to0xHexString() + "<br>" + intToPlusMinusDecimalAndHex(value) + " bytes from origin");
        }
    }

    /**
     * A label that displays, in hex, an address with non-negative position relative to an origin.
     * A right-button menu is available with some useful commands.
     * VM positions are non-negative and displayed without a '+' prefix.
     * The address does not update if contents at location get moved.
     */
    public static class AsAddressWithPosition extends LocationLabel {

        public AsAddressWithPosition(Inspection inspection, int value, Address origin) {
            super(inspection, value, origin);
            assert value >= 0;
            redisplay();
        }

        public final void redisplay() {
            setFont(style().hexDataFont());
            updateText();
        }

        @Override
        protected final void updateText() {
            final Address address = origin.plus(value);
            setText(address.toHexString());
            setWrappedToolTipText(address.to0xHexString() + " (position " + intToDecimalAndHex(value) + " bytes from start)");
        }
    }

    /**
     * A label that displays, in decimal, a non-negative position relative to some (optionally specified) origin.
     * If an origin is specified, then a ToolTip shows that actual address and a right-button
     * menu is available with some useful commands.
     * VM positions are non-negative and displayed without a '+' prefix.
     * The address does not update if contents at location get moved.
     */
    public static class AsPosition extends LocationLabel {

        public AsPosition(Inspection inspection, int value) {
            this(inspection, value, null);
        }

        public AsPosition(Inspection inspection, int value, Address origin) {
            super(inspection, value, origin);
            redisplay();
        }

        public final void redisplay() {
            setFont(style().decimalDataFont());
            updateText();
        }

        @Override
        protected final void updateText() {
            setText(Integer.toString(value));
            if (origin != null) {
                setWrappedToolTipText(origin.plus(value).to0xHexString() + "<br>Position " + intToDecimalAndHex(value) + " bytes from start");
            } else {
                setWrappedToolTipText("Position " + intToDecimalAndHex(value));
            }
        }
    }

    /**
     * A label that displays, in decimal, an offset in bytes from some origin;
     * if an origin is specified, then a ToolTip shows that actual address and a right-button
     * menu is available with some useful commands.
     * The address does not update if contents at location get moved.
     */
    public static class AsOffset extends LocationLabel {

        private int indexScalingFactor;

        public AsOffset(Inspection inspection, int indexScalingFactor) {
            super(inspection, 0, Address.zero());
            this.indexScalingFactor = indexScalingFactor;
            redisplay();
        }

        public AsOffset(Inspection inspection) {
            super(inspection, 0, Address.zero());
            this.indexScalingFactor = 0;
            redisplay();
        }

        public void redisplay() {
            setFont(style().decimalDataFont());
            updateText();
        }

        @Override
        protected void updateText() {
            String toolTip = origin.plus(value).to0xHexString();
            if (indexScalingFactor != 0) {
                toolTip += "<br>index=" + Integer.toString(value / indexScalingFactor);
            }
            toolTip += "<br>" + intToPlusMinusDecimalAndHex(value) + " bytes from origin";
            setWrappedToolTipText(toolTip);
            setText(intToPlusMinusDecimal(value));

        }
    }

    /**
     * A label that displays, in decimal, an offset in words from some origin;
     * if an origin is specified, then a ToolTip shows that actual address and a right-button
     * menu is available with some useful commands.
     * Note that the offset is set as bytes, but displayed as words.
     * The address does not update if contents at location get moved.
     */
    public static class AsWordOffset extends LocationLabel {

        /**
         * Creates a label that displays an offset from an origin as words.
         *
         * @param offset offset in bytes from the origin
         * @param origin the base address for display
         */
        public AsWordOffset(Inspection inspection, int offset, Address origin) {
            super(inspection, offset, origin);
            redisplay();
        }

        public AsWordOffset(Inspection inspection, int offset) {
            this(inspection, offset, Address.zero());
        }

        public AsWordOffset(Inspection inspection) {
            this(inspection, 0, Address.zero());
        }

        public void redisplay() {
            setFont(style().decimalDataFont());
            updateText();
        }

        @Override
        protected void updateText() {
            final int wordOffset = value / vm().platform().nBytesInWord();
            setWrappedToolTipText(origin.plus(value).to0xHexString() + "<br>offset= " + intToPlusMinusDecimalAndHex(wordOffset) + " words from origin)");
            setText(intToPlusMinusDecimal(wordOffset));
        }
    }

    /**
     * A label that displays a memory location in decimal indexed form (" <prefix>[<index>]");
     * A ToolTip shows that actual address and a right-button
     * menus is available with some useful commands.
     * The address does not update if contents at location get moved.
     */
    public static class AsIndex extends LocationLabel {
        private final String prefix;
        private int index;

        /**
         * A label that displays a memory location <origin> + <offset> as "<prefix>[<index>]",
         * with a ToolTip giving more detail.
         *
         * @param prefix optional textual prefix to the value display
         * @param index the logical index of the value being displayed
         * @param offset the offset in bytes from origin of the value being displayed
         * @param origin the base location in VM memory from which the location of the value is computed
         */
        public AsIndex(Inspection inspection, String prefix, int index, int offset, Address origin) {
            super(inspection, offset, origin);
            this.prefix = prefix;
            this.index = index;
            redisplay();
        }

        public void redisplay() {
            setFont(style().decimalDataFont());
            updateText();
        }

        /**
         * Sets the value of the label to a new location in VM memory.
         *
         * @param index the logical index of the value being displayed
         * @param offset the offset in bytes from origin of the value being displayed
         * @param origin the base location in VM memory from which the location of the value is computed
         */
        public void setValue(int index, int offset, Address origin) {
            this.index = index;
            this.value = offset;
            this.origin = origin;
            updateText();
        }

        @Override
        protected void updateText() {
            setText(prefix  + "[" + index + "]");
            if (origin != null) {
                setWrappedToolTipText(origin.plus(value).to0xHexString() + "<br>" + intToPlusMinusDecimalAndHex(value) + " bytes from origin");
            } else {
                setWrappedToolTipText(intToPlusMinusDecimalAndHex(value) + " bytes from origin");
            }
        }
    }

    /**
     * A label that displays a textual label,
     * with associated memory location information (origin
     * and position) displayed in the ToolTip text.
     * Displays nothing if the label is null.
     */
    public static class AsTextLabel extends LocationLabel {

        private String labelText;

        public AsTextLabel(Inspection inspection, Address origin) {
            super(inspection, 0, origin);
            redisplay();
        }

        public final void setLocation(String labelText, int value) {
            this.labelText = labelText;
            setValue(value);
        }

        public final void redisplay() {
            setFont(style().defaultFont());
            updateText();
        }

        @Override
        protected final void updateText() {
            setText(labelText);
            if (labelText != null && !labelText.equals("")) {
                setWrappedToolTipText(origin.plus(value).to0xHexString() + " (position " + intToDecimalAndHex(value) + " bytes from start)");
            } else {
                setToolTipText(null);
            }
        }
    }

}
