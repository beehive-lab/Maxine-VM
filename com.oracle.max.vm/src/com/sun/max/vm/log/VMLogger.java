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
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import java.util.*;
import java.util.regex.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * A {@linkplain VMLogger} defines a set of operations, cardinality {@code N} each identified by an {@code int} code in the
 * range {@code [0 .. N-1]}. A series of "log" methods are provided, that take the operation code and a varying number
 * of {@link Word} arguments (up to {@link VMLog.Record#MAX_ARGS}).
 * <p>
 * The thread (id) generating the log record is automatically recorded.
 * <p>
 * A logger typically will implement the {@link VMLogger#operationName(int)} method that returns a descriptive name for
 * the operation.
 * <h2>Enabling Logging</h2>
 * <p>
 * Logging is enabled on a per logger basis through the use of a standard {@code -XX:+LogXXX} option derived from the
 * logger name. Tracing to the {@link Log} stream is also available through {@code -XX:+TraceXXX}, and a default
 * implementation is provided, although this can be overridden. Enabling tracing also enables logging, as the trace is
 * driven from the log. <b>N.B.</b>It is not possible to check the options until the VM startup has reached a certain
 * point. In order not to lose logging in the early phases, logging, but not tracing, is always enabled on VM startup.
 * <p>
 * Fine control over which operations are logged (and therefore traced) is provided by the
 * {@code -XX:LogXXXInclude=pattern} and {@code -XX:LogXXXExclude=pattern} options. The {@code pattern} is a regular
 * expression in the syntax expected by {@link java.util.regex.Pattern} and refers to the operation names returned by
 * {@link VMLogger#operationName(int)}. By default all operations are logged. However, if the include option is set,
 * only those operations that match the pattern are logged. In either case, if the exclude option is provided, the set
 * is reduced by those operations that match the exclude pattern.
 * <p>
 * The management of log records is handled in a separate class; a subclass of {@link VMLog}. A {@linkplain VMLogger instance}
 * requests a {@link VMLog.Record record} that can store a given number of arguments from the singleton {@link #vmLog}
 * instance and then records the values. The format of the log record is opaque to allow a variety of implementations.
 * <p>
 * <h2>Performance</h2>
 * In simple use logging affects performance even when disabled because the disabled check happens inside the
 * {@link VMLogger} log methods, so the cost of the argument marshalling and method call is always paid when used in the
 * straightforward manner, e.g.:
 *
 * {@code  logger.log(op, arg1, arg2);}
 *
 * If performance is an issue, replace the above with a guarded call, vis:
 *
 * <pre>
 * if (logger.enabled()) {
 *     logger.log(op, arg1, arg2);
 * }
 * </pre>
 *
 * The {@code enabled} method is always {@link INLINE inlined}.
 *
 * N.B. The guard can be a more complex condition. However, it is important not to use disjunctive conditions that could
 * result in a value of {@code true} for the guard when {@code logger.enabled()} would return false, E.g.,
 *
 * <pre>
 * if {a || b} {
 *     logger.log(op, arg1, arg2);
 * }
 * </pre>
 *
 * Conjunctive conditions can be useful. For example, say we wanted to suppress logging until a counter reaches a
 * certain value:
 *
 * <pre>
 * if (logger.enabled() &amp;&amp; count &gt;= value) {
 *     logger.log(op, arg1, arg2);
 * }
 * </pre>
 * <p>
 * <h2>Dependent Loggers</h2>
 * It is possible to have one logger override the default settings for other loggers. E.g., say we have loggers A and B,
 * but we want a way to turn both loggers on with a single overriding option. The way to do this is to create a logger,
 * say ALL, typically with no operations, that forces A and B into the enabled state if, and only if, it is itself
 * enabled. This can be achieved by overriding {@link #checkOptions()} for the ALL logger, and calling the
 * {@link #forceDependentLoggerState} method. See {@link Heap#gcAllLogger} for an example of this.
 * <p>
 * It is also possible for a logger, say C, to inherit the settings of another logger, say ALL, again by forcing ALL to
 * check its options from within C's {@code checkOptions} and then use ALL's values to set C's settings. This is
 * appropriate when ALL cannot know about C for abstraction reasons. See {@link #checkDominantLoggerOptions>}.
 * <p>
 * N.B. The order in which loggers have their options checked by the normal VM startup is unspecified. Hence, a logger
 * must always force the checking of a dependent logger's options before accessing its state.
 * <p>
 * Logging (for all loggers) may be enabled/disabled for a given thread, which can be useful to avoid meta-circularities
 * in low-level code, see {@link VMLog#setThreadState(boolean)}.
 *
 * <h2>Type Safety</h2>
 * The standard type-safe way to log a collection of heteregenously typed values would be to first define a class
 * containing fields that correspond to the values, then acquire an instance of such a class, store the values in the
 * fields and then save the instance in the log. Note that this generally involves allocation; at best it involves
 * acquiring a pre-allocated instance in some way. It also necessarily involves a level of indirection in the log buffer
 * itself, as the buffer is constrained to be a container of reference values.
 *
 * Since {@link VMLog} is a low level mechanism that must function in parts of the VM where allocation is impossible, for
 * example, during garbage collection, the standard approach is not appropriate. It is also important to minimize the
 * storage overhead for log records and the performance overhead of logging the data. Therefore, a less type safe
 * approach is adopted, that is partly mitigated by automatic generation of logger code at VM image build time.
 * <p>
 * The automatic generation, see {@link VMLoggerGenerator}, is driven from an interface defining the logger operations
 * that is tagged with the {@link VMLoggerInterface} annotation. Since this is only used during image generation the
 * interface should also be tagged with {@link HOSTED_ONLY}. The logging operations are defined as methods
 * in the interface. In order to preserve the parameter names in the generated code, each parameter should also be annotated
 * with {@link VMLogParam}, e.g.:
 *
 * <pre>
 * &#64HOSTED_ONLY
 * &#64VMLoggerInterface
 * private interface ExampleLoggerInterface {
 *   void foo(
 *       &#64VMLogParam(name = "classActor") ClassActor classActor,
 *       &#64VMLogParam(name = "base") Pointer base);
 *
 *   void bar(
 *       &#64VMLogParam(name = "count") SomeClass someClass, int count);
 * }
 * </pre>
 * The logger class should contain the comment pair:
 * <pre>
 * // START GENERATED CODE
 * // END GENERATED CODE
 * </pre>
 * somewhere in the source, typically at the end of the class.
 * When {@link VMLoggerGenerator} is executed it scans all VM classes for interfaces annotated with {@link VMLoggerInterface}
 * and then generates an abstract class containing the log methods, abstract method definitions for the associated {@code trace}
 * methods, and an implementation of the {@link #trace(Record r)} method that decodes the operation and invokes the
 * appropriate {@code trace} method.
 * <p>
 * The developer then defines the concrete implementation class that inherits from the automatically generated class
 * and, if required implements the trace methods, e.g, from the {@link ExampleLoggerOwner} class:
 * <pre>
 * public static final class ExampleLogger extends ExampleLoggerAuto {
 *      ExampleLogger() {
 *         super("Example", "an example logger.");
 *      }
 *
 *     &#64Override
 *     protected void traceFoo(ClassActor classActor, Pointer base) {
 *         Log.print("Class "); Log.print(classActor.name.string);
 *         Log.print(", base:"); Log.println(base);
 *     }
 *
 *     &#64Override
 *     protected void traceBar(SomeClass someClass, int count) {
 *       // SomeClass specific tracing
 *     }
 * }
 * </pre>
 * Note that if an argument name is not identified {@link VMLogParam} it will be defined as {@code argN}, where
 * {@code N} is the argument index.
 * <p>
 * {@link VMLogger} has built-in support for several standard reference types, that have alternate representations
 * as scalar values, such as {@link ClassActor}. As a general principle, reference types without an alternate, unique, scalar
 * representation should be avoided as log method arguments. However, this is sometimes difficult or inconvenient, so
 * it is possible to store references types. These should be passed using {@link VMLogger#objectArg(Object)}
 * and retrieved using {@link VMLogger#toObject(Record, int)}. This is automatically handled by the generator.
 * <b>N.B.</b> storing reference types in the log makes them reachable until such time as they are overwritten.
 * It is assumed that {@code Enum} types are always stored using their ordinal value. The generator creates the
 * appropriate conversions methods. It assumes that the {@code enum} declares the following field:
 * <pre>
 * public static final EnumType[] VALUES = values();
 * </pre>
 *
 * <h3>Tracing</h3>
 * When the tracing option for a logger is enabled, {@link #doTrace(Record)} is invoked immediately after the log record is created.
 * After checking that calls to the {@link Log} class are possible, {@link Log#lock} is called, then
 * {@link #trace(Record)} is called, followed by {@link Log#unlock}.
 * <p>
 * A default implementation of {@link #trace} is provided that calls methods in the {@link Log} class to print the
 * logger name, thread name and arguments. There are two ways to customize the output. The first is to override the
 * {@link #logArg(int, Word)} method to customize the output of a particular argument - the default action is to print
 * the value as a hex number. The second is to override {@link #trace} and do full customization.
 * <h2>Inspector Integration</h2>
 * <p>
 * The Inspector is generally able to display the log arguments appropriately, by using reflection to discover the types of the
 * arguments.
 * <p>
 * Two additional mechanisms are available for Inspector customization. The first is an override to generate a custom
 * {@link String} representation of a log argument:
 *
 * <pre>
 * &#64HOSTED_ONLY
 * public String inspectedArgValue(int op, int argNum, Word argValue);
 * </pre>
 *
 * If this method is defined for a given logger then the Inspector will call it for the given operation and argument
 * and, if it returns a non-null value, use the result.
 * <p>
 * The second is an override for a logger-defined argument value class:
 *
 * <pre>
 * &#64HOSTED_ONLY
 * public static String inspectedValue(Word argValue);
 * </pre>
 *
 * If this method is defined for the class and no standard customization is available, it will be called and, if the
 * result is non-null it will be used.
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

    /**
     *  GC support, array of bitmaps identifying reference-valued arguments for each operation.
     *  {@code null} if no operations have reference valued arguments.
     */
    final int[] operationRefMaps;

    private VMLog vmLog;

    @HOSTED_ONLY
    private static VMLog hostedVMLog;

    /**
     * Create a new logger instance.
     * @param name name used in option name, i.e., -XX:+Logname
     * @param numOps number of logger operations
     * @param optionDescription if not {@code null}, string used in option description.
     * @param operationRefMaps reference maps for operation parameters.
     */
    @HOSTED_ONLY
    public VMLogger(String name, int numOps, String optionDescription, int[] operationRefMaps) {
        this.name = name;
        this.numOps = numOps;
        this.operationRefMaps = operationRefMaps;
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
     * Convenience variant for null operationRefMaps.
     * @param name
     * @param numOps
     * @param optionDescription
     */
    @HOSTED_ONLY
    public VMLogger(String name, int numOps, String optionDescription) {
        this(name, numOps, optionDescription, null);
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
        this.operationRefMaps = null;
        loggerId = -1;
        logOption = traceOption = null;
        logIncludeOption = logExcludeOption = null;
        logOp = null;
        optionsChecked = true;
    }

    @HOSTED_ONLY
    public void setVMLog(VMLog vmLog, VMLog hostedVMLog) {
        this.vmLog = vmLog;
        VMLogger.hostedVMLog = hostedVMLog;
        checkOptions();
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
     * @param argNum the argument index in the original log call, {@code [0 .. argCount - 1]}
     * @param arg the argument value from the original log call
     * @return a custom string or null if no custom decoding for this arg
     */
    @HOSTED_ONLY
    public String inspectedArgValue(int op, int argNum, Word arg) {
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
     * Lock the log for the current thread.
     * Use this if you must to have a sequence on non-interleaved log records.
     * Implemented using {@link Log#lock}.
     * @return {@code true} if safepoints were disabled by the lock method.
     */
    public boolean lock() {
        return Log.lock();
    }

    /**
     * Unlock a locked log.
     * @param lockDisabledSafepoints value return from matching {@ink #lock} call.
     */
    public void unlock(boolean lockDisabledSafepoints) {
        Log.unlock(lockDisabledSafepoints);
    }

    /**
     * Implements the default trace option {@code -XX:+TraceXXX}.
     * {@link Log#lock()} and {@link Log#unlock(boolean)} are
     * already handled by the caller.
     * @param r
     */
    protected void trace(Record r) {
        Log.print("Thread \"");
        Log.print(toVmThreadName(r.getThreadId()));
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
        if (loggerId < 0) {
            return;
        }
        logEnabled = true;
        traceEnabled = false;
        optionsChecked = false;
        // At VM startup we log everything; this gets refined once the VM is up in checkOptions.
        // This is because we cannot control the logging until the VM has parsed the PRISTINE options.
        setDefaultLogOptionsState(true);
    }

    private void setDefaultLogOptionsState(boolean value) {
        for (int i = 0; i < numOps; i++) {
            logOp.set(i, value);
        }
    }

    /**
     * Check the command line options that control this logger. This is done once to allow linked loggers to control
     * each other without worrying about ordering.
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
            // If include option given, the default is everything disabled, otherwise enabled
            setDefaultLogOptionsState(logInclude == null ? true : false);
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
        optionsChecked = true;
    }

    /**
     * Set the enabled options of this logger based on those of a dominant logger.
     * @param dominantLogger
     */
    protected void checkDominantLoggerOptions(VMLogger dominantLogger) {
        dominantLogger.checkOptions();
        // Turn on if dominant options are set.
        if (dominantLogger.enabled()) {
            enable(true);
        }
        if (dominantLogger.traceEnabled()) {
            enableTrace(true);
        }
    }

    /**
     * Force the state of the given dependent loggers to have the same enabled state as this logger.
     * @param loggers
     */
    protected void forceDependentLoggerState(VMLogger... loggers) {
        // force the checking of the dependent loggers now
        for (VMLogger logger : loggers) {
            logger.checkOptions();
        }
        // Now enforce our state on them.
        for (VMLogger logger : loggers) {
            if (enabled()) {
                logger.enable(true);
            }
            if (traceEnabled()) {
                logger.enableTrace(true);
            }
        }
    }

    private Record logSetup(int op, int argCount) {
        Record r = null;
        VMLog vmLogToUse = MaxineVM.isHosted() ? hostedVMLog : vmLog;
        if (logEnabled && logOp.get(op) && vmLogToUse.threadIsEnabled()) {
            r = vmLogToUse.getRecord(argCount);
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

    // Convenience methods for logging typed arguments as {@link Word values}.

    @HOSTED_ONLY
    private static class ObjectArg extends Word {
        Object arg;

        ObjectArg(Object arg) {
            super(0);
            this.arg = arg;
        }

        static Object getArg(Record r, int argNum) {
            /*
            Class<ObjectArg> type = null;
            ObjectArg objectArg = Utils.cast(type, r.getArg(argNum));
            */
            return ((ObjectArg) r.getArg(argNum)).arg;
        }

        static Word toArg(Object object) {
            /*
            ObjectArg result = new ObjectArg(object);
            Class<Word> type = null;
            return Utils.cast(type, result);
            */
            return new ObjectArg(object);
        }
    }

    @INLINE
    public static Word booleanArg(boolean value) {
        return value ? Word.allOnes() : Word.zero();
    }

    @INLINE
    public static Word intArg(int i) {
        return Address.fromInt(i);
    }

    @INLINE
    public static Word longArg(long i) {
        return Address.fromLong(i);
    }

    @INLINE
    public static Word vmThreadArg(VmThread vmThread) {
        return Address.fromInt(vmThread.id());
    }

    @INLINE
    public static Word intervalArg(Interval interval) {
        return Address.fromInt(interval.ordinal());
    }

    @INLINE
    public static Word classActorArg(ClassActor classActor) {
        return intArg(classActor.id);
    }

    @INLINE
    public static Word objectArg(Object object) {
        if (MaxineVM.isHosted()) {
            return ObjectArg.toArg(object);
        } else {
            return Reference.fromJava(object).toOrigin();
        }
    }

    @INLINE
    public static Word twoIntArgs(int a, int b) {
        return Address.fromLong(((long) a) << 32 | b);
    }

    /* Convenience methods for retrieving logged values for {@link #trace) overrides.
     * Supports the auto-generation of the logger boiler plate code.
     */

    @INLINE
    public static Word toWord(Record r, int argNum) {
        return r.getArg(argNum);
    }

    @INLINE
    public static Pointer toPointer(Record r, int argNum) {
        return r.getArg(argNum).asPointer();
    }

    @INLINE
    public static Address toAddress(Record r, int argNum) {
        return r.getArg(argNum).asAddress();
    }

    @INLINE
    public static Size toSize(Record r, int argNum) {
        return r.getArg(argNum).asSize();
    }

    @INLINE
    public static byte toByte(Record r, int argNum) {
        return (byte) r.getIntArg(argNum);
    }

    @INLINE
    public static int toInt(Record r, int argNum) {
        return r.getIntArg(argNum);
    }

    @INLINE
    public static long toLong(Record r, int argNum) {
        return r.getLongArg(argNum);
    }

    @INLINE
    public static boolean toBoolean(Record r, int argNum) {
        return r.getBooleanArg(argNum);
    }

    @INLINE public static int toIntArg1(long arg) {
        return (int) (arg >> 32 & 0xFFFFFFFF);
    }

    @INLINE public static int toIntArg2(long arg) {
        return (int) (arg & 0xFFFFFFFF);
    }

    @INLINE
    public static VmThread toVmThread(Record r, int argNum) {
        return toVmThread(r.getArg(argNum));
    }

    @INLINE
    public static MethodActor toMethodActor(Record r, int argNum) {
        return toMethodActor(r.getArg(argNum));
    }

    @INLINE
    public static ClassActor toClassActor(Record r, int argNum) {
        return toClassActor(r.getArg(argNum));
    }

    @INLINE
    public static MethodActor toMethodActor(Word arg) {
        return MethodID.toMethodActor(MethodID.fromWord(arg));
    }

    @INLINE
    public static ClassActor toClassActor(Word arg) {
        return ClassID.toClassActor(arg.asAddress().toInt());
    }

    public static String toVmThreadName(int id) {
        if (MaxineVM.isHosted()) {
            return "Thread[id=" + id + "]";
        } else {
            VmThread vmThread = VmThreadMap.ACTIVE.getVmThreadForID(id);
            return vmThread == null ? "DEAD" : vmThread.getName();
        }
    }

    public static VmThread toVmThread(Word arg) {
        return  VmThreadMap.ACTIVE.getVmThreadForID(arg.asAddress().toInt());
    }

    @INLINE
    public static Interval toInterval(Record r, int argNum) {
        return Interval.VALUES[r.getIntArg(argNum)];
    }

    @INLINE
    public static Object toObject(Record r, int argNum) {
        if (MaxineVM.isHosted()) {
            return ObjectArg.getArg(r, argNum);
        } else {
            return Reference.fromOrigin(r.getArg(argNum).asPointer()).toJava();
        }
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native String asString(Object arg);

    @INLINE
    public static String toString(Record r, int argNum) {
        if (MaxineVM.isHosted()) {
            return (String) toObject(r, argNum);
        }
        return asString(toObject(r, argNum));
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native TargetMethod asTargetMethod(Object arg);

    @INLINE
    public static TargetMethod toTargetMethod(Record r, int argNum) {
        if (MaxineVM.isHosted()) {
            return (TargetMethod) toObject(r, argNum);
        }
        return asTargetMethod(toObject(r, argNum));
    }

    // check that loggers are up to date in VM image

    static {
        checkGenerateSourcesInSync();
    }

    @HOSTED_ONLY
    private static void checkGenerateSourcesInSync() {
        try {
            Class<?> updatedSource = VMLoggerGenerator.generate(true);
            if (updatedSource != null) {
                FatalError.unexpected("VMLogger " + updatedSource + " is out of sync.\n" + "Run 'mx loggen', recompile " + updatedSource.getName() + " (or refresh it in your IDE)" +
                                " and restart the bootstrapping process.\n\n");
            }
        } catch (Exception exception) {
            FatalError.unexpected("Error while generating VMLogger sources", exception);
        }
    }

}
