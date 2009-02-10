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
    ADDRESS("Addr.", "Memory address of field", -1),
    POSITION("Pos.", "Relative position of field (bytes)", 20),
    TYPE("Type", "Type of field", 20),
    NAME("Field", "Field name", 20),
    VALUE("Value", "Field value", 20),
    REGION("Region", "Memory region pointed to by value", 20);

    private final String _columnLabel;
    private final String _toolTipText;
    private final int _minWidth;

    private ObjectFieldColumnKind(String label, String toolTipText, int minWidth) {
        _columnLabel = label;
        _toolTipText = toolTipText;
        _minWidth = minWidth;
    }

    /**
     * @return text to appear in the column header
     */
    public String label() {
        return _columnLabel;
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

    @Override
    public String toString() {
        return _columnLabel;
    }

    public static final IndexedSequence<ObjectFieldColumnKind> VALUES = new ArraySequence<ObjectFieldColumnKind>(values());

}
