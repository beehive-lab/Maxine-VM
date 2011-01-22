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

public class TirNestedLocal extends TirInstruction {
    private final TirTreeCall call;
    private final int slot;

    public TirNestedLocal(TirTreeCall call, int slot) {
        this.call = call;
        this.slot = slot;
    }

    @Override
    public void accept(TirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "NESTED LOCAL slot: " + slot;
    }

    private Kind kind = Kind.VOID;

    @Override
    public Kind kind() {
        return kind;
    }

    @Override
    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public int slot() {
        return slot;
    }

    public TirTreeCall call() {
        return call;
    }
}
