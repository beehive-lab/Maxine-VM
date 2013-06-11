/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal.amd64;

import static com.oracle.graal.amd64.AMD64.*;
import static com.oracle.graal.api.code.ValueUtil.*;
import static com.oracle.max.vm.ext.graal.MaxGraal.unimplemented;

import java.io.*;

import com.oracle.graal.amd64.*;
import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.asm.*;
import com.oracle.graal.asm.amd64.*;
import com.oracle.graal.asm.amd64.AMD64Address.*;
import com.oracle.graal.compiler.gen.*;
import com.oracle.graal.compiler.amd64.*;
import com.oracle.graal.compiler.target.*;
import com.oracle.graal.lir.*;
import com.oracle.graal.lir.amd64.*;
import com.oracle.graal.lir.amd64.AMD64Move.*;
import com.oracle.graal.lir.asm.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.phases.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.oracle.max.vm.ext.graal.nodes.MaxSafepointNode.Op;
import com.sun.max.platform.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;

public class MaxAMD64Backend extends Backend {

    public static class MaxAMD64LIRGenerator extends AMD64LIRGenerator {
        public MaxAMD64LIRGenerator(StructuredGraph graph, CodeCacheProvider runtime, TargetDescription target, FrameMap frameMap, CallingConvention cc, LIR lir) {
            super(graph, runtime, target, frameMap, cc, lir);
        }

        @Override
        public void visitSafepointNode(SafepointNode node) {
            if (node instanceof MaxSafepointNode) {
                MaxSafepointNode maxSafepointNode = (MaxSafepointNode) node;
                switch (maxSafepointNode.op) {
                    case SAFEPOINT_POLL: {
                        // explicit request for safepoint in code
                        emitSafepoint(node, maxSafepointNode.op);
                        break;
                    }

                    case HERE: {
                        RegisterValue rip = AMD64.rip.asValue(node.kind());
                        Variable result = newVariable(node.kind());
                        AMD64AddressValue here = emitAddress(rip, 0, Constant.INT_0, 0);
                        emitMove(result, here);
                        setResult(node, result);
                        emitSafepoint(node, maxSafepointNode.op);
                        break;
                    }
                    case INFO: {
                        /*
                        FrameState stateDuring = stateAfter().duplicateModified(stateAfter().bci, false, kind());
                        LIRDebugInfo info = gen.stateFor(stateDuring);
                        gen.append(AMD64SafepointOpcode.SAFEPOINT.create(info));
                        */
                        unimplemented("MaxSafepointNode.INFO");
                        break;
                    }
                    case BREAKPOINT: {
                        /*
                        gen.append(AMD64BreakpointOpcode.BREAKPOINT.create());
                        */
                        unimplemented("MaxSafepointNode.BREAKPOINT");
                        break;
                    }
                    case PAUSE: {
                        break;
                    }
                }

            } else {
                // generic Graal safepoint
                emitSafepoint(node, Op.SAFEPOINT_POLL);
            }
        }

        private void emitSafepoint(SafepointNode node, MaxSafepointNode.Op op) {
            LIRFrameState info = state(node);
            append(new MaxAMD64SafepointOp(info, op));
        }

        public void visitMaxCompareAndSwap(MaxCompareAndSwapNode node) {
            // This code up to the setResuklt is identical to AMD64LIRGenerator.visitCompareAndSwap
            Kind kind = node.newValue().kind();
            assert kind == node.expected().kind();

            Value expected = loadNonConst(operand(node.expected()));
            Variable newValue = load(operand(node.newValue()));

            AMD64AddressValue address;
            int displacement = node.displacement();
            Value index = operand(node.offset());
            if (isConstant(index) && NumUtil.isInt(asConstant(index).asLong() + displacement)) {
                assert !runtime.needsDataPatch(asConstant(index));
                displacement += (int) asConstant(index).asLong();
                address = new AMD64AddressValue(kind, load(operand(node.object())), displacement);
            } else {
                address = new AMD64AddressValue(kind, load(operand(node.object())), load(index), Scale.Times1, displacement);
            }

            RegisterValue rax = AMD64.rax.asValue(kind);
            emitMove(rax, expected);
            append(new CompareAndSwapOp(rax, address, rax, newValue));
            Variable result = newVariable(node.kind());
            emitMove(result, rax);

            setResult(node, result);
        }

