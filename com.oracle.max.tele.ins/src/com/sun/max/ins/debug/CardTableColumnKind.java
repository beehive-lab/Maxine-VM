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
 * Defines the columns that can be displayed marks applied to a region of memory by a {@link MaxCardTable}.
 */
public enum CardTableColumnKind implements ColumnKind {

    CARD_TABLE_INDEX("byte #", "Index in map of the mark byte", true, 12) {
        @Override
        public boolean canBeMadeInvisible() {
            return false;
        }
    },
    WORD_INDEX("word #", "Word-based index of the word containing the byte", false, 8),
//    CARDTABLE_WORD_ADDRESS("Word address", "Address of word in the map containing the mark byte", false, 20),
//    CARD_TABLE_WORD_INDEX("byte/word #", "index of the mark byte in the map word containing the mark byte", false, 8),
//    WORD_BITS("word bits", "contents of the map word, with the mark bit bracketed", false, 16),
//    MARK_BIT("Mark", "The mark bit covering this word and the color of the object's mark", true, 16),
    HEAP_ADDRESS("Covered Address", "Heap address covered by this bitmap position", true, 20);

    private final String label;
    private final String toolTipText;
    private final boolean defaultVisibility;
    private final int minWidth;

    CardTableColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
        this.label = label;
        this.toolTipText = toolTipText;
        this.defaultVisibility = defaultVisibility;
        this.minWidth = minWidth;
    }

    public boolean isSupported(MaxVM vm) {
        return true;
    }

    public String label() {
        return label;
    }


    public String toolTipText() {
        return toolTipText;
    }


    public boolean canBeMadeInvisible() {
        return true;
    }


    public boolean defaultVisibility() {
        return true;
    }


    public int minWidth() {
        return -1;
    }

}
