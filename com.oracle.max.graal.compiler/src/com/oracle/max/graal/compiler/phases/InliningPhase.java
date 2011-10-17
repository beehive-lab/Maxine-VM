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
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.cri.*;
import com.oracle.max.graal.extensions.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.java.*;
import com.sun.cri.bytecode.*;
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

    private int inliningSize;
    private final Collection<InvokeNode> hints;

    private final PriorityQueue<InlineInfo> inlineCandidates = new PriorityQueue<InlineInfo>();
    private NodeMap<InlineInfo> inlineInfos;

    private CompilerGraph graph;

    public InliningPhase(GraalContext context, GraalRuntime runtime, CiTarget target, Collection<InvokeNode> hints) {
        super(context);
        this.runtime = runtime;
        this.target = target;
        this.hints = hints;
    }

    private abstract static class InlineInfo implements Comparable<InlineInfo> {
        public final InvokeNode invoke;
        public final double weight;

        public InlineInfo(InvokeNode invoke, double weight) {
            this.invoke = invoke;
            this.weight = weight;
        }

        @Override
        public int compareTo(InlineInfo o) {
            return (weight < o.weight) ? -1 : (weight > o.weight) ? 1 : 0;
        }

        public abstract void inline(CompilerGraph graph);
    }

    private class StaticInlineInfo extends InlineInfo {
        public final RiResolvedMethod concrete;

        public StaticInlineInfo(InvokeNode invoke, double weight, RiResolvedMethod concrete) {
            super(invoke, weight);
            this.concrete = concrete;
        }

        @Override
        public void inline(CompilerGraph compilerGraph) {
            CompilerGraph graph = GraphBuilderPhase.cachedGraphs.get(concrete);
            if (graph != null) {
                if (GraalOptions.TraceInlining) {
                    TTY.println("Reusing graph for %s", methodName(concrete, invoke));
                }
            } else {
                if (GraalOptions.TraceInlining) {
                    TTY.println("Building graph for %s, locals: %d, stack: %d", methodName(concrete, invoke), concrete.maxLocals(), concrete.maxStackSize());
                }
                graph = new CompilerGraph(runtime);
                new GraphBuilderPhase(context, runtime, concrete, null).apply(graph, true, false);
                if (GraalOptions.ProbabilityAnalysis) {
                    new DeadCodeEliminationPhase(context).apply(graph, true, false);
                    new ComputeProbabilityPhase(context).apply(graph, true, false);
                }
                new CanonicalizerPhase(context, target).apply(graph, true, false);

                if (GraalOptions.ParseBeforeInlining && !parsedMethods.containsKey(concrete)) {
                    parsedMethods.put(concrete, graphComplexity(graph));
                }
            }

            InliningUtil.inline(invoke, graph, null);
        }

        @Override
        public String toString() {
            return "static inlining " + CiUtil.format("%H.%n(%p):%r", concrete, false);
        }
    }

    private class TypeGuardInlineInfo extends StaticInlineInfo {

        public final RiResolvedType type;
        public final double probability;

        public TypeGuardInlineInfo(InvokeNode invoke, double weight, RiResolvedMethod concrete, RiResolvedType type, double probability) {
            super(invoke, weight, concrete);
            this.type = type;
            this.probability = probability;
        }

        @Override
        public void inline(CompilerGraph graph) {
            IsTypeNode isType = graph.unique(new IsTypeNode(invoke.receiver(), type));
            FixedGuardNode guard = graph.add(new FixedGuardNode(isType));
            assert invoke.predecessor() != null;
            invoke.predecessor().replaceFirstSuccessor(invoke, guard);
            guard.setNext(invoke);

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

        public AssumptionInlineInfo(InvokeNode invoke, double weight, RiResolvedMethod concrete) {
            super(invoke, weight, concrete);
        }

        @Override
        public void inline(CompilerGraph graph) {
            if (GraalOptions.TraceInlining) {
                String targetName = CiUtil.format("%H.%n(%p):%r", invoke.target, false);
                String concreteName = CiUtil.format("%H.%n(%p):%r", concrete, false);
                TTY.println("recording concrete method assumption: %s -> %s", targetName, concreteName);
            }
            graph.assumptions().recordConcreteMethod(invoke.target, concrete);
            super.inline(graph);
        }

        @Override
        public String toString() {
            return "inlining with assumption " + CiUtil.format("%H.%n(%p):%r", concrete, false);
        }
    }

    @Override
    protected void run(Graph graph) {
        this.graph = (CompilerGraph) graph;
        inlineInfos = graph.createNodeMap();

        if (hints != null) {
            scanInvokes(hints);
        } else {
            scanInvokes(graph.getNodes(InvokeNode.class));
        }

        while (graph.getNodeCount() < GraalOptions.MaximumInstructionCount && !inlineCandidates.isEmpty()) {
            InlineInfo info = inlineCandidates.remove();
            if (info.weight > GraalOptions.MaximumInlineWeight) {
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
            if (info.invoke.isAlive()) {
                info.inline(this.graph);
                if (GraalOptions.TraceInlining) {
                    TTY.println("inlining %f: %s", info.weight, info);
                }
                if (GraalOptions.TraceInlining) {
                    //printGraph("After " + info, this.graph);
                }
                // get the new nodes here, the canonicalizer phase will reset the mark
                newNodes = graph.getNewNodes();
                new CanonicalizerPhase(context, target, true).apply(graph);
                new PhiSimplificationPhase(context).apply(graph);
                if (GraalOptions.Intrinsify) {
                    new IntrinsificationPhase(context, runtime).apply(graph);
                }
                if (GraalOptions.Meter) {
                    context.metrics.InlinePerformed++;
                }
            }
            if (newNodes != null) {
                scanInvokes(newNodes);
            }
        }
    }

    private void scanInvokes(Iterable<? extends Node> newNodes) {
        graph.mark();
        for (Node node : newNodes) {
            if (node != null) {
                if (node instanceof InvokeNode) {
                    InvokeNode invoke = (InvokeNode) node;
                    scanInvoke(invoke);
                }
                for (Node usage : node.usages().snapshot()) {
                    if (usage instanceof InvokeNode) {
                        InvokeNode invoke = (InvokeNode) usage;
                        scanInvoke(invoke);
                    }
                }
            }
        }
    }

    private void scanInvoke(InvokeNode invoke) {
        InlineInfo info = inlineInvoke(invoke);
        if (info != null) {
            if (GraalOptions.Meter) {
                context.metrics.InlineConsidered++;
            }

            inlineCandidates.add(info);
        }
    }

    private InlineInfo inlineInvoke(InvokeNode invoke) {
        if (!checkInvokeConditions(invoke)) {
            return null;
        }
        RiResolvedMethod parent = invoke.stateAfter().method();

        if (invoke.opcode() == Bytecodes.INVOKESPECIAL || invoke.target.canBeStaticallyBound()) {
            if (checkTargetConditions(invoke.target)) {
                double weight = inliningWeight(parent, invoke.target, invoke);
                return new StaticInlineInfo(invoke, weight, invoke.target());
            }
            return null;
        }
        if (invoke.receiver().exactType() != null) {
            RiResolvedType exact = invoke.receiver().exactType();
            assert exact.isSubtypeOf(invoke.target().holder()) : exact + " subtype of " + invoke.target().holder();
            RiResolvedMethod resolved = exact.resolveMethodImpl(invoke.target());
            if (checkTargetConditions(resolved)) {
                double weight = inliningWeight(parent, resolved, invoke);
                return new StaticInlineInfo(invoke, weight, resolved);
            }
            return null;
        }
        RiResolvedType holder = invoke.target().holder();

        if (invoke.receiver().declaredType() != null) {
            RiType declared = invoke.receiver().declaredType();
            // the invoke target might be more specific than the holder (happens after inlining: locals lose their declared type...)
            // TODO (ls) fix this
            if (declared instanceof RiResolvedType && ((RiResolvedType) declared).isSubtypeOf(invoke.target().holder())) {
                holder = (RiResolvedType) declared;
            }
        }

        RiResolvedMethod concrete = holder.uniqueConcreteMethod(invoke.target);
        if (concrete != null) {
            if (checkTargetConditions(concrete)) {
                double weight = inliningWeight(parent, concrete, invoke);
                return new AssumptionInlineInfo(invoke, weight, concrete);
            }
            return null;
        }
        RiTypeProfile profile = parent.typeProfile(invoke.bci);
        if (profile != null && profile.probabilities != null && profile.probabilities.length > 0 && profile.morphism == 1) {
            if (GraalOptions.InlineWithTypeCheck) {
                // type check and inlining...
                concrete = profile.types[0].resolveMethodImpl(invoke.target);
                if (concrete != null && checkTargetConditions(concrete)) {
                    double weight = inliningWeight(parent, concrete, invoke);
                    return new TypeGuardInlineInfo(invoke, weight, concrete, profile.types[0], profile.probabilities[0]);
                }
                return null;
            } else {
                if (GraalOptions.TraceInlining) {
                    TTY.println("not inlining %s because GraalOptions.InlineWithTypeCheck == false", methodName(invoke.target, invoke));
                }
                return null;
            }
        } else {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because no monomorphic receiver could be found", methodName(invoke.target, invoke));
            }
            return null;
        }
    }

    private static String methodName(RiResolvedMethod method) {
        return CiUtil.format("%H.%n(%p):%r", method, false) + " (" + method.codeSize() + " bytes)";
    }

    private static String methodName(RiResolvedMethod method, InvokeNode invoke) {
        if (invoke != null) {
            RiMethod parent = invoke.stateAfter().method();
            return parent.name() + "@" + invoke.bci + ": " + CiUtil.format("%H.%n(%p):%r", method, false) + " (" + method.codeSize() + " bytes)";
        } else {
            return CiUtil.format("%H.%n(%p):%r", method, false) + " (" + method.codeSize() + " bytes)";
        }
    }

    private boolean checkInvokeConditions(InvokeNode invoke) {
        if (!invoke.canInline()) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because the invoke is manually set to be non-inlinable", methodName(invoke.target, invoke));
            }
            return false;
        }
        if (invoke.stateAfter() == null) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because the invoke has no after state", methodName(invoke.target, invoke));
            }
            return false;
        }
        if (invoke.stateAfter().locksSize() > 0) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because of locks", methodName(invoke.target, invoke));
            }
            return false;
        }
        if (invoke.predecessor() == null) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because the invoke is dead code", methodName(invoke.target, invoke));
            }
            return false;
        }
        if (invoke.stateAfter() == null) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because of missing frame state", methodName(invoke.target, invoke));
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

    private double inliningWeight(RiResolvedMethod caller, RiResolvedMethod method, InvokeNode invoke) {
        double ratio;
        if (hints != null && hints.contains(invoke)) {
            ratio = 1000000;
        } else {
            if (GraalOptions.ProbabilityAnalysis) {
                ratio = invoke.probability();
            } else {
                RiTypeProfile profile = caller.typeProfile(invoke.bci);
                if (profile != null && profile.count > 0) {
                    RiResolvedMethod parent = invoke.stateAfter().method();
                    ratio = profile.count / (float) parent.invocationCount();
                } else {
                    ratio = 1;
                }
            }
        }

        final double normalSize;
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
                    CompilerGraph graph = new CompilerGraph(runtime);
                    new GraphBuilderPhase(context, runtime, method, null).apply(graph, true, false);
                    new CanonicalizerPhase(context, target).apply(graph, true, false);
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

    public static int graphComplexity(CompilerGraph graph) {
        int result = 0;
        for (Node node : graph.getNodes()) {
            if (node instanceof ConstantNode || node instanceof LocalNode || node instanceof EntryPointNode || node instanceof ReturnNode || node instanceof UnwindNode) {
                result += 0;
            } else if (node instanceof PhiNode) {
                result += 5;
            } else if (node instanceof MergeNode || node instanceof InvokeNode || node instanceof LoopEndNode || node instanceof EndNode) {
                result += 0;
            } else if (node instanceof ControlSplitNode) {
                result += ((ControlSplitNode) node).blockSuccessorCount();
            } else {
                result += 1;
            }
        }
//        ReturnNode ret = graph.getReturn();
//        if (ret != null && ret.result() != null) {
//            if (ret.result().kind == CiKind.Object && ret.result().exactType() != null) {
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
