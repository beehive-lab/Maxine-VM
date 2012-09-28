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
package com.sun.max.vm.log.test;

import java.util.*;
import java.util.concurrent.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.VMLog.Flusher;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.log.nat.VMLogNative.NativeRecord;
import com.sun.max.vm.log.nat.thread.var.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.ti.*;

/**
 * A stress test for {@link VMLog}.
 * Must be included in the boot image, which is controlled by the "max.vmlog.stresstest" property.
 * Registers as a {@link VMTI} handler as a way of getting control.
 */
public class VMLogStressTest {

    private static class XFlusherDebugLogger extends VMLogger {
        XFlusherDebugLogger() {
            super("XFlusherDebug", 1, "debug XFlusher");
        }
    }

    private static final XFlusherDebugLogger xFlusherDebugLogger = new XFlusherDebugLogger();

    private static class XFlusher extends Flusher {

        @Override
        public void flushRecord(VmThread vmThread, Record r, int uuid) {
            NativeRecord nativeRecord = (NativeRecord) r;
            xFlusherDebugLogger.log(0, nativeRecord.address);
            logger.trace(r);
        }

    }

    private static class XVMLog extends VMLogNativeThreadVariableUnbound {
        private static final String X_RECORD_NAME = "X_RECORD";
        private static final String X_BUFFER_NAME = "X_BUFFER";
        private static final String X_BUFFER_OFFSETS_NAME = "X_BUFFER_OFFSETS";
        private static final VmThreadLocal X_RECORD = new VmThreadLocal(X_RECORD_NAME, true, "VMLog Stress Test Record");
        private static final VmThreadLocal X_BUFFER = new VmThreadLocal(X_BUFFER_NAME, false, "VMLog Stress Test  buffer");
        private static final VmThreadLocal X_BUFFER_OFFSETS = new VmThreadLocal(X_BUFFER_OFFSETS_NAME, false, "VMLog Stress Test buffer first/next offsets");

        @Override
        public void initialize(MaxineVM.Phase phase) {
            super.initialize(phase);
            if (MaxineVM.isHosted() && phase == MaxineVM.Phase.BOOTSTRAPPING) {
                setNativeRecordThreadLocal(X_RECORD);
                setBufferThreadLocals(X_BUFFER, X_BUFFER_OFFSETS);
            }
        }

        int numLogEntries() {
            return logEntries;
        }
    }

    private static class Tester extends Thread {
        private Random rand;
        private LoggedData loggedData;

        Tester(int i) {
            rand = new Random(46713 + i);
            setName("Tester-" + i);
        }

        @Override
        public void run() {
            loggedData = new LoggedData();
            loggedDataMap.put(VmThread.fromJava(this).id(), loggedData);
            long uuid = 0;
            int myIterations = iterations;

            try {
                while (myIterations > 0) {
                    int argc = rand.nextInt(4);
                    long[] args = new long[argc + 1];
                    for (int i = 1; i <= args.length; i++) {
                        args[i - 1] = rand.nextLong();
                    }
                    int index = loggedData.save(new Data(uuid, args));
                    switch (argc) {
                        case 0:
                            logger.logFoo1(uuid, index, args[0]);
                            break;
                        case 1:
                            logger.logFoo2(uuid, index, args[0], args[1]);
                            break;
                        case 2:
                            logger.logFoo3(uuid, index, args[0], args[1], args[2]);
                            break;
                        case 3:
                            logger.logFoo4(uuid, index, args[0], args[1], args[2], args[3]);
                    }
                    uuid++;
                    myIterations--;
                }
            } catch (Throwable ex) {
                Log.println(ex.getMessage());
                MaxineVM.native_exit(1);
            }
        }

    }

    private static class Data {
        final long uuid;
        final long[] data;

        Data(long uuid, long[] data) {
            this.uuid = uuid;
            this.data = data;
        }
    }

    /**
     * A per-thread store of logged records.
     *
     */
    private static class LoggedData {
        /**
         * index where next record will be stored (circular buffer).
         */
        int index;
        /**
         * Circular buffer of logged records.
         */
        Data[] dataStore = new Data[xvmLog.numLogEntries() * 2];
        /**
         * The last uuid checked.
         */
        long lastUuid = -1;

        /**
         * Stores the data in the buffer and returns the index at which it was stored.
         * @param data
         * @return
         */
        int save(Data data) {
            int result = index;
            dataStore[index++] = data;
            if (index >= dataStore.length) {
                index = 0;
            }
            return result;
        }

        /**
         * Checks that the data at index {@code uuid} matches {@code args}.
         * and that records are delivered in the correct order with no duplicates/omissions.
         * @param uuid
         * @param args
         */
        void check(long uuid, long index, long[] args) {
            asert(uuid == lastUuid + 1);
            lastUuid = uuid;
            Data storedData = dataStore[(int) index];
            asert(storedData.uuid == uuid);
            asert(storedData.data.length == args.length);
            for (int i = 0; i < args.length; i++) {
                asert(storedData.data[i] == args[i]);
            }
        }

        @NEVER_INLINE
        void asert(boolean value) {
            if (!value) {
                throw new RuntimeException("logged data mismatch");
            }
        }
    }

    private static int iterations = 1000000;

