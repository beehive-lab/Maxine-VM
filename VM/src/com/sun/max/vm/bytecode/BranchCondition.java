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
package com.sun.max.vm.bytecode;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.vm.value.*;

/**
 * Enumerates the conditions for bytecode branch instructions. Unconditional branches are treated as a conditional case
 * where the condition is {@linkplain #NONE "none"}. The second operand of a branch instruction may be implicit in the
 * instruction opcode. For example, {@link Bytecodes#IFLT} implies a second operand value of 0.
 *
 * @author Laurent Daynes
 * @author Michael Bebenita
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