        @Override
        public void visitBreakpointNode(BreakpointNode i) {
            unimplemented("MaxAMD64LIRGenerator.visitBreakpointNode");
        }

        @Override
        public void emitUnwind(Value operand) {
            unimplemented("MaxAMD64LIRGenerator.emitUnwind");
        }

        @Override
        protected void emitDirectCall(DirectCallTargetNode callTarget, Value result, Value[] parameters, Value[] temps, LIRFrameState callState) {
            append(new AMD64Call.DirectCallOp((ResolvedJavaMethod) callTarget.target(), result, parameters, temps, callState));
        }

        @Override
        protected void emitIndirectCall(IndirectCallTargetNode callTarget, Value result, Value[] parameters, Value[] temps, LIRFrameState callState) {
            // The current register allocator cannot handle variables at call sites, need a fixed
            // register.
            AllocatableValue targetAddress = AMD64.rax.asValue();
            emitMove(targetAddress, operand(callTarget.computedAddress()));
            append(new AMD64Call.IndirectCallOp(callTarget.target(), result, parameters, temps, targetAddress, callState));
        }

        @Override
        public void emitDeoptimize(DeoptimizationAction action, DeoptimizingNode deopting) {
            append(new MaxAMD64DeoptimizeOp(action, deopting.getDeoptimizationReason(), state(deopting)));
        }

        @Override
        public Value emitLoad(Kind kind, Value address, DeoptimizingNode deopting) {
            AMD64AddressValue loadAddress = asAddressValue(address);
            Variable result = newVariable(kind);
            append(new LoadOp(kind, result, loadAddress, deopting != null ? state(deopting) : null));
            return result;
        }

        @Override
        public void emitStore(Kind kind, Value address, Value inputVal, DeoptimizingNode deopting) {
            AMD64AddressValue storeAddress = asAddressValue(address);
            LIRFrameState state = deopting != null ? state(deopting) : null;

            if (isConstant(inputVal)) {
                Constant c = asConstant(inputVal);
                if (canStoreConstant(c)) {
                    append(new StoreConstantOp(kind, storeAddress, c, state, false));
                    return;
                }
            }

            Variable input = load(inputVal);
            append(new StoreOp(kind, storeAddress, input, state));
        }

        @Override
        public void visitCompareAndSwap(CompareAndSwapNode node) {
            Kind kind = node.newValue().kind();
            assert kind == node.expected().kind();

            Value expected = loadNonConst(operand(node.expected()));
            Variable newValue = load(operand(node.newValue()));

            AMD64AddressValue address;
            int displacement = node.displacement();
            Value index = operand(node.offset());
            if (isConstant(index) && NumUtil.isInt(asConstant(index).asLong() + displacement)) {
                assert !runtime.needsDataPatch(asConstant(index));
                displacement += (int) asConstant(index).asLong();
                address = new AMD64AddressValue(kind, load(operand(node.object())), displacement);
            } else {
                address = new AMD64AddressValue(kind, load(operand(node.object())), load(index), Scale.Times1, displacement);
            }

            RegisterValue rax = AMD64.rax.asValue(kind);
            emitMove(rax, expected);
            append(new CompareAndSwapOp(rax, address, rax, newValue));

            Variable result = newVariable(node.kind());
            append(new AMD64ControlFlow.CondMoveOp(result, Condition.EQ, load(Constant.TRUE), Constant.FALSE));
            setResult(node, result);
        }

    }

    protected static class MaxAMD64FrameContext implements FrameContext {

        private ClassMethodActor callee;

