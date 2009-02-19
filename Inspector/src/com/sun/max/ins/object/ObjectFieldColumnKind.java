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
 * Defines the columns supported by the object inspector when viewing object fields.
 *
 * @author Michael Van De Vanter
 */
public enum ObjectFieldColumnKind {
    TAG("Tag", "Tags: register targets, watchpoints, ...", true, -1) {
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
    REGION("Region", "Memory region pointed to by value", false, 20);

    private final String _label;
    private final String _toolTipText;
    private final boolean _defaultVisibility;
    private final int _minWidth;

    private ObjectFieldColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
        _label = label;
        _toolTipText = toolTipText;
        _defaultVisibility = defaultVisibility;
        _minWidth = minWidth;
    }

    /**
     * @return text to appear in the column header
     */
    public String label() {
        return _label;
    }

    /**
     * @return text to appear in the column header's toolTip, null if none specified
     */
    public String toolTipText() {
        return _toolTipText;
    }

    /**
     * @return minimum width allowed for this column when resized by user; -1 if none specified.
     */
    public int minWidth() {
        return _minWidth;
    }

    /**
     * Determines if this column should be visible by default; default true.
     */
    public boolean defaultVisibility() {
        return _defaultVisibility;
    }

    /**
     * @return whether this column kind can be made invisible; default true.
     */
    public boolean canBeMadeInvisible() {
        return true;
    }

    @Override
    public String toString() {
        return _label;
    }

    public static final IndexedSequence<ObjectFieldColumnKind> VALUES = new ArraySequence<ObjectFieldColumnKind>(values());

}
