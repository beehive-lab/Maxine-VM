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
package com.sun.max.vm;

import java.util.*;
import java.util.regex.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * A circular buffer of logged VM operations.
 * A VM operation is defined by a {@link Logger} instance, which
 * is typically associated with some component of the VM.
 * A logger defines a set of operations, cardinality {@code N} each identified
 * by an {@code int} code in the range {@code [0 .. N-1]}.
 * A series of "log" methods are provided, that take the operation code
 * and a varying number of {@link Word} arguments. Currently these
 * must not be {@link Reference} types as no GC support is provided
 * for values in the log buffer. The thread (id) generating the log record
 * is automatically recorded.
 * <p>
 * A logger typically will implement the {@link Logger#operationName(int)}
 * method that returns a descriptive name for the operation.
 * <p>
 * Logging is enabled on a per logger basis through the use of
 * a standard {@code -XX:+LogXXX} option derived from the logger name.
 * Tracing to the {@link Log} stream is also available through {@code -XX:+TraceXXX},
 * and a default implementation is provided, although this can be overridden.
 */
public class VMLog {

    public static class Record {
        private static final int LOGGER_ID_SHIFT = 4;
        private static final int LOGGER_ID_MASK = 0xFFF0;
        private static final int OPERATION_SHIFT = 16;
        private static final int OPERATION_MASK = 0xFFFF0000;
        private static final int ARGCOUNT_MASK = 0xF;

        /**
         * Encodes the loggerId, the operation and the argument count.
         * Bits 0-3: argument count
         * Bits 4-15: logger id
         * Bits 16-31: operation id
         * Zero means not in use.
         */
        @INSPECTED
        public int oplc;
        @INSPECTED
        public int threadId;
        @INSPECTED
        public Word arg1;
        @INSPECTED
        public Word arg2;
        @INSPECTED
        public Word arg3;
        @INSPECTED
        public Word arg4;
        @INSPECTED
        public Word arg5;
        @INSPECTED
        public Word arg6;
        @INSPECTED
        public Word arg7;

        private Record() {

        }

        public static int getOperation(int oplc) {
            return oplc >> Record.OPERATION_SHIFT;
        }

        public static int getLoggerId(int oplc) {
            return oplc & Record.LOGGER_ID_MASK;
        }

        public static int getArgCount(int oplc) {
            return oplc & Record.ARGCOUNT_MASK;
        }
    }

    /**
     * Inspector use only.
     */
    @HOSTED_ONLY
    public static class HostedRecord extends Record {
        public final long id;
        public HostedRecord(long id, int oplc, int threadId, Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6, Word arg7) {
            this.id = id;
            this.oplc = oplc;
            this.threadId = threadId;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
            this.arg5 = arg5;
            this.arg6 = arg6;
            this.arg7 = arg7;
        }
    }

    public static class Logger {
        private static int nextLoggerId = 1;

        /**
         * Descriptive name also used to create option names.
         */
        public final String name;
        /**
         * Creates a unique id when combined with operation id. Identifies the logger in the loggers map.
         */
        private final int loggerId;
        /**
         * Number of distinct operations that can be logged.
         */
        private final int numOps;
        /**
         * Bit n is set of operation n is to be logged.
         */
        private final BitSet logOp;

        private final VMBooleanXXOption logOption;
        private final VMBooleanXXOption traceOption;
        private final VMStringOption logIncludeOption;
        private final VMStringOption logExcludeOption;
        private boolean log;
        private boolean trace;

        private int[] argCounts = new int[8];

        @HOSTED_ONLY
        protected Logger(String name, int numOps) {
            this.name = name;
            this.numOps = numOps;
            loggerId = nextLoggerId++ << Record.LOGGER_ID_SHIFT;
            logOp = new BitSet(numOps);
            // At VM startup we log everything; this gets refined once the VM is up in checkLogging.
            // This is because we cannot control the logging until the VM has parsed the PRISTINE options.
            log = true;
            for (int i = 0; i < numOps; i++) {
                logOp.set(i, true);
            }
            String logName = "Log" + name;
            logOption = new VMBooleanXXOption("-XX:-" + logName, "Log" + name);
            traceOption = new VMBooleanXXOption("-XX:-" + "Trace" + name, "Trace" + name);
            logIncludeOption = new VMStringOption("-XX:" + logName + "Include=", false, null, "list of " + name + " operations to include");
            logExcludeOption = new VMStringOption("-XX:" + logName + "Exclude=", false, null, "list of " + name + " operations to exclude");
            VMOptions.register(logOption, MaxineVM.Phase.PRISTINE);
            VMOptions.register(traceOption, MaxineVM.Phase.PRISTINE);
            VMOptions.register(logIncludeOption, MaxineVM.Phase.PRISTINE);
            VMOptions.register(logExcludeOption, MaxineVM.Phase.PRISTINE);
            vmLog.loggers.put(loggerId, this);
        }

        public String threadName(int id) {
            VmThread vmThread = VmThreadMap.ACTIVE.getVmThreadForID(id);
            return vmThread == null ? "DEAD" : vmThread.getName();
        }

        /**
         * Provides a mnemonic name for the given operation.
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
        protected String argString(int op, int argNum, Word arg) {
            return Long.toHexString(arg.asAddress().toLong());
        }

        protected boolean traceEnabled() {
            return trace;
        }

        /**
         * Implements the default trace option {@code -XX:+TraceXXX}.
         * {@link Log#lock()} and {@link Log#unlock(boolean)} are
         * handled by the caller.
         * @param r
         */
        protected void trace(Record r) {
            Log.print("Thread \"");
            Log.print(threadName(r.threadId));
            Log.print("\" ");
            Log.print(name);
            Log.print('.');
            int op = Record.getOperation(r.oplc);
            Log.print(operationName(op));
            int argCount = Record.getArgCount(r.oplc);
            for (int i = 0; i < argCount; i++) {
                Log.print(' ');
                Word arg = Word.zero();
                // Checkstyle: stop
                switch (i) {
                    case 0: arg = r.arg1; break;
                    case 1: arg = r.arg2; break;
                    case 2: arg = r.arg3; break;
                    case 3: arg = r.arg4; break;
                    case 4: arg = r.arg5; break;
                    case 5: arg = r.arg6; break;
                    case 6: arg = r.arg7; break;
                }
                // Checkstyle: resume
                Log.print(argString(op, i, arg));
            }
            Log.println();
        }

        protected void checkLogOptions() {
            trace = traceOption.getValue();
            log = trace | logOption.getValue();
            if (log) {
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

        private Record log(int op, int argCount) {
            Record r = null;
            if (log && logOp.get(op)) {
                r = getRecord(argCount);
                r.oplc = (op << Record.OPERATION_SHIFT) | loggerId | argCount;
                r.threadId = VmThread.current().id();
                argCounts[argCount]++;
            }
            return r;
        }

        public void log(int op) {
            Record r = log(op, 0);
            if (r != null && trace) {
                doTrace(r);
            }
        }

        public void log(int op, Word arg1) {
            Record r = log(op, 1);
            if (r != null) {
                r.arg1 = arg1;
            }
            if (r != null && trace) {
                doTrace(r);
            }
        }

        public void log(int op, Word arg1, Word arg2) {
            Record r = log(op, 2);
            if (r != null) {
                r.arg1 = arg1;
                r.arg2 = arg2;
            }
            if (r != null && trace) {
                doTrace(r);
            }
        }

        public void log(int op, Word arg1, Word arg2, Word arg3) {
            Record r = log(op, 3);
            if (r != null) {
                r.arg1 = arg1;
                r.arg2 = arg2;
                r.arg3 = arg3;
            }
            if (r != null && trace) {
                doTrace(r);
            }
        }

        public void log(int op, Word arg1, Word arg2, Word arg3, Word arg4) {
            Record r = log(op, 4);
            if (r != null) {
                r.arg1 = arg1;
                r.arg2 = arg2;
                r.arg3 = arg3;
                r.arg4 = arg4;
            }
            if (r != null && trace) {
                doTrace(r);
            }
        }

        public void log(int op, Word arg1, Word arg2, Word arg3, Word arg4, Word arg5) {
            Record r = log(op, 5);
            if (r != null) {
                r.arg1 = arg1;
                r.arg2 = arg2;
                r.arg3 = arg3;
                r.arg4 = arg4;
                r.arg5 = arg5;
            }
            if (r != null && trace) {
                doTrace(r);
            }
        }

        public void log(int op, Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6) {
            Record r = log(op, 6);
            if (r != null) {
                r.arg1 = arg1;
                r.arg2 = arg2;
                r.arg3 = arg3;
                r.arg4 = arg4;
                r.arg5 = arg5;
                r.arg6 = arg6;
            }
            if (r != null && trace) {
                doTrace(r);
            }
        }

        public void log(int op, Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6, Word arg7) {
            Record r = log(op, 7);
            if (r != null) {
                r.arg1 = arg1;
                r.arg2 = arg2;
                r.arg3 = arg3;
                r.arg4 = arg4;
                r.arg5 = arg5;
                r.arg6 = arg6;
                r.arg7 = arg7;
            }
            if (r != null && trace) {
                doTrace(r);
            }
        }

        private void doTrace(Record r) {
            boolean lockDisabledSafepoints = Log.lock();
            trace(r);
            Log.unlock(lockDisabledSafepoints);
        }

    }

    private static final String LOG_SIZE_PROPERTY = "max.vmlog.size";
    private final static int DEFAULT_LOG_SIZE = 8192;

    @INSPECTED
    private final int logSize;
    @INSPECTED
    private Map<Integer, Logger> loggers;
    @INSPECTED
    public final Record[] buffer;
    @INSPECTED
    private volatile int nextId;

    @INSPECTED
    private static VMLog vmLog = new VMLog();

    private static final int nextIdOffset = ClassActor.fromJava(VMLog.class).findLocalInstanceFieldActor("nextId").offset();

    public static void initialize() {

    }

    private VMLog() {
        String logSizeProperty = System.getProperty(LOG_SIZE_PROPERTY);
        if (logSizeProperty != null) {
            logSize = Integer.parseInt(logSizeProperty);
        } else {
            logSize = DEFAULT_LOG_SIZE;
        }
        buffer = new Record[logSize];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new Record();
        }
        loggers = new HashMap<Integer, Logger>();
    }

    private static Record getRecord(int argCount) {
        int myId = vmLog.nextId;
        while (Reference.fromJava(vmLog).compareAndSwapInt(nextIdOffset, myId, myId + 1) != myId) {
            myId = vmLog.nextId;
        }
        Record r = vmLog.buffer[myId % vmLog.logSize];
        r.oplc = 0; // mark not in use
        return r;
    }

    /**
     * Called once the VM is up to check for limitations on logging.
     *
     */
    public static void checkLogOptions() {
        for (Logger logger : vmLog.loggers.values()) {
            logger.checkLogOptions();
        }
    }

}
