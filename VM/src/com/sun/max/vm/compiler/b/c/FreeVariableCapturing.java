/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.compiler.b.c;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;

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
        private final Binding _parent;
        private final CirVariable _boundVariable;

        private Binding(Binding parent, CirVariable boundVariable) {
            _parent = parent;
            _boundVariable = boundVariable;
        }

        @Override
        public int hashCode() {
            return _boundVariable.hashCode();
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
            if (variable == b._boundVariable) {
                return false;
            }
            b = b._parent;
        }
        return true;
    }

    private final class BlockCallSite {
        private final BlockInfo _callerInfo;
        private final Binding _binding;
        private final CirCall _call;

        private BlockCallSite(BlockInfo callerInfo, Binding binding, CirCall call) {
            _callerInfo = callerInfo;
            _binding = binding;
            _call = call;
        }

        @Override
        public int hashCode() {
            int result = _callerInfo.hashCode();
            if (_binding != null) {
                result *= _binding.hashCode();
            }
            return result;
        }
    }

    private final class BlockInfo {
        private final CirBlock _block;

        private BlockInfo(CirBlock block) {
            _block = block;
        }

        private final AppendableSequence<BlockCallSite> _callSites = new LinkSequence<BlockCallSite>();
        private final GrowableDeterministicSet<CirVariable> _freeVariables = new LinkedIdentityHashSet<CirVariable>();
    }

    private final BirToCirMethodTranslation _translation;
    private final AppendableSequence<BlockInfo> _blockInfos = new LinkSequence<BlockInfo>();
    private final Map<CirBlock, BlockInfo> _blockInfoMap = new IdentityHashMap<CirBlock, BlockInfo>();

    private BlockInfo blockToInfo(CirBlock block) {
        return _blockInfoMap.get(block);
    }

    private class CirBlockVisitor extends CirVisitor {
        @Override
        public void visitBlock(CirBlock block) {
            if (!_blockInfoMap.containsKey(block)) {
                final BlockInfo info = new BlockInfo(block);
                _blockInfos.append(info);
                _blockInfoMap.put(block, info);
            }
        }
    }

    FreeVariableCapturing(BirToCirMethodTranslation translation) {
        _translation = translation;
        CirVisitingTraversal.apply(translation.cirClosure(), new CirBlockVisitor());
    }

    private final class Inspection {
        private final CirNode _node;
        private final Binding _binding;

        private Inspection(CirNode node, Binding binding) {
            _node = node;
            _binding = binding;
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
        CirNode node = info._block.closure();
        Binding binding = null;
        while (true) {
            if (node instanceof CirCall) {
                final CirCall call = (CirCall) node;
                addValues(call.arguments(), inspectionList, binding);
                CirJavaFrameDescriptor javaFrameDescriptor = call.javaFrameDescriptor();
                while (javaFrameDescriptor != null) {
                    addValues(javaFrameDescriptor.locals(), inspectionList, binding);
                    addValues(javaFrameDescriptor.stackSlots(), inspectionList, binding);
                    javaFrameDescriptor = javaFrameDescriptor.parent();
                }
                node = call.procedure();
                if (node instanceof CirBlock) {
                    final BlockInfo calleeInfo = blockToInfo((CirBlock) node);
                    assert calleeInfo != null : "no callee info found";
                    assert calleeInfo._callSites != null;
                    calleeInfo._callSites.append(new BlockCallSite(info, binding, call));
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
                        if (isUnbound(variable, binding) && !info._freeVariables.contains(variable)) {
                            info._freeVariables.add(variable);
                        }
                    }
                    if (inspectionList.isEmpty()) {
                        return;
                    }
                    final Inspection inspection = inspectionList.removeFirst();
                    node = inspection._node;
                    binding = inspection._binding;
                }
            }
        }
    }

    private final class Propagation {
        private final CirVariable _parameter;
        private final BlockCallSite _callSite;

        private Propagation(CirVariable parameter, BlockCallSite callSite) {
            _parameter = parameter;
            _callSite = callSite;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Propagation) {
                final Propagation propagation = (Propagation) other;
                return _parameter == propagation._parameter && _callSite == propagation._callSite;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return _parameter.hashCode() ^ _callSite.hashCode();
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
        final LinkedList<Propagation> toDo = new LinkedList<Propagation>();
        for (BlockInfo info : _blockInfos) {
            traceUnboundVariables(info);
            for (CirVariable parameter : info._freeVariables) {
                for (BlockCallSite callSite : info._callSites) {
                    toDo.add(new Propagation(parameter, callSite));
                }
            }
        }
        final Set<Propagation> done = new HashSet<Propagation>();
        while (!toDo.isEmpty()) {
            final Propagation propagation = toDo.removeFirst();
            if (!done.contains(propagation)) {
                done.add(propagation);
                if (isUnbound(propagation._parameter, propagation._callSite._binding)) {
                    propagation._callSite._callerInfo._freeVariables.add(propagation._parameter);
                    for (BlockCallSite callSite : propagation._callSite._callerInfo._callSites) {
                        toDo.add(new Propagation(propagation._parameter, callSite));
                    }
                }
            }
        }
    }

    private void traceUnboundVariables(BlockInfo info) {
        if (Trace.hasLevel(4)) {
            Trace.line(4, info._block + " has unbound variables " + info._freeVariables);
        }
    }

    private void declareFreeVariablesAsParameters() {
        for (BlockInfo info : _blockInfos) {
            traceDeclareFreeVariables(info);
            final CirVariable[] free = Sequence.Static.toArray(info._freeVariables, CirVariable.class);
            final CirVariable[] params = info._block.closure().parameters();

            final CirVariable[] all = CirClosure.newParameters(free.length + params.length);
            for (int i = 0; i < free.length; i++) {
                all[i] = free[i];
            }
            for (int i = 0; i < params.length; i++) {
                all[i + free.length] = params[i];
            }
            info._block.closure().setParameters(all);
            assert info._block.closure().verifyParameters();
        }
    }

    private void traceDeclareFreeVariables(BlockInfo info) {
        if (Trace.hasLevel(4)) {
            Trace.line(4, info._block + " has propagated parameters " + info._freeVariables);
        }
    }

    private void addCanonicalArgumentsToCall(CirCall call, BlockInfo info) {
        final CirValue[] args = call.arguments();
        if (args.length == 0 && info._freeVariables.isEmpty()) {
            call.setArguments(CirCall.NO_ARGUMENTS);
            return;
        }

        final CirValue[] free = Sequence.Static.toArray(info._freeVariables, CirValue.class);
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
        final CirClosure closure = _translation.cirClosure();
        final CirVariable[] parameters = closure.parameters();
        final CirValue[] arguments = closure.body().arguments();
        for (int i = 0; i < arguments.length; i++) {
            if (!com.sun.max.lang.Arrays.contains(parameters, arguments[i])) {
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
        final LinkedList<CirCall> toDo = new LinkedList<CirCall>();
        final CirCall firstCall = _translation.cirClosure().body();
        toDo.add(firstCall);
        while (!toDo.isEmpty()) {
            boolean havingUndefined = false;
            final CirCall call = toDo.remove();
            final CirBlock block = (CirBlock) call.procedure();
            int i = 0;
            while (i < call.arguments().length) {
                if (call.arguments()[i] == CirValue.UNDEFINED) {
                    havingUndefined = true;
                    CirBetaReduction.applySingle(block.closure(), block.closure().parameters()[i], CirValue.UNDEFINED);
                    block.closure().removeParameter(i);
                    for (BlockCallSite blockCallSite : blockToInfo(block)._callSites) {
                        blockCallSite._call.removeArgument(i);
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
                            toDo.add(c);
                        }
                        super.visitCall(c);
                    }
                };
                traversal.run();
            }
        }
    }

    public void run() {
        for (BlockInfo info : _blockInfos) {
            findUnboundVariablesInBlock(info);
        }
        propagateUnboundVariablesToCallers();
        declareFreeVariablesAsParameters();
        for (BlockInfo info : _blockInfos) {
            for (BlockCallSite callSite : info._callSites) {
                addCanonicalArgumentsToCall(callSite._call, info);
            }
        }
        addCanonicalArgumentsToCall(_translation.cirClosure().body(), _blockInfos.first());
        terminateJavaLocals();
    }

}
