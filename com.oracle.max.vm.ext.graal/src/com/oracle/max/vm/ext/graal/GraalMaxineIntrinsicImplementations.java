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
package com.oracle.max.vm.ext.graal;

import static com.oracle.max.cri.intrinsics.IntrinsicIDs.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.oracle.max.cri.intrinsics.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.DeoptimizeNode.DeoptAction;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.platform.*;

public class GraalMaxineIntrinsicImplementations {
    protected static class NotImplementedIntrinsic extends GraalIntrinsicImpl {
        @Override
        public ValueNode createGraph(StructuredGraph graph, RiResolvedMethod method, RiRuntime runtime, NodeList<ValueNode> args) {
            throw new UnsupportedOperationException("intrinsic not implemented");
        }
    }

    protected static class MembarIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, int barriers) {
            return graph.add(new MembarNode(barriers));
        }
    }

    protected static class NormalizeCompareIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, int opcode, ValueNode x, ValueNode y) {
            switch (opcode) {
                // Checkstyle: off
                case Bytecodes.LCMP: assert x.kind() == CiKind.Long; break;
                case Bytecodes.DCMPG:
                case Bytecodes.DCMPL: assert x.kind() == CiKind.Double; break;
                case Bytecodes.FCMPG:
                case Bytecodes.FCMPL: assert x.kind() == CiKind.Float; break;
                default: throw new CiBailout("Unsupported compare bytecode: " + opcode);
                // Checkstyle: on
            }
            return graph.unique(new NormalizeCompareNode(x, y, opcode == Bytecodes.FCMPL || opcode == Bytecodes.DCMPL));
        }
    }


    protected static class IntegerUDivIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ValueNode x, ValueNode y) {
            return graph.unique(new IntegerUDivNode(x.kind(), x, y));
        }
    }

    protected static class IntegerURemIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ValueNode x, ValueNode y) {
            return graph.unique(new IntegerURemNode(x.kind(), x, y));
        }
    }

    protected static class CompareIntrinsic extends GraalIntrinsicImpl {
        private final Condition condition;

        public CompareIntrinsic(Condition condition) {
            this.condition = condition;
        }

        public ValueNode create(StructuredGraph graph, ValueNode x, ValueNode y) {
            return MaterializeNode.create(graph.unique(new CompareNode(x, condition, y)), graph);
        }
    }


    protected static class BitIntrinsic extends GraalIntrinsicImpl {
        private final MaxineMathIntrinsicsNode.Op op;

        public BitIntrinsic(MaxineMathIntrinsicsNode.Op op) {
            this.op = op;
        }

        public ValueNode create(StructuredGraph graph, ValueNode value) {
            return graph.unique(new MaxineMathIntrinsicsNode(value, op));
        }
    }

    protected static class UnsafeCastIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, RiResolvedMethod method, ValueNode value) {
            RiType toType = method.signature().returnType(method.holder());
            if (!(toType instanceof RiResolvedType)) {
                throw new CiBailout("Cannot unsafe cast to an unresolved type: %s", toType);
            }

            CiKind from = value.kind();
            CiKind to = toType.kind(true).stackKind();
            if (Platform.target().sizeInBytes(from) != Platform.target().sizeInBytes(from) || from == CiKind.Float || from == CiKind.Double || to == CiKind.Float || to == CiKind.Double) {
                throw new CiBailout("Unsupported unsafe cast from " + from + " to " + to);
            }

            return graph.unique(new MaxineUnsafeCastNode(value, (RiResolvedType) toType));
        }
    }

    protected static class PointerReadOffsetIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, RiResolvedMethod method, ValueNode pointer, ValueNode offset) {
            return graph.add(new ExtendedUnsafeLoadNode(pointer, null, ensureLong(graph, offset), method.signature().returnKind(true)));
        }
    }

    protected static class PointerReadIndexIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, RiResolvedMethod method, ValueNode pointer, ValueNode displacement, ValueNode index) {
            return graph.add(new ExtendedUnsafeLoadNode(pointer, displacement, ensureLong(graph, index), method.signature().returnKind(true)));
        }
    }

    protected static class PointerWriteOffsetIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, RiResolvedMethod method, ValueNode pointer, ValueNode offset, ValueNode value) {
            RiType dataType = method.signature().argumentTypeAt(method.signature().argumentCount(false) - 1, null);
            CiKind kind = dataType.kind(true);
            return graph.add(new ExtendedUnsafeStoreNode(pointer, null, ensureLong(graph, offset), value, kind));
        }
    }

    protected static class PointerWriteIndexIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, RiResolvedMethod method, ValueNode pointer, ValueNode displacement, ValueNode index, ValueNode value) {
            RiType dataType = method.signature().argumentTypeAt(method.signature().argumentCount(false) - 1, null);
            CiKind kind = dataType.kind(true);
            return graph.add(new ExtendedUnsafeStoreNode(pointer, displacement, ensureLong(graph, index), value, kind));
        }
    }

    protected static class PointerCompareAndSwapIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ValueNode pointer, ValueNode offset, ValueNode expectedValue, ValueNode newValue) {
            return graph.add(new CompareAndSwapNode(pointer, ensureLong(graph, offset), expectedValue, newValue, true));
        }
    }

    /**
     * Processes the value producing the scaled-index or the byte offset for a pointer operation.
     * If compiling for a 64-bit platform and the value is an {@link CiKind#Int} parameter,
     * then a conversion is inserted to sign extend the int to a word.
     *
     * This is required as the value is used as a 64-bit value and so the high 32 bits
     * need to be correct.
     */
    private static ValueNode ensureLong(Graph graph, ValueNode value) {
        if (value.kind() == CiKind.Int && Platform.target().arch.is64bit()) {
            return graph.unique(new ConvertNode(ConvertNode.Op.I2L, value));
        }
        return value;
    }


    protected static class ReadRegisterIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, RiResolvedMethod method, RiRuntime runtime, int registerId) {
            CiRegister register = runtime.getRegisterConfig(method).getRegisterForRole(registerId);
            if (register == null) {
                throw new CiBailout("Unsupported READREG operand " + registerId);
            }
            return graph.add(new ReadRegisterNode(register, method.signature().returnKind(true)));
        }
    }

    protected static class WriteRegisterIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, RiResolvedMethod method, RiRuntime runtime, int registerId, ValueNode value) {
            CiRegister register = runtime.getRegisterConfig(method).getRegisterForRole(registerId);
            if (register == null) {
                throw new CiBailout("Unsupported WRITEREG operand " + registerId);
            }
            return graph.add(new WriteRegisterNode(register, value));
        }
    }

    protected static class SafepointIntrinsic extends GraalIntrinsicImpl {
        private SafepointNode.Op op;

        public SafepointIntrinsic(SafepointNode.Op op) {
            this.op = op;
        }

        public ValueNode create(StructuredGraph graph) {
            return graph.add(new SafepointNode(op));
        }
    }

    protected static class UncommonTrapIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph) {
            return graph.add(new DeoptimizeNode(DeoptAction.InvalidateReprofile));
        }
    }

    protected static class StackAllocateIntrinsic extends GraalIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, RiResolvedMethod method, int size) {
            return graph.add(new StackAllocateNode(size, (RiResolvedType) method.signature().returnType(null)));
        }
    }


    public static void initialize(IntrinsicImpl.Registry registry) {
        registry.add(MEMBAR, new MembarIntrinsic());

        registry.add(UCMP_AE, new CompareIntrinsic(Condition.AE));
        registry.add(UCMP_AT, new CompareIntrinsic(Condition.AT));
        registry.add(UCMP_BE, new CompareIntrinsic(Condition.BE));
        registry.add(UCMP_BT, new CompareIntrinsic(Condition.BT));

        registry.add(UDIV, new IntegerUDivIntrinsic());
        registry.add(UREM, new IntegerURemIntrinsic());

        registry.add(LSB, new BitIntrinsic(MaxineMathIntrinsicsNode.Op.LSB));
        registry.add(MSB, new BitIntrinsic(MaxineMathIntrinsicsNode.Op.MSB));

        registry.add(UNSAFE_CAST, new UnsafeCastIntrinsic());

        registry.add(READREG, new ReadRegisterIntrinsic());
        registry.add(WRITEREG, new WriteRegisterIntrinsic());
        registry.add(IFLATCHBITREAD, new NotImplementedIntrinsic());

        registry.add(PREAD_OFF, new PointerReadOffsetIntrinsic());
        registry.add(PREAD_IDX, new PointerReadIndexIntrinsic());
        registry.add(PWRITE_OFF, new PointerWriteOffsetIntrinsic());
        registry.add(PWRITE_IDX, new PointerWriteIndexIntrinsic());
        registry.add(PCMPSWP, new PointerCompareAndSwapIntrinsic());

        registry.add(SAFEPOINT_POLL, new SafepointIntrinsic(SafepointNode.Op.SAFEPOINT_POLL));
        registry.add(INFO, new SafepointIntrinsic(SafepointNode.Op.INFO));
        registry.add(HERE, new SafepointIntrinsic(SafepointNode.Op.HERE));
        registry.add(UNCOMMON_TRAP, new UncommonTrapIntrinsic());

        registry.add(PAUSE, new SafepointIntrinsic(SafepointNode.Op.PAUSE));
        registry.add(BREAKPOINT_TRAP, new SafepointIntrinsic(SafepointNode.Op.BREAKPOINT));
        registry.add(STACKHANDLE, new NotImplementedIntrinsic());
        registry.add(ALLOCA, new StackAllocateIntrinsic());

        registry.add(CMP_BYTECODE, new NormalizeCompareIntrinsic());
    }
}
