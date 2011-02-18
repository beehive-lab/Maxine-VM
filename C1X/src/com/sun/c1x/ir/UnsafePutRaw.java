/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.cri.ci.*;

/**
 * The {@code UnsafePutRaw} instruction represents an unsafe store operation.
 *
 * @author Ben L. Titzer
 */
public final class UnsafePutRaw extends UnsafeRawOp {

    Value value;

    /**
     * Constructs a new UnsafeGetRaw instruction.
     * @param opKind the kind of the operation
     * @param addr the instruction generating the base address
     * @param value the instruction generating the value to store
     */
    public UnsafePutRaw(CiKind opKind, Value addr, Value value) {
        super(opKind, addr, false);
        this.value = value;
    }

    /**
     * Gets the instruction generating the value that will be stored.
     * @return the instruction generating the value
     */
    public Value value() {
        return value;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        super.inputValuesDo(closure);
        value = closure.apply(value);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitUnsafePutRaw(this);
    }

    public int log2scale() {
        return log2Scale;
    }
}
