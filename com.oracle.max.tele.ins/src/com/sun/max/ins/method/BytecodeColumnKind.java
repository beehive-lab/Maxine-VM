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
package com.sun.max.ins.method;

import com.sun.max.ins.debug.*;

/**
 * Defines the columns supported by the view; the content includes one of each
 * kind.  The visibility of them, however, may be changed by the user.
 *
 * @author Michael Van De Vanter
 */
public enum BytecodeColumnKind implements ColumnKind {

    TAG("Tag", "Tags:  IP, stack return, breakpoints", true, 20) {
        @Override
        public boolean canBeMadeInvisible() {
            return false;
        }
    },
    NUMBER("No.", "Index of instruction in the method", false, 15),
    BCI("bci", "Index of instruction in bytes from method start", true, 15),
    INSTRUCTION("Instr.", "Instruction mnemonic", true, -1),
    OPERAND1("Operand 1", "Instruction operand 1", true, -1),
    OPERAND2("Operand 2", "Instruction operand 2", true, -1),
    SOURCE_LINE("Line", "Line number in source code (may be approximate)", true, -1),
    BYTES("Bytes", "Instruction bytes", false, -1);

    private final String label;
    private final String toolTipText;
    private final boolean defaultVisibility;
    private final int minWidth;

    private BytecodeColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
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

    public boolean canBeMadeInvisible() {
        return true;
    }

    public boolean defaultVisibility() {
        return defaultVisibility;
    }

    public int minWidth() {
        return minWidth;
    }

    @Override
    public String toString() {
        return label;
    }

}

