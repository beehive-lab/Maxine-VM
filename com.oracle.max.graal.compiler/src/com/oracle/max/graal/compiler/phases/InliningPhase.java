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
package com.oracle.max.graal.compiler.phases;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.GraalCompiler.*;
import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.compiler.observer.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.cri.*;
import com.oracle.max.graal.extensions.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class InliningPhase extends Phase {
    /*
     * - Detect method which only call another method with some parameters set to constants: void foo(a) -> void foo(a, b) -> void foo(a, b, c) ...
     *   These should not be taken into account when determining inlining depth.
     * - honor the result of overrideInliningDecision(0, caller, invoke.bci, method, true);
     */

    private static final int MAX_ITERATIONS = 1000;

    private final GraalRuntime runtime;
    private final CiTarget target;

    private final GraalCompiler compiler;

    private int inliningSize;
    private final Collection<Invoke> hints;

    private final PriorityQueue<InlineInfo> inlineCandidates = new PriorityQueue<InlineInfo>();
    private NodeMap<InlineInfo> inlineInfos;

    private StructuredGraph graph;
    private CiAssumptions assumptions;

    public InliningPhase(GraalCompiler compiler, GraalRuntime runtime, CiTarget target, Collection<Invoke> hints, CiAssumptions assumptions) {
        // TODO passing in the compiler object is bad - but currently this is the only way to access the extension phases that must be run immediately after parsing.
        this.compiler = compiler;
        this.runtime = runtime;
        this.target = target;
        this.hints = hints;
        this.assumptions = assumptions;
    }

    private abstract static class InlineInfo implements Comparable<InlineInfo> {
        public final Invoke invoke;
        public final double weight;
        public final int level;

        public InlineInfo(Invoke invoke, double weight, int level) {
            this.invoke = invoke;
            this.weight = weight;
            this.level = level;
        }

        @Override
        public int compareTo(InlineInfo o) {
            return (weight < o.weight) ? -1 : (weight > o.weight) ? 1 : 0;
        }

        public abstract void inline(StructuredGraph graph);
    }

    private class IntrinsicInlineInfo extends InlineInfo {
        public final StructuredGraph intrinsicGraph;

        public IntrinsicInlineInfo(Invoke invoke, StructuredGraph intrinsicGraph) {
            super(invoke, 0, 0);
            this.intrinsicGraph = graph;
        }

        @Override
        public void inline(StructuredGraph compilerGraph) {
            InliningUtil.inline(invoke, intrinsicGraph, true);
        }

        @Override
        public String toString() {
            return "intrinsic inlining " + CiUtil.format("%H.%n(%p):%r", invoke.callTarget().targetMethod(), false);
        }
    }

    private class StaticInlineInfo extends InlineInfo {
        public final RiResolvedMethod concrete;

        public StaticInlineInfo(Invoke invoke, double weight, int level, RiResolvedMethod concrete) {
            super(invoke, weight, level);
            this.concrete = concrete;
        }

        @Override
        public void inline(StructuredGraph compilerGraph) {
            StructuredGraph graph = GraphBuilderPhase.cachedGraphs.get(concrete);
            if (graph != null) {
                if (GraalOptions.TraceInlining) {
                    TTY.println("Reusing graph for %s", methodName(concrete, invoke));
                }
            } else {
                if (GraalOptions.TraceInlining) {
                    TTY.println("Building graph for %s, locals: %d, stack: %d", methodName(concrete, invoke), concrete.maxLocals(), concrete.maxStackSize());
                }
                graph = new StructuredGraph();
                new GraphBuilderPhase(runtime, concrete).apply(graph, context, true, false);

                compiler.runPhases(PhasePosition.AFTER_PARSING, graph);

                if (GraalOptions.ProbabilityAnalysis) {
                    new DeadCodeEliminationPhase().apply(graph, context, true, false);
                    new ComputeProbabilityPhase().apply(graph, context, true, false);
                }
                new CanonicalizerPhase(target, runtime, assumptions).apply(graph, context, true, false);

                if (GraalOptions.ParseBeforeInlining && !parsedMethods.containsKey(concrete)) {
                    parsedMethods.put(concrete, graphComplexity(graph));
                }
            }

            runtime.notifyInline(invoke.stateAfter().outermostFrameState().method(), concrete);
            InliningUtil.inline(invoke, graph, true);
        }

        @Override
        public String toString() {
            return "static inlining " + CiUtil.format("%H.%n(%p):%r", concrete, false);
        }
    }

    private class TypeGuardInlineInfo extends StaticInlineInfo {

        public final RiResolvedType type;
        public final double probability;

        public TypeGuardInlineInfo(Invoke invoke, double weight, int level, RiResolvedMethod concrete, RiResolvedType type, double probability) {
            super(invoke, weight, level, concrete);
            this.type = type;
            this.probability = probability;
        }

        @Override
        public void inline(StructuredGraph graph) {
            IsTypeNode isType = graph.unique(new IsTypeNode(invoke.callTarget().receiver(), type));
            FixedGuardNode guard = graph.add(new FixedGuardNode(isType));
            assert invoke.predecessor() != null;
            invoke.predecessor().replaceFirstSuccessor(invoke.node(), guard);
            guard.setNext(invoke.node());

            if (GraalOptions.TraceInlining) {
                TTY.println("inlining with type check, type probability: %5.3f", probability);
            }
            super.inline(graph);
        }

        @Override
        public String toString() {
            return "type-checked inlining " + CiUtil.format("%H.%n(%p):%r", concrete, false);
        }
    }

    private class AssumptionInlineInfo extends StaticInlineInfo {

        public AssumptionInlineInfo(Invoke invoke, double weight, int level, RiResolvedMethod concrete) {
            super(invoke, weight, level, concrete);
        }

        @Override
        public void inline(StructuredGraph graph) {
            if (GraalOptions.TraceInlining) {
                String targetName = CiUtil.format("%H.%n(%p):%r", invoke.callTarget().targetMethod(), false);
                String concreteName = CiUtil.format("%H.%n(%p):%r", concrete, false);
                TTY.println("recording concrete method assumption: %s -> %s", targetName, concreteName);
            }
            assumptions.recordConcreteMethod(invoke.callTarget().targetMethod(), concrete);
            super.inline(graph);
        }

        @Override
        public String toString() {
            return "inlining with assumption " + CiUtil.format("%H.%n(%p):%r", concrete, false);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void run(StructuredGraph graph) {
        this.graph = graph;
        inlineInfos = graph.createNodeMap();

        if (hints != null) {
            Iterable<? extends Node> hints = Util.uncheckedCast(this.hints);
            scanInvokes(hints, 0);
        } else {
            scanInvokes(graph.getNodes(InvokeNode.class), 0);
            scanInvokes(graph.getNodes(InvokeWithExceptionNode.class), 0);
        }

        while (!inlineCandidates.isEmpty()) {
            InlineInfo info = inlineCandidates.remove();
            double penalty = Math.pow(GraalOptions.InliningSizePenaltyExp, graph.getNodeCount() / (double) GraalOptions.MaximumDesiredSize) / GraalOptions.InliningSizePenaltyExp;
            if (info.weight > GraalOptions.MaximumInlineWeight / (1 + penalty * GraalOptions.InliningSizePenalty)) {
                if (GraalOptions.TraceInlining) {
                    TTY.println("not inlining (cut off by weight):");
                    while (info != null) {
                        TTY.println("    %f %s", info.weight, info);
                        info = inlineCandidates.poll();
                    }
                }
                return;
            }
            Iterable<Node> newNodes = null;
            if (info.invoke.node().isAlive()) {
                try {
                    info.inline(this.graph);
                    if (GraalOptions.TraceInlining) {
                        TTY.println("inlining %f: %s", info.weight, info);
                    }
                    if (GraalOptions.TraceInlining) {
                        context.observable.fireCompilationEvent(new CompilationEvent(null, "after inlining " + info, graph, true, false));
                    }
                    // get the new nodes here, the canonicalizer phase will reset the mark
                    newNodes = graph.getNewNodes();
                new CanonicalizerPhase(target, runtime, true, assumptions).apply(graph);
                    new PhiSimplificationPhase().apply(graph, context);
                    if (GraalOptions.Intrinsify) {
                        new IntrinsificationPhase(runtime).apply(graph, context);
                    }
                    if (GraalOptions.Meter) {
                        context.metrics.InlinePerformed++;
                    }
                } catch (CiBailout bailout) {
                    // TODO determine if we should really bail out of the whole compilation.
                    throw bailout;
                } catch (AssertionError e) {
                    throw new VerificationError(e).addContext(info.toString());
                } catch (RuntimeException e) {
                    throw new VerificationError(e).addContext(info.toString());
                } catch (VerificationError e) {
                    throw e.addContext(info.toString());
                }
            }
            if (newNodes != null && info.level <= GraalOptions.MaximumInlineLevel) {
                scanInvokes(newNodes, info.level + 1);
            }
        }
    }

    private void scanInvokes(Iterable<? extends Node> newNodes, int level) {
        graph.mark();
        for (Node node : newNodes) {
            if (node != null) {
                if (node instanceof Invoke) {
                    Invoke invoke = (Invoke) node;
                    scanInvoke(invoke, level);
                }
                for (Node usage : node.usages().snapshot()) {
                    if (usage instanceof Invoke) {
                        Invoke invoke = (Invoke) usage;
                        scanInvoke(invoke, level);
                    }
                }
            }
        }
    }

    private void scanInvoke(Invoke invoke, int level) {
        InlineInfo info = inlineInvoke(invoke, level);
        if (info != null) {
            if (GraalOptions.Meter) {
                context.metrics.InlineConsidered++;
            }

            inlineCandidates.add(info);
        }
    }

    private InlineInfo inlineInvoke(Invoke invoke, int level) {
        if (!checkInvokeConditions(invoke)) {
            return null;
        }
        RiResolvedMethod parent = invoke.stateAfter().method();
        MethodCallTargetNode callTarget = invoke.callTarget();
        Graph intrinsicGraph = runtime.intrinsicGraph(parent, invoke.bci(), callTarget.targetMethod(), callTarget.arguments());
        if (intrinsicGraph != null) {
            System.out.println("!!! intrinsic inlining " + invoke.callTarget().targetMethod());
            return new IntrinsicInlineInfo(invoke, graph);
        }

        if (callTarget.invokeKind() == InvokeKind.Special || callTarget.targetMethod().canBeStaticallyBound()) {
            if (checkTargetConditions(callTarget.targetMethod())) {
                double weight = inliningWeight(parent, callTarget.targetMethod(), invoke);
                return new StaticInlineInfo(invoke, weight, level, callTarget.targetMethod());
            }
            return null;
        }
        if (callTarget.receiver().exactType() != null) {
            RiResolvedType exact = callTarget.receiver().exactType();
            assert exact.isSubtypeOf(callTarget.targetMethod().holder()) : exact + " subtype of " + callTarget.targetMethod().holder();
            RiResolvedMethod resolved = exact.resolveMethodImpl(callTarget.targetMethod());
            if (checkTargetConditions(resolved)) {
                double weight = inliningWeight(parent, resolved, invoke);
                return new StaticInlineInfo(invoke, weight, level, resolved);
            }
            return null;
        }
        RiResolvedType holder = callTarget.targetMethod().holder();

        if (callTarget.receiver().declaredType() != null) {
            RiType declared = callTarget.receiver().declaredType();
            // the invoke target might be more specific than the holder (happens after inlining: locals lose their declared type...)
            // TODO (ls) fix this
            if (declared instanceof RiResolvedType && ((RiResolvedType) declared).isSubtypeOf(holder)) {
                holder = (RiResolvedType) declared;
            }
        }

        RiResolvedMethod concrete = holder.uniqueConcreteMethod(callTarget.targetMethod());
        if (concrete != null && assumptions != null) {
            if (checkTargetConditions(concrete)) {
                double weight = inliningWeight(parent, concrete, invoke);
                return new AssumptionInlineInfo(invoke, weight, level, concrete);
            }
            return null;
        }
        RiTypeProfile profile = parent.typeProfile(invoke.bci());
        if (profile != null && profile.probabilities != null && profile.probabilities.length > 0 && profile.morphism == 1) {
            if (GraalOptions.InlineWithTypeCheck) {
                // type check and inlining...
                concrete = profile.types[0].resolveMethodImpl(callTarget.targetMethod());
                if (concrete != null && checkTargetConditions(concrete)) {
                    double weight = inliningWeight(parent, concrete, invoke);
                    return new TypeGuardInlineInfo(invoke, weight, level, concrete, profile.types[0], profile.probabilities[0]);
                }
                return null;
            } else {
                if (GraalOptions.TraceInlining) {
                    TTY.println("not inlining %s because GraalOptions.InlineWithTypeCheck == false", methodName(callTarget.targetMethod(), invoke));
                }
                return null;
            }
        } else {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because no monomorphic receiver could be found", methodName(callTarget.targetMethod(), invoke));
            }
            return null;
        }
    }

    private static String methodName(RiResolvedMethod method) {
        return CiUtil.format("%H.%n(%p):%r", method, false) + " (" + method.codeSize() + " bytes)";
    }

    private static String methodName(RiResolvedMethod method, Invoke invoke) {
        if (invoke != null && invoke.stateAfter() != null) {
            RiMethod parent = invoke.stateAfter().method();
            return parent.name() + "@" + invoke.bci() + ": " + CiUtil.format("%H.%n(%p):%r", method, false) + " (" + method.codeSize() + " bytes)";
        } else {
            return CiUtil.format("%H.%n(%p):%r", method, false) + " (" + method.codeSize() + " bytes)";
        }
    }

    private boolean checkInvokeConditions(Invoke invoke) {
        if (!invoke.canInline()) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because the invoke is manually set to be non-inlinable", methodName(invoke.callTarget().targetMethod(), invoke));
            }
            return false;
        }
        if (invoke.stateAfter() == null) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because the invoke has no after state", methodName(invoke.callTarget().targetMethod(), invoke));
            }
            return false;
        }
        if (invoke.stateAfter().locksSize() > 0) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because of locks", methodName(invoke.callTarget().targetMethod(), invoke));
            }
            return false;
        }
        if (invoke.predecessor() == null) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because the invoke is dead code", methodName(invoke.callTarget().targetMethod(), invoke));
            }
            return false;
        }
        if (invoke.stateAfter() == null) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because of missing frame state", methodName(invoke.callTarget().targetMethod(), invoke));
            }
        }
        return true;
    }

    private boolean checkTargetConditions(RiMethod method) {
        if (!(method instanceof RiResolvedMethod)) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because it is unresolved", method.toString());
            }
            return false;
        }
        RiResolvedMethod resolvedMethod = (RiResolvedMethod) method;
        if (runtime.mustNotInline(resolvedMethod)) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because the CRI set it to be non-inlinable", methodName(resolvedMethod));
            }
            return false;
        }
        if (Modifier.isNative(resolvedMethod.accessFlags())) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because it is a native method", methodName(resolvedMethod));
            }
            return false;
        }
        if (Modifier.isAbstract(resolvedMethod.accessFlags())) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because it is an abstract method", methodName(resolvedMethod));
            }
            return false;
        }
        if (!resolvedMethod.holder().isInitialized()) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because of non-initialized class", methodName(resolvedMethod));
            }
            return false;
        }
        return true;
    }

    public static final Map<RiMethod, Integer> parsedMethods = new HashMap<RiMethod, Integer>();

    private double inliningWeight(RiResolvedMethod caller, RiResolvedMethod method, Invoke invoke) {
        double ratio;
        if (hints != null && hints.contains(invoke)) {
            ratio = 1000000;
        } else {
            if (GraalOptions.ProbabilityAnalysis) {
                ratio = invoke.node().probability();
            } else {
                RiTypeProfile profile = caller.typeProfile(invoke.bci());
                if (profile != null && profile.count > 0) {
                    RiResolvedMethod parent = invoke.stateAfter().method();
                    ratio = profile.count / (float) parent.invocationCount();
                } else {
                    ratio = 1;
                }
            }
        }

        final double normalSize;
        // TODO(ls) get rid of this magic, it's here to emulate the old behavior for the time being
        if (ratio < 0.01) {
            ratio = 0.01;
        }
        if (ratio < 0.5) {
            normalSize = 10 * ratio / 0.5;
        } else if (ratio < 2) {
            normalSize = 10 + (35 - 10) * (ratio - 0.5) / 1.5;
        } else if (ratio < 20) {
            normalSize = 35;
        } else if (ratio < 40) {
            normalSize = 35 + (350 - 35) * (ratio - 20) / 20;
        } else {
            normalSize = 350;
        }

        int count;
        if (GraalOptions.ParseBeforeInlining) {
            if (!parsedMethods.containsKey(method)) {
                StructuredGraph graph = new StructuredGraph();
                new GraphBuilderPhase(runtime, method, null).apply(graph, context, true, false);
                new CanonicalizerPhase(target, runtime, assumptions).apply(graph, context, true, false);
                count = graphComplexity(graph);
                parsedMethods.put(method, count);
            } else {
                count = parsedMethods.get(method);
            }
        } else {
            count = method.codeSize();
        }

        return count / normalSize;
    }

    public static int graphComplexity(StructuredGraph graph) {
        int result = 0;
        for (Node node : graph.getNodes()) {
            if (node instanceof ConstantNode || node instanceof LocalNode || node instanceof BeginNode || node instanceof ReturnNode || node instanceof UnwindNode) {
                result += 0;
            } else if (node instanceof PhiNode) {
                result += 5;
            } else if (node instanceof MergeNode || node instanceof Invoke || node instanceof LoopEndNode || node instanceof EndNode) {
                result += 0;
            } else if (node instanceof ControlSplitNode) {
                result += ((ControlSplitNode) node).blockSuccessorCount();
            } else {
                result += 1;
            }
        }
//        ReturnNode ret = graph.getReturn();
//        if (ret != null && ret.result() != null) {
//            if (ret.result().kind() == CiKind.Object && ret.result().exactType() != null) {
//                result -= 5;
//            }
//        }
        return Math.max(1, result);
    }

    public static ThreadLocal<ServiceLoader<InliningGuide>> guideLoader = new ThreadLocal<ServiceLoader<InliningGuide>>();

    private boolean overrideInliningDecision(int iteration, RiMethod caller, int bci, RiMethod target, boolean previousDecision) {
        ServiceLoader<InliningGuide> serviceLoader = guideLoader.get();
        if (serviceLoader == null) {
            serviceLoader = ServiceLoader.load(InliningGuide.class);
            guideLoader.set(serviceLoader);
        }

        boolean neverInline = false;
        boolean alwaysInline = false;
        for (InliningGuide guide : serviceLoader) {
            InliningHint hint = guide.getHint(iteration, caller, bci, target);

            if (hint == InliningHint.ALWAYS) {
                alwaysInline = true;
            } else if (hint == InliningHint.NEVER) {
                neverInline = true;
            }
        }

        if (neverInline && alwaysInline) {
            if (GraalOptions.TraceInlining) {
                TTY.println("conflicting inlining hints");
            }
        } else if (neverInline) {
            return false;
        } else if (alwaysInline) {
            return true;
        }
        return previousDecision;
    }
}
