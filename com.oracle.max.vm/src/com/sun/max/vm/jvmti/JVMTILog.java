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
package com.sun.max.vm.jvmti;

import java.util.*;
import java.util.regex.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * A circular buffer of logged JVMTI upcalls.
 */
public class JVMTILog {

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
        public int op;
        @INSPECTED
        public int threadId;
        @INSPECTED
        public int id;
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

        @HOSTED_ONLY
        public Record(int op, int threadId, int startIndex, Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6) {
            this.op = op;
            this.threadId = threadId;
            this.id = startIndex;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
            this.arg5 = arg5;
            this.arg6 = arg6;
        }

        private Record() {

        }

        public static int getOperation(int op) {
            return op >> Record.OPERATION_SHIFT;
        }

        public static int getLoggerId(int op) {
            return op & Record.LOGGER_ID_MASK;
        }

        public static int getArgCount(int op) {
            return op & Record.ARGCOUNT_MASK;
        }

    }

    public abstract static class Logger {
        private static int nextLoggerId = 1;

        /**
         * Decriptive name also used to create option names.
         */
        public final String name;
        /**
         * Creates a unique id when combined with operation id. Identifies the logger in the loggers map.
         */
        private int loggerId;
        /**
         * Number of distinct operations that can be logged.
         */
        private int numOps;
        /**
         * Bit n is set of operation n is to be logged.
         */
        private BitSet logOp;

        private VMBooleanXXOption logOption;
        private VMStringOption logIncludeOption;
        private VMStringOption logExcludeOption;
        private boolean log;

        @HOSTED_ONLY
        protected Logger(String name, int numOps) {
            this.name = name;
            this.numOps = numOps;
            loggerId = nextLoggerId++ << Record.LOGGER_ID_SHIFT;
            logOp = new BitSet(numOps);
            // at VM startup we log everything; this gets refined once the VM is up in checkLogging
            // this is because we cannot control the logging until the VM has parsed the PRISTINE options.
            log = true;
            for (int i = 0; i < numOps; i++) {
                logOp.set(i, true);
            }
            String logName = "Log" + name;
            logOption = new VMBooleanXXOption("-XX:-" + logName, "Log " + name);
            logIncludeOption = new VMStringOption("-XX:" + logName + "Include", false, null, "list of " + name + " operations to include");
            logExcludeOption = new VMStringOption("-XX:" + logName + "Exclude", false, null, "list of " + name + " operations to exclude");
            VMOptions.register(logOption, MaxineVM.Phase.PRISTINE);
            VMOptions.register(logIncludeOption, MaxineVM.Phase.PRISTINE);
            VMOptions.register(logExcludeOption, MaxineVM.Phase.PRISTINE);
            loggers.put(loggerId, this);
        }

        public abstract String operationName(int op);

        private void checkLogging() {
            log = logOption.getValue();
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
                r = get();
                r.op = (op << Record.OPERATION_SHIFT) | loggerId | argCount;
                r.threadId = VmThread.current().id();
            }
            return r;
        }

        void log(int op) {
            log(op, 0);
        }

        void log(int op, Word arg1) {
            Record r = log(op, 1);
            if (r != null) {
                r.arg1 = arg1;
            }
        }

        void log(int op, Word arg1, Word arg2) {
            Record r = log(op, 2);
            if (r != null) {
                r.arg1 = arg1;
                r.arg2 = arg2;
            }
        }

        void log(int op, Word arg1, Word arg2, Word arg3) {
            Record r = log(op, 3);
            if (r != null) {
                r.arg1 = arg1;
                r.arg2 = arg2;
                r.arg3 = arg3;
            }
        }

        void log(int op, Word arg1, Word arg2, Word arg3, Word arg4) {
            Record r = log(op, 4);
            if (r != null) {
                r.arg1 = arg1;
                r.arg2 = arg2;
                r.arg3 = arg3;
                r.arg4 = arg4;
            }
        }

        void log(int op, Word arg1, Word arg2, Word arg3, Word arg4, Word arg5) {
            Record r = log(op, 5);
            if (r != null) {
                r.arg1 = arg1;
                r.arg2 = arg2;
                r.arg3 = arg3;
                r.arg4 = arg4;
                r.arg5 = arg5;
            }
        }

        void log(int op, Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6) {
            Record r = log(op, 6);
            if (r != null) {
                r.arg1 = arg1;
                r.arg2 = arg2;
                r.arg3 = arg3;
                r.arg4 = arg4;
                r.arg5 = arg5;
                r.arg6 = arg6;
            }
        }

    }

    private static final String LOG_SIZE_PROPERTY = "com.oracle.max.jvmti.logsize";
    private final static int DEFAULT_LOG_SIZE = 8192;
    static int logSize = DEFAULT_LOG_SIZE;

    @INSPECTED
    private static Map<Integer, Logger> loggers = new HashMap<Integer, Logger>();

    @INSPECTED
    private static JVMTILog singleton = new JVMTILog();

    @INSPECTED
    public final Record[] buffer;
    @INSPECTED
    private volatile int nextId;

    private static final int nextIdOffset = ClassActor.fromJava(JVMTILog.class).findLocalInstanceFieldActor("nextId").offset();

    public JVMTILog() {
        String logSizeProperty = System.getProperty(LOG_SIZE_PROPERTY);
        if (logSizeProperty != null) {
            logSize = Integer.parseInt(logSizeProperty);
        }
        buffer = new Record[logSize];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new Record();
        }
    }

    private static Record get() {
        int myId = singleton.nextId;
        while (Reference.fromJava(singleton).compareAndSwapInt(nextIdOffset, myId, myId + 1) != myId) {
            myId = singleton.nextId;
        }
        Record r = singleton.buffer[myId % logSize];
        r.id = myId;
        r.op = 0; // mark not in use
        // clear out old values
        r.threadId = 0;
        r.arg1 = Word.zero();
        r.arg2 = Word.zero();
        r.arg3 = Word.zero();
        r.arg4 = Word.zero();
        r.arg5 = Word.zero();
        r.arg6 = Word.zero();
        return r;
    }

    /**
     * Called once the VM is up to check for limitations on JVMTI logging.
     *
     */
    static void checkLogging() {
        for (Logger logger : loggers.values()) {
            logger.checkLogging();
        }
    }

}
