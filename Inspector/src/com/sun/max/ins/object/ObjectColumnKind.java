/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.object;

import com.sun.max.ins.debug.*;

/**
 * Defines the columns supported by the object inspector.
 *
 * @author Michael Van De Vanter
 */
public enum ObjectColumnKind implements ColumnKind {

    TAG("Tag", "Tags: register targets, watchpoints, ...", true, 10) {
        @Override
        public boolean canBeMadeInvisible() {
            return false;
        }
    },
    ADDRESS("Addr.", "Memory address of field", false, -1),
    OFFSET("Offset", "Field location relative to object origin (bytes)", false, 20),
    TYPE("Type", "Type of field", true, 20),
    NAME("Field", "Field name", true, 20),
    VALUE("Value", "Field value", true, 20),
    BYTES("As bytes", "Field value as bytes", false, 20),
    REGION("Region", "Memory region pointed to by value", false, 20);

    private final String label;
    private final String toolTipText;
    private final boolean defaultVisibility;
    private final int minWidth;

    private ObjectColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
        this.label = label;
        this.toolTipText = toolTipText;
        this.defaultVisibility = defaultVisibility;
        this.minWidth = minWidth;
    }

    public String label() {
        return label;
    }

    public String toolTipText() {
        return toolTipText;
    }

    public int minWidth() {
        return minWidth;
    }

    public boolean defaultVisibility() {
        return defaultVisibility;
    }

    public boolean canBeMadeInvisible() {
        return true;
    }

    @Override
    public String toString() {
        return label;
    }

}
