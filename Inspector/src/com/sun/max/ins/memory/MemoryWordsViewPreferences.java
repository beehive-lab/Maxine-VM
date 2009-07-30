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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;


/**
 * Persistent preferences for viewing sequences of memory words in the VM.
 *
 * @author Michael Van De Vanter
  */
public class MemoryWordsViewPreferences extends com.sun.max.ins.gui.TableColumnVisibilityPreferences<MemoryWordsColumnKind> {

    private static MemoryWordsViewPreferences globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing these tables..
     */
    public static MemoryWordsViewPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new MemoryWordsViewPreferences(inspection);
        }
        return globalPreferences;
    }

    /**
     * A per-instance set of view preferences, initialized to the global preferences.
     * @param defaultPreferences the global defaults for this kind of view
     */
    public MemoryWordsViewPreferences(TableColumnVisibilityPreferences<MemoryWordsColumnKind> defaultPreferences) {
        super(defaultPreferences);
    }

    private MemoryWordsViewPreferences(Inspection inspection) {
        super(inspection, "memoryWordsViewPrefs", MemoryWordsColumnKind.class, MemoryWordsColumnKind.VALUES);
    }

    @Override
    protected boolean canBeMadeInvisible(MemoryWordsColumnKind columnType) {
        return columnType.canBeMadeInvisible();
    }

    @Override
    protected boolean defaultVisibility(MemoryWordsColumnKind columnType) {
        return columnType.defaultVisibility();
    }

    @Override
    protected String label(MemoryWordsColumnKind columnType) {
        return columnType.label();
    }
}
