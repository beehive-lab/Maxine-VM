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
 * Defines the columns that can be displayed describing a breakpoint in the VM.
 *
 * @author Michael Van De Vanter
 */
public enum BreakpointsColumnKind implements ColumnKind {
    TAG ("Kind", "[T]arget, [B]ytecode, [S]ource", true, 20),
    ENABLED("En", "Enabled?", true, 5),
    DESCRIPTION("Description", "How/where the breakpoint was created", true, -1) {
        @Override
        public boolean canBeMadeInvisible() {
            return false;
        }
    },
    LOCATION("Locn", "Location of the breakpoint", true, -1),
    CONDITION("Condition", "Optional conditional spec.", true, -1),
    TRIGGER_THREAD("Thread", "Name of thread currently stopped at breakpoint", true, 1);

    private final String label;
    private final String toolTipText;
    private final boolean defaultVisibility;
    private final int minWidth;

    private BreakpointsColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
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

    public static final IndexedSequence<BreakpointsColumnKind> VALUES = new ArraySequence<BreakpointsColumnKind>(values());
}
