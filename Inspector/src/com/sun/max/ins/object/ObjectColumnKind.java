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

    public static final IndexedSequence<ObjectColumnKind> VALUES = new ArraySequence<ObjectColumnKind>(values());
}
