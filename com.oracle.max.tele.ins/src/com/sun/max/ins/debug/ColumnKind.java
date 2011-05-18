/*
 * Copyright (c) 2009, 2009, Oracle and/or its affiliates. All rights reserved.
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
 * ColumnKind general interface; must be implemented by Enums.
 *
 * @author Hannes Payer
 * @author Michael Van De Vanter
 */
public interface ColumnKind {

    /**
     * @return text to appear in the column header
     */
    String label();

    /**
     * @return text to appear in the column header's toolTip, null if none specified.
     */
    String toolTipText();

    /**
     * @return whether this column kind can be made invisible; default true.
     */
    boolean canBeMadeInvisible();

    /**
     * Determines if this column should be visible by default; default true.
     */
    boolean defaultVisibility();

    /**
     * @return minimum width allowed for this column when resized by user; -1 if none specified.
     */
    int minWidth();

    int ordinal();
    String name();
    String toString();
}
