/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.c1x.intrinsics;

import static com.oracle.max.cri.intrinsics.IntrinsicIDs.*;

import com.oracle.max.cri.intrinsics.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * Implementation of Maxine intrinsics for C1X.
 * Since C1X does not allow the addition of HIR instructions by a project other than the main C1X project,
 * the actual HIR classes implementing the intrinsics are still in the C1X project.
 */
public class C1XIntrinsicImplementations {
    public static class UnsignedCompareIntrinsic implements C1XIntrinsicImpl {
        public final Condition condition;

        public UnsignedCompareIntrinsic(Condition condition) {
            this.condition = condition;
        }

        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            CiKind kind = args[0].kind;
            assert args.length == (MutableFrameState.isTwoSlot(kind) ? 4 : 2);
            Value left = args[0];
            Value right = args[MutableFrameState.isTwoSlot(kind) ? 2 : 1];
            return b.append(new UnsignedCompareOp(condition, left, right));
        }
    }

    public static class UnsignedDivideIntrinsic implements C1XIntrinsicImpl {
        public final int opcode;

        public UnsignedDivideIntrinsic(int opcode) {
            this.opcode = opcode;
        }

        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            CiKind kind = args[0].kind;
            assert args.length == (MutableFrameState.isTwoSlot(kind) ? 4 : 2);
            Value left = args[0];
            Value right = args[MutableFrameState.isTwoSlot(kind) ? 2 : 1];
            return b.append(new ArithmeticOp(opcode, kind, left, right, false, stateBefore));
        }
    }

    public static class ConvertIntrinsic implements C1XIntrinsicImpl {
        public final Convert.Op opcode;

        public ConvertIntrinsic(Convert.Op opcode) {
            this.opcode = opcode;
        }

        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            return b.append(new Convert(opcode, args[0], target.signature().returnKind(false)));
        }
    }

    public static class MemoryBarrierIntrinsic implements C1XIntrinsicImpl {
        @Override
        public Value createHIR(GraphBuilder b, RiMethod target, Value[] args, boolean isStatic, FrameState stateBefore) {
            assert args.length == 1;
            if (!args[0].isConstant() || args[0].kind != CiKind.Int) {
                throw new CiBailout("instrinc parameter for barrier must be compile time integer constant");
            }
            int barriers = args[0].asConstant().asInt();

            int explicitMemoryBarriers = barriers & ~b.compilation.target.arch.implicitMemoryBarriers;
            if (explicitMemoryBarriers != 0) {
                b.append(new MemoryBarrier(explicitMemoryBarriers));
            }
            return null;
        }
    }


    public static void initialize(IntrinsicImpl.Registry registry) {
        registry.add(UCMP_AT, new UnsignedCompareIntrinsic(Condition.AT));
        registry.add(UCMP_AE, new UnsignedCompareIntrinsic(Condition.AE));
        registry.add(UCMP_BT, new UnsignedCompareIntrinsic(Condition.BT));
        registry.add(UCMP_BE, new UnsignedCompareIntrinsic(Condition.BE));

        registry.add(UDIV, new UnsignedDivideIntrinsic(Op2.UDIV));
        registry.add(UREM, new UnsignedDivideIntrinsic(Op2.UREM));

        registry.add(MEMBAR, new MemoryBarrierIntrinsic());

        registry.add("java.lang.Float", "floatToRawIntBits", "(F)I", new ConvertIntrinsic(Convert.Op.MOV_F2I));
        registry.add("java.lang.Float", "intBitsToFloat", "(I)F", new ConvertIntrinsic(Convert.Op.MOV_I2F));
        registry.add("java.lang.Double", "doubleToRawLongBits", "(D)J", new ConvertIntrinsic(Convert.Op.MOV_D2L));
        registry.add("java.lang.Double", "longBitsToDouble", "(J)D", new ConvertIntrinsic(Convert.Op.MOV_L2D));
    }
}
