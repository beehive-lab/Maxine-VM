/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.max.graal.compiler;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.alloc.*;
import com.oracle.max.graal.compiler.asm.*;
import com.oracle.max.graal.compiler.gen.*;
import com.oracle.max.graal.compiler.gen.LIRGenerator.DeoptimizationStub;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.observer.*;
import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.compiler.schedule.*;
import com.oracle.max.graal.extensions.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * This class encapsulates global information about the compilation of a particular method,
 * including a reference to the runtime, statistics about the compiled code, etc.
 */
public final class GraalCompilation {
    public final GraalCompiler compiler;
    public final RiResolvedMethod method;
    public final RiRegisterConfig registerConfig;
    public final CiStatistics stats;
    public final FrameState placeholderState;

    public final Graph<EntryPointNode> graph;

    private FrameMap frameMap;

    private LIR lir;

    public static ThreadLocal<ServiceLoader<Optimizer>> optimizerLoader = new ThreadLocal<ServiceLoader<Optimizer>>();

    /**
     * Creates a new compilation for the specified method and runtime.
     *
     * @param context the compilation context
     * @param compiler the compiler
     * @param method the method to be compiled or {@code null} if generating code for a stub
     * @param osrBCI the bytecode index for on-stack replacement, if requested
     * @param stats externally supplied statistics object to be used if not {@code null}
     */
    public GraalCompilation(GraalContext context, GraalCompiler compiler, RiResolvedMethod method, Graph<EntryPointNode> graph, int osrBCI, CiStatistics stats) {
        if (osrBCI != -1) {
            throw new CiBailout("No OSR supported");
        }
        this.compiler = compiler;
        this.graph = graph;
        this.method = method;
        this.stats = stats == null ? new CiStatistics() : stats;
        this.registerConfig = method == null ? compiler.compilerStubRegisterConfig : compiler.runtime.getRegisterConfig(method);
        this.placeholderState = method != null && method.minimalDebugInfo() ? new FrameState(method, 0, 0, 0, 0, false) : null;

        if (context().isObserved() && method != null) {
            context().observable.fireCompilationStarted(new CompilationEvent(this));
        }
    }

    public GraalCompilation(GraalContext context, GraalCompiler compiler, RiResolvedMethod method, int osrBCI, CiStatistics stats) {
        this(context, compiler, method, new Graph<EntryPointNode>(new EntryPointNode(compiler.runtime)), osrBCI, stats);
    }


    public void close() {
        //
    }

    public LIR lir() {
        return lir;
    }

    /**
     * Converts this compilation to a string.
     * @return a string representation of this compilation
     */
    @Override
    public String toString() {
        return "compile: " + method;
    }

    /**
     * Returns the frame map of this compilation.
     * @return the frame map
     */
    public FrameMap frameMap() {
        return frameMap;
    }

    private TargetMethodAssembler createAssembler() {
        AbstractAssembler asm = compiler.backend.newAssembler(registerConfig);
        TargetMethodAssembler assembler = new TargetMethodAssembler(context(), asm);
        assembler.setFrameSize(frameMap.frameSize());
        assembler.targetMethod.setCustomStackAreaOffset(frameMap.offsetToCustomArea());
        return assembler;
    }

    public CiResult compile() {
        CiTargetMethod targetMethod;
        try {
            emitHIR();
            emitLIR();
            targetMethod = emitCode();

            if (GraalOptions.Meter) {
                context().metrics.BytecodesCompiled += method.codeSize();
            }
        } catch (CiBailout b) {
            return new CiResult(null, b, stats);
        } catch (VerificationError e) {
            throw e.addContext("method", CiUtil.format("%H.%n(%p):%r", method));
        } catch (Throwable t) {
            if (GraalOptions.BailoutOnException) {
                return new CiResult(null, new CiBailout("Exception while compiling: " + method, t), stats);
            } else {
                throw new RuntimeException("Exception while compiling: " + method, t);
            }
        } finally {
            if (context().isObserved()) {
                context().observable.fireCompilationFinished(new CompilationEvent(this));
            }
        }

        return new CiResult(targetMethod, null, stats);
    }

