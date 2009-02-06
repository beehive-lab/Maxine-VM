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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.tele.*;

/**
 * A text field specialized for use in the {@link Inspector}.
 * Appears like a {@link JLabel}, except that the
 * text can be selected and copied by a user.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class InspectorLabel extends JTextField implements InspectionHolder, TextSearchable, Prober {

    private final Inspection _inspection;

    public InspectorLabel(Inspection inspection, String text) {
        super(text);
        _inspection = inspection;
        setEditable(false);
    }

    public InspectorLabel(Inspection inspection) {
        this(inspection, null);
    }

    public final Inspection inspection() {
        return _inspection;
    }

    public final InspectorStyle style() {
        return _inspection.style();
    }

    public final InspectionFocus focus() {
        return _inspection.focus();
    }

    public final InspectionActions actions() {
        return _inspection.actions();
    }

    public final TeleVM teleVM() {
        return _inspection.teleVM();
    }

    @Override
    public String getSearchableText() {
        return getText();
    }

    /**
     * Prevents the border from being drawn so that this field looks like a {@link JLabel}.
     */
    @Override
    protected void paintBorder(Graphics g) {
        // super.paintBorder(g);
    }
}
