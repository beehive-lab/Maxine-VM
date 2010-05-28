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

import java.util.*;

import com.sun.max.ins.debug.*;

/**
 * Defines the columns that can be displayed describing a region of memory word values in the VM.
 *
 * @author Michael Van De Vanter
 */
public enum MemoryWordsColumnKind implements ColumnKind {
    TAG("Tag", "Additional information", true, -1),
    ADDRESS("Addr.", "Memory address", true, -1),
    WORD("Word", "Offset relative to origin (words)", false, 10),
    OFFSET("Offset", "Offset relative to origin (bytes)", true, 10),
    VALUE("Value", "Value as a word", true, 20),
    BYTES("Bytes", "Word as bytes", false, 20),
    CHAR("Char", "Word as 8 bit chars", false, 20),
    UNICODE("Unicode", "Word as 16 bit chars", false, 20),
    FLOAT("Float", "Word as single precision float", false, 20),
    DOUBLE("Double", "Word as double precision float", false, 20),
    REGION("Region", "Memory region pointed to by value", true, 20);

    private final String columnLabel;
    private final String toolTipText;
    private final boolean defaultVisibility;
    private final int minWidth;

    private MemoryWordsColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
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

    public static final List<MemoryWordsColumnKind> VALUES = Collections.unmodifiableList(Arrays.asList(values()));

}