        @Override
        public void enter(TargetMethodAssembler tasm) {
            if (callee.isTemplate()) {
                return;
            }

            FrameMap frameMap = tasm.frameMap;
            int frameSize = frameMap.frameSize();

            AMD64MacroAssembler asm = (AMD64MacroAssembler) tasm.asm;

            AdapterGenerator generator = AdapterGenerator.forCallee(callee, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            if (generator != null) {
                ByteArrayOutputStream os = new ByteArrayOutputStream(8);
                generator.adapt(callee, os);
                byte[] bytes = os.toByteArray();
                for (byte b : bytes) {
                    asm.codeBuffer.emitByte(b & 0xFF);
                }
            }

            // PushFrame
            asm.decrementq(rsp, frameSize);
            CalleeSaveLayout csl = frameMap.registerConfig.getCalleeSaveLayout();
            if (csl != null && csl.size != 0) {
                int frameToCSA = frameMap.offsetToCalleeSaveArea();
                assert frameToCSA >= 0;
                asm.save(csl, frameToCSA);
            }

            if (!callee.isVmEntryPoint()) {
                int lastFramePage = frameSize / Platform.platform().pageSize;
                // emit multiple stack bangs for methods with frames larger than a page
                for (int i = 0; i <= lastFramePage; i++) {
                    int offset = (i + GraalOptions.StackShadowPages.getValue()) * Platform.platform().pageSize;
                    // Deduct 'frameSize' to handle frames larger than the shadow
                    bangStackWithOffset(tasm, asm, offset - frameSize);
                }
            }

        }

        /**
         * @param offset the offset RSP at which to bang. Note that this offset is relative to RSP after RSP has been
         *            adjusted to allocated the frame for the method. It denotes an offset "down" the stack.
         *            For very large frames, this means that the offset may actually be negative (i.e. denoting
         *            a slot "up" the stack above RSP).
         */
        private static void bangStackWithOffset(TargetMethodAssembler tasm, AMD64MacroAssembler masm, int offset) {
            masm.movq(new AMD64Address(rsp, -offset), rax);
        }


        @Override
        public void leave(TargetMethodAssembler tasm) {
            if (callee.isTemplate()) {
                return;
            }

            int frameSize = tasm.frameMap.frameSize();
            AMD64MacroAssembler asm = (AMD64MacroAssembler) tasm.asm;

            // PopFrame
            CalleeSaveLayout csl = tasm.frameMap.registerConfig.getCalleeSaveLayout();

            if (csl != null && csl.size != 0) {
                tasm.compilationResult.setRegisterRestoreEpilogueOffset(asm.codeBuffer.position());
                // saved all registers, restore all registers
                int frameToCSA = tasm.frameMap.offsetToCalleeSaveArea();
                asm.restore(csl, frameToCSA);
            }

            asm.incrementq(rsp, frameSize);
        }

    }

    public MaxAMD64Backend(CodeCacheProvider runtime, TargetDescription target) {
        super(runtime, target);
        AMD64AsmOptions.UseNormalNop = true; // TODO temp help for looking at code in Inspector
    }

    @Override
    public LIRGenerator newLIRGenerator(StructuredGraph graph, FrameMap frameMap, CallingConvention cc, LIR lir) {
        return new MaxAMD64LIRGenerator(graph, runtime(), target, frameMap, cc, lir);
    }

    @Override
    public void emitCode(TargetMethodAssembler tasm, LIRGenerator lirGen, ResolvedJavaMethod method) {
        MaxAMD64FrameContext maxFrameContext = (MaxAMD64FrameContext) tasm.frameContext;
        maxFrameContext.callee = (ClassMethodActor) MaxResolvedJavaMethod.getRiResolvedMethod(method);
        lirGen.lir.emitCode(tasm);
    }

    @Override
    public TargetMethodAssembler newAssembler(LIRGenerator lirGen, CompilationResult compilationResult) {
        FrameMap frameMap = lirGen.frameMap();
        AbstractAssembler masm = new AMD64MacroAssembler(target, frameMap.registerConfig);
        FrameContext frameContext = new MaxAMD64FrameContext();
        TargetMethodAssembler tasm = new TargetMethodAssembler(target, runtime(), frameMap, masm, frameContext, compilationResult);
        tasm.setFrameSize(frameMap.frameSize());
        // TODO tasm.compilationResult.setCustomStackAreaOffset(frameMap.offsetForStackSlot(???));
        return tasm;
    }

    @Override
    protected AbstractAssembler createAssembler(FrameMap frameMap) {
        return new AMD64MacroAssembler(target, frameMap.registerConfig);
    }

}
