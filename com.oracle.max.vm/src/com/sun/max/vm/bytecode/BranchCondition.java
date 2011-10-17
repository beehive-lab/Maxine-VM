/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.bytecode;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.vm.value.*;

/**
 * Enumerates the conditions for bytecode branch instructions. Unconditional branches are treated as a conditional case
 * where the condition is {@linkplain #NONE "none"}. The second operand of a branch instruction may be implicit in the
 * instruction opcode. For example, {@link Bytecodes#IFLT} implies a second operand value of 0.
 */
public enum BranchCondition {
    /**
     * Constant denoting an unconditional jump.
     */
    NONE {
        @Override
        public BranchCondition opposite() {
            return NONE;
        }
    },

    /**
     * Constant denoting a jump is taken when the input operands of a branch instruction are equal.
     */
    EQ {
        @Override
        public BranchCondition opposite() {
            return NE;
        }
    },

    /**
     * Constant denoting a jump is taken when the input operands of a branch instruction are not equal.
     */
    NE {
        @Override
        public BranchCondition opposite() {
            return EQ;
        }
    },

    /**
     * Constant denoting a jump is taken when the first input operand of a branch instruction is less than the second
     * operand.
     */
    LT {
        @Override
        public BranchCondition opposite() {
            return GE;
        }
    },

    /**
     * Constant denoting a jump is taken when the first input operand of a branch instruction is greater than or equal
     * to the second operand.
     */
    GE {
        @Override
        public BranchCondition opposite() {
            return LT;
        }
    },

    /**
     * Constant denoting a jump is taken when the first input operand of a branch instruction is greater than the second
     * operand.
     */
    GT {
        @Override
        public BranchCondition opposite() {
            return LE;
        }
    },

    /**
     * Constant denoting a jump is taken when the first input operand of a branch instruction is less than or equal to
     * the second operand.
     */
    LE {
        @Override
        public BranchCondition opposite() {
            return GT;
        }
    };

    /**
     * The logical opposite of this {@link BranchCondition}, (e.g. the opposite of "<=" is ">".)
     */
    public abstract BranchCondition opposite();

    /**
     * Evaluates this {@link BranchCondition} on the specified values.
     */
    public boolean evaluate(Value a, Value b) {
        assert a.kind() == b.kind();
        switch (this) {
            case EQ:
                return a.equals(b);
            case GE:
                return a.compareTo(b) >= 0;
            case GT:
                return a.compareTo(b) > 0;
            case LE:
                return a.compareTo(b) <= 0;
            case LT:
                return a.compareTo(b) < 0;
            case NE:
                return !a.equals(b);
            default:
                assert false;
        }
        return false;
    }

    /**
     * Immutable (and thus sharable) view of the enum constants defined by this class.
     */
    public static final List<BranchCondition> VALUES = Arrays.asList(values());
}
