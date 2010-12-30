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
package com.sun.max.vm.cps.hotpath.compiler;

import java.util.*;

import com.sun.max.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.AsynchronousProfiler.*;
import com.sun.max.vm.cps.hotpath.compiler.Console.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.cps.tir.pipeline.*;
import com.sun.max.vm.thread.*;

/**
 * This class is responsible for capturing trace events from JITed methods. Each thread has its own
 * {@linkplain ObjectThreadLocal thread-local} tracer object associated with it.
 *
 * @author Michael Bebenita
 */
public abstract class Tracer {
    public static OptionSet optionSet = new OptionSet();
    public static Option<Integer> resetThreshold = optionSet.newIntegerOption("RS", 1000, "Reset threhold.");
    public static Option<Integer> recordingThreshold = optionSet.newIntegerOption("RT", 10, "Record threhold.");
    public static Option<Integer> numberOfRecordingTriesAllowed = optionSet.newIntegerOption("TR", 3, "Tries.");
    public static Option<Integer> numberOfCyclesAllowed = optionSet.newIntegerOption("CYCLES", 0, "Number of times to unroll hotpaths.");
    public static Option<Integer> numberOfBackwardJumpsAllowed = optionSet.newIntegerOption("BJUMPS", 3, "Number of times to inner cycles.");
    public static Option<Integer> numberOfBytecodesAllowed = optionSet.newIntegerOption("LEN", 1024, "Number of bytecodes allowed.");
    public static Option<Boolean> nestTrees = optionSet.newBooleanOption("NEST", true, "Nest trees.");
    public static Option<Boolean> printState = optionSet.newBooleanOption("PT", true, "(P)rints the Tracers's Execution.");
    public static Option<Integer> numberOfBranchesAllowed = optionSet.newIntegerOption("BRANCHES", 64, "Number of branch traces allowed.");
    public static Option<Integer> maxTries = optionSet.newIntegerOption("TRIES", 16, "Number of tries.");

    public static Option<Integer> branchMetricThreshold = optionSet.newIntegerOption("BRANCH_METRIC", 16, "Brach metric threshold.");

    public enum AbortReason {
        FAILED_LENGTH_METRIC, UNSUPPORTED_BYTECODE, EXCEEDED_NUMBER_OF_BACKWARD_JUMPS_ALLOWED, REACHED_TREE_ANCHOR, BREACHED_SCOPE, UNEXPECTED_EXCEPTION, FAILED_BRANCH_METRIC
    }

    public enum BranchMetric {
        EXPLICIT_BRANCH,
        EXPLICIT_NULL_CHECK,
        IMPLICIT_NULL_CHECK,
        IMPLICIT_TYPE_CHECK,
        IMPLICIT_BRANCH,
        SWITCH_BRANCH;

        public int weight() {
            switch (this) {
                case EXPLICIT_BRANCH:
                    return 1;
                case EXPLICIT_NULL_CHECK:
                    return 1;
                case IMPLICIT_NULL_CHECK:
                    return 0;
                case IMPLICIT_TYPE_CHECK:
                    return 0;
                case IMPLICIT_BRANCH:
                    return 0;
                case SWITCH_BRANCH:
                    return 2;
            }
            return 0;
        }
    }

    private static boolean evaluateBranchMetric(Scope scope) {
        return scope.branchMetric < branchMetricThreshold.getValue();
    }

    private static boolean evaluateLengthMetric(Scope scope) {
        return scope.numberOfBytecodes < numberOfBytecodesAllowed.getValue();
    }

    public abstract class Scope {
        protected final TreeAnchor treeAnchor;
        protected TirTrace trace;
        protected TirRecorder recorder;

        private int numberOfCycles;
        private int numberOfBackwardJumps;
        private int numberOfBytecodes;

        protected int branchMetric;

        public Scope(TreeAnchor anchor) {
            treeAnchor = anchor;
            isRecording = true;
            isTracing = true;
        }

