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
 * Defines the columns that can be displayed describing a watchpoint in the VM.
 *
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public enum WatchpointsColumnKind implements ColumnKind {

    TAG("Tag", "Additional information", true, -1),
    START("Start", "Starting address", true, 20),
    SIZE("Size", "Size of watched region, in bytes", true, 6),
    END("End", "Ending address", false, 20),
    DESCRIPTION("Description", "Description of how watchpoint was created", true, 30),
    REGION("Region", "Memory region pointed to by value", false, 20),
    READ("R", "Should watchpoint trap when location is read?", true, 5),
    WRITE("W", "Should watchpoint trap when location is written?", true, 5),
    EXEC("X", "Should watchpoint trap when location is executed?", false, 5),
    GC("GC", "Active during GC?", true, 5),
    EAGER("Eager relocation", "Watchpoint relocation update mechanism", false, 5),
    TRIGGERED_THREAD("Thread", "Name of thread currently stopped at breakpoint", true, 1),
    ADDRESS_TRIGGERED("Address", "Address where watchpoint was triggered", true, 1),
    CODE_TRIGGERED("Code", "Access type which triggered watchpoint", true, 1);

    private final String label;
    private final String toolTipText;
    private final boolean defaultVisibility;
    private final int minWidth;

    private WatchpointsColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
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

    public static final IndexedSequence<WatchpointsColumnKind> VALUES = new ArraySequence<WatchpointsColumnKind>(values());
}
