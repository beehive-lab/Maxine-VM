/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.profilers.sampling;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.ProgramError;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.SymbolTable;
import com.sun.max.vm.classfile.constant.Utf8Constant;
import com.sun.max.vm.heap.Heap;
import com.sun.max.vm.layout.Layout;
import com.sun.max.vm.reference.Reference;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.VmThread;
import com.sun.max.unsafe.*;
import com.sun.max.vm.thread.VmThreadLocal;
import com.sun.max.vm.type.SignatureDescriptor;

import static com.sun.max.vm.thread.VmThread.currentTLA;
import static com.sun.max.vm.thread.VmThreadLocal.ETLA;

/**
 * Heap sampling profiler. A thread allocating memory periodically stops all the threads, and records it own stack.
 * Period of sampling is measured in bytes.
 */
public final class HeapSamplingProfiler extends SamplingProfiler {
    /*
     * Thread being sampled.
     */
    private VmThread sampledThread;

    /**
     * Current period in bytes.
     */
    private int currentPeriod;

    /**
     * Heap sampling profiler name.
     */
    private static final String HEAP_SAMPLING_PROFILER_NAME = "Heap Sampling Profiler";

    /**
     * Empirically derived default sampling period.
     */
    private static final int DEFAULT_PERIOD = 4096;

    /**
     * The default flat argument.
     */
    private static final boolean DEFAULT_FLAT = false;

    /**
     * The minimum stack depth of a sample.
     */
    private static final int MINIMUM_DEPTH = 16;

    /**
     * The default stack depth of a sample.
     */
    private static final int DEFAULT_DEPTH = 16;

    /**
     *  Class method actor marking the method in the stack below which the methods should be gathered.
     */
    private static final ClassMethodActor stackTraceGatheringStartMarker = initializeStackTraceGatheringStartMarker();

    /**
     * Per thread count of heap allocations used during heap sampling profiling. When its value exceeds
     * {@link #currentPeriod} it is reset and a corresponding number of samples is recorded.
     */
    private static final VmThreadLocal SAMPLING_ALLOCATION_COUNTER =
            new VmThreadLocal("SAMPLING_ALLOCATION_COUNTER", false, "Allocation counter used during heap sampling profiling", VmThreadLocal.Nature.Single);

    private static long getSamplingAllocationCounterForCurrentThread() {
        final Pointer etla = ETLA.load(currentTLA());
        Pointer apt = SAMPLING_ALLOCATION_COUNTER.load(etla);
        return apt.asSize().toLong();
    }

    private static void incrementSamplingAllocationCounterForCurrentThread(Size size) {
        final Pointer etla = ETLA.load(currentTLA());
        Pointer apt = SAMPLING_ALLOCATION_COUNTER.load(etla);
        SAMPLING_ALLOCATION_COUNTER.store(etla, apt.plus(size));
    }

    private static void resetSamplingAllocationCounterForCurrentThread() {
        final Pointer etla = ETLA.load(currentTLA());
        SAMPLING_ALLOCATION_COUNTER.store(etla, Word.zero());
    }

    /**
     * Initializer of {@link #stackTraceGatheringStartMarker}.
     */
    private static ClassMethodActor initializeStackTraceGatheringStartMarker() {
        Utf8Constant name = SymbolTable.makeSymbol("sampleAllocation");
        return ClassActor.fromJava(HeapSamplingProfiler.class).findClassMethodActor(name, null);
    }

    public HeapSamplingProfiler(String optionPrefix, String optionValue) {
        super(HEAP_SAMPLING_PROFILER_NAME);
        this.samplingProfilerName = HEAP_SAMPLING_PROFILER_NAME;
        this.defaultPeriod = DEFAULT_PERIOD;
        this.defaultFlat = DEFAULT_FLAT;
        this.defaultDepth = DEFAULT_DEPTH;
        this.minimumDepth = MINIMUM_DEPTH;
        this.stackTraceGatherer = new StackTraceGatherer(HEAP_SAMPLING_PROFILER_NAME);
        this.optionPrefix = optionPrefix;
        create(optionValue);
        this.currentPeriod = this.samplePeriod;
        if (trackSystemThreads) {
            stackTraceGatherer.initVMOperationThreadSample();
        }
    }

    @Override
    public void run() {
        theProfiler = VmThread.fromJava(this);
        if (!useDedicatedThread && dumpInterval == 0) {
            throw ProgramError.unexpected("Dedicated sampling profiling thread is not expected to run");
        }
        while (true) {
            try {
                Thread.sleep(dumpInterval);
                if (isProfiling) {
                    if (logSampleTimes) {
                        final long now = System.nanoTime();
                        boolean state = Log.lock();
                        Log.print(HEAP_SAMPLING_PROFILER_NAME + " running at ");
                        Log.println(now);
                        Log.unlock(state);
                    }
                    dumpTraces();
                }
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * Samples allocation of an object.
     */
    @NEVER_INLINE
    public void sampleAllocation(Object allocatedObject) {
        incrementSamplingAllocationCounterForCurrentThread(Layout.size(Reference.fromJava(allocatedObject)));
        long samples = isProfiling ? getSamplingAllocationCounterForCurrentThread() / currentPeriod : 0;
        if (samples > 0) {
            VmThread currentThread = VmThread.current();
            // do not profile profiling thread
            if (currentThread == theProfiler) {
                return;
            }
            if (sampledThread == VmThread.current()) {
                throw ProgramError.unexpected("Recursive allocation sampling. Try to increase sampling period.");
            }
            if (currentThread.isVmOperationThread()) {
                if (trackSystemThreads) {
                    // save possibly live sampling data
                    VmThread savedSampledThread = sampledThread;
                    long savedSamplesIncrement = sampleCountIncrement;
                    // do sampling of {@link VMOperation} thread
                    sampledThread = currentThread;
                    sampleCountIncrement = samples;
                    stackTraceGatherer.doVMOperationThread();
                    sampleCount += sampleCountIncrement;
                    currentPeriod = samplePeriod + (rand.nextBoolean() ? rand.nextInt(jiggle) : -rand.nextInt(jiggle));
                    // restore possibly live sampling data
                    sampleCountIncrement = savedSamplesIncrement;
                    sampledThread = savedSampledThread;
                    resetSamplingAllocationCounterForCurrentThread();
                }
            } else {
                // section should be synchronized with sorting, dumping and with itself
                synchronized (this) {
                    sampledThread = VmThread.current();
                    sampleCountIncrement = samples;
                    stackTraceGatherer.submit();
                    sampleCount += sampleCountIncrement;
                    currentPeriod = samplePeriod + (rand.nextBoolean() ? rand.nextInt(jiggle) : -rand.nextInt(jiggle));
                    sampledThread = null;
                    resetSamplingAllocationCounterForCurrentThread();
                }
            }
        }
    }

    class StackTraceGatherer extends SamplingProfiler.StackTraceGatherer {

        StackTraceGatherer(String name) {
            super(name);
        }

        @Override
        protected ClassMethodActor getStackTraceGatheringStartMarker(VmThread vmThread) {
            return stackTraceGatheringStartMarker;
        }

        /**
         * Operate on thread allocating memory.
         */
        @Override
        protected boolean operateOnThread(VmThread thread) {
            final boolean operate = thread == sampledThread &&
                    (!isSystemThread(thread) || trackSystemThreads);
            return operate;
        }
    }

    protected void printSamplesInPeriodUnits(long samples) {
        long kilobytes = samples * samplePeriod / 1024;
        printSpacesForLongOfPrintSize(kilobytes, 8);
        Log.print(kilobytes);
        Log.print("KB");
    }
}
