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
    protected InspectorMenu createLocationMenu() {
        final Address address = origin.plus(value);
        final InspectorMenu menu = new InspectorMenu();
        menu.add(inspection().actions().copyWord(address, null));
        menu.add(inspection().actions().inspectMemory(address, null));
        menu.add(inspection().actions().inspectMemoryWords(address, null));
        if (maxVM().watchpointsEnabled()) {
            menu.add(inspection().actions().setWordWatchpoint(address, null));
        }
        return menu;
    }

    /**
     * Resets the text associated with the label.
     */
    protected abstract void updateText();

    /**
     * @return string describing the relative location in both decimal and hex, with no "+" prefix.
     */
    protected final String unsignedLocationText() {
        return Integer.toString(value) + "(0x" + Integer.toHexString(value) + ")";
    }

    /**
     * @return string describing the relative location in both decimal and hex, with a "+" prefix when non-negative
     */
    protected final String signedLocationText() {
        return (value >= 0 ? "+" : "") + Integer.toString(value) + "(0x" + Integer.toHexString(value) + ")";
    }

    /**
     * @return string displaying the location as a hex address in the standard format.
     */
    protected final String addressText() {
        if (origin == null) {
            return "";
        }
        return "Address:  0x" + origin.plus(value).toHexString();
    }

    protected LocationLabel(Inspection inspection, int value, Address origin) {
        super(inspection, null);
        this.value = value;
        this.origin = origin;
        if (origin != null) {
            addMouseListener(new InspectorMouseClickAdapter(inspection) {
                @Override
                public void procedure(final MouseEvent mouseEvent) {
                    switch (MaxineInspector.mouseButtonWithModifiers(mouseEvent)) {
                        case MouseEvent.BUTTON3: {
                            final InspectorMenu menu = createLocationMenu();
                            menu.popupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
            });
        }
    }

    public final void refresh(boolean force) {
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            updateText();
        }
    }

    public final void setValue(int value) {
        this.value = value;
        updateText();
    }

    public final void setValue(int value, Address origin) {
        this.value = value;
        this.origin = origin;
        updateText();
    }

    /**
     * A label that displays, in hex, an address relative to an origin.
     * A right-button menu is available with some useful commands.
     */
    public static class AsAddressWithOffset extends LocationLabel {

        public AsAddressWithOffset(Inspection inspection, int offset, Address origin) {
            super(inspection, offset, origin);
            redisplay();
        }

        public AsAddressWithOffset(Inspection inspection) {
            this(inspection, 0, Address.zero());
        }

        public final void redisplay() {
            setFont(style().hexDataFont());
            setForeground(style().hexDataColor());
            setBackground(style().hexDataBackgroundColor());
            updateText();
        }

        @Override
        protected final void updateText() {
            setText(origin.plus(value).toHexString());
            setToolTipText("Offset: " + signedLocationText() + ", " + addressText());
        }
    }

    /**
     * A label that displays, in hex, an address with non-negative position relative to an origin.
     * A right-button menu is available with some useful commands.
     * Maxine positions are non-negative and displayed without a '+' prefix.
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
            setForeground(style().hexDataColor());
            setBackground(style().hexDataBackgroundColor());
            updateText();
        }

        @Override
        protected final void updateText() {
            setText(origin.plus(value).toHexString());
            setToolTipText("Position: " + unsignedLocationText() + ", " + addressText());
        }
    }

    /**
     * A label that displays, in decimal, a non-negative position relative to some (optionally specified) origin.
     * If an origin is specified, then a ToolTip shows that actual address and a right-button
     * menu is available with some useful commands.
     * Maxine positions are non-negative and displayed without a '+' prefix.
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
            setForeground(style().decimalDataColor());
            setBackground(style().decimalDataBackgroundColor());
            updateText();
        }

        @Override
        protected final void updateText() {
            setText(Integer.toString(value));
            if (origin != null) {
                setToolTipText("Position: " + unsignedLocationText() + ", " + addressText());
            } else {
                setToolTipText("Position: " + unsignedLocationText());
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

        public AsOffset(Inspection inspection, int offset, Address origin, int indexScalingFactor) {
            super(inspection, offset, origin);
            this.indexScalingFactor = indexScalingFactor;
            redisplay();
        }

        public AsOffset(Inspection inspection, int offset) {
            this(inspection, offset, Address.zero(), 0);
        }

        public AsOffset(Inspection inspection) {
            this(inspection, 0, Address.zero(), 0);
        }

        public void redisplay() {
            setFont(style().decimalDataFont());
            setForeground(style().decimalDataColor());
            setBackground(style().decimalDataBackgroundColor());
            updateText();
        }

        @Override
        protected void updateText() {
            setText((value >= 0 ? "+" : "") + Integer.toString(value));
            StringBuilder text = new StringBuilder("Offset: ").append(signedLocationText());
            if (indexScalingFactor != 0) {
                text.append(", Index: ").append(value / indexScalingFactor);
            }
            if (origin != null) {
                text.append(", ").append(addressText());
            }
            setToolTipText(text.toString());
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
            setForeground(style().decimalDataColor());
            setBackground(style().decimalDataBackgroundColor());
            updateText();
        }

        @Override
        protected void updateText() {
            final int wordOffset = value / maxVM().wordSize().toInt();
            final String shortText = (wordOffset >= 0 ? "+" : "") + Integer.toString(wordOffset);
            setText(shortText);
            StringBuilder sb = new StringBuilder(50);
            sb.append("Offset: ");
            sb.append(shortText).append("(0x").append(Integer.toHexString(wordOffset)).append(") words");
            if (origin != null) {
                sb.append(", ").append(addressText());
            }
            setToolTipText(sb.toString());
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
         */
        public AsIndex(Inspection inspection, String prefix, int index, int value, Address origin) {
            super(inspection, value, origin);
            this.prefix = prefix;
            this.index = index;
            redisplay();
        }

        public void redisplay() {
            setFont(style().decimalDataFont());
            setForeground(style().decimalDataColor());
            setBackground(style().decimalDataBackgroundColor());
            updateText();
        }

        public void setValue(int index, int value, Address origin) {
            this.index = index;
            this.value = value;
            this.origin = origin;
            updateText();
        }

        @Override
        protected void updateText() {
            setText(prefix  + "[" + index + "]");
            if (origin != null) {
                setToolTipText("Offset: " + signedLocationText() + ", " + addressText());
            } else {
                setToolTipText("Offset: " + signedLocationText());
            }
        }
    }

    /**
     * A label that displays a textual label,
     * with associated memory location information (origin
     * and position) displayed in the ToolTip text.
     */
    public static class AsTextLabel extends LocationLabel {

        private String labelText;

        public AsTextLabel(Inspection inspection, Address origin) {
            super(inspection, 0, origin);
            redisplay();
        }

        public final void setLocation(String labelText, int value) {
            this.labelText = (labelText == null) ? "" : labelText;
            setValue(value);
        }

        public final void redisplay() {
            setFont(style().defaultTextFont());
            setForeground(style().defaultTextColor());
            setBackground(style().defaultTextBackgroundColor());
            updateText();
        }

        @Override
        protected final void updateText() {
            setText(labelText);
            setToolTipText(labelText + " Position: " + unsignedLocationText() + ", " + addressText());
        }
    }


}
