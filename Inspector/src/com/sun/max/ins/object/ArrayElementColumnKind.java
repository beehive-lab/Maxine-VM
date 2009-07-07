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
package com.sun.max.ins.object;

import com.sun.max.collect.*;


/**
 * Defines the columns supported by the object inspector when viewing array elements.
 *
 * @author Michael Van De Vanter
 */
public enum ArrayElementColumnKind {
    TAG("Tag", "Tags: register targets, watchpoints, ...", true, -1) {
        @Override
        public boolean canBeMadeInvisible() {
            return false;
        }
    },
    ADDRESS("Addr.", "Memory address of element", true, -1),
    POSITION("Pos.", "Relative position of element (bytes)", true, 10),
    NAME("Elem.", "Array element name", true, 10),
    VALUE("Value", "Element value", true, 5),
    REGION("Region", "Memory region pointed to by value", true, -1);

    private final String label;
    private final String toolTipText;
    private final boolean defaultVisibility;
    private final int minWidth;

    private ArrayElementColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
        this.label = label;
        this.toolTipText = toolTipText;
        this.defaultVisibility = defaultVisibility;
        this.minWidth = minWidth;
    }

    /**
     * @return text to appear in the column header
     */
    public String label() {
        return label;
    }

    /**
     * @return text to appear in the column header's toolTip, null if none specified
     */
    public String toolTipText() {
        return toolTipText;
    }

    /**
     * @return minimum width allowed for this column when resized by user; -1 if none specified.
     */
    public int minWidth() {
        return minWidth;
    }

    /**
     * Determines if this column should be visible by default; default true.
     */
    public boolean defaultVisibility() {
        return defaultVisibility;
    }

    /**
     * @return whether this column kind can be made invisible; default true.
     */
    public boolean canBeMadeInvisible() {
        return true;
    }

    @Override
    public String toString() {
        return label;
    }

    public static final IndexedSequence<ArrayElementColumnKind> VALUES = new ArraySequence<ArrayElementColumnKind>(values());

}
