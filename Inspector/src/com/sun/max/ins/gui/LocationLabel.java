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
/*VCSID=44729e52-41a6-406c-b991-33e8e715ec05*/
package com.sun.max.ins.gui;

import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.unsafe.*;


/**
 * A selectable, lightweight label for displaying memory
 * locations in the {@link TeleVM} with different interpretations.
 *
 * @author Michael Van De Vanter
 */
public abstract class LocationLabel extends InspectorLabel {

    protected int _value;
    protected final Address _base;
    private long _epoch = -1;

    /**
     * @return a menu containing actions suitable for a generic memory location.
     */
    protected InspectorMenu createLocationMenu() {
        final Address address = _base.plus(_value);
        final InspectorMenu menu = new InspectorMenu();
        menu.add(inspection().inspectionMenus().getCopyWordAction(address));
        menu.add(inspection().inspectionMenus().getInspectMemoryAction(address));
        menu.add(inspection().inspectionMenus().getInspectMemoryWordsAction(address));
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
        return Integer.toString(_value) + "(0x" + Integer.toHexString(_value) + ")";
    }

    /**
     * @return string describing the relative location in both decimal and hex, with a "+" prefix when non-negative
     */
    protected final String signedLocationText() {
        return (_value >= 0 ? "+" : "") + Integer.toString(_value) + "(0x" + Integer.toHexString(_value) + ")";
    }

    /**
     * @return string displaying the location as a hex address in the standard format.
     */
    protected final String addressText() {
        if (_base == null) {
            return "";
        }
        return "Address:  0x" + _base.plus(_value).toHexString();
    }

    protected LocationLabel(Inspection inspection, int value, Address base) {
        super(inspection, null);
        _value = value;
        _base = base;
        if (_base != null) {
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

    public final void refresh(long epoch) {
        if (epoch > _epoch) {
            updateText();
            _epoch = epoch;
        }
    }

    public final void setValue(int value) {
        _value = value;
        updateText();
    }

    /**
     * A label that displays, in hex, an address relative to an origin.
     * A right-button menu is available with some useful commands.
     */
    public static class AsAddressWithOffset extends LocationLabel {

        public AsAddressWithOffset(Inspection inspection, int offset, Address base) {
            super(inspection, offset, base);
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
            setText(_base.plus(_value).toHexString());
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

        public AsAddressWithPosition(Inspection inspection, int value, Address base) {
            super(inspection, value, base);
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
            setText(_base.plus(_value).toHexString());
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

        public AsPosition(Inspection inspection, int value, Address base) {
            super(inspection, value, base);
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
            setText(Integer.toString(_value));
            if (_base != null) {
                setToolTipText("Position: " + unsignedLocationText() + ", " + addressText());
            } else {
                setToolTipText("Position: " + unsignedLocationText());
            }
        }
    }

    /**
     * A label that displays, in decimal, an offset from some origin;
     * if an origin is specified, then a ToolTip shows that actual address and a right-button
     * menu is available with some useful commands.
     * The address does not update if contents at location get moved.
     */
    public static class AsOffset extends LocationLabel {

        public AsOffset(Inspection inspection, int offset) {
            this(inspection, offset, null);
        }

        public AsOffset(Inspection inspection, int offset, Address origin) {
            super(inspection, offset, origin);
            redisplay();
        }

        public void redisplay() {
            setFont(style().decimalDataFont());
            setForeground(style().decimalDataColor());
            setBackground(style().decimalDataBackgroundColor());
            updateText();
        }

        @Override
        protected void updateText() {
            setText((_value >= 0 ? "+" : "") + Integer.toString(_value));
            if (_base != null) {
                setToolTipText("Offset: " + signedLocationText() + ", " + addressText());
            } else {
                setToolTipText("Offset: " + signedLocationText());
            }
        }
    }

    /**
     * A label that displays a memory location in decimal indexed form (" <prefix>[<index>]");
     * A ToolTip shows that actual address and a right-button
     * menus is available with some useful commands.
     * The address does not update if contents at location get moved.
     */
    public static final class AsIndex extends LocationLabel {
        private final String _prefix;
        private final int _index;

        /**
         * A label that displays a memory location <base> + <offset> as "<prefix>[<index>]",
         * with a ToolTip giving more detail.
         */
        public AsIndex(Inspection inspection, String prefix, int index, int value, Address base) {
            super(inspection, value, base);
            _prefix = prefix;
            _index = index;
            redisplay();
        }

        public void redisplay() {
            setFont(style().decimalDataFont());
            setForeground(style().decimalDataColor());
            setBackground(style().decimalDataBackgroundColor());
            updateText();
        }

        @Override
        protected void updateText() {
            setText(_prefix  + "[" + _index + "]");
            if (_base != null) {
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

        private String _labelText;

        public AsTextLabel(Inspection inspection, Address base) {
            super(inspection, 0, base);
            redisplay();
        }

        public final void setLocation(String labelText, int value) {
            _labelText = (labelText == null) ? "" : labelText;
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
            setText(_labelText);
            setToolTipText(_labelText + " Position: " + unsignedLocationText() + ", " + addressText());
        }
    }


}
