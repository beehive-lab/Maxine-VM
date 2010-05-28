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
package com.sun.max.ins.method;

import java.util.*;

import com.sun.max.ins.debug.*;

/**
 * Defines the columns supported by the target code inspector; the view includes one of each
 * kind.  The visibility of them, however, may be changed by the user.
 *
 * @author Michael Van De Vanter
 */
public enum TargetCodeColumnKind implements ColumnKind {

    TAG("Tag", "Tags:  IP, stack return, breakpoints", true, 20) {
        @Override
        public boolean canBeMadeInvisible() {
            return false;
        }
    },
    NUMBER("No.", "Index of instruction in the method", false, 15),
    ADDRESS("Addr.", "Memory address of target instruction start", false, -1),
    POSITION("Pos.", "Position in bytes of target instruction start", false, 20),
    LABEL("Label", "Labels synthesized during disassembly", true, -1),
    INSTRUCTION("Instr.", "Instruction mnemonic", true, -1),
    OPERANDS("Operands", "Instruction operands", true, -1),
    SOURCE_LINE("Line", "Line number in source code (may be approximate)", true, -1),
    BYTES("Bytes", "Instruction bytes", false, -1);

    private final String label;
    private final String toolTipText;
    private final boolean defaultVisibility;
    private final int minWidth;

    private TargetCodeColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
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

    public static final List<TargetCodeColumnKind> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
}