    private static class VMTIHandler extends NullVMTIHandler {
        @Override
        public void vmInitialized() {
            int numThreads = 1;
            String extArg = System.getProperty("max.vmlog.stresstest");
            if (extArg == null) {
                return;
            }

            String[] args = extArg.split(",");
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("t=")) {
                    // TODO
                } else if (arg.startsWith("c=")) {
                    iterations = getValue(arg);
                }
            }

            logger.enable(true);

            Thread[] threads = new Thread[numThreads];
            for (int t = 0; t < numThreads; t++) {
                threads[t] = new Tester(t);
                threads[t].start();
            }

        }

        private static int getValue(String arg) {
            int ix = arg.indexOf('=');
            return Integer.parseInt(arg.substring(ix + 1));
        }
    }

    private static final XVMLogger logger = new XVMLogger();
    private static final XVMLog xvmLog = new XVMLog();

    static {
        xvmLog.initialize(MaxineVM.Phase.BOOTSTRAPPING);
        xvmLog.registerCustom(logger, new XFlusher());
        VMTI.registerEventHandler(new VMTIHandler());
    }

    private static volatile boolean done;

    private static Map<Integer, LoggedData> loggedDataMap = new ConcurrentHashMap<Integer, LoggedData>();

    private static class XVMLogger extends XVMLoggerAuto {
        XVMLogger() {
            super("Stress Tester");
        }

        @NEVER_INLINE
        private static LoggedData getLoggedData(int threadId) {
            LoggedData result = loggedDataMap.get(threadId);
            assert result != null;
            return result;
        }

        @Override
        @NEVER_INLINE
        protected void traceFoo1(int threadId, long uuid, long index, long arg1) {
            getLoggedData(threadId).check(uuid, index, new long[] {arg1});
        }

        @Override
        @NEVER_INLINE
        protected void traceFoo2(int threadId, long uuid, long index, long arg1, long arg2) {
            getLoggedData(threadId).check(uuid, index, new long[] {arg1, arg2});
        }

        @Override
        @NEVER_INLINE
        protected void traceFoo3(int threadId, long uuid, long index, long arg1, long arg2, long arg3) {
            getLoggedData(threadId).check(uuid, index, new long[] {arg1, arg2, arg3});
        }

        @Override
        @NEVER_INLINE
        protected void traceFoo4(int threadId, long uuid, long index, long arg1, long arg2, long arg3, long arg4) {
            getLoggedData(threadId).check(uuid, index, new long[] {arg1, arg2, arg3, arg4});
        }
    }

    @HOSTED_ONLY
    @VMLoggerInterface(hidden = true, traceThread = true)
    interface XVMLoggerInterface {
        void foo1(long uuid, long index, long value1);
        void foo2(long uuid, long index, long value1, long value2);
        void foo3(long uuid, long index, long value1, long value2, long value3);
        void foo4(long uuid, long index, long value1, long value2, long value3, long value4);
    }

// START GENERATED CODE
    private static abstract class XVMLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            Foo1, Foo2, Foo3,
            Foo4;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected XVMLoggerAuto(String name) {
            super(name, Operation.VALUES.length, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logFoo1(long arg1, long arg2, long arg3) {
            log(Operation.Foo1.ordinal(), longArg(arg1), longArg(arg2), longArg(arg3));
        }
        protected abstract void traceFoo1(int threadId, long arg1, long arg2, long arg3);

        @INLINE
        public final void logFoo2(long arg1, long arg2, long arg3, long arg4) {
            log(Operation.Foo2.ordinal(), longArg(arg1), longArg(arg2), longArg(arg3), longArg(arg4));
        }
        protected abstract void traceFoo2(int threadId, long arg1, long arg2, long arg3, long arg4);

        @INLINE
        public final void logFoo3(long arg1, long arg2, long arg3, long arg4, long arg5) {
            log(Operation.Foo3.ordinal(), longArg(arg1), longArg(arg2), longArg(arg3), longArg(arg4), longArg(arg5));
        }
        protected abstract void traceFoo3(int threadId, long arg1, long arg2, long arg3, long arg4, long arg5);

        @INLINE
        public final void logFoo4(long arg1, long arg2, long arg3, long arg4, long arg5,
                long arg6) {
            log(Operation.Foo4.ordinal(), longArg(arg1), longArg(arg2), longArg(arg3), longArg(arg4), longArg(arg5),
                longArg(arg6));
        }
        protected abstract void traceFoo4(int threadId, long arg1, long arg2, long arg3, long arg4, long arg5,
                long arg6);

        @Override
        protected void trace(Record r) {
            int threadId = r.getThreadId();
            switch (r.getOperation()) {
                case 0: { //Foo1
                    traceFoo1(threadId, toLong(r, 1), toLong(r, 2), toLong(r, 3));
                    break;
                }
                case 1: { //Foo2
                    traceFoo2(threadId, toLong(r, 1), toLong(r, 2), toLong(r, 3), toLong(r, 4));
                    break;
                }
                case 2: { //Foo3
                    traceFoo3(threadId, toLong(r, 1), toLong(r, 2), toLong(r, 3), toLong(r, 4), toLong(r, 5));
                    break;
                }
                case 3: { //Foo4
                    traceFoo4(threadId, toLong(r, 1), toLong(r, 2), toLong(r, 3), toLong(r, 4), toLong(r, 5), toLong(r, 6));
                    break;
                }
            }
        }
    }

// END GENERATED CODE

}
