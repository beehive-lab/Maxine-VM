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
package com.sun.c1x.ir;

import com.sun.c1x.value.*;
import com.sun.cri.ri.*;

/**
 * The {@code NewObjectArray} instruction represents an allocation of an object array.
 *
 * @author Thomas Wuerthinger
 */
public final class NewObjectArrayClone extends NewArray {

    final Value referenceArray;

    /**
     * Constructs a new NewObjectArray instruction.
     * @param elementClass the class of elements in this array
     * @param length the instruction producing the length of the array
     * @param stateBefore the state before the allocation
     * @param cpi the constant pool index
     * @param constantPool the constant pool
     */
    public NewObjectArrayClone(Value length, Value referenceArray, FrameState stateBefore) {
        super(length, stateBefore);
        this.referenceArray = referenceArray;
    }

    /**
     * Gets the exact type of this instruction.
     * @return the exact type of this instruction
     */
    @Override
    public RiType exactType() {
        return referenceArray.exactType();
    }

    @Override
    public RiType declaredType() {
        return referenceArray.declaredType();
    }

    public Value referenceArray() {
        return referenceArray;
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitNewObjectArrayClone(this);
    }
}
