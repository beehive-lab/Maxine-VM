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
package com.sun.max.ins.debug;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;


/**
 * Persistent preferences for viewing thread local values in the VM.
 *
 * @author Michael Van De Vanter
  */
public final class ThreadLocalsViewPreferences extends TableColumnVisibilityPreferences<ThreadLocalsColumnKind> {

    private static ThreadLocalsViewPreferences _globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing a table of ThreadLocals.
     */
    static ThreadLocalsViewPreferences globalPreferences(Inspection inspection) {
        if (_globalPreferences == null) {
            _globalPreferences = new ThreadLocalsViewPreferences(inspection);
        }
        return _globalPreferences;
    }

    /**
     * @return a GUI panel suitable for setting global preferences for this kind of view.
     */
    public static JPanel globalPreferencesPanel(Inspection inspection) {
        return globalPreferences(inspection).getPanel();
    }

    /**
     * Creates a persistent, global set of preferences for view preferences.
     */
    private ThreadLocalsViewPreferences(Inspection inspection) {
        super(inspection, "threadLocalsViewPrefs", ThreadLocalsColumnKind.class, ThreadLocalsColumnKind.VALUES);
    }

    @Override
    protected boolean canBeMadeInvisible(ThreadLocalsColumnKind columnType) {
        return columnType.canBeMadeInvisible();
    }

    @Override
    protected boolean defaultVisibility(ThreadLocalsColumnKind columnType) {
        return columnType.defaultVisibility();
    }

    @Override
    protected String label(ThreadLocalsColumnKind columnType) {
        return columnType.label();
    }
}
