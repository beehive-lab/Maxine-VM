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
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.VmThread;
import com.sun.max.unsafe.*;

/**
 * CPU sampling profiler. Runs a thread that periodically wakes up, stops all the threads, and records their stack.
 * Note that the stack is gathered regardless of the state of the thread, e.g., it may be blocked.
 * Period of sampling is measured in milliseconds.
 */
public final class CPUSamplingProfiler extends SamplingProfiler {

    /**
     * CPU sampling profiler name.
     */
    private static final String CPU_SAMPLING_PROFILER_NAME = "CPU Sampling Profiler";

    /**
     * Empirically derived default sampling period.
     */
    private static final int DEFAULT_PERIOD = 10;

    /**
     * The default flat argument.
     */
    private static final boolean DEFAULT_FLAT = true;

    /**
     * The minimum stack depth of a sample.
     */
    private static final int MINIMUM_DEPTH = 16;

    /**
     * The default stack depth of a sample.
     */
    private static final int DEFAULT_DEPTH = 16;

    public CPUSamplingProfiler(String optionPrefix, String optionValue) {
        super("CPUSamplingProfiler");
        this.samplingProfilerName = CPU_SAMPLING_PROFILER_NAME;
        this.useDedicatedThread = true;
        this.defaultPeriod = DEFAULT_PERIOD;
        this.defaultFlat = DEFAULT_FLAT;
        this.defaultDepth = DEFAULT_DEPTH;
        this.minimumDepth = MINIMUM_DEPTH;
        this.sampleCountIncrement = 1;
        this.stackTraceGatherer = new StackTraceGatherer(CPU_SAMPLING_PROFILER_NAME);
        this.optionPrefix = optionPrefix;
        create(optionValue);
    }

    @Override
    public void run() {
        theProfiler = VmThread.fromJava(this);
        long lastDump = System.nanoTime();
        while (true) {
            try {
                final int thisJiggle = rand.nextInt(jiggle);
                final int thisPeriod = samplePeriod + (rand.nextBoolean() ? thisJiggle : -thisJiggle);
                Thread.sleep(thisPeriod);
                final long now = System.nanoTime();
                if (isProfiling) {
                    if (logSampleTimes) {
                        boolean state = Log.lock();
                        Log.print(CPU_SAMPLING_PROFILER_NAME + " running at ");
                        Log.println(now);
                        Log.unlock(state);
                    }
                    // section should be synchronized with sorting and dumping
                    synchronized (this) {
                        stackTraceGatherer.submit();
                        sampleCount++;
                    }
                    if (dumpInterval > 0 && now > lastDump + dumpInterval * 1000000L) {
                        dumpTraces();
                        lastDump = now;
                    }
                }
            } catch (InterruptedException ex) {
            }
        }
    }

    class StackTraceGatherer extends SamplingProfiler.StackTraceGatherer {

        StackTraceGatherer(String name) {
            super(name);
        }

        protected ClassMethodActor getStackTraceGatheringStartMarker(VmThread vmThread) {
            return null;
        }

        @Override
        protected boolean operateOnThread(VmThread thread) {
            final boolean ignore = thread == theProfiler ||
                    (isSystemThread(thread) && !trackSystemThreads);
            return !ignore;
        }
    }

    protected void printSamplesInPeriodUnits(long samples) {
        long milliseconds = samples * samplePeriod;
        printSpacesForLongOfPrintSize(milliseconds, 8);
        Log.print(milliseconds);
        Log.print("ms");
    }
}
