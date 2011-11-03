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
import com.sun.cri.bytecode.*;
import com.sun.cri.ri.*;

/**
 * Instruction implementing the semantics of {@link Bytecodes#ALLOCA}.
 */
public final class StackAllocate extends Instruction {

    private Value size;
    public final RiType declaredType;

    /**
     * Creates a new StackAllocate instance.
     */
    public StackAllocate(Value size, RiType declaredType) {
        super(declaredType.kind(false));
        this.size = size;
        this.declaredType = declaredType;
        setFlag(Flag.NonNull);
        eliminateNullCheck();
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitStackAllocate(this);
    }

    @Override
    public void inputValuesDo(ValueClosure closure) {
        size = closure.apply(size);
    }

    /**
     * Gets the instruction that produced the size argument.
     */
    public Value size() {
        return size;
    }

    @Override
    public RiResolvedType declaredType() {
        return (declaredType instanceof RiResolvedType) ? (RiResolvedType) declaredType : null;
    }

    @Override
    public void print(LogStream out) {
        out.print("alloca(").print(Util.valueString(size)).print(")");
    }
}
