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

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import static com.oracle.max.cri.intrinsics.IntrinsicIDs.*;

import com.oracle.max.cri.intrinsics.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.DeoptimizeNode.DeoptAction;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.extended.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.platform.*;

public class GraalMaxineIntrinsicImplementations {
    private static class NotImplementedIntrinsic implements GraalIntrinsicImpl {
        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            throw new UnsupportedOperationException("intrinsic not implemented");
        }
    }


    private static class NormalizeCompareIntrinsic implements GraalIntrinsicImpl {

        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            assert args.length == 3 && args[0].isConstant() && args[0].kind() == CiKind.Int : target;
            int opcode = args[0].asConstant().asInt();
            // TODO(cwi): Why the separation when both branches do the same?
            if (args[1].kind() == CiKind.Long || args[1].kind() == CiKind.Double) {
                assert opcode == Bytecodes.LCMP || opcode == Bytecodes.DCMPG || opcode == Bytecodes.DCMPL;
                return graph.unique(new NormalizeCompareNode(args[1], args[2], opcode == Bytecodes.FCMPL || opcode == Bytecodes.DCMPL));
            } else {
                assert opcode == Bytecodes.FCMPG || opcode == Bytecodes.FCMPL;
                assert args[1].kind() == CiKind.Float;
                return graph.unique(new NormalizeCompareNode(args[1], args[2], opcode == Bytecodes.FCMPL || opcode == Bytecodes.DCMPL));
            }
        }
    }


    private static class IntegerDivIntrinsic implements GraalIntrinsicImpl {
        private final boolean remainder;

        public IntegerDivIntrinsic(boolean remainder) {
            this.remainder = remainder;
        }

        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            assert args.length == 2 && args[0].kind() == args[1].kind() && (args[0].kind() == CiKind.Int || args[0].kind() == CiKind.Long);
            if (remainder) {
                return graph.unique(new IntegerURemNode(args[0].kind(), args[0], args[1]));
            } else {
                return graph.unique(new IntegerUDivNode(args[0].kind(), args[0], args[1]));
            }
        }
    }

    private static class CompareIntrinsic implements GraalIntrinsicImpl {

        private final Condition condition;

        public CompareIntrinsic(Condition condition) {
            this.condition = condition;
        }

        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            assert args.length == 2;
            assert args[0].kind() == CiKind.Int || args[0].kind() == CiKind.Long;
            return MaterializeNode.create(graph.unique(new CompareNode(args[0], condition, args[1])), graph);
        }
    }


    private static class BitIntrinsic implements GraalIntrinsicImpl {
        private final MaxineMathIntrinsicsNode.Op op;

        public BitIntrinsic(MaxineMathIntrinsicsNode.Op op) {
            this.op = op;
        }

        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            assert args.length == 1;
            return graph.unique(new MaxineMathIntrinsicsNode(args[0], op));
        }
    }

    private static boolean isTwoSlot(CiKind kind) {
        assert kind != CiKind.Void && kind != CiKind.Illegal;
        return kind == CiKind.Long || kind == CiKind.Double;
    }

    private static class UnsafeCastIntrinsic implements GraalIntrinsicImpl {
        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            RiSignature signature = target.signature();
            int argCount = signature.argumentCount(false);
            RiType accessingClass = caller.holder();
            RiType fromType;
            RiType toType = signature.returnType(accessingClass);
            assert args.length == 1 || (args.length == 2 && isTwoSlot(args[0].kind())) : "method with @UNSAFE_CAST must have exactly 1 argument";
            if (argCount == 1) {
                fromType = signature.argumentTypeAt(0, accessingClass);
            } else {
                assert argCount == 0 : "method with @UNSAFE_CAST must have exactly 1 argument";
                fromType = target.holder();
            }
            if (!(toType instanceof RiResolvedType)) {
                throw new CiBailout("Cannot unsafe cast to an unresolved type: %s", toType);
            }
            CiKind from = fromType.kind(true).stackKind();
            CiKind to = toType.kind(true).stackKind();
            if (Platform.target().sizeInBytes(from) != Platform.target().sizeInBytes(from) || from == CiKind.Float || from == CiKind.Double || to == CiKind.Float || to == CiKind.Double) {
                throw new CiBailout("Unsupported unsafe cast from " + fromType + " to " + toType);
            }
            return graph.unique(new UnsafeCastNode(args[0], (RiResolvedType) toType));
        }
    }

    private static class PointerReadIntrinsic implements GraalIntrinsicImpl {
        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            assert args.length == 2 || args.length == 3;
            ValueNode pointer = args[0];
            ValueNode displacement = args.length == 3 ? args[1] : null;
            ValueNode offsetOrIndex = offsetOrIndex(graph, args.length == 3 ? args[2] : args[1]);

            if (displacement == null) {
                return graph.add(new UnsafeLoadNode(pointer, offsetOrIndex, target.signature().returnKind(true)));
            } else {
                if (displacement.isConstant()) {
                    return graph.add(new UnsafeLoadNode(pointer, displacement.asConstant().asInt(), offsetOrIndex, target.signature().returnKind(true)));
                } else {
                    return graph.add(new ExtendedUnsafeLoadNode(pointer, displacement, offsetOrIndex, target.signature().returnKind(true)));
                }
            }
        }
    }

    private static class PointerWriteIntrinsic implements GraalIntrinsicImpl {
        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            // Last parameter can be a double word, in which case args ends with a null slot that must be ignored.
            int numArgs = args[args.length - 1] == null ? args.length - 1 : args.length;

            assert numArgs == 3 || numArgs == 4;
            ValueNode pointer = args[0];
            ValueNode displacement = numArgs == 4 ? args[1] : null;
            ValueNode offsetOrIndex = offsetOrIndex(graph, numArgs == 4 ? args[2] : args[1]);
            ValueNode value = args[numArgs - 1];

            RiType dataType = target.signature().argumentTypeAt(target.signature().argumentCount(false) - 1, null);
            CiKind kind = dataType.kind(true);
            if (displacement == null) {
                return graph.add(new UnsafeStoreNode(pointer, offsetOrIndex, value, kind));
            } else {
                if (displacement.isConstant()) {
                    return graph.add(new UnsafeStoreNode(pointer, displacement.asConstant().asInt(), offsetOrIndex, value, kind));
                } else {
                    return graph.add(new ExtendedUnsafeStoreNode(pointer, displacement, offsetOrIndex, value, kind));
                }
            }
        }
    }

    private static class PointerCompareAndSwapIntrinsic implements GraalIntrinsicImpl {
        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            assert args.length == 4 || args.length == 6;
            ValueNode pointer = args[0];
            ValueNode offset = offsetOrIndex(graph, args[1]);
            ValueNode expectedValue = args[2];
            ValueNode newValue = args[args.length == 6 ? 4 : 3];
            return graph.add(new CompareAndSwapNode(pointer, offset, expectedValue, newValue, true));
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
    private static ValueNode offsetOrIndex(Graph graph, ValueNode offsetOrIndex) {
        if (offsetOrIndex.kind() == CiKind.Int && Platform.target().arch.is64bit()) {
            return graph.unique(new ConvertNode(ConvertNode.Op.I2L, offsetOrIndex));
        }
        return offsetOrIndex;
    }


    private static class ReadRegisterIntrinsic implements GraalIntrinsicImpl {
        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            assert args.length == 1;
            int registerId = intConstant(args[0], target);

            CiRegister register = runtime.getRegisterConfig(target).getRegisterForRole(registerId);
            if (register == null) {
                throw new CiBailout("Unsupported READREG operand " + registerId);
            }
            ReadRegisterNode load = graph.add(new ReadRegisterNode(register, target.signature().returnKind(false)));
            return load;
        }
    }

    private static class WriteRegisterIntrinsic implements GraalIntrinsicImpl {
        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            assert args.length == 2;
            int registerId = intConstant(args[0], target);
            ValueNode value = args[1];

            CiRegister register = runtime.getRegisterConfig(target).getRegisterForRole(registerId);
            if (register == null) {
                throw new CiBailout("Unsupported READREG operand " + registerId);
            }
            return graph.add(new WriteRegisterNode(register, value));
        }
    }

    private static int intConstant(ValueNode value, RiResolvedMethod target) {
        if (!value.isConstant() || value.kind() != CiKind.Int) {
            throw new CiBailout("instrinc parameter must be compile time integer constant for invoke " + target);
        }
        return value.asConstant().asInt();
    }

    private static class SafepointIntrinsic implements GraalIntrinsicImpl {
        private SafepointNode.Op op;

        public SafepointIntrinsic(SafepointNode.Op op) {
            this.op = op;
        }

        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            assert args.length == 0;
            return graph.add(new SafepointNode(op));
        }
    }

    private static class UncommonTrapIntrinsic implements GraalIntrinsicImpl {
        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            return graph.add(new DeoptimizeNode(DeoptAction.InvalidateReprofile));
        }
    }

    private static class StackHandleIntrinsic implements GraalIntrinsicImpl {
        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            throw new UnsupportedOperationException("intrinsic not implemented");
//            return b.append(new StackHandle(args[0], target.signature().returnType(null)));
        }
    }

    private static class StackAllocateIntrinsic implements GraalIntrinsicImpl {
        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            return graph.add(new StackAllocateNode(intConstant(args[0], target), (RiResolvedType) target.signature().returnType(null)));
        }
    }

    private static class IfLatchBitReadIntrinsic implements GraalIntrinsicImpl {
        @Override
        public ValueNode createHIR(RiRuntime runtime, StructuredGraph graph, RiResolvedMethod caller, RiResolvedMethod target, ValueNode[] args) {
            throw new UnsupportedOperationException("intrinsic not implemented");
        }
    }

    public static void initialize(IntrinsicImpl.Registry registry) {
        registry.add(UCMP_AE, new CompareIntrinsic(Condition.AE));
        registry.add(UCMP_AT, new CompareIntrinsic(Condition.AT));
        registry.add(UCMP_BE, new CompareIntrinsic(Condition.BE));
        registry.add(UCMP_BT, new CompareIntrinsic(Condition.BT));

        registry.add(UDIV, new IntegerDivIntrinsic(false));
        registry.add(UREM, new IntegerDivIntrinsic(true));

        registry.add(LSB, new BitIntrinsic(MaxineMathIntrinsicsNode.Op.LSB));
        registry.add(MSB, new BitIntrinsic(MaxineMathIntrinsicsNode.Op.MSB));

        registry.add(UNSAFE_CAST, new UnsafeCastIntrinsic());

        registry.add(READREG, new ReadRegisterIntrinsic());
        registry.add(WRITEREG, new WriteRegisterIntrinsic());
        registry.add(IFLATCHBITREAD, new IfLatchBitReadIntrinsic());

        registry.add(PREAD, new PointerReadIntrinsic());
        registry.add(PWRITE, new PointerWriteIntrinsic());
        registry.add(PCMPSWP, new PointerCompareAndSwapIntrinsic());

        registry.add(SAFEPOINT_POLL, new SafepointIntrinsic(SafepointNode.Op.SAFEPOINT_POLL));
        registry.add(INFO, new SafepointIntrinsic(SafepointNode.Op.INFO));
        registry.add(HERE, new SafepointIntrinsic(SafepointNode.Op.HERE));
        registry.add(UNCOMMON_TRAP, new UncommonTrapIntrinsic());

        registry.add(PAUSE, new SafepointIntrinsic(SafepointNode.Op.PAUSE));
        registry.add(BREAKPOINT_TRAP, new SafepointIntrinsic(SafepointNode.Op.BREAKPOINT));
        registry.add(STACKHANDLE, new StackHandleIntrinsic());
        registry.add(ALLOCA, new StackAllocateIntrinsic());

        registry.add(CMP_BYTECODE, new NormalizeCompareIntrinsic());
    }
}
