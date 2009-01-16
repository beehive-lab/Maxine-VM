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
 * Base class for {@link Inspector} text fields.
 * Appears like a {@link JLabel}, except that the
 * text can be selected and copied by a user.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class InspectorLabel extends JTextField implements TextSearchable, Prober {

    private final Inspection _inspection;

    /**
     * @return the {@link Inspection} session in which this label is participating.
     */
    public final Inspection inspection() {
        return _inspection;
    }

    public InspectorLabel(Inspection inspection) {
        this(inspection, null);
    }

    public InspectorLabel(Inspection inspection, String text) {
        super(text);
        _inspection = inspection;
        setEditable(false);
    }

    /**
     * @return the current {@link InspectorStyle} being followed for visual display parameters.
     */
    public final InspectorStyle style() {
        return _inspection.style();
    }

    /**
     * @return the VM being inspected in this session
     */
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