    /**
     * Builds the graph, optimizes it.
     */
    public void emitHIR() {
        try {
            context().timers.startScope("HIR");

            if (graph.start().next() == null) {
                new GraphBuilderPhase(context(), compiler.runtime, method, stats).apply(graph);
                new DeadCodeEliminationPhase(context()).apply(graph);
            }

            if (GraalOptions.ProbabilityAnalysis) {
                new ComputeProbabilityPhase(context()).apply(graph);
            }

            if (GraalOptions.Intrinsify) {
                new IntrinsificationPhase(context(), compiler.runtime).apply(graph);
            }

            if (GraalOptions.Inline) {
                if (GraalOptions.UseNewInlining) {
                    new InliningPhase(context(), compiler.runtime, compiler.target, null).apply(graph);
                } else {
                    new OldInliningPhase(context(), this, null).apply(graph);
                }
                new DeadCodeEliminationPhase(context()).apply(graph);
            }

            if (GraalOptions.OptCanonicalizer) {
                new CanonicalizerPhase(context(), compiler.target).apply(graph);
            }

            if (GraalOptions.Extend) {
                extensionOptimizations(graph);
            }

            if (GraalOptions.OptLoops) {
                graph.mark();
                new FindInductionVariablesPhase(context()).apply(graph);
                if (GraalOptions.OptCanonicalizer) {
                    new CanonicalizerPhase(context(), compiler.target, true).apply(graph);
                }
            }

            if (GraalOptions.EscapeAnalysis) {
                new EscapeAnalysisPhase(this).apply(graph);
                new CanonicalizerPhase(context(), compiler.target).apply(graph);
            }

            if (GraalOptions.OptGVN) {
                new GlobalValueNumberingPhase(context()).apply(graph);
            }

            graph.mark();
            new LoweringPhase(context(), compiler.runtime).apply(graph);
            new CanonicalizerPhase(context(), compiler.target, true).apply(graph);

            if (GraalOptions.OptLoops) {
                graph.mark();
                new RemoveInductionVariablesPhase(context()).apply(graph);
                if (GraalOptions.OptCanonicalizer) {
                    new CanonicalizerPhase(context(), compiler.target, true).apply(graph);
                }
            }

            if (GraalOptions.Lower) {
                new FloatingReadPhase(context()).apply(graph);
                if (GraalOptions.OptReadElimination) {
                    new ReadEliminationPhase(context()).apply(graph);
                }
            }
            new RemovePlaceholderPhase(context()).apply(graph);
            new DeadCodeEliminationPhase(context()).apply(graph);
            IdentifyBlocksPhase schedule = new IdentifyBlocksPhase(context(), true);
            schedule.apply(graph);
            if (stats != null) {
                stats.loopCount = schedule.loopCount();
            }

            if (context().isObserved()) {
                Map<String, Object> debug = new HashMap<String, Object>();
                debug.put("schedule", schedule);
                context().observable.fireCompilationEvent(new CompilationEvent(this, "After IdentifyBlocksPhase", graph, true, false, debug));
            }

            List<Block> blocks = schedule.getBlocks();
            List<LIRBlock> lirBlocks = new ArrayList<LIRBlock>();
            Map<Block, LIRBlock> map = new HashMap<Block, LIRBlock>();
            for (Block b : blocks) {
                LIRBlock block = new LIRBlock(b.blockID());
                map.put(b, block);
                block.setInstructions(b.getInstructions());
                block.setLinearScanNumber(b.blockID());
                block.setLoopDepth(b.loopDepth());
                block.setLoopIndex(b.loopIndex());

                if (b.isLoopEnd()) {
                    block.setLinearScanLoopEnd();
                }

                if (b.isLoopHeader()) {
                    block.setLinearScanLoopHeader();
                }

                block.setFirstInstruction(b.firstNode());
                block.setLastInstruction(b.lastNode());
                lirBlocks.add(block);
            }

            for (Block b : blocks) {
                for (Block succ : b.getSuccessors()) {
                    map.get(b).blockSuccessors().add(map.get(succ));
                }

                for (Block pred : b.getPredecessors()) {
                    map.get(b).blockPredecessors().add(map.get(pred));
                }
            }

            NodeMap<LIRBlock> valueToBlock = new NodeMap<LIRBlock>(graph);
            for (LIRBlock b : lirBlocks) {
                for (Node i : b.getInstructions()) {
                    valueToBlock.set(i, b);
                }
            }
            LIRBlock startBlock = valueToBlock.get(graph.start());
            assert startBlock != null;
            assert startBlock.blockPredecessors().size() == 0;


            context().timers.startScope("Compute Linear Scan Order");
            try {
                ComputeLinearScanOrder clso = new ComputeLinearScanOrder(lirBlocks.size(), stats.loopCount, startBlock);
                List<LIRBlock> linearScanOrder = clso.linearScanOrder();
                List<LIRBlock> codeEmittingOrder = clso.codeEmittingOrder();

                int z = 0;
                for (LIRBlock b : linearScanOrder) {
                    b.setLinearScanNumber(z++);
                }

                lir = new LIR(startBlock, linearScanOrder, codeEmittingOrder, valueToBlock);

                if (context().isObserved()) {
                    context().observable.fireCompilationEvent(new CompilationEvent(this, "After linear scan order", graph, true, false));
                }
            } catch (AssertionError t) {
                    context().observable.fireCompilationEvent(new CompilationEvent(this, "AssertionError in ComputeLinearScanOrder", graph, true, false, true));
                throw t;
            } catch (RuntimeException t) {
                    context().observable.fireCompilationEvent(new CompilationEvent(this, "RuntimeException in ComputeLinearScanOrder", graph, true, false, true));
                throw t;
            } finally {
                context().timers.endScope();
            }
        } finally {
            context().timers.endScope();
        }
    }

