/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.b.c;

import java.util.*;

import com.sun.max.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.collect.*;

/**
 * Augments every block in a method
 * with parameters that capture all free variables
 * in the respective block's body.
 * When such a parameter is passed down at a call site,
 * it may constitute a free variable as well.
 * We then propagate it up to the caller block.
 * This applies transitively, even around cycles.
 * Termination is assured, because each parameter can only
 * appear once per block.
 *
 * @author Bernd Mathiske
 */
final class FreeVariableCapturing {

    private final class Binding {
        private final Binding parent;
        private final CirVariable boundVariable;

        private Binding(Binding parent, CirVariable boundVariable) {
            this.parent = parent;
            this.boundVariable = boundVariable;
        }

        @Override
        public int hashCode() {
            return boundVariable.hashCode();
        }
    }

    private Binding bind(CirVariable[] variables, Binding binding) {
        Binding b = binding;
        for (CirVariable variable : variables) {
            b = new Binding(b, variable);
        }
        return b;
    }

    private boolean isUnbound(CirVariable variable, Binding binding) {
        Binding b = binding;
        while (b != null) {
            if (variable == b.boundVariable) {
                return false;
            }
            b = b.parent;
        }
        return true;
    }

    private final class BlockCallSite {
        private final BlockInfo callerInfo;
        private final Binding binding;
        private final CirCall call;

        private BlockCallSite(BlockInfo callerInfo, Binding binding, CirCall call) {
            this.callerInfo = callerInfo;
            this.binding = binding;
            this.call = call;
        }

        @Override
        public int hashCode() {
            int result = callerInfo.hashCode();
            if (binding != null) {
                result *= binding.hashCode();
            }
            return result;
        }
    }

    private final class BlockInfo {
        private final CirBlock block;

        private BlockInfo(CirBlock block) {
            this.block = block;
        }

        private final List<BlockCallSite> callSites = new LinkedList<BlockCallSite>();
        private final LinkedIdentityHashSet<CirVariable> freeVariables = new LinkedIdentityHashSet<CirVariable>();
    }

    private final BirToCirMethodTranslation translation;
    private final List<BlockInfo> blockInfos = new LinkedList<BlockInfo>();
    private final Map<CirBlock, BlockInfo> blockInfoMap = new IdentityHashMap<CirBlock, BlockInfo>();

    private BlockInfo blockToInfo(CirBlock block) {
        return blockInfoMap.get(block);
    }

    private class CirBlockVisitor extends CirVisitor {
        @Override
        public void visitBlock(CirBlock block) {
            if (!blockInfoMap.containsKey(block)) {
                final BlockInfo info = new BlockInfo(block);
                blockInfos.add(info);
                blockInfoMap.put(block, info);
            }
        }
    }

    FreeVariableCapturing(BirToCirMethodTranslation translation) {
        this.translation = translation;
        CirVisitingTraversal.apply(translation.cirClosure(), new CirBlockVisitor());
    }

    private final class Inspection {
        private final CirNode node;
        private final Binding binding;

        private Inspection(CirNode node, Binding binding) {
            this.node = node;
            this.binding = binding;
        }
    }

    private void addValues(CirValue[] values, LinkedList<Inspection> inspectionList, Binding binding) {
        for (CirValue value : values) {
            if (value != null) {
                inspectionList.add(new Inspection(value, binding));
            }
        }
    }

