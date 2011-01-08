/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.vm.cps.tir;

import com.sun.max.vm.cps.tir.pipeline.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class TirConstant extends TirInstruction {
    private final Value value;

    @Override
    public Kind kind() {
        return value.kind();
    }

    public static TirConstant fromObject(Object object) {
        return new TirConstant(ReferenceValue.from(object));
    }

    public static TirConstant fromKind(Kind kind) {
        return new TirConstant(kind.zeroValue());
    }

    public TirConstant(Value value) {
        this.value = value;
    }

    @Override
    public void accept(TirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "#" + value.toString();
    }

    public Value value() {
        return value;
    }
}