    private void extensionOptimizations(Graph graph) {

        ServiceLoader<Optimizer> serviceLoader = optimizerLoader.get();
        if (serviceLoader == null) {
            serviceLoader = ServiceLoader.load(Optimizer.class);
            optimizerLoader.set(serviceLoader);
        }

        for (Optimizer o : serviceLoader) {
            o.optimize(compiler.runtime, graph);
        }
    }

    public void initFrameMap(int numberOfLocks) {
        frameMap = this.compiler.backend.newFrameMap(this, method, numberOfLocks);
    }

    private void emitLIR() {
        context().timers.startScope("LIR");
        try {
            if (GraalOptions.GenLIR) {
                context().timers.startScope("Create LIR");
                LIRGenerator lirGenerator = null;
                try {
                    initFrameMap(maxLocks());

                    lirGenerator = compiler.backend.newLIRGenerator(this);

                    for (LIRBlock b : lir.linearScanOrder()) {
                        lirGenerator.doBlock(b);
                    }
                } finally {
                    context().timers.endScope();
                }

                if (GraalOptions.PrintLIR && !TTY.isSuppressed()) {
                    LIRList.printLIR(lir.linearScanOrder());
                }

                new LinearScan(this, lir, lirGenerator, frameMap()).allocate();

                lir.setDeoptimizationStubs(lirGenerator.deoptimizationStubs());
            }
        } catch (Error e) {
            if (context().isObserved() && GraalOptions.PlotOnError) {
                context().observable.fireCompilationEvent(new CompilationEvent(this, e.getClass().getSimpleName() + " in emitLIR", graph, true, false, true));
            }
            throw e;
        } catch (RuntimeException e) {
            if (context().isObserved() && GraalOptions.PlotOnError) {
                context().observable.fireCompilationEvent(new CompilationEvent(this, e.getClass().getSimpleName() + " in emitLIR", graph, true, false, true));
            }
            throw e;
        } finally {
            context().timers.endScope();
        }
    }

    private CiTargetMethod emitCode() {
        if (GraalOptions.GenLIR && GraalOptions.GenCode) {
            context().timers.startScope("Create Code");
            try {
                final TargetMethodAssembler tma = createAssembler();
                final LIRAssembler lirAssembler = compiler.backend.newLIRAssembler(this, tma);
                lirAssembler.emitCode(lir.codeEmittingOrder());

                // generate code for slow cases
                lirAssembler.emitLocalStubs();

                // generate deoptimization stubs
                ArrayList<DeoptimizationStub> deoptimizationStubs = lir.deoptimizationStubs();
                if (deoptimizationStubs != null) {
                    for (DeoptimizationStub stub : deoptimizationStubs) {
                        lirAssembler.emitDeoptizationStub(stub);
                    }
                }

                // generate traps at the end of the method
                lirAssembler.emitTraps();

                CiTargetMethod targetMethod = tma.finishTargetMethod(method, compiler.runtime, lirAssembler.registerRestoreEpilogueOffset, false);
                if (!graph.start().assumptions().isEmpty()) {
                    targetMethod.setAssumptions(graph.start().assumptions());
                }

                if (context().isObserved()) {
                    context().observable.fireCompilationEvent(new CompilationEvent(this, "After code generation", graph, false, true, targetMethod));
                }
                return targetMethod;
            } finally {
                context().timers.endScope();
            }
        }

        return null;
    }

    /**
     * Gets the maximum number of locks in the graph's frame states.
     */
    public int maxLocks() {
        int maxLocks = 0;
        for (FrameState node : graph.getNodes(FrameState.class)) {
            int lockCount = 0;
            FrameState current = node;
            while (current != null) {
                lockCount += current.locksSize();
                current = current.outerFrameState();
            }
            if (lockCount > maxLocks) {
                maxLocks = lockCount;
            }
        }
        return maxLocks;
    }

    private GraalContext context() {
        return compiler.context;
    }

    public void printGraph(String phase, Graph graph) {
        if (context().isObserved()) {
            context().observable.fireCompilationEvent(new CompilationEvent(this, phase, graph, true, false));
        }
    }
}
