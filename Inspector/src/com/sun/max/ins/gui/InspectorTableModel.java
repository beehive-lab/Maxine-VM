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

import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.tele.*;

/**
 * A table model specialized for Inspector's table-based views.
 *
 * @author Michael Van De Vanter
 */
public abstract class InspectorTableModel extends AbstractTableModel implements InspectionHolder {

    private final Inspection inspection;
    private final String tracePrefix;

    public InspectorTableModel(Inspection inspection) {
        this.inspection = inspection;
        this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
    }

    public final Inspection inspection() {
        return inspection;
    }

    public final MaxVM maxVM() {
        return inspection.maxVM();
    }

    public final MaxVMState vmState() {
        return inspection.vmState();
    }

    public final MaxCodeManager codeManager() {
        return inspection.codeManager();
    }

    public final MaxBreakpointFactory breakpointFactory() {
        return inspection.breakpointFactory();
    }

    public final MaxWatchpointFactory watchpointFactory() {
        return inspection.watchpointFactory();
    }

    public final boolean watchpointsEnabled() {
        return inspection.watchpointsEnabled();
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

    /**
     * @return default prefix text for trace messages; identifies the class being traced.
     */
    protected String tracePrefix() {
        return tracePrefix;
    }

    public void refresh() {
        fireTableDataChanged();
    }
}
