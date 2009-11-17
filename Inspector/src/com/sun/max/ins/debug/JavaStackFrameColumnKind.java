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
package com.sun.max.ins.debug;

import com.sun.max.collect.*;

/**
 * Defines the columns that can be displayed describing a Java stack frame in the VM.
 *
 * @author Michael Van De Vanter
 */
public enum JavaStackFrameColumnKind implements ColumnKind {

    TAG("Tag", "Tags: register targets, watchpoints, ...", true, -1) {
        @Override
        public boolean canBeMadeInvisible() {
            return false;
        }
    },
    NAME("Name", "Abstract name of the frame slot", true, -1),
    ADDRESS ("Address", "Absolute memory location of the frame slot", false, -1),
    OFFSET_SP ("Offset(SP)", "Offset in bytes from the Stack Pointer of the frame slot", false, -1),
    OFFSET_FP ("Offset(FP)", "Offset in bytes from the Frame Pointer of the frame slot", false, -1),
    VALUE("Value", "value as a word of the frame slot", true, 20),
    REGION("Region", "Memory region pointed to by value of the frame slot", false, 20);

    private final String label;
    private final String toolTipText;
    private final boolean defaultVisibility;
    private final int minWidth;

    private JavaStackFrameColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
        this.label = label;
        this.toolTipText = toolTipText;
        this.defaultVisibility = defaultVisibility;
        this.minWidth = minWidth;
        assert defaultVisibility || canBeMadeInvisible();
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

    @Override
    public String toString() {
        return label;
    }

    public boolean canBeMadeInvisible() {
        return true;
    }

    public boolean defaultVisibility() {
        return defaultVisibility;
    }

    public static final IndexedSequence<JavaStackFrameColumnKind> VALUES = new ArraySequence<JavaStackFrameColumnKind>(values());

}