    private void findUnboundVariablesInBlock(BlockInfo info) {
        final LinkedList<Inspection> inspectionList = new LinkedList<Inspection>();
        CirNode node = info.block.closure();
        Binding binding = null;
        while (true) {
            if (node instanceof CirCall) {
                final CirCall call = (CirCall) node;
                addValues(call.arguments(), inspectionList, binding);
                CirJavaFrameDescriptor javaFrameDescriptor = call.javaFrameDescriptor();
                while (javaFrameDescriptor != null) {
                    addValues(javaFrameDescriptor.locals, inspectionList, binding);
                    addValues(javaFrameDescriptor.stackSlots, inspectionList, binding);
                    javaFrameDescriptor = javaFrameDescriptor.parent();
                }
                node = call.procedure();
                if (node instanceof CirBlock) {
                    final BlockInfo calleeInfo = blockToInfo((CirBlock) node);
                    assert calleeInfo != null : "no callee info found";
                    assert calleeInfo.callSites != null;
                    calleeInfo.callSites.add(new BlockCallSite(info, binding, call));
                }
            } else {
                assert node instanceof CirValue;
                if (node instanceof CirClosure) {
                    final CirClosure closure = (CirClosure) node;
                    node = closure.body();
                    binding = bind(closure.parameters(), binding);
                } else {
                    if (node instanceof CirVariable) {
                        final CirVariable variable = (CirVariable) node;
                        if (isUnbound(variable, binding) && !info.freeVariables.contains(variable)) {
                            info.freeVariables.add(variable);
                        }
                    }
                    if (inspectionList.isEmpty()) {
                        return;
                    }
                    final Inspection inspection = inspectionList.removeFirst();
                    node = inspection.node;
                    binding = inspection.binding;
                }
            }
        }
    }

    private final class Propagation {
        private final CirVariable parameter;
        private final BlockCallSite callSite;

        private Propagation(CirVariable parameter, BlockCallSite callSite) {
            this.parameter = parameter;
            this.callSite = callSite;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Propagation) {
                final Propagation propagation = (Propagation) other;
                return parameter == propagation.parameter && callSite == propagation.callSite;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return parameter.hashCode() ^ callSite.hashCode();
        }
    }

    /**
     * If a variable is unbound in a block,
     * we check every call site of that block
     * whether the variable is unbound there as well.
     * If so, we keep record that the variable is unbound in the caller also.
     * In theory, this proceeds transitively up the caller chain
     * until we encounter either the variable being bound
     * or already having checked the same variable at the same call site.
     */
    private void propagateUnboundVariablesToCallers() {
        final LinkedList<Propagation> propagateToDo = new LinkedList<Propagation>();
        for (BlockInfo info : blockInfos) {
            traceUnboundVariables(info);
            for (CirVariable parameter : info.freeVariables) {
                for (BlockCallSite callSite : info.callSites) {
                    propagateToDo.add(new Propagation(parameter, callSite));
                }
            }
        }
        final Set<Propagation> done = new HashSet<Propagation>();
        while (!propagateToDo.isEmpty()) {
            final Propagation propagation = propagateToDo.removeFirst();
            if (!done.contains(propagation)) {
                done.add(propagation);
                if (isUnbound(propagation.parameter, propagation.callSite.binding)) {
                    propagation.callSite.callerInfo.freeVariables.add(propagation.parameter);
                    for (BlockCallSite callSite : propagation.callSite.callerInfo.callSites) {
                        propagateToDo.add(new Propagation(propagation.parameter, callSite));
                    }
                }
            }
        }
    }

    private void traceUnboundVariables(BlockInfo info) {
        if (Trace.hasLevel(4)) {
            Trace.line(4, info.block + " has unbound variables " + info.freeVariables);
        }
    }

    private void declareFreeVariablesAsParameters() {
        for (BlockInfo info : blockInfos) {
            traceDeclareFreeVariables(info);
            final CirVariable[] free = info.freeVariables.toArray(new CirVariable[info.freeVariables.size()]);
            final CirVariable[] params = info.block.closure().parameters();

            final CirVariable[] all = CirClosure.newParameters(free.length + params.length);
            for (int i = 0; i < free.length; i++) {
                all[i] = free[i];
            }
            for (int i = 0; i < params.length; i++) {
                all[i + free.length] = params[i];
            }
            info.block.closure().setParameters(all);
            assert info.block.closure().verifyParameters();
        }
    }

    private void traceDeclareFreeVariables(BlockInfo info) {
        if (Trace.hasLevel(4)) {
            Trace.line(4, info.block + " has propagated parameters " + info.freeVariables);
        }
    }

