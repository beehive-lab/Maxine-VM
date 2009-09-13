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
import javax.swing.border.*;

import com.sun.max.ins.*;
import com.sun.max.tele.*;

/**
 * A label specialized for use in the {@link Inspector}.
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class InspectorLabel extends JLabel implements InspectionHolder, TextSearchable, Prober {

    private final Inspection inspection;

    /**
     * A label for use in the inspector, by default not opaque.
     *
     * @param text label text
     * @param toolTipText text for ToolTips
     */
    public InspectorLabel(Inspection inspection, String text, String toolTipText) {
        super(text);
        this.inspection = inspection;
        setToolTipText(toolTipText);
        setOpaque(false);
    }

    /**
     * A label for use in the inspector: can be selected and copied.
     * @param text label text
     */
    public InspectorLabel(Inspection inspection, String text) {
        this(inspection, text, null);
    }

    /**
     * A label for use in the inspector: can be selected and copied.
     */
    public InspectorLabel(Inspection inspection) {
        this(inspection, null, null);
    }

    public final Inspection inspection() {
        return inspection;
    }

    public final MaxVM maxVM() {
        return inspection.maxVM();
    }

    public final MaxVMState maxVMState() {
        return inspection.maxVM().maxVMState();
    }

    public InspectorGUI gui() {
        return inspection.gui();
    }

    public final InspectorStyle style() {
        return inspection.style();
    }

    public final InspectionFocus focus() {
        return inspection.focus();
    }

    public final InspectionActions actions() {
        return inspection.actions();
    }

    public String getSearchableText() {
        return getText();
    }

    /**
     * Wraps this label in a {@link JPanel} with a border.
     * This is needed, since this type of label cannot
     * display user supplied borders correctly
     *
     * @return a newly created panel containing this label, with border.
     * @see JComponent#setBorder(Border)
     */
    public JPanel getBorderWrappedPanel(Border border) {
        final JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(border);
        panel.add(this, BorderLayout.CENTER);
        panel.setToolTipText(getToolTipText());
        return panel;
    }

    /**
     * Prevents the border from being drawn so that this field looks like a {@link JLabel}.
     */
    @Override
    protected void paintBorder(Graphics g) {
        //super.paintBorder(g);
    }
}
