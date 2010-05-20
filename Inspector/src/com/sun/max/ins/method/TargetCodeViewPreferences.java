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
package com.sun.max.ins.method;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;

/**
 * Persistent preferences for viewing disassembled target code in the VM.
 *
 * @author Michael Van De Vanter
  */
public class TargetCodeViewPreferences extends TableColumnVisibilityPreferences<TargetCodeColumnKind> {

    private static TargetCodeViewPreferences globalPreferences;

    public static TargetCodeViewPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new TargetCodeViewPreferences(inspection);
        }
        return globalPreferences;
    }

    // Prefix for all persistent column preferences in view
    private static final String TARGET_CODE_COLUMN_PREFERENCE = "targetCodeViewColumn";

    /**
     * Creates the global, persistent set of preferences.
     */
    private TargetCodeViewPreferences(Inspection inspection) {
        super(inspection, TARGET_CODE_COLUMN_PREFERENCE, TargetCodeColumnKind.VALUES);
        // There are no view preferences beyond the column choices, so no additional saving needed here.
    }

    /**
     * Creates a non-persistent set of preferences by cloning another set of preferences (i.e. the globally persistent set).
     */
    public TargetCodeViewPreferences(TableColumnVisibilityPreferences<TargetCodeColumnKind> otherPreferences) {
        super(otherPreferences);
    }
}
