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
package com.sun.max.vm.compiler.instrument;

import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.adaptive.*;
import com.sun.max.vm.profile.*;

/**
 * This class collects dynamic profiling data for methods as they are running,
 * including such metrics as their invocation count, counters on particular locations
 * in the code (either at the bytecode level or the machine code level), etc.
 *
 * @author Ben L. Titzer
 */
public class MethodInstrumentation {

    /**
     * The method associated with this instrumentation.
     */
    protected final ClassMethodActor classMethodActor;

    /**
     * Any recompilation alarms attached to the compiled code of this method.
     */
    protected final AppendableSequence<RecompilationAlarm> recompilationAlarms;

    /**
     * Bytecode counters which have been used to instrument the machine code.
     */
    protected final AppendableSequence<LocationCounter> bytecodeCounters;

    private final IntHashMap<CallCounterTable> callCollection;

    /**
     * Hotpath counters associated with loop headers.
     */
    protected final IntHashMap<TreeAnchor> hotpathCounters;

    /**
     * @return the {@link TreeAnchor} associated with the specified bytecode location.
     */
    public TreeAnchor hotpathCounter(int position, int threshold) {
        TreeAnchor counter = hotpathCounters.get(position);
        if (counter == null) {
            counter = new TreeAnchor(new BytecodeLocation(classMethodActor, position), threshold);
            hotpathCounters.put(position, counter);
        }
        return counter;
    }

    /**
     * This constructor creates a new method instrumentation object for the specified class
     * method actor.
     * @param classMethodActor the class method actor
     */
    public MethodInstrumentation(ClassMethodActor classMethodActor) {
        this.classMethodActor = classMethodActor;
        this.bytecodeCounters = new ArrayListSequence<LocationCounter>();
        this.recompilationAlarms = new ArrayListSequence<RecompilationAlarm>();
        this.callCollection = new IntHashMap<CallCounterTable>();
        this.hotpathCounters = new IntHashMap<TreeAnchor>();
    }

    private RecompilationAlarm recompilationAlarm;
    private AlarmCounter recompilationAlarmCounter;
    public RecompilationAlarm recompilationAlarm() {
        if (recompilationAlarm == null) {
            recompilationAlarm = newRecompilationAlarm(0, true);
            if (recompilationAlarm != null) {
                recompilationAlarms.append(recompilationAlarm);
                recompilationAlarmCounter = recompilationAlarm.counter();
            }
        }
        return recompilationAlarm;
    }

    /**
     * This method creates a new alarm that triggers recompilation of this method when
     * the specified count is reached. This is typically used to create a counter
     * that is incremented at the beginning of a method, but can also be used
     * on loop edges as well, or on other expensive operations. Note that this method
     * will return {@code null} if no recompilation alarm should be used
     * (e.g. if recompilation is disabled).
     * @param count the threshold after which to trigger a recompilation; if 0, then use
     * the default recompilation threshold
     * @param entry a boolean denoting whether this recompilation alarm is likely to be
     * placed at the entrypoint of a method (and thus should be used in the estimation of
     * the invocation count)
     * @return a new recompilation alarm; null if no recompilation alarm should be used
     */
    public RecompilationAlarm newRecompilationAlarm(int count, boolean entry) {
        final int threshold = count == 0 ? AdaptiveCompilationScheme.defaultRecompilationThreshold0 : count;
        if (threshold > 0) {
            final RecompilationAlarm newRecompilationAlarm = new RecompilationAlarm(threshold, entry);
            recompilationAlarms.append(newRecompilationAlarm);
            return newRecompilationAlarm;
        }
        // threshold <= 0 implies that no recompilation alarm should be used.
        return null;
    }

    /**
     * This method creates a new counter for the specified range of locations. The counter
     * can be used to record the frequency of particular branches of the code. Note that
     * the range of locations specified can be in terms of bytecode indices or machine code
     * addresses.
     * @param start the start of the range of locations
     * @param end the end of the range of locations
     * @return a new location counter that can be used in instrumenting the code
     */
    public LocationCounter newLocationCounter(int start, int end) {
        final LocationCounter bytecodeCounter = new LocationCounter(start, end);
        bytecodeCounters.append(bytecodeCounter);
        return bytecodeCounter;
    }

    public LocationCounter newLocationCounter(int pos) {
        return newLocationCounter(pos, pos);
    }

