/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

}
