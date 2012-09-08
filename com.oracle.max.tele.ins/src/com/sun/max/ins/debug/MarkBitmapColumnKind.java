/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.debug;

import com.sun.max.tele.*;

/**
 * Defines the columns that can be displayed the coloring being applied to a region of memory
 * by a {@link MaxMarkBitmap}.
 */
public enum MarkBitmapColumnKind implements ColumnKind {

    TAG("Tag", "Additional information", true, -1),
    BIT_INDEX("bit #", "Index of first bit of the mark", true, -1),
    BITMAP_WORD_ADDRESS("@bit", "Address of word containing first bit of mark", true, -1),
    HEAP_ADDRESS("Address", "Heap address associated to this the mark", true, -1),
    COLOR("Color", "Color of the mark", true, -1);

    private final String label;
    private final String toolTipText;
    private final boolean defaultVisibility;
    private final int minWidth;

    MarkBitmapColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
        this.label = label;
        this.toolTipText = toolTipText;
        this.defaultVisibility = defaultVisibility;
        this.minWidth = minWidth;
    }
    @Override
    public String label() {
        return label;
    }

    @Override
    public String toolTipText() {
        return toolTipText;
    }

    @Override
    public boolean canBeMadeInvisible() {
        return false;
    }

    @Override
    public boolean defaultVisibility() {
        return true;
    }

    @Override
    public int minWidth() {
        return -1;
    }

}
