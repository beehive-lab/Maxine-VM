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
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Support for logging/tracing in a VM module, with Inspector integration.
 *
 * See <a href="https://wikis.oracle.com/display/MaxineVM/Type-based+Logging">the Wiki page</a> for more details.
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

    /**
     * The {@link VMLog} used by this logger.
     */
    private VMLog vmLog;

    @HOSTED_ONLY
    /**
     * The {@code hosted} {@link VMLog}. There is only one (currently).
     */
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

    /**
     * Special case variant that makes an invisible logger that is controlled by explicit code in the VM.
     * In particular, it has no user-visible log/trace command line options.
     * It is not registered with the standard {@link VMLog} instance, so the controller must
     * explicitly associate a {@link VMLog} instance with it.
     * It's {@link #loggerId} is always 1, i.e., there must be a 1-1 relationship
     * between such a logger and its associated {@link VMLog} instance.
     *
     * N.B. This may be called at runtime.
     * @param numOps
     * @param operationRefMaps
     */
    public VMLogger(String name, int numOps, int[] operationRefMaps) {
        this.name = name;
        this.numOps = numOps;
        this.operationRefMaps = operationRefMaps;
        logOp = new BitSet(numOps);
        for (int i = 0; i < numOps; i++) {
            logOp.set(i, true);
        }
        logOption = traceOption = null;
        logIncludeOption = logExcludeOption = null;
        optionsChecked = true;
        loggerId = 1;
    }

    @HOSTED_ONLY
    void setVMLog(VMLog vmLog, VMLog hostedVMLog) {
        this.vmLog = vmLog;
        VMLogger.hostedVMLog = hostedVMLog;
        checkOptions();
    }

    /**
     * Call for custom (hidden) loggers.
     * @param vmLog
     */
    void setVMLog(VMLog vmLog) {
        this.vmLog = vmLog;
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

    @INLINE
    public final boolean opEnabled(int op) {
        return logOp.get(op);
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
     * @param lockDisabledSafepoints value return from matching {@link #lock} call.
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

    /* Convenience methods for logging typed arguments as {@link Word values}.
     * Where possible these use canonical representations that do not involve
     * references.
     */

    /**
     * For {@link HOSTED_ONLY} logging of arbitrary object types.
     */
    @HOSTED_ONLY
    public static class ObjectArg extends Word {
        Object arg;

        ObjectArg(Object arg) {
            super(0);
            this.arg = arg;
        }

        public static Object getArg(Record r, int argNum) {
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
    public static Word floatArg(float f) {
        return Address.fromInt(Float.floatToRawIntBits(f));
    }

    @INLINE
    public static Word doubleArg(double d) {
        return Address.fromLong(Double.doubleToRawLongBits(d));
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
    public static Word methodActorArg(MethodActor methodActor) {
        return MethodID.fromMethodActor(methodActor);
    }

    @INLINE
    public static Word classLoaderArg(ClassLoader arg) {
        return objectArg(arg);
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
    public static Word codePointerArg(CodePointer codePointer) {
        return Address.fromLong(codePointer.toTaggedLong());
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
    public static float toFloat(Record r, int argNum) {
        return Float.intBitsToFloat(r.getIntArg(argNum));
    }

    @INLINE
    public static double toDouble(Record r, int argNum) {
        return Double.longBitsToDouble(r.getLongArg(argNum));
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
    public static ClassMethodActor toClassMethodActor(Record r, int argNum) {
        if (MaxineVM.isHosted()) {
            return (ClassMethodActor) toMethodActor(r, argNum);
        }
        return asClassMethodActor(toMethodActor(r.getArg(argNum)));
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native ClassMethodActor asClassMethodActor(Object arg);

    @INLINE
    public static ClassActor toClassActor(Record r, int argNum) {
        return toClassActor(r.getArg(argNum));
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native ClassLoader asClassLoader(Object arg);

    @INLINE
    public static ClassLoader toClassLoader(Record r, int argNum) {
        if (MaxineVM.isHosted()) {
            return (ClassLoader) ObjectArg.getArg(r, argNum);
        } else {
            return asClassLoader(toObject(r, argNum));
        }
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
    private static native Throwable asThrowable(Object arg);

    @INLINE
    public static Throwable toThrowable(Record r, int argNum) {
        if (MaxineVM.isHosted()) {
            return (Throwable) toObject(r, argNum);
        }
        return asThrowable(toObject(r, argNum));
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

    @INTRINSIC(UNSAFE_CAST)
    private static native Stub asStub(Object arg);

    @INLINE
    public static Stub toStub(Record r, int argNum) {
        if (MaxineVM.isHosted()) {
            return (Stub) toObject(r, argNum);
        }
        return asStub(toObject(r, argNum));
    }

    @INLINE
    public static CodePointer toCodePointer(Record r, int argNum) {
        return CodePointer.fromTaggedLong(toLong(r, argNum));
    }

    // check that loggers are up to date in VM image

    static {
        JavaPrototype.registerGeneratedCodeCheckerCallback(new GeneratedCodeCheckerCallback());
    }

    @HOSTED_ONLY
    private static class GeneratedCodeCheckerCallback implements JavaPrototype.GeneratedCodeCheckerCallback {

        @Override
        public void checkGeneratedCode() {
            try {
                ArrayList<Class<?>> updatedSources = VMLoggerGenerator.generate(true);
                StringBuilder sb = new StringBuilder();
                if (updatedSources != null) {
                    for (Class<?> source : updatedSources) {
                        sb.append(source.getSimpleName());
                        sb.append(' ');
                    }
                    FatalError.unexpected("VMLogger(s) " + sb.toString() + " is/are out of sync.\n" + "Run 'mx loggen', recompile (or refresh in your IDE)" +
                                    " and restart the bootstrapping process.\n\n");
                }
            } catch (Exception exception) {
                FatalError.unexpected("Error while generating VMLogger sources", exception);
            }
        }
    }

}
