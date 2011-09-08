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
package com.sun.max.vm.value;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.bytecode.*;

/**
 * Constants denoting comparison operations that can be performed between pairs of {@linkplain Value values} of the same
 * {@linkplain Value#kind() kind}. The comparison operations fall into one of these categories:
 * <dl>
 * <dt>Equality</dt>
 * <dd>Comparisons between two values for equality or inequality.</dd>
 * <dt>Arithmetic</dt>
 * <dd>Comparisons to determine if the first value is greater than, equal to or less than the second value. The
 * semantics of these comparisons with respect to signedness depends on the kinds of the values. For example, comparing
 * two {@code char} values makes an unsigned comparison where as comparing two {@code int} values is a signed
 * comparison. These comparisons can only be applied to value kinds that have well defined semantics for arithmetic
 * comparison.</dd>
 * <dt>Unsigned arithmetic</dt>
 * <dd>Comparisons to determine if the first value is greater than, equal to or less than the second value. These
 * comparisons always treat the values as unsigned and can only be applied to value kinds that have well defined
 * semantics for unsigned arithmetic comparison.</dd>
 * </dl>
 */
public enum ValueComparator {

    EQUAL {
        @Override
        public final ValueComparator complement() {
            return NOT_EQUAL;
        }

        @Override
        final boolean compare(Value left, Value right) {
            return left.equals(right);
        }
    },
    NOT_EQUAL {
        @Override
        public final ValueComparator complement() {
            return EQUAL;
        }

        @Override
        final boolean compare(Value left, Value right) {
            return !left.equals(right);
        }
    },
    LESS_THAN {
        @Override
        public final ValueComparator complement() {
            return GREATER_EQUAL;
        }

        @Override
        final boolean compare(Value left, Value right) {
            return left.compareTo(right) < 0;
        }
    },
    LESS_EQUAL {
        @Override
        public final ValueComparator complement() {
            return GREATER_THAN;
        }

        @Override
        final boolean compare(Value left, Value right) {
            return left.compareTo(right) <= 0;
        }
    },
    GREATER_EQUAL {
        @Override
        public final ValueComparator complement() {
            return LESS_THAN;
        }

        @Override
        final boolean compare(Value left, Value right) {
            return left.compareTo(right) >= 0;
        }
    },
    GREATER_THAN {
        @Override
        public final ValueComparator complement() {
            return LESS_EQUAL;
        }

        @Override
        final boolean compare(Value left, Value right) {
            return left.compareTo(right) > 0;
        }
    },
    UNSIGNED_GREATER_THAN {
        @Override
        public final ValueComparator complement() {
            return UNSIGNED_LESS_EQUAL;
        }

        @Override
        final boolean compare(Value left, Value right) {
            return left.unsignedCompareTo(right) > 0;
        }
    },

    UNSIGNED_LESS_EQUAL {
        @Override
        public final ValueComparator complement() {
            return UNSIGNED_GREATER_THAN;
        }

        @Override
        final boolean compare(Value left, Value right) {
            return left.unsignedCompareTo(right) <= 0;
        }
    },
    UNSIGNED_GREATER_EQUAL {
        @Override
        public final ValueComparator complement() {
            return UNSIGNED_LESS_THAN;
        }

        @Override
        final boolean compare(Value left, Value right) {
            return left.unsignedCompareTo(right) >= 0;
        }
    },
    UNSIGNED_LESS_THAN {
        @Override
        public final ValueComparator complement() {
            return UNSIGNED_GREATER_EQUAL;
        }

        @Override
        final boolean compare(Value left, Value right) {
            return left.unsignedCompareTo(right) < 0;
        }
    };

    public static final List<ValueComparator> VALUES = Arrays.asList(values());
    public abstract ValueComparator complement();

    /**
     * Evaluates the relationship between two values of the same kind.
     *
     * @param left the left value
     * @param right the right value
     * @return {@code true} if relationship denoted by this comparator holds between {@code left} and {@code right},
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code left}'s {@linkplain #kind() kind} is not the same as {@code right}'s
     *             kind or the semantics of comparison for the values' kind is undefined
     */
    public final boolean evaluate(Value left, Value right) {
        if (left.kind() != right.kind()) {
            throw new IllegalArgumentException("Cannot perform unsigned comparison between values of different kinds: " + left.kind() + " and " + right.kind());
        }
        return compare(left, right);
    }
    abstract boolean compare(Value left, Value right);

    public static ValueComparator fromBranchCondition(BranchCondition condition) {
        switch (condition) {
            case EQ:
                return EQUAL;
            case GE:
                return GREATER_EQUAL;
            case GT:
                return GREATER_THAN;
            case LE:
                return LESS_EQUAL;
            case LT:
                return LESS_THAN;
            case NE:
                return NOT_EQUAL;
            default:
                throw ProgramError.unknownCase();
        }
    }

    public String symbol() {
        switch (this) {
            case EQUAL:
                return "==";
            case GREATER_EQUAL:
                return ">=";
            case GREATER_THAN:
                return ">";
            case LESS_EQUAL:
                return "<=";
            case LESS_THAN:
                return "<";
            case NOT_EQUAL:
                return "!=";
            default:
                return toString();
        }
    }
}
