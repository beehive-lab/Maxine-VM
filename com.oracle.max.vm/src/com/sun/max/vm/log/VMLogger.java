/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.log;

import static com.sun.max.vm.log.VMLog.*;

import java.util.*;
import java.util.regex.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * A {@link VMLogger} defines a set of operations, cardinality {@code N} each identified
 * by an {@code int} code in the range {@code [0 .. N-1]}.
 * A series of "log" methods are provided, that take the operation code
 * and a varying number of {@link Word} arguments (up to {@link VMLog.Record#MAX_ARGS}).
 * Currently these must not be {@link Reference} types as no GC support is provided
 * for values in the log buffer. N.B. This likely may change in the future.
 * The thread (id) generating the log record is automatically recorded.
 * <p>
 * A logger typically will implement the {@link VMLogger#operationName(int)}
 * method that returns a descriptive name for the operation.
 * <p>
 * Logging is enabled on a per logger basis through the use of
 * a standard {@code -XX:+LogXXX} option derived from the logger name.
 * Tracing to the {@link Log} stream is also available through {@code -XX:+TraceXXX},
 * and a default implementation is provided, although this can be overridden.
 * Enabling tracing also enables logging, as the trace is driven from the log.
 * <b>N.B.</b>It is not possible to check the options until the VM startup has reached
 * a certain point. In order not to lose logging in the early phases, logging, but not
 * tracing, is always enabled initially.
 *<p>
 * Fine control over which operations are logged (and therefore traced) is provided
 * by the {@code -XX:LogXXXInclude=pattern} and {@code -XX:LogXXXExclude=pattern} options.
 * The {@code pattern} is a regular expression in the syntax expected by {@link java.util.regex.Pattern}
 * and refers to the operation names returned by {@link VMLogger#operationName(int)}.
 * By default all operations are logged. However, if the include option is set only
 * those operations that match the pattern are logged. In either case, if the exclude
 * option is provided, the set is reduced by those operations that match the exclude pattern.
 * <p>
 * The management of log records is handled in a separate class; a subclass of {@log VMLog}.
 * {@link VMLogger instance} requests a {@link VMLog.Record record} that can store a given
 * number of arguments from the singleton {@link #vmLog} instance and then records the values.
 * The format of the log record is opaque to allow a variety of implementations.
 * <p>
 * <h2>Performance</h2>
 * In simple use logging affects performance even when disabled because the disabled
 * check happens inside the {@link VMLogger} log methods, so the cost of the argument marshalling
 * and method call is always paid when used in the straightforward manner, e.g.:
 *
 * {@code  logger.log(op, arg1, arg2);}
 *
 * If performance is an issue, replace the above with a guarded call, vis:
 *
 * {@code if (logger.enabled()) { logger.log(op, arg1, arg2);}}
 *
 * The {@code enabled} method is always inlined.
 *
 * N.B. The guard can be a more complex condition. However, it is important not
 * to use disjunctive conditions that could result in a value of {@code true} for
 * the guard when {@code logger.enabled()} would return false when one of the conditions
 * returned {@code true}. E.g.,
 *<pre>
 * if {a || b} {
 *     logger.log(op, arg1, arg2);
 * }
 * </pre>
 * Conjunctive conditions can be useful. For example, say we wanted to suppress
 * logging until a counter reaches a certain value:
 * <pre>
 * if (logger.enabled() && count >= value) {
 *     logger.log(op, arg1, arg2);
 * }
 * </pre>
 * <p>
 * It is possible to have one logger override the default settings for other loggers.
 * E.g., say we have loggers A and B, but we want a way to turn both loggers on with
 * a single overriding option. The way to do this is to create a logger, say ALL, typically with
 * no operations, that forces A and B into the enabled state if, and only if, it is itself
 * enabled. This can be achieved by overriding {@link #checkOptions()} for the ALL logger.
 * See {@link Heap#gcLogger} for an example of this.
 * <p>
 * It is also possible for a logger, say C, to inherit the settings of another logger, say ALL,
 * again by forcing ALL to check its options from within C's {@code checkOptions} and then use ALL's
 * values to set C's settings. This is appropriate when ALL cannot know about C for abstraction
 * reasons.
 * <p>
 * N.B. The order in which loggers have their options checked by the normal VM startup is unspecified.
 * Hence, a logger must always force the checking of a dependent logger's options before accessing its state.
 * <p>
 * Logging (for all loggers) may be enabled/disabled for a given thread, which can be useful to avoid nasty circularities,
 * see {@link VMLog#setThreadState(boolean)}.
 * <h2>Useful Conventions</h2>
 * To simplify the display of arguments by the Inspector, given that {@link Reference} values
 * cannot be logged, and to mitigate the code changes necessary should that change (likely),
 * it is recommended to follow the conventions below:
 *
 * Create a public {@code enum} named {@code Operation} inside the logger class, with mnemomic
 * names for the operations, e.g.:
 * <pre>
 * public static class AllocationLogger extends VMLogger {
 *     public enum Operation {
 *         Clone, ...
 *     }
 *     ...
 * }
 * </pre>
 * Note that the enum constants are not all upper case, as is conventional, because they are used
 * to match the log method names.
 * <p>
 * Override the {@link #operationName(int)}method as follows:
 * <pre>
 * public String operationName(int op) {
 *     return Operation.values()[op].name();
 * }
 * </pre>
 * <p>
 * Create log methods with arguments that reflect the values you want to log (even if they are reference types}, e.g.:
 *
 * <pre>
 * void logClone(Hub hub, Object clone) {
 *     log(Operation.Copy.ordinal(), hub.classActor.id, Layout.originToCell(ObjectAccess.toOrigin(clone)));
 * }
 * </pre>
 * Then invoke {@code logClone} at the appropriate place in the code, typically guarded by a check that
 * the logger is enabled.
 * <p>
 * The Inspector will be able to display the arguments appropriately, by using reflection to discover the
 * types of the arguments. If reference value logging becomes possible, only the implementation of
 * the {@code logXXX} methods will need to be changed.
 * <p>
 * These methods will typically not exist in the boot image as they will be inlined at the use site.
 *
 * <h3>Tracing</h3>
 * When the tracing option is enabled, {@link #doTrace(Record)} is invoked immediately after
 * the log record is created. After checking that calls to the {@link Log} class are possible,
 * {@link Log#lock} is called, then {@link #trace(Record)} is called, followed by {@link Log#unlock}.
 * <p>
 * A default implementation of {@link #trace} is provided that calls methods in the
 * {@link Log} class to print the logger name, thread name and arguments. There are two ways to
 * customize the output. The first is to override the {@link #logArg(int, Word)} method
 * to customize the output of a particular argument - the default action is to print the value
 * as a hex number. The second is to override {@link #trace}  and do full customization.
 *
 */
public class VMLogger {
    /**
     * Convenience enum for BEGIN/END intervals.
     */
    public enum Interval {
        BEGIN, END;
        public static final Interval[] VALUES = values();
    }

    private static int nextLoggerId = 1;

    /**
     * Descriptive name also used to create option names.
     */
    public final String name;
    /**
     * Creates a unique id when combined with operation id. Identifies the logger in the loggers map.
     */
    public final int loggerId;
    /**
     * Number of distinct operations that can be logged.
     */
    private final int numOps;
    /**
     * Bit n is set iff operation n is to be logged.
     */
    private final BitSet logOp;

    public final VMBooleanXXOption logOption;
    public final VMBooleanXXOption traceOption;
    public final VMStringOption logIncludeOption;
    public final VMStringOption logExcludeOption;
    private boolean logEnabled;
    private boolean traceEnabled;
    private boolean optionsChecked;

    private VMLog vmLog;

    @HOSTED_ONLY
    private static VMLog hostedVMLog;

    /**
     * Create a new logger instance.
     * @param name name used in option name, i.e., -XX:+Logname
     * @param numOps number of logger operations
     * @param optionDescription if not {@code null}, string used in option description.
     */
    @HOSTED_ONLY
    public VMLogger(String name, int numOps, String optionDescription) {
        this.name = name;
        this.numOps = numOps;
        loggerId = nextLoggerId++;
        logOp = new BitSet(numOps);
        String logName = "Log" + name;
        String description = optionDescription ==  null ? name : " " + optionDescription;
        logOption = new VMBooleanXXOption("-XX:-" + logName, "Log" + description);
        traceOption = new VMBooleanXXOption("-XX:-" + "Trace" + name, "Trace" + description);
        logIncludeOption = new VMStringOption("-XX:" + logName + "Include=", false, null, "list of " + name + " operations to include");
        logExcludeOption = new VMStringOption("-XX:" + logName + "Exclude=", false, null, "list of " + name + " operations to exclude");
        VMOptions.register(logOption, MaxineVM.Phase.PRISTINE);
        VMOptions.register(traceOption, MaxineVM.Phase.PRISTINE);
        VMOptions.register(logIncludeOption, MaxineVM.Phase.PRISTINE);
        VMOptions.register(logExcludeOption, MaxineVM.Phase.PRISTINE);
        VMLog.registerLogger(this);
    }

    /**
     * If you want to have the external existence of a logger be conditional on some value,
     * e.g., the image build mode, this constructor can be used in the case where value
     * is false. It effectively produces a null logger that is invisible.
     */
    @HOSTED_ONLY
    protected VMLogger() {
        this.name = "NULL";
        this.numOps = 0;
        loggerId = 0;
        logOption = traceOption = null;
        logIncludeOption = logExcludeOption = null;
        logOp = null;
    }

    @HOSTED_ONLY
    public void setVMLog(VMLog vmLog, VMLog hostedVMLog) {
        this.vmLog = vmLog;
        VMLogger.hostedVMLog = hostedVMLog;
        checkOptions();
    }

    public static String threadName(int id) {
        if (MaxineVM.isHosted()) {
            return "Thread[id=" + id + "]";
        } else {
            VmThread vmThread = VmThreadMap.ACTIVE.getVmThreadForID(id);
            return vmThread == null ? "DEAD" : vmThread.getName();
        }
    }

    /**
     * Provides a mnemonic name for the given operation.
     * Default is {@code OpN}.
     */
    public String operationName(int op) {
        return "Op " + Integer.toString(op);
    }

    /**
     * Provides a custom string decoding of an argument value. Intended for simple Inspector use only.
     * @param op the operation id
     * @param argNum the argument index in the original log call, {@code [0 .. argCount - 1])
     * @param arg the argument value from the original log call
     * @return a custom string or null if no custom decoding for this arg
     */
    @HOSTED_ONLY
    public String inspectedArg(int op, int argNum, Word arg) {
        return null;
    }

    /**
     * Custom logging of an argument. Default prints as hex.
     * @param argNum
     * @param arg
     */
    protected void logArg(int argNum, Word arg) {
        Log.print(arg);
    }

    @INLINE
    public final boolean enabled() {
        return logEnabled;
    }

    @INLINE
    public final boolean traceEnabled() {
        return traceEnabled;
    }

    public void enable(boolean value) {
        logEnabled = value;
    }

    public void enableTrace(boolean value) {
        if (value && !logEnabled) {
            logEnabled = true;
        }
        traceEnabled = value;
    }

    /**
     * Implements the default trace option {@code -XX:+TraceXXX}.
     * {@link Log#lock()} and {@link Log#unlock(boolean)} are
     * already handled by the caller.
     * @param r
     */
    protected void trace(Record r) {
        Log.print("Thread \"");
        Log.print(threadName(r.getThreadId()));
        Log.print("\" ");
        Log.print(name);
        Log.print('.');
        Log.print(operationName(r.getOperation()));
        int argCount = r.getArgCount();
        for (int i = 1; i <= argCount; i++) {
            Log.print(' ');
            logArg(i, r.getArg(i));
        }
        Log.println();
    }

    /**
     * Called to reset the logger state to default values in case they were enabled during image build.
     */
    public void setDefaultState() {
        // At VM startup we log everything; this gets refined once the VM is up in checkLogOptions.
        // This is because we cannot control the logging until the VM has parsed the PRISTINE options.
        logEnabled = true;
        traceEnabled = false;
        optionsChecked = false;
        for (int i = 0; i < numOps; i++) {
            logOp.set(i, true);
        }
    }

    /**
     * Check the command line options that control this logger.
     * This is done once to allow linked loggers to control each other without
     * worrying about ordering.
     */
    public void checkOptions() {
        if (optionsChecked) {
            return;
        }
        traceEnabled = traceOption.getValue();
        logEnabled = traceEnabled | logOption.getValue();
        if (logEnabled) {
            String logInclude = logIncludeOption.getValue();
            String logExclude = logExcludeOption.getValue();
            if (logInclude != null || logExclude != null) {
                for (int i = 0; i < numOps; i++) {
                    logOp.set(i, logInclude == null ? true : false);
                }
                if (logInclude != null) {
                    Pattern inclusionPattern = Pattern.compile(logInclude);
                    for (int i = 0; i < numOps; i++) {
                        if (inclusionPattern.matcher(operationName(i)).matches()) {
                            logOp.set(i, true);
                        }
                    }
                }
                if (logExclude != null) {
                    Pattern exclusionPattern = Pattern.compile(logExclude);
                    for (int i = 0; i < numOps; i++) {
                        if (exclusionPattern.matcher(operationName(i)).matches()) {
                            logOp.set(i, false);
                        }
                    }
                }
            }
        }
        optionsChecked = true;
    }

    private Record logSetup(int op, int argCount) {
        Record r = null;
        if (logEnabled && logOp.get(op) && vmLog.threadIsEnabled()) {
            r = (MaxineVM.isHosted() ? hostedVMLog : vmLog).getRecord(argCount);
            r.setHeader(op, argCount, loggerId);
        }
        return r;
    }

    public void log(int op) {
        Record r = logSetup(op, 0);
        if (r != null && traceEnabled) {
            doTrace(r);
        }
    }

    public void log(int op, Word arg1) {
        Record r = logSetup(op, 1);
        if (r != null) {
            r.setArgs(arg1);
        }
        if (r != null && traceEnabled) {
            doTrace(r);
        }
    }

    public void log(int op, Word arg1, Word arg2) {
        Record r = logSetup(op, 2);
        if (r != null) {
            r.setArgs(arg1, arg2);
        }
        if (r != null && traceEnabled) {
            doTrace(r);
        }
    }

    public void log(int op, Word arg1, Word arg2, Word arg3) {
        Record r = logSetup(op, 3);
        if (r != null) {
            r.setArgs(arg1, arg2, arg3);
        }
        if (r != null && traceEnabled) {
            doTrace(r);
        }
    }

    public void log(int op, Word arg1, Word arg2, Word arg3, Word arg4) {
        Record r = logSetup(op, 4);
        if (r != null) {
            r.setArgs(arg1, arg2, arg3, arg4);
        }
        if (r != null && traceEnabled) {
            doTrace(r);
        }
    }

    public void log(int op, Word arg1, Word arg2, Word arg3, Word arg4, Word arg5) {
        Record r = logSetup(op, 5);
        if (r != null) {
            r.setArgs(arg1, arg2, arg3, arg4, arg5);
        }
        if (r != null && traceEnabled) {
            doTrace(r);
        }
    }

    public void log(int op, Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6) {
        Record r = logSetup(op, 6);
        if (r != null) {
            r.setArgs(arg1, arg2, arg3, arg4, arg5, arg6);
        }
        if (r != null && traceEnabled) {
            doTrace(r);
        }
    }

    public void log(int op, Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6, Word arg7) {
        Record r = logSetup(op, 7);
        if (r != null) {
            r.setArgs(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        }
        if (r != null && traceEnabled) {
            doTrace(r);
        }
    }

    public void log(int op, Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6, Word arg7, Word arg8) {
        Record r = logSetup(op, 8);
        if (r != null) {
            r.setArgs(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
        }
        if (r != null && traceEnabled) {
            doTrace(r);
        }
    }

    private void doTrace(Record r) {
        if (MaxineVM.isHosted()) {
            trace(r);
        } else {
            if (!DynamicLinker.isCriticalLinked()) {
                return;
            }
            boolean lockDisabledSafepoints = Log.lock();
            try {
                trace(r);
            } finally {
                Log.unlock(lockDisabledSafepoints);
            }
        }
    }

    // Convenience methods for handling logging/tracing arguments.

    @INLINE
    public static Word intArg(int i) {
        return Address.fromInt(i);
    }

    @INLINE
    public static Word longArg(long i) {
        return Address.fromLong(i);
    }

    @INLINE
    public static Word threadArg(VmThread vmThread) {
        return Address.fromInt(vmThread.id());
    }

    @INLINE
    public static MethodActor toMethodActor(Word arg) {
        return MethodID.toMethodActor(MethodID.fromWord(arg));
    }

    @INLINE
    public static ClassActor toClassActor(Word arg) {
        return ClassID.toClassActor(arg.asAddress().toInt());
    }

}
