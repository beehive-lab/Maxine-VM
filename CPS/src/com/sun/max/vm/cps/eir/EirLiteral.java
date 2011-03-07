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
package com.sun.max.vm.cps.eir;

import com.sun.max.asm.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.value.*;

public class EirLiteral extends EirLocation.Constant {

    private final int index;
    private final Label label = new Label();

    /**
     * Creates an object representing a scalar or reference constant used by a target instruction.
     *
     * @param index
     *                if {@code value} is a scalar, then this is the index of the first byte of the scalar value in a
     *                byte array encoding all the scalar literals allocated from the same {@link EirLiteralPool}. If
     *                {@code value} is a reference, then this is the index of the reference in the object array holding
     *                all the reference literals allocated from the same {@link EirLiteralPool}.
     * @param value
     *                a scalar or reference value
     */
    EirLiteral(int index, Value value) {
        super(value);
        this.index = index;
    }

    public int index() {
        return index;
    }

    @Override
    public EirLocationCategory category() {
        return EirLocationCategory.LITERAL;
    }

    @Override
    public String toString() {
        return value().kind().character + ":" + index + "(" + value().toString() + ")";
    }

    public Label asLabel() {
        return label;
    }

    @Override
    public TargetLocation toTargetLocation() {
        if (value().kind().isReference) {
            return new TargetLocation.ReferenceLiteral(index);
        }
        return new TargetLocation.ScalarLiteral(index);
    }

}
