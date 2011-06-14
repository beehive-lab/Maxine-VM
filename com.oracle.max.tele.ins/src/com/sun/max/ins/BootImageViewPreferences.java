/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.ins;

import javax.swing.*;

import com.sun.max.ins.gui.*;
import com.sun.max.vm.*;

/**
 * Persistent preferences for viewing {@link VMConfiguration} information in the VM boot image.
  */
public final class BootImageViewPreferences extends TableColumnVisibilityPreferences<BootImageColumnKind> {

    private static BootImageViewPreferences globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing  {@link VMConfiguration} information in the VM boot image.
     */
    static BootImageViewPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new BootImageViewPreferences(inspection);
        }
        return globalPreferences;
    }

    // Prefix for all persistent column preferences in view
    private static final String BOOTIMAGE_COLUMN_PREFERENCE = "bootImageViewColumn";

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
    private BootImageViewPreferences(Inspection inspection) {
        super(inspection, BOOTIMAGE_COLUMN_PREFERENCE, BootImageColumnKind.values());
        // There are no view preferences beyond the column choices, so no additional machinery needed here.
    }
}
