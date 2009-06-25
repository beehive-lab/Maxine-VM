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
package com.sun.max.ins.memory;

import javax.swing.*;

import com.sun.max.ins.*;


/**
 * Persistent preferences for viewing memory regions in the VM.
 *
 * @author Michael Van De Vanter
  */
public final class MemoryRegionsViewPreferences extends com.sun.max.ins.gui.TableColumnVisibilityPreferences<MemoryRegionsColumnKind> {

    private static MemoryRegionsViewPreferences globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing a table of memory regions.
     */
    static MemoryRegionsViewPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new MemoryRegionsViewPreferences(inspection);
        }
        return globalPreferences;
    }

    /**
     * @return a GUI panel suitable for setting global preferences for this kind of view.
     */
    public static JPanel globalPreferencesPanel(Inspection inspection) {
        return globalPreferences(inspection).getPanel();
    }

    /**
    * Creates a set of preferences specified for use by singleton instances, where local and
    * persistent global choices are identical.
    */
    private MemoryRegionsViewPreferences(Inspection inspection) {
        super(inspection, "memoryRegionsViewPrefs", MemoryRegionsColumnKind.class, MemoryRegionsColumnKind.VALUES);
    }

    @Override
    protected boolean canBeMadeInvisible(MemoryRegionsColumnKind columnType) {
        return columnType.canBeMadeInvisible();
    }

    @Override
    protected boolean defaultVisibility(MemoryRegionsColumnKind columnType) {
        return columnType.defaultVisibility();
    }

    @Override
    protected String label(MemoryRegionsColumnKind columnType) {
        return columnType.label();
    }
}