        /**
         * Indicates that the {@link Tracer.Scope} is receiving trace notifications from trace instrumented code.
         */
        protected boolean isTracing = false;

        /**
         * Indicates that the {@link Tracer.Scope} is currently recording trace notifications.
         */
        protected boolean isRecording = false;

        public void completeRecording(TreeAnchor anchor) {
            isRecording = false;
            isTracing = false;
        }

        public void record(BytecodeLocation location) {
            recorder.record(location);
        }

        public void recordNesting(TreeAnchor anchor, Bailout bailout) {
            recorder.recordNesting(anchor, bailout);
        }

        public void abort(AbortReason abortReason) {
            isRecording = false;
            isTracing = false;
            if (printState.getValue()) {
                Console.println(Color.RED, abortReason.toString());
            }
        }

        public void profileBranch(BranchMetric metric) {
            branchMetric += metric.weight();
        }
    }

    public class Trunk extends Scope {
        private final TirTree tree;
        public Trunk(TreeAnchor anchor) {
            super(anchor);
            tree = TirState.fromAnchor(anchor);
            trace = new TirTrace(tree, null);
            recorder = new TirRecorder(Tracer.this, this, tree.entryState().copy(), trace);
        }

        @Override
        public void completeRecording(TreeAnchor anchor) {
            super.completeRecording(anchor);
            tree.append(trace);
            tree.commitFlags();
            trace.complete(recorder.takeSnapshot(anchor));
            treeAnchor.setTree(tree);
            if (printState.getValue()) {
                TirPrintSink.print(tree);
            }
            TirCompiler.compile(tree);
            AsynchronousProfiler.event(CounterMetric.TRUNKS);
        }
    }

    public class Branch extends Scope {
        private final TirTree tree;
        private final TraceAnchor traceAnchor;
        public Branch(TreeAnchor treeAnchor, TraceAnchor traceAnchor, TirGuard guard) {
            super(treeAnchor);
            this.traceAnchor = traceAnchor;
            this.tree = treeAnchor.tree();
            this.trace = new TirTrace(tree, traceAnchor);
            this.recorder = new TirRecorder(Tracer.this, this, guard.state().copy(), trace);
        }

        @Override
        public void completeRecording(TreeAnchor anchor) {
            super.completeRecording(anchor);
            tree.append(trace);
            tree.commitFlags();
            trace.complete(recorder.takeSnapshot(anchor));
            traceAnchor.setTrace(trace);
            if (printState.getValue()) {
                TirPrintSink.print(tree);
            }
            TirCompiler.compile(tree);
            AsynchronousProfiler.event(CounterMetric.BRANCHES);
        }
    }

    private List<Scope> scopes = new ArrayList<Scope>();
    private BytecodeLocation currentLocation;

    protected Scope active() {
        return Utils.last(scopes);
    }

    protected boolean begin(TreeAnchor anchor) {
        // Have we tried to record a trace too many times? If so, don't record anymore.
        if (anchor.incrementNumberOfTries() >= maxTries.getValue()) {
            return false;
        }

        return true;
    }

    protected void finish() {
        // TODO Auto-generated method stub

    }

    public void abort(AbortReason reason) {
        for (Scope scope : scopes) {
            if (scope.isRecording) {
                scope.abort(reason);
            }
        }
    }

