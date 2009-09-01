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

    public String name() {
        return "Alternate";
    }

    /*
     * Defaults
     */
    private final int defaultFontSize = 14;
    private final Font defaultFont = new Font("SansSerif", Font.PLAIN, defaultFontSize);
    private final Color defaultTextColor = Color.BLACK;

    /*
     * Text labels, in English
     */
    private final Font textLabelFont = new Font("SansSerif", Font.BOLD, defaultFontSize);

    /*
     * Hexadecimal data
     */
    private final Font hexDataFont = new Font("Monospaced", Font.PLAIN, defaultFontSize);

    /*
     * Java names
     */
    private final int javaNameSize = defaultFontSize + 2;
    private final Font javaNameFont = new Font("SansSerif", Font.PLAIN, javaNameSize);

    public int defaultTextFontSize() {
        return defaultFontSize;
    }

    public Font defaultFont() {
        return defaultFont;
    }

    @Override
    public Color defaultTextColor() {
        return defaultTextColor;
    }

    @Override
    public Font textLabelFont() {
        return textLabelFont;
    }

    @Override
    public Font hexDataFont() {
        return hexDataFont;
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
        return javaNameSize;
    }
    @Override
    public Font javaNameFont() {
        return javaNameFont;
    }
}
