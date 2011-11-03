/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.nodes.spi;

import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.DeoptimizeNode.DeoptAction;
import com.oracle.max.graal.nodes.calc.*;
import com.sun.cri.ci.*;

public abstract class LIRGeneratorTool extends ValueVisitor {
    public abstract CiTarget target();

    public abstract CiValue operand(ValueNode object);
    public abstract CiVariable newVariable(CiKind kind);
    public abstract CiValue setResult(ValueNode x, CiValue operand);

    public abstract CiVariable emitMove(CiValue input);
    public abstract void emitMove(CiValue src, CiValue dst);
    public abstract CiVariable emitLoad(CiAddress loadAddress, CiKind kind, boolean canTrap);
    public abstract void emitStore(CiAddress storeAddress, CiValue input, CiKind kind, boolean canTrap);
    public abstract CiVariable emitLea(CiAddress address);

    public abstract CiVariable emitNegate(CiValue input);
    public abstract CiVariable emitAdd(CiValue a, CiValue b);
    public abstract CiVariable emitSub(CiValue a, CiValue b);
    public abstract CiVariable emitMul(CiValue a, CiValue b);
    public abstract CiVariable emitDiv(CiValue a, CiValue b);
    public abstract CiVariable emitRem(CiValue a, CiValue b);
    public abstract CiVariable emitUDiv(CiValue a, CiValue b);
    public abstract CiVariable emitURem(CiValue a, CiValue b);

    public abstract CiVariable emitAnd(CiValue a, CiValue b);
    public abstract CiVariable emitOr(CiValue a, CiValue b);
    public abstract CiVariable emitXor(CiValue a, CiValue b);

    public abstract CiVariable emitShl(CiValue a, CiValue b);
    public abstract CiVariable emitShr(CiValue a, CiValue b);
    public abstract CiVariable emitUShr(CiValue a, CiValue b);

    public abstract CiVariable emitConvert(ConvertNode.Op opcode, CiValue inputVal);

    public abstract void emitDeoptimizeOn(Condition of, DeoptAction action);
}