    /**
     * This method computes an estimate of the number of times this method has been invoked.
     * This is typically taken from recompilation alarms placed at method entries. This method
     * is meant for use in e.g. inlining heuristics. This is only an estimate for a number of
     * reasons, e.g. another thread could currently be calling the method or each of the
     * counters involved in the estimate might not be thread safe, etc.
     * @return an estimate of the number of times this method has been invoked
     */
    public int estimateInvocationCount() {
        int total = 0;
        for (RecompilationAlarm alarm : recompilationAlarms) {
            if (alarm.entryPoint) {
                total += alarm.counter.getCount();
            }
        }
        return total;
    }

    /**
     * This class implements an alarm that can be used to trigger the recompilation of a method
     * after a specified number of invocations (or other events) occur.
     *
     * @author Ben L. Titzer
     */
    public class RecompilationAlarm implements Runnable {

        /**
         * Records whether this recompilation alarm was inserted at the entrypoint of a method;
         * if so, then it serves as an approximation of the invocation count.
         */
        private final boolean entryPoint;

        /**
         * The alarm counter object which is used to trigger recompilation.
         */
        private final AlarmCounter counter;

        public RecompilationAlarm(int executions, boolean entryPoint) {
            this.entryPoint = entryPoint;
            counter = new AlarmCounter(executions, this);
        }

        /**
         * Called when the alarm counter reaches its maximum value. This method calls
         * the adaptive compilation scheme to trigger a recompilation.
         */
        public void run() {
            final Phase phase = MaxineVM.host().phase();
            if (phase == MaxineVM.Phase.RUNNING) {
                // VM is started up completely, increase the optimization level
                AdaptiveCompilationScheme.increaseOptimizationLevel(classMethodActor, true, CompilationDirective.DEFAULT);
            } else {
                // if we are still starting the VM, reset the count and try later
                counter.reset();
            }
        }

        /**
         * Reset the counter to zero.
         * @param count
         */
        public void reset(int count) {
            counter.reset(count);
        }

        /**
         * Get the alarm counter associated with this recompilation alarm.
         * @return
         */
        public AlarmCounter counter() {
            return counter;
        }
    }

    /**
     * This class is to be used as instrumentation counter for static invocation.
     *
     * @author Yi Guo
     * @author Aziz Ghuloum
     */
    private final class StaticCallCounter extends LocationCounter {
        private String type;
        private StaticMethodActor receiverMethodActor;
        /**
         * @param offset of the bytecode instruction
         * @param type of the invocation: static/special...
         * @param method actor
         */
        public StaticCallCounter(int bytecodeOffset, String type, StaticMethodActor actor) {
            super(bytecodeOffset);
            this.type = type;
            this.receiverMethodActor = actor;
        }
    }

    /**
     * This class is used as a counter table for virtual method invocation.
     *
     * @author Yi Guo
     * @author Aziz Ghuloum
     */
    public final class CallCounterTable {
        /**
         * The linked list is designed not to break if accessed by two or more threads simultaneously, though may record may lost.
         * Currently we keep all records for statistics. It will be changed later.
         *
         * @author Yi Guo
         * @author Aziz Ghuloum
         */
        private final class CallEntry {
            private Hub hub;
            private int count = 1;
            private CallEntry(Hub hub, CallEntry next) {
                this.hub = hub;
                this.next = next;
            }
            private CallEntry next;
        }

        private int bytecodeOffset;
        private volatile CallEntry history;

        private CallCounterTable(int bytecodeOffset) {
            this.bytecodeOffset = bytecodeOffset;
        }

        public void record(Hub hub) {
            CallEntry p = history;
            while (p != null) {
                if (p.hub == hub) {
                    p.count++;
                    return;
                }
                p = p.next;
            }

            p = history;
            history = new CallEntry(hub, p);
        }
    }

    public CallCounterTable newCounterTable(int bytecodeOffset) {
        final CallCounterTable table = new CallCounterTable(bytecodeOffset);
        callCollection.put(bytecodeOffset, table);
        return table;
    }

    private static final int CALL_COUNT_THRESHOLD = 1000;
    private static final double FREQUENT_HUB_THRESHOLD = 0.9;

    public Hub getMostFrequentlyUsedHub(int bytecodeOffset) {
        final CallCounterTable table = callCollection.get(bytecodeOffset);
        if (table == null || table.history == null) {
            return null;
        }
        int total = 0;
        int max = 0;
        Hub maxHub = null;
        CallCounterTable.CallEntry entry = table.history;
        while (entry != null) {
            total += entry.count;
            if (entry.count > max) {
                max = entry.count;
                maxHub = entry.hub;
            }
            entry = entry.next;
        }
        if (total >= CALL_COUNT_THRESHOLD) {
            if (max > (total * FREQUENT_HUB_THRESHOLD)) {
                return maxHub;
            }
        }
        return null;
    }


}
