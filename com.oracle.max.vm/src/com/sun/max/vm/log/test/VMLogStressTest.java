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
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.VMLog.Flusher;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.log.nat.thread.var.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.ti.*;

/**
 * A stress test for {@link VMLog}.
 * Must be included in the boot image, which is controlled by the "max.vmlog.stresstest" property.
 * Registers as a {@link VMTI} handler as a way of getting control.
 */
public class VMLogStressTest {

    private static class XFlusher extends Flusher {

        @Override
        public void flushRecord(VmThread vmThread, Record r, int uuid) {
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
        private long uuid;
        private LoggedData loggedData;

        Tester(int i) {
            rand = new Random(46713 + i);
            setName("Tester-" + i);
        }

        @Override
        public void run() {
            loggedData = new LoggedData();
            loggedDataMap.put(VmThread.fromJava(this).id(), loggedData);
            while (!done) {
                int argc = rand.nextInt(4);
                int[] args = new int[argc + 1];
                for (int i = 1; i <= args.length; i++) {
                    args[i - 1] = rand.nextInt();
                }
                loggedData.save(new Data(uuid, args));
                switch (argc) {
                    case 0:
                        logger.logFoo1(uuid, args[0]);
                        break;
                    case 1:
                        logger.logFoo2(uuid, args[0], args[1]);
                        break;
                    case 2:
                        logger.logFoo3(uuid, args[0], args[1], args[2]);
                        break;
                    case 3:
                        logger.logFoo4(uuid, args[0], args[1], args[2], args[3]);
                }
                uuid++;
            }
        }
    }

    private static class Data {
        long uuid;
        int[] data;

        Data(long uuid, int[] data) {
            this.uuid = uuid;
            this.data = data;
        }
    }

    private static class LoggedData {
        int index;
        Data[] dataStore = new Data[xvmLog.numLogEntries() * 2];

        void save(Data data) {
            dataStore[index++] = data;
            if (index >= dataStore.length) {
                index = 0;
            }
        }

        void check(long uuid, int[] args) {

        }
    }

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
                if (arg.equals("t")) {
                    // TODO
                }
            }

            Thread[] threads = new Thread[numThreads];
            for (int t = 0; t < numThreads; t++) {
                threads[t] = new Tester(t);
                threads[t].start();
            }

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

        @Override
        protected void traceFoo1(int threadId, long uuid, int arg1) {
            loggedDataMap.get(threadId).check(uuid, new int[] {arg1});
        }

        @Override
        protected void traceFoo2(int threadId, long uuid, int arg1, int arg2) {
            loggedDataMap.get(threadId).check(uuid, new int[] {arg1, arg2});
        }

        @Override
        protected void traceFoo3(int threadId, long uuid, int arg1, int arg2, int arg3) {
            loggedDataMap.get(threadId).check(uuid, new int[] {arg1, arg2, arg3});
        }

        @Override
        protected void traceFoo4(int threadId, long uuid, int arg1, int arg2, int arg3, int arg4) {
            loggedDataMap.get(threadId).check(uuid, new int[] {arg1, arg2, arg3, arg4});
        }
    }

    @HOSTED_ONLY
    @VMLoggerInterface(hidden = true, traceThread = true)
    static interface XVMLoggerInterface {
        void foo1(long uuid, int value1);
        void foo2(long uuid, int value1, int value2);
        void foo3(long uuid, int value1, int value2, int value3);
        void foo4(long uuid, int value1, int value2, int value3, int value4);
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
        public final void logFoo1(long arg1, int arg2) {
            log(Operation.Foo1.ordinal(), longArg(arg1), intArg(arg2));
        }
        protected abstract void traceFoo1(int threadId, long arg1, int arg2);

        @INLINE
        public final void logFoo2(long arg1, int arg2, int arg3) {
            log(Operation.Foo2.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3));
        }
        protected abstract void traceFoo2(int threadId, long arg1, int arg2, int arg3);

        @INLINE
        public final void logFoo3(long arg1, int arg2, int arg3, int arg4) {
            log(Operation.Foo3.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), intArg(arg4));
        }
        protected abstract void traceFoo3(int threadId, long arg1, int arg2, int arg3, int arg4);

        @INLINE
        public final void logFoo4(long arg1, int arg2, int arg3, int arg4, int arg5) {
            log(Operation.Foo4.ordinal(), longArg(arg1), intArg(arg2), intArg(arg3), intArg(arg4), intArg(arg5));
        }
        protected abstract void traceFoo4(int threadId, long arg1, int arg2, int arg3, int arg4, int arg5);

        @Override
        protected void trace(Record r) {
            int threadId = r.getThreadId();
            switch (r.getOperation()) {
                case 0: { //Foo1
                    traceFoo1(threadId, toLong(r, 1), toInt(r, 2));
                    break;
                }
                case 1: { //Foo2
                    traceFoo2(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3));
                    break;
                }
                case 2: { //Foo3
                    traceFoo3(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toInt(r, 4));
                    break;
                }
                case 3: { //Foo4
                    traceFoo4(threadId, toLong(r, 1), toInt(r, 2), toInt(r, 3), toInt(r, 4), toInt(r, 5));
                    break;
                }
            }
        }
    }

// END GENERATED CODE

}
