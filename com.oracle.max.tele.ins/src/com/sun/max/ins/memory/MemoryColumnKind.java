/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.memory;

import com.sun.max.ins.debug.*;

/**
 * Definition for the columns that can be displayed describing
 * a region of memory word values in the VM.
 */
public enum MemoryColumnKind implements ColumnKind {
    TAG("Tag", "Additional information", true, -1),
    ADDRESS("Addr.", "Memory address", true, -1),
    MMTAG("GC", "Memory management information", false, -1),
    WORD("Word", "Offset relative to origin (words)", false, 10),
    OFFSET("Offset", "Offset relative to origin (bytes)", true, 10),
    VALUE("Value", "Value as a word", true, 20),
    BYTES("Bytes", "Word as bytes", false, 20),
    CHAR("Char", "Word as 8 bit chars", false, 20),
    UNICODE("Unicode", "Word as 16 bit chars", false, 20),
    FLOAT("Float", "Word as single precision float", false, 20),
    DOUBLE("Double", "Word as double precision float", false, 20),
    MM_STATUS("MM Stat", "Heap memory status", false, 20),
    MARK_BITS("Mark bits", "Contents of the Mark Bitmap word covering this address", false, 20),
    REGION("Region", "Memory region pointed to by value", true, 20);

    private final String columnLabel;
    private final String toolTipText;
    private final boolean defaultVisibility;
    private final int minWidth;

    private MemoryColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
        this.columnLabel = label;
        this.toolTipText = toolTipText;
        this.defaultVisibility = defaultVisibility;
        this.minWidth = minWidth;
    }

    public String label() {
        return columnLabel;
    }

    public String toolTipText() {
        return toolTipText;
    }

    public int minWidth() {
        return minWidth;
    }

    @Override
    public String toString() {
        return columnLabel;
    }

    public boolean canBeMadeInvisible() {
        return true;
    }

    public boolean defaultVisibility() {
        return defaultVisibility;
    }

}
