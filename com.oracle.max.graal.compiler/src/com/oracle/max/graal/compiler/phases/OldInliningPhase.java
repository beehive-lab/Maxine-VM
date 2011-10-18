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
import com.oracle.max.graal.extensions.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.java.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class OldInliningPhase extends Phase {
    /*
     * - Detect method which only call another method with some parameters set to constants: void foo(a) -> void foo(a, b) -> void foo(a, b, c) ...
     *   These should not be taken into account when determining inlining depth.
     */

    public static final HashMap<RiMethod, Integer> methodCount = new HashMap<RiMethod, Integer>();

    private static final int MAX_ITERATIONS = 1000;

    private final GraalCompilation compilation;

    private int inliningSize;
    private final Collection<InvokeNode> hints;

    public OldInliningPhase(GraalContext context, GraalCompilation compilation, Collection<InvokeNode> hints) {
        super(context);
        this.compilation = compilation;
        this.hints = hints;
    }

    private Queue<InvokeNode> newInvokes = new ArrayDeque<InvokeNode>();
    private CompilerGraph graph;

    @Override
    protected void run(Graph graph) {
        this.graph = (CompilerGraph) graph;

        float ratio = GraalOptions.MaximumInlineRatio;
        inliningSize = compilation.method.codeSize();

        if (hints != null) {
            newInvokes.addAll(hints);
        } else {
            for (InvokeNode invoke : graph.getNodes(InvokeNode.class)) {
                newInvokes.add(invoke);
            }
        }

        for (int iterations = 0; iterations < MAX_ITERATIONS; iterations++) {
            Queue<InvokeNode> queue = newInvokes;
            newInvokes = new ArrayDeque<InvokeNode>();
            for (InvokeNode invoke : queue) {
                if (!invoke.isDeleted()) {
                    if (GraalOptions.Meter) {
                        context.metrics.InlineConsidered++;
                    }

                    RiResolvedMethod code = inlineInvoke(invoke, iterations, ratio);
                    if (code != null) {
                        if (graph.getNodeCount() > GraalOptions.MaximumInstructionCount) {
                            break;
                        }

                        inlineMethod(invoke, code);
                        if (GraalOptions.TraceInlining) {
                            if (methodCount.get(code) == null) {
                                methodCount.put(code, 1);
                            } else {
                                methodCount.put(code, methodCount.get(code) + 1);
                            }
                        }
                        if (GraalOptions.Meter) {
                            context.metrics.InlinePerformed++;
                        }
                    }
                }
            }
            if (newInvokes.isEmpty()) {
                break;
            }

//            new DeadCodeEliminationPhase().apply(graph);

            ratio *= GraalOptions.MaximumInlineRatio;
        }

        if (GraalOptions.TraceInlining) {
            int inlined = 0;
            int duplicate = 0;
            for (Map.Entry<RiMethod, Integer> entry : methodCount.entrySet()) {
                inlined += entry.getValue();
                duplicate += entry.getValue() - 1;
            }
            if (inlined > 0) {
                TTY.println("overhead: %d (%5.3f %%)", duplicate, duplicate * 100.0 / inlined);
            }
        }
    }

    private RiResolvedMethod inlineInvoke(InvokeNode invoke, int iterations, float ratio) {
        RiResolvedMethod parent = invoke.stateAfter().method();
        RiTypeProfile profile = parent.typeProfile(invoke.bci);
        if (GraalOptions.Intrinsify) {
            if (GraalOptions.Extend && intrinsicGraph(parent, invoke.bci, invoke.target, invoke.arguments()) != null) {
                return invoke.target;
            }
            if (compilation.compiler.runtime.intrinsicGraph(parent, invoke.bci, invoke.target, invoke.arguments()) != null) {
                // Always intrinsify.
                return invoke.target;
            }
        }
        if (!checkInvokeConditions(invoke)) {
            return null;
        }

        if (invoke.opcode() == Bytecodes.INVOKESPECIAL || invoke.target.canBeStaticallyBound()) {
            if (checkTargetConditions(invoke.target, iterations) && checkSizeConditions(parent, iterations, invoke.target, invoke, profile, ratio)) {
                return invoke.target;
            }
            return null;
        }
        if (invoke.receiver().exactType() != null) {
            RiResolvedType exact = invoke.receiver().exactType();
            assert exact.isSubtypeOf(invoke.target().holder()) : exact + " subtype of " + invoke.target().holder();
            RiResolvedMethod resolved = exact.resolveMethodImpl(invoke.target());
            if (checkTargetConditions(resolved, iterations) && checkSizeConditions(parent, iterations, resolved, invoke, profile, ratio)) {
                return resolved;
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
            if (checkTargetConditions(concrete, iterations) && checkSizeConditions(parent, iterations, concrete, invoke, profile, ratio)) {
                if (GraalOptions.TraceInlining) {
                    String targetName = CiUtil.format("%H.%n(%p):%r", invoke.target, false);
                    String concreteName = CiUtil.format("%H.%n(%p):%r", concrete, false);
                    TTY.println("recording concrete method assumption: %s -> %s", targetName, concreteName);
                }
                graph.assumptions().recordConcreteMethod(invoke.target, concrete);
                return concrete;
            }
            return null;
        }
        if (profile != null && profile.probabilities != null && profile.probabilities.length > 0 && profile.morphism == 1) {
            if (GraalOptions.InlineWithTypeCheck) {
                // type check and inlining...
                concrete = profile.types[0].resolveMethodImpl(invoke.target);
                if (concrete != null && checkTargetConditions(concrete, iterations) && checkSizeConditions(parent, iterations, concrete, invoke, profile, ratio)) {
                    IsTypeNode isType = compilation.graph.unique(new IsTypeNode(invoke.receiver(), profile.types[0]));
                    FixedGuardNode guard = compilation.graph.add(new FixedGuardNode(isType));
                    assert invoke.predecessor() != null;
                    invoke.predecessor().replaceFirstSuccessor(invoke, guard);
                    guard.setNext(invoke);

                    if (GraalOptions.TraceInlining) {
                        TTY.println("inlining with type check, type probability: %5.3f", profile.probabilities[0]);
                    }
                    return concrete;
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

    private String methodName(RiResolvedMethod method) {
        return CiUtil.format("%H.%n(%p):%r", method, false) + " (" + method.codeSize() + " bytes)";
    }

    private String methodName(RiResolvedMethod method, InvokeNode invoke) {
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

    private boolean checkTargetConditions(RiMethod method, int iterations) {
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

    private boolean checkSizeConditions(RiMethod caller, int iterations, RiResolvedMethod method, InvokeNode invoke, RiTypeProfile profile, float adjustedRatio) {
        int maximumSize = GraalOptions.MaximumTrivialSize;
        int maximumCompiledSize = GraalOptions.MaximumTrivialCompSize;
        double ratio = 0;
        if ((profile != null && profile.count > 0) || !GraalOptions.UseBranchPrediction) {
            RiResolvedMethod parent = invoke.stateAfter().method();
            if (GraalOptions.ProbabilityAnalysis) {
                ratio = invoke.probability();
            } else {
                ratio = profile.count / (float) parent.invocationCount();
            }
            if (ratio >= GraalOptions.FreqInlineRatio) {
                maximumSize = GraalOptions.MaximumFreqInlineSize;
                maximumCompiledSize = GraalOptions.MaximumFreqInlineCompSize;
            } else if (ratio >= 1 * (1 - adjustedRatio)) {
                maximumSize = GraalOptions.MaximumInlineSize;
                maximumCompiledSize = GraalOptions.MaximumInlineCompSize;
            }
        }
        if (hints != null && hints.contains(invoke)) {
            maximumSize = GraalOptions.MaximumFreqInlineSize;
            maximumCompiledSize = GraalOptions.MaximumFreqInlineCompSize;
        }
        boolean oversize;
        int compiledSize = method.compiledCodeSize();
        if (compiledSize < 0) {
            oversize = (method.codeSize() > maximumSize);
            if (oversize && GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because method.codeSize()=%d > maximumSize=%d ", methodName(method, invoke), method.codeSize(), maximumSize);
            }
        } else {
            oversize = (compiledSize > maximumCompiledSize);
        }
        if (oversize || iterations >= GraalOptions.MaximumInlineLevel || (method == compilation.method && iterations > GraalOptions.MaximumRecursiveInlineLevel)) {
            if (GraalOptions.TraceInlining) {
                TTY.println("not inlining %s because of size (bytecode: %d, bytecode max: %d, compiled: %d, compiled max: %d, ratio %5.3f, %s) or inlining level",
                                methodName(method, invoke), method.codeSize(), maximumSize, compiledSize, maximumCompiledSize, ratio, profile);
            }
            if (GraalOptions.Extend) {
                boolean newResult = overrideInliningDecision(iterations, caller, invoke.bci, method, false);
                if (GraalOptions.TraceInlining && newResult) {
                    TTY.println("overridden inlining decision");
                }
                return newResult;
            }

            return false;
        }
        if (GraalOptions.TraceInlining) {
            TTY.println("inlining %s (size: %d, max size: %d, ratio %5.3f, %s)", methodName(method, invoke), method.codeSize(), maximumSize, ratio, profile);
        }
        if (GraalOptions.Extend) {
            boolean newResult = overrideInliningDecision(iterations, caller, invoke.bci, method, true);
            if (GraalOptions.TraceInlining && !newResult) {
                TTY.println("overridden inlining decision");
            }
            return newResult;
        }
        return true;
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


    public static ThreadLocal<ServiceLoader<Intrinsifier>> intrinsicLoader = new ThreadLocal<ServiceLoader<Intrinsifier>>();

    private Graph intrinsicGraph(RiMethod parent, int bci, RiMethod target, List<ValueNode> arguments) {
        ServiceLoader<Intrinsifier> serviceLoader = intrinsicLoader.get();
        if (serviceLoader == null) {
            serviceLoader = ServiceLoader.load(Intrinsifier.class);
            intrinsicLoader.set(serviceLoader);
        }

        for (Intrinsifier intrinsifier : serviceLoader) {
            Graph result = intrinsifier.intrinsicGraph(compilation.compiler.runtime, parent, bci, target, arguments);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private void inlineMethod(InvokeNode invoke, RiResolvedMethod method) {
        CompilerGraph graph = null;
        if (GraalOptions.Intrinsify) {
            RiResolvedMethod parent = invoke.stateAfter().method();
            if (GraalOptions.Extend) {
                graph = (CompilerGraph) intrinsicGraph(parent, invoke.bci, method, invoke.arguments());
            }
            if (graph == null) {
                graph = (CompilerGraph) compilation.compiler.runtime.intrinsicGraph(parent, invoke.bci, method, invoke.arguments());
            }
        }
        if (graph != null) {
            if (GraalOptions.TraceInlining) {
                TTY.println("Using intrinsic graph");
            }
        } else {
            graph = GraphBuilderPhase.cachedGraphs.get(method);
        }

        if (graph != null) {
            if (GraalOptions.TraceInlining) {
                TTY.println("Reusing graph for %s", methodName(method, invoke));
            }
        } else {
            if (GraalOptions.TraceInlining) {
                TTY.println("Building graph for %s, locals: %d, stack: %d", methodName(method, invoke), method.maxLocals(), method.maxStackSize());
            }
            graph = new CompilerGraph(compilation.compiler.runtime);
            new GraphBuilderPhase(context, compilation.compiler.runtime, method, compilation.stats).apply(graph, true, false);
            if (GraalOptions.ProbabilityAnalysis) {
                new DeadCodeEliminationPhase(context).apply(graph, true, false);
                new ComputeProbabilityPhase(context).apply(graph, true, false);
            }
        }

        InliningUtil.inline(invoke, graph, newInvokes);

        if (GraalOptions.TraceInlining) {
            //printGraph("After inlining " + CiUtil.format("%H.%n(%p):%r", method, false), compilation.graph);
        }
    }
}
