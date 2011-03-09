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
package com.sun.max.vm.cps.tir.pipeline;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.dir.transform.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.state.*;
import com.sun.max.vm.cps.ir.IrBlock.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.cps.tir.TirInstruction.*;
import com.sun.max.vm.cps.tir.pipeline.TirToDirTranslator.VariableAllocator.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public class TirToDirTranslator extends TirPipelineFilter  {
    public static class VariableAllocator {
        private static enum VariableType {
            CLEAN, DIRTY
        }

        private int serial = 0;
        private final ArrayList<DirVariable>[] dirtyStacks;
        private ArrayList<DirVariable> dirtyStack(Kind kind) {
            return dirtyStacks[kind.asEnum.ordinal()];
        }

        private Mapping<TirInstruction, DirVariable> bindings = new IdentityHashMapping<TirInstruction, DirVariable>();

        public VariableAllocator() {
            Class<ArrayList<DirVariable>[]> type = null;
            dirtyStacks = Utils.cast(type, new ArrayList[KindEnum.VALUES.size()]);
            for (Kind kind : new Kind[] {Kind.INT, Kind.FLOAT, Kind.LONG, Kind.DOUBLE, Kind.REFERENCE, Kind.VOID}) {
                dirtyStacks[kind.asEnum.ordinal()] = new ArrayList<DirVariable>();
            }
        }

        public DirVariable allocate(Kind kind, VariableType type) {
            if (type == VariableType.CLEAN || dirtyStack(kind).isEmpty()) {
                return new DirVariable(kind, serial++);
            }
            ArrayList<DirVariable> dirtyStack = dirtyStack(kind);
            return dirtyStack.remove(dirtyStack.size() - 1);
        }

        public void bind(TirInstruction instruction, DirVariable variable) {
            bindings.put(instruction, variable);
        }

        public DirVariable allocate(TirInstruction instruction, VariableType type) {
            DirVariable variable = variable(instruction);
            if (variable == null) {
                variable = allocate(instruction.kind(), type);
                bind(instruction, variable);
            }
            return variable;
        }

        public DirVariable variable(TirInstruction instruction) {
            return bindings.get(instruction);
        }

        public boolean isLive(TirInstruction instruction) {
            return variable(instruction) != null;
        }

        public void free(TirInstruction instruction) {
            final DirVariable variable = bindings.remove(instruction);
            ProgramError.check(variable != null);
            dirtyStack(variable.kind()).add(variable);
        }
    }

    private static final ClassMethodActor bailoutMethod = HotpathSnippet.CallBailout.SNIPPET.executable;
    private static final ClassMethodActor saveRegistersMethod = HotpathSnippet.SaveRegisters.SNIPPET.executable;

    private DirTree dirTree;
    private DirVariable[] parameters;
    private LinkedList<DirBlock> blocks = new LinkedList<DirBlock>();
    private LinkedIdentityHashSet<DirGoto> loopPatchList = new LinkedIdentityHashSet<DirGoto>();
    private VariableAllocator allocator = new VariableAllocator();

    /*
     * === LOCAL =================
     *
     * === PROLOGUE ==============
     *
     * === TRACE =================
     *
     * === TRACE =================
     *
     * === TRACE =================
     *
     * === BAILOUT ===============
     *
     * ===========================
     */

    private DirBlock localBlock = new DirBlock(Role.NORMAL);
    private DirBlock prologueBlock = new DirBlock(Role.NORMAL);
    private DirBlock bailoutBlock = new DirBlock(Role.NORMAL);
    private DirVariable bailoutGuard = allocator.allocate(Kind.REFERENCE, VariableType.CLEAN);

    public TirToDirTranslator() {
        super(TirPipelineOrder.REVERSE, TirVoidSink.SINK);
    }

    public DirTree method() {
        ProgramError.check(dirTree.isGenerated());
        return dirTree;
    }

    private DirBlock current() {
        return blocks.getFirst();
    }

    private void emitBlockIfNotEmpty(DirBlock block) {
        if (block.isEmpty() == false) {
            emitBlock(block);
        }
    }

    private void emitBlock(DirBlock block) {
        block.setSerial(blocks.size());
        if (blocks.isEmpty() == false) {
            link(block, current());
        }
        blocks.addFirst(block);
    }

    private void link(DirBlock a, DirBlock b) {
        a.successors().add(b);
        b.predecessors().add(a);
    }

    private void emitInstruction(DirInstruction... instructions) {
        for (int i = instructions.length - 1; i >= 0; i--) {
            current().instructions().add(0, instructions[i]);
        }
    }

    private DirVariable var(TirInstruction instruction) {
        return (DirVariable) use(instruction);
    }

    private DirValue use(TirInstruction instruction) {
        if (instruction instanceof TirConstant) {
            final TirConstant constant = (TirConstant) instruction;
            return new DirConstant(constant.value());
        } else if (instruction instanceof Placeholder) {
            return DirValue.UNDEFINED;
        }
        return allocator.allocate(instruction, variableType(instruction));
    }

    private DirValue[] useMany(TirInstruction... instruction) {
        final DirValue[] variables = new DirValue[instruction.length];
        for (int i = 0; i < instruction.length; i++) {
            variables[i] = use(instruction[i]);
        }
        return variables;
    }

    private VariableType variableType(TirInstruction instruction) {
        if (isInvariant(instruction)) {
            return VariableType.CLEAN;
        }
        return VariableType.DIRTY;
    }

    private void emitCall(ClassMethodActor method, TirState state, DirValue... arguments) {
        final DirVariable result;
        if (method.resultKind() != Kind.VOID) {
            result = allocator.allocate(method.resultKind(), VariableType.DIRTY);
        } else {
            result = null;
        }
        final DirJavaFrameDescriptor frameDescriptor = tirStateToJavaFrameDescriptor(state);
        final DirMethodCall call = new DirMethodCall(result, new DirMethodValue(method), arguments, null, false, frameDescriptor);
        emitInstruction(call);
    }

    private DirJavaFrameDescriptor tirStateToJavaFrameDescriptor(TirState state) {
        final DirJavaFrameDescriptor frameDescriptor;
        if (state != null) {
            final DirValue[] locals = useMany(state.getLocalSlots());
            final DirValue[] stack = useMany(state.getStackSlots());
            final BytecodeLocation location = state.last().location();
            frameDescriptor = new DirJavaFrameDescriptor(null, location.classMethodActor, location.bci, locals, stack);
        } else {
            frameDescriptor = null;
        }
        return frameDescriptor;
    }

    @Override
    public void beginTree() {
        dirTree = new DirTree(tree(), tree().anchor().method());
        parameters = new DirVariable[tree().entryState().length()];
        tree().entryState().visit(new StateVisitor<TirInstruction>() {
            @Override
            public void visit(TirInstruction entry) {
                final TirLocal local = (TirLocal) entry;
                if (local.flags().isRead()) {
                    parameters[index] = (DirVariable) use(entry);
                } else {
                    parameters[index] = allocator.allocate(Kind.VOID, VariableType.DIRTY);
                }
                if (!local.kind().isCategory1) {
                    parameters[index + 1] = allocator.allocate(Kind.VOID, VariableType.DIRTY);
                }
            }
        });

        emitBlock(bailoutBlock);
        emitInstruction(new DirReturn(bailoutGuard));
        emitCall(bailoutMethod, null, bailoutGuard);
        emitCall(saveRegistersMethod, null);
    }

    @Override
    public void beginTrace() {
        emitBlock(new DirBlock(Role.NORMAL));
        final DirGoto dirGoto = new DirGoto(null);
        loopPatchList.add(dirGoto);
        emitInstruction(dirGoto);
        emitLoopbacks(tree().entryState(), trace().tailState());
    }

    @Override
    public void endTree() {
        patchLoops();
        emitBlockIfNotEmpty(prologueBlock);
        emitBlockIfNotEmpty(localBlock);
        dirTree.setGenerated(parameters, new ArrayList<DirBlock>(blocks), false);
    }

    @Override
    public void visit(TirBuiltinCall call) {
        final DirVariable result = allocator.variable(call);
        final DirBuiltinCall dirCall = new DirBuiltinCall(result, call.builtin(), useMany(call.operands()), null, null);
        emitInstruction(dirCall);
    }

    @Override
    public void visit(TirMethodCall call) {
        emitCall(call.method(), call.state(), useMany(call.operands()));
    }

    @Override
    public void visit(TirGuard guard) {
        // Jump to bail-out block if guard fails.
        final DirSwitch dirSwitch = new DirSwitch(guard.kind(),
                                                  guard.valueComparator().complement(),
                                                  use(guard.operand0()),
                                                  new DirValue[] {use(guard.operand1())},
                                                  new DirBlock[] {bailoutBlock}, current());
        emitBlock(new DirBlock(Role.NORMAL));
        emitInstruction(dirSwitch);

        // Capture state.
        final DirInfopoint guardpoint = new DirInfopoint(null, tirStateToJavaFrameDescriptor(guard.state()), Bytecodes.INFO);
        emitInstruction(guardpoint);

        // Assign guard constant to the bail-out guard variable.
        final DirAssign dirAssignment = new DirAssign(bailoutGuard, createConstant(guard));
        emitInstruction(dirAssignment);
    }

    private DirConstant createConstant(Object object) {
        return new DirConstant(ObjectReferenceValue.from(object));
    }

    @Override
    public void visit(TirDirCall call) {
        FatalError.unexpected("Dir Inlining not supported yet.");
    }

    private void patchLoops() {
        for (DirGoto dirGoto : loopPatchList) {
            dirGoto.setTargetBlock(current());
        }
    }

    /**
     * Generate assignments for loop variables, variables that are used and updated on the trace. We need to generate
     * assignments for parallel moves. For example: (x,y,z) <= (y,x,x), would require the following assignment sequence:
     *
     * z <= x;
     * t <= x; We need a temporary t, to break the cycle.
     * x <= y;
     * y <= t;
     *
     * even more intelligently, we could generate:
     *
     * z <= x;
     * x <= y;
     * y <= z;
     *
     */
    private void emitLoopbacks(TirState entryState, TirState tailState) {
        final ArrayList<Pair<TirInstruction, TirLocal>> writes = new ArrayList<Pair<TirInstruction, TirLocal>>();

        // Accumulate loop variables that need to be written back. These are locals that are used
        // in the trace tree and then updated.
        entryState.compare(tailState, new StatePairVisitor<TirInstruction, TirInstruction>() {
            @Override
            public void visit(TirInstruction entry, TirInstruction tail) {
                final TirLocal local = (TirLocal) entry;
                if (local.flags().isRead() && local != tail) {
                    writes.add(new Pair<TirInstruction, TirLocal>(tail, local));
                }
            }
        });

        // Emit writes in topological order until there are no writes left. If we end up in a cycle, we emit a
        // temporary variable.
        // Checkstyle: stop
        while (writes.isEmpty() == false) {
            for (int i = 0; i < writes.size(); i++) {
                final Pair<TirInstruction, TirLocal> write = writes.get(i);
                boolean writeDestinationIsUsed = false;
                // Can we find another write that has a dependency on the destination?
                for (int j = 0; j < writes.size(); j++) {
                    if (writes.get(j).first() == write.second() && i != j) {
                        writeDestinationIsUsed = true;
                        break;
                    }
                }
                if (writeDestinationIsUsed == false) {
                    // No dependency, just emit the move and remove it from the work list.
                    emitWrite(var(write.second()), use(write.first()));
                    writes.remove(i--);
                } else {
                    ProgramError.unexpected();
                    // final DirVariable temporay = createVariable(write.first().kind());
                    // There's a dependency on the destination. Save the destination to a temporary.
                    // emitWrite(use(write.second()), temporay);
                    // emitWrite(use(write.first()), def(write.second()));
                }
            }
        }
        // Checkstyle: resume
    }

    private void emitWrite(DirVariable destination, DirValue source) {
        emitInstruction(new DirAssign(destination, source));
    }

    private void emit(DirMethod method, DirValue... arguments) {

    }

    private List<DirBlock> inline(DirMethod method, DirValue... arguments) {
        Trace.stream().println(method.traceToString());

        final IdentityHashMap<DirBlock, DirBlock> blockMap = new IdentityHashMap<DirBlock, DirBlock>();
        final IdentityHashMap<DirValue, DirValue> valueMap = new IdentityHashMap<DirValue, DirValue>();

        // Map parameters onto arguments.
        for (int i = 0; i < method.parameters().length; i++) {
            valueMap.put(method.parameters()[i], arguments[i]);
        }

        // Map blocks.
        for (DirBlock block : method.blocks()) {
            blockMap.put(block, new DirBlock(block.role()));
        }

        // Map instructions.
        for (DirBlock block : method.blocks()) {
            for (DirInstruction instruction : block.instructions()) {
                instruction.acceptVisitor(new DirVisitor() {
                    public void visitAssign(DirAssign dirAssign) {
                        FatalError.unimplemented();
                    }

                    public void visitBuiltinCall(DirBuiltinCall dirBuiltinCall) {
                        FatalError.unimplemented();
                    }

                    public void visitGoto(DirGoto dirGoto) {
                        FatalError.unimplemented();
                    }

                    public void visitMethodCall(DirMethodCall dirMethodCall) {
                        FatalError.unimplemented();
                        // final DirVariable result = dirMethodCall.result();
                        // final DirVariable callResult = _allocator.allocate(result.kind(), VariableType.DIRTY);
                        // final DirMethodCall call = new DirMethodCall(callResult, dirMethodCall.method(), );
                    }

                    public void visitReturn(DirReturn dirReturn) {
                        FatalError.unimplemented();
                    }

                    public void visitInfopoint(DirInfopoint safepoint) {
                        FatalError.unimplemented();
                    }

                    public void visitSwitch(DirSwitch dirSwitch) {
                        FatalError.unimplemented();
                    }

                    public void visitThrow(DirThrow dirThrow) {
                        FatalError.unimplemented();
                    }

                    public void visitJump(DirJump dirJump) {
                        FatalError.unimplemented();
                    }
                });
            }
        }

        return new ArrayList<DirBlock>(blockMap.values());
    }
}
