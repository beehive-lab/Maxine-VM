/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.c1x.ir;

import com.oracle.max.criutils.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;

/**
 * Unsigned comparisons.
 *
 * @see UnsignedComparisons
 */
public final class UnsignedCompareOp extends Op2 {

    /**
     * One of the constants defined in {@link UnsignedComparisons} denoting the type of this comparison.
     */
    public final Condition condition;

    /**
     * Creates a new compare operation.
     *
     * @param opcode the bytecode opcode
     * @param op the comparison type
     * @param x the first input
     * @param y the second input
     */
    public UnsignedCompareOp(Condition condition, Value x, Value y) {
        super(CiKind.Int, -1, x, y);
        this.condition = condition;
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitUnsignedCompareOp(this);
    }

    @Override
    public void print(LogStream out) {
        out.print(Util.valueString(x())).
            print(' ').
            print(condition.toString()).
            print(' ').
            print(Util.valueString(y()));
    }

    @Override
    public int valueNumber() {
        return Util.hash2(condition.ordinal(), x, y);
    }
}
