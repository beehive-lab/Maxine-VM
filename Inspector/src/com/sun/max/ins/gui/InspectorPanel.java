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
 * A panel specialized for use in the Maxine Inspector.
 *
 * @author Michael Van De Vanter
 */
public class InspectorPanel extends JPanel implements Prober, InspectionHolder {

    private final Inspection _inspection;

    /**
     * Creates a new {@JPanel} for use in the {@link Inspection}.
     */
    public InspectorPanel(Inspection inspection) {
        super();
        _inspection = inspection;
    }

    /**
     * Creates a new {@JPanel} specialized for use in the Maxine Inspector.
     * @param inspection
     * @param layoutManager specifies layout for the panel.
     */
    public InspectorPanel(Inspection inspection, LayoutManager layoutManager) {
        super(layoutManager);
        _inspection = inspection;
        setOpaque(true);
        setBackground(inspection.style().defaultBackgroundColor());
    }

    public final Inspection inspection() {
        return _inspection;
    }

    public MaxVM maxVM() {
        return _inspection.maxVM();
    }

    public final InspectorStyle style() {
        return _inspection.style();
    }

    public final InspectionFocus focus() {
        return _inspection.focus();
    }

    public InspectionActions actions() {
        return _inspection.actions();
    }

    public void redisplay() {
    }

    public void refresh(long epoch, boolean force) {
    }

}
