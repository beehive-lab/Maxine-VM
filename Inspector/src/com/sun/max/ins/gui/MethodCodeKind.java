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
package com.sun.max.ins.gui;


/**
 * Constants denoting the kinds of code that can be inspected for a method.
 *
 * @author Michael Van De Vanter
 */
public enum MethodCodeKind {
    MACHINE_CODE("Machine Code", true),
    BYTECODES("Bytecodes", false),
    JAVA_SOURCE("Java Source", false);

    private final String label;
    private final boolean defaultVisibility;

    private MethodCodeKind(String label, boolean defaultVisibility) {
        this.label = label;
        this.defaultVisibility = defaultVisibility;
    }

    /**
     * Determines if it the display of this source kind is implemented.
     *
     * TODO (mlvdv) This is a hack until source code viewing is implemented
     */
    public boolean isImplemented() {
        return this != JAVA_SOURCE;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Determines if this kind should be visible by default in new inspectors.
     */
    public boolean defaultVisibility() {
        return defaultVisibility;
    }

}