    public boolean isTracing() {
        for (Scope scope : scopes) {
            if (scope.isTracing) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} if any
     */
    public boolean isRecording() {
        for (Scope scope : scopes) {
            if (scope.isRecording) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is called whenever a backward jump is taken and before the target is executed.
     */
    public void visitAnchor(TreeAnchor anchor) {
        if (isRecording()) {
            // Attempt to complete traces.
            for (int i = scopes.size() - 1; i >= 0; i--) {
                final Scope scope = scopes.get(i);
                if (scope.isRecording) {
                    if (scope.treeAnchor == anchor) {
                        if (scope.numberOfCycles++ >= numberOfCyclesAllowed.getValue()) {
                            scope.completeRecording(anchor);
                        }
                        return;
                    }
                    if (scope.numberOfBackwardJumps++ >= numberOfBackwardJumpsAllowed.getValue()) {
                        scope.abort(AbortReason.EXCEEDED_NUMBER_OF_BACKWARD_JUMPS_ALLOWED);
                    }
                }
            }
            // We've reached an out of scope TreeAnchor, try to nest tree.
            if (anchor.hasTree()) {
                if (nestTrees.getValue()) {
                    final Bailout bailout = evaluateTree(anchor.tree());
                    for (Scope scope : scopes) {
                        if (scope.isRecording) {
                            scope.recordNesting(anchor, bailout);
                        }
                    }
                } else {
                    abort(AbortReason.REACHED_TREE_ANCHOR);
                }
            }
        } else {
            anchor.incrementFrequency();
            if (anchor.hasTree()) {
                AsynchronousProfiler.eventExecute(anchor.tree());
                final Bailout bailout = evaluateTree(anchor.tree());
                AsynchronousProfiler.eventBailout(anchor.tree(), bailout);
                if (bailout.tree().traces().size() < numberOfBranchesAllowed.getValue() + 1) {
                    createScope(bailout);
                }
                return;
            }
            if (anchor.frequency() >= resetThreshold.getValue()) {
                anchor.resetFrequency();
            }
            if (anchor.frequency() >= recordingThreshold.getValue() &&
                anchor.frequency() < recordingThreshold.getValue() + numberOfRecordingTriesAllowed.getValue()) {
                createScope(anchor);
            }
        }
    }

    public void visitBytecode(BytecodeLocation location) {
        setCurrentLocation(location);
        try {
            for (Scope scope : scopes) {
                if (scope.isRecording) {
                    if (evaluateBranchMetric(scope) == false) {
                        scope.abort(AbortReason.FAILED_BRANCH_METRIC);
                        continue;
                    }

                    if (evaluateLengthMetric(scope) == false) {
                        scope.abort(AbortReason.FAILED_LENGTH_METRIC);
                        continue;
                    }

                    scope.numberOfBytecodes++;
                    scope.record(location);
                }
            }
        } catch (Throwable e) {
            abort(AbortReason.UNEXPECTED_EXCEPTION);
            e.printStackTrace();
        }

    }

    private void setCurrentLocation(BytecodeLocation location) {
        currentLocation = location;
    }

    protected BytecodeLocation currentLocation() {
        return currentLocation;
    }

    private void createScope(Bailout bailout) {
        final TreeAnchor treeAnchor = bailout.tree().anchor();
        TraceAnchor traceAnchor = bailout.guard().anchor();
        if (traceAnchor == null) {
            traceAnchor = new TraceAnchor(bailout.guard().location(), bailout.guard());
            bailout.guard().setAnchor(traceAnchor);
        }
        createScope(treeAnchor, traceAnchor, bailout.guard());
    }

    private void createScope(TreeAnchor treeAnchor, TraceAnchor traceAnchor, TirGuard guard) {
        if (printState.getValue()) {
            Console.println(Color.RED, "RECORDING STARTED AT SIDE EXIT @" + traceAnchor);
        }
        final Scope scope = new Branch(treeAnchor, traceAnchor, guard);
        scopes.add(scope);
    }

    private void createScope(TreeAnchor anchor) {
        if (printState.getValue()) {
            Console.println(Color.RED, "RECORDING STARTED @" + anchor);
        }
        final Scope scope = new Trunk(anchor);
        scopes.add(scope);
    }

    public List<Scope> scopes() {
        return  scopes;
    }

    protected abstract boolean evaluateIcmpBranch(BranchCondition condition);
    protected abstract boolean evaluateAcmpBranch(BranchCondition condition);
    protected abstract boolean evaluateNullBranch(BranchCondition condition);
    protected abstract boolean evaluateBranch(BranchCondition condition);
    protected abstract Bailout evaluateTree(TirTree tree);

    protected abstract Object evaluateObject(int stackDepth);
    protected abstract int evaluateInt(int stackDepth);
}
