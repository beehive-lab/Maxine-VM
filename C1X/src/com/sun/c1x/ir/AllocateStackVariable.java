/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;

/**
 * Instruction implementing the semantics of {@link Bytecodes#ALLOCSTKVAR}.
 *
 * @author Doug Simon
 */
public final class AllocateStackVariable extends Instruction {

    /**
     * The value that will be used to initialize the stack slot by allocated by this instruction.
     */
    private Value value;

    /**
     * Creates a new LoadStackAddress instance.
     */
    public AllocateStackVariable(Value value) {
        super(CiKind.Word);
        setFlag(Flag.NonNull);
        this.value = value;
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitLoadStackAddress(this);
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply to each instruction
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        value = closure.apply(value);
    }

    /**
     * Gets the instruction that produced the size argument.
     */
    public Value value() {
        return value;
    }
}
