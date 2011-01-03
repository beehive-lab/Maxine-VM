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
package com.sun.max.vm.cps.cir.variable;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.type.*;

/**
 * A variable that refers to a certain thread stack position,
 * i.e. either a local or an operand stack slot in the JVM spec sense
 *
 * @author Bernd Mathiske
 */
public abstract class CirSlotVariable extends CirVariable {

    private final int slotIndex;

    public CirSlotVariable(int serial, Kind kind, int slotIndex) {
        super(serial, kind);
        this.slotIndex = slotIndex;
    }

    public int slotIndex() {
        return slotIndex;
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitSlotVariable(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitSlotVariable(this, scope);
    }

}
