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

import java.awt.*;

import com.sun.max.ins.*;

/**
 * A style for playing with different parameters; not ready to be used.
 *
 * @author Michael Van De Vanter
 *
 */
public class AlternateInspectorStyle extends InspectorStyleAdapter {

    public AlternateInspectorStyle(Inspection inspection) {
        super(inspection);
    }

    @Override
    public String name() {
        return "Alternate";
    }

    /*
     * Defaults
     */
    private final int _defaultFontSize = 14;
    private final Font _defaultFont = new Font("SansSerif", Font.PLAIN, _defaultFontSize);
    private final Color _defaultTextColor = Color.BLACK;

    /*
     * Text labels, in English
     */
    private final Font _textLabelFont = new Font("SansSerif", Font.BOLD, _defaultFontSize);

    /*
     * Hexadecimal data
     */
    private final Font _hexDataFont = new Font("Monospaced", Font.PLAIN, _defaultFontSize);

    /*
     * Java names
     */
    private final int _javaNameSize = _defaultFontSize + 2;
    private final Font _javaNameFont = new Font("SansSerif", Font.PLAIN, _javaNameSize);

    public int defaultTextFontSize() {
        return _defaultFontSize;
    }

    public Font defaultFont() {
        return _defaultFont;
    }

    @Override
    public Color defaultTextColor() {
        return _defaultTextColor;
    }

    @Override
    public Font textLabelFont() {
        return _textLabelFont;
    }

    @Override
    public Font hexDataFont() {
        return _hexDataFont;
    }

    @Override
    public int hexDataFontSize() {
        return defaultTextFontSize();
    }

    @Override
    public Color hexDataColor() {
        return defaultTextColor();
    }

    @Override
    public int javaNameFontSize() {
        return _javaNameSize;
    }
    @Override
    public Font javaNameFont() {
        return _javaNameFont;
    }
}