    private void addCanonicalArgumentsToCall(CirCall call, BlockInfo info) {
        final CirValue[] args = call.arguments();
        if (args.length == 0 && info.freeVariables.isEmpty()) {
            call.setArguments(CirCall.NO_ARGUMENTS);
            return;
        }

        final CirValue[] free = info.freeVariables.toArray(new CirVariable[info.freeVariables.size()]);
        final CirValue[] all = CirCall.newArguments(free.length + args.length);
        for (int i = 0; i < free.length; i++) {
            all[i] = free[i];
        }
        for (int i = 0; i < args.length; i++) {
            all[i + free.length] = args[i];
        }
        call.setArguments(all);
    }

    /**
     * A Java local may be reachable from multiple execution paths,
     * but only be defined in a subset of those.
     * Thus it becomes globally unbound.
     * The algorithm so far will have propagated
     * it all the way up to the outermost block.
     * And it will have passed the variable
     * that represents the Java local as an argument to the block.
     * But that's not correct in case of the outermost block,
     * because there is no prior definition of the variable.
     * As a temporary measure, we assign 'null' to the variable.
     * This enables AlphaConversion to proceed,
     * which enables BetaReduction,
     * which is needed in the next step.
     * After AplhaConversion the translator will further clean up
     * Java locals by calling 'pruneJavaLocals' (see below).
     */
    private void terminateJavaLocals() {
        final CirClosure closure = translation.cirClosure();
        final CirVariable[] parameters = closure.parameters();
        final CirValue[] arguments = closure.body().arguments();
        for (int i = 0; i < arguments.length; i++) {
            if (!(com.sun.max.Utils.indexOfIdentical(parameters, arguments[i]) >= 0)) {
                arguments[i] = CirValue.UNDEFINED;
            }
        }
    }

    /**
     * Here we get rid of undefined Java locals by beta-reducing
     * their variables all the way back down to the bottom.
     * This is in lieu of a liveness analysis for Java locals
     * (which we do not perform during bytecode to CIR translation).
     * In practice, this procedure rarely does much.
     * It's ok for it though to perform quite some extra work when necessary,
     * as the above 'terminateJavaLocals()' is all we need to do in the frequent case.
     */
    void pruneJavaLocals() {
        final LinkedList<CirCall> pruneToDo = new LinkedList<CirCall>();
        final CirCall firstCall = translation.cirClosure().body();
        pruneToDo.add(firstCall);
        while (!pruneToDo.isEmpty()) {
            boolean havingUndefined = false;
            final CirCall call = pruneToDo.remove();
            final CirBlock block = (CirBlock) call.procedure();
            int i = 0;
            while (i < call.arguments().length) {
                if (call.arguments()[i] == CirValue.UNDEFINED) {
                    havingUndefined = true;
                    CirBetaReduction.applySingle(block.closure(), block.closure().parameters()[i], CirValue.UNDEFINED);
                    block.closure().removeParameter(i);
                    for (BlockCallSite blockCallSite : blockToInfo(block).callSites) {
                        blockCallSite.call.removeArgument(i);
                    }
                    if (call == firstCall) {
                        // the first call is not registered among the call sites in the block info
                        firstCall.removeArgument(i);
                    }
                } else {
                    i++;
                }
            }
            if (havingUndefined) {
                final CirTraversal traversal = new CirTraversal.OutsideBlocks(block.closure().body()) {
                    @Override
                    public void visitCall(CirCall c) {
                        if (c.procedure() instanceof CirBlock) {
                            pruneToDo.add(c);
                        }
                        super.visitCall(c);
                    }
                };
                traversal.run();
            }
        }
    }

    public void run() {
        for (BlockInfo info : blockInfos) {
            findUnboundVariablesInBlock(info);
        }
        propagateUnboundVariablesToCallers();
        declareFreeVariablesAsParameters();
        for (BlockInfo info : blockInfos) {
            for (BlockCallSite callSite : info.callSites) {
                addCanonicalArgumentsToCall(callSite.call, info);
            }
        }
        addCanonicalArgumentsToCall(translation.cirClosure().body(), Utils.first(blockInfos));
        terminateJavaLocals();
    }

}
