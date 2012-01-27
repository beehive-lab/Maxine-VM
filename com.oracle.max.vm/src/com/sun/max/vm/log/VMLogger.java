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
 * and a varying number of {@link Word} arguments (up to {@value VMLog.Record#MAX_ARGS}).
 * Currently these must not be {@link Reference} types as no GC support is provided
 * for values in the log buffer. The thread (id) generating the log record
 * is automatically recorded.
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
 * Performance: Logging affects performance even when disabled because the disabled
 * check happens inside the {@link VMLogger} log methods, so the cost of the argument marshalling
 * and method call is always paid when used in the straightforward manner, e.g.:
 *
 * {@code  logger.log("Operation", arg1, arg2);}
 *
 * If performance is an issue, replace the above with a guarded call, vis:
 *
 * {@code if (logger.enabled()) { logger.log("Operation", arg1, arg2);}
 *
 * The {@code enabled} method is always inlined.
 *
 */
public class VMLogger {
    private static int nextLoggerId = 1;

    /**
     * Descriptive name also used to create option names.
     */
    public final String name;
    /**
     * Creates a unique id when combined with operation id. Identifies the logger in the loggers map.
     */
    final int loggerId;
    /**
     * Number of distinct operations that can be logged.
     */
    private final int numOps;
    /**
     * Bit n is set iff operation n is to be logged.
     */
    private final BitSet logOp;

    private final VMBooleanXXOption logOption;
    private final VMBooleanXXOption traceOption;
    private final VMStringOption logIncludeOption;
    private final VMStringOption logExcludeOption;
    private boolean logEnabled;
    private boolean traceEnabled;

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
    protected VMLogger(String name, int numOps, String optionDescription) {
        this.name = name;
        this.numOps = numOps;
        loggerId = nextLoggerId++;
        logOp = new BitSet(numOps);
        // At VM startup we log everything; this gets refined once the VM is up in checkLogging.
        // This is because we cannot control the logging until the VM has parsed the PRISTINE options.
        logEnabled = true;
        for (int i = 0; i < numOps; i++) {
            logOp.set(i, true);
        }
        String logName = "Log" + name;
        String description = optionDescription ==  null ? name : optionDescription;
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

    @HOSTED_ONLY
    public void setVMLog(VMLog vmLog, VMLog hostedVMLog) {
        this.vmLog = vmLog;
        VMLogger.hostedVMLog = hostedVMLog;
        checkLogOptions();
    }

    public String threadName(int id) {
        VmThread vmThread = VmThreadMap.ACTIVE.getVmThreadForID(id);
        return vmThread == null ? "DEAD" : vmThread.getName();
    }

    /**
     * Provides a mnemonic name for the given operation.
     * Default is {@code OpN}.
     */
    public String operationName(int op) {
        return "Op " + Integer.toString(op);
    }

    /**
     * Provides a string decoding of an argument value.
     * @param op the operation id
     * @param argNum the argument index in the original log call, {@code [0 .. argCount - 1])
     * @param arg the argument value from the original log call
     * @return a descriptive string. Default implementation is raw value as hex.
     */
    protected String argString(int argNum, Word arg) {
        return Long.toHexString(arg.asAddress().toLong());
    }

    @INLINE
    public final boolean enabled() {
        return logEnabled;
    }

    @INLINE
    public final boolean traceEnabled() {
        return traceEnabled;
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
            Log.print(argString(i, r.getArg(i)));
        }
        Log.println();
    }

    protected void checkLogOptions() {
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
    }

    private Record logSetup(int op, int argCount) {
        Record r = null;
        if (logEnabled && logOp.get(op)) {
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
    public static MethodActor toMethodActor(Word arg) {
        return MethodID.toMethodActor(MethodID.fromWord(arg));
    }

    @INLINE
    public static ClassActor toClassActor(Word arg) {
        return ClassID.toClassActor(arg.asAddress().toInt());
    }

}
