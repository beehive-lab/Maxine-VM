/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ir;

import com.sun.c1x.value.ConstType;

/**
 * Condition codes used in conditionals.
 */
public enum Condition {
    eql("=="), neq("!="), lss("<"), leq("<="), gtr(">"), geq(">=");

    public final String _operator;


    private Condition(String operator) {
        _operator = operator;
    }

    /**
     * Negate this conditional.
     * @return the condition that represents the negation
     */
    public final Condition negate() {
        switch (this) {
            case eql: return neq;
            case neq: return eql;
            case lss: return geq;
            case leq: return gtr;
            case gtr: return leq;
            case geq: return lss;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Mirror this conditional (i.e. commute "a op b" to "b op' a")
     * @return the condition representing the equivalent commuted operation
     */
    public final Condition mirror() {
        switch (this) {
            case eql: return eql;
            case neq: return neq;
            case lss: return gtr;
            case leq: return geq;
            case gtr: return lss;
            case geq: return leq;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Checks if this conditional operation is commutative.
     * @return <code>true</code> if this operation is commutative
     */
    public final boolean isCommutative() {
        return this == eql || this == neq;
    }

    /**
     * Attempts to fold a comparison between two constants and return the result.
     * @param lt the constant on the left side of the comparison
     * @param rt the constant on the right side of the comparison
     * @return <code>Boolean.TRUE</code> if the comparison is known to be true,
     * <code>Boolean.FALSE</code> if the comparison is known to be false, <code>null</code> otherwise.
     */
    public Boolean foldCondition(ConstType lt, ConstType rt) {
        switch (lt.basicType()) {
            case Int: {
                int x = lt.asInt();
                int y = rt.asInt();
                switch (this) {
                    case eql: return x == y;
                    case neq: return x != y;
                    case lss: return x < y;
                    case leq: return x <= y;
                    case gtr: return x > y;
                    case geq: return x >= y;
                }
                break;
            }
            case Long: {
                long x = lt.asLong();
                long y = rt.asLong();
                switch (this) {
                    case eql: return x == y;
                    case neq: return x != y;
                    case lss: return x < y;
                    case leq: return x <= y;
                    case gtr: return x > y;
                    case geq: return x >= y;
                }
                break;
            }
            case Object: {
                Object x = lt.asObject();
                Object y = rt.asObject();
                switch (this) {
                    case eql: return x == y;
                    case neq: return x != y;
                }
                break;
            }
            // XXX: folding of floating comparisons should be possible
        }
        return null;
    }
}
