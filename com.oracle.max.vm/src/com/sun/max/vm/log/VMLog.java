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
package com.sun.max.vm.log;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.thread.*;

/**
 * A circular buffer of logged VM operations.
 * A logged VM operation is defined by a {@link VMLogger} instance, which
 * is typically associated with some component of the VM,and registered
 * with the {@link #registerLogger(VMLogger)} method.
 *
 * A variety of implementations of the log buffer are possible,
 * varying in performance, space overhead and complexity.
 * To allow experimentation, a specific logger is chosen by a factory class
 * at image build time, based on a system property. The default implementation is very
 * simple but space inefficient.
 *
 * Since logging has to execute before monitors are available, any synchronization
 * must be handled with compare and swap operations.
 */
public abstract class VMLog {

    /**
     * Factory class to choose implementation at image build time via property.
     */
    public static class Factory {
        private static final String VMLOG_FACTORY_CLASS = "max.vmlog.class";
        public static final String DEFAULT_VMLOG_CLASS = "java.def.VMLogDefault";

        private static VMLog create() {
            String prop = System.getProperty(VMLOG_FACTORY_CLASS);
            String className = DEFAULT_VMLOG_CLASS;
            if (prop != null) {
                className = prop;
            }
            String name = VMLog.class.getPackage().getName() + "." + className;
            try {
                return (VMLog) Class.forName(name).newInstance();
            } catch (Exception ex) {
                ProgramError.unexpected("failed to create " + name, ex);
                return null;
            }
        }

        public static boolean is(String name) {
            String prop = System.getProperty(VMLOG_FACTORY_CLASS);
            return prop == null ? name.equals(DEFAULT_VMLOG_CLASS) : name.equals(prop);
        }
    }

    /**
     * The bare essentials of a log record.
     */
    public static class Record {
        static final int ARGCOUNT_MASK = 0x7;
        static final int LOGGER_ID_SHIFT = 3;
        static final int LOGGER_ID_MASK = 0xF;
        static final int OPERATION_SHIFT = 7;
        static final int OPERATION_MASK = 0x1FF;
        static final int THREAD_SHIFT = 16;
        static final int THREAD_MASK = 0x7FFF;
        public static final int FREE = 0x80000000;
        public static final int MAX_ARGS = 7;

        /**
         * Encodes the loggerId, the operation and the argument count.
         * Bits 0-2: argument count (max 7)
         * Bits 3-6: logger id (max 16)
         * Bits 7-15: operation id (max 512)
         * Bits 16-30: threadId (max 32768)
         * Bit 31: FREE (1) for implementations that have free lists
         */
        @INSPECTED
        public volatile int header;

        final void setHeader(int op, int argCount, int loggerId) {
            header = (VmThread.current().id() << THREAD_SHIFT) | (op << Record.OPERATION_SHIFT) |
                      (loggerId << Record.LOGGER_ID_SHIFT) | argCount;
        }

        public int getArgCount() {
            return getArgCount(header);
        }

        public static int getArgCount(int header) {
            return header & Record.ARGCOUNT_MASK;
        }

        public int getOperation() {
            return getOperation(header);
        }

        public static int getOperation(int header) {
            return (header >> Record.OPERATION_SHIFT) & OPERATION_MASK;
        }

        public static int getLoggerId(int header) {
            return (header >> LOGGER_ID_SHIFT) & Record.LOGGER_ID_MASK;
        }

        public int getThreadId() {
            return getThreadId(header);
        }

        public static int getThreadId(int header) {
            return (header >> THREAD_SHIFT) & THREAD_MASK;
        }

        public static boolean isFree(int header) {
            return (header & FREE) != 0;
        }

    }

    public static class Record1 extends Record {
        @INSPECTED
        public Word arg1;
    }

    public static class Record2 extends Record1 {
        @INSPECTED
        public Word arg2;
    }

    public static class Record3 extends Record2 {
        @INSPECTED
        public Word arg3;
    }

    public static class Record4 extends Record3 {
        @INSPECTED
        public Word arg4;
    }

    public static class Record5 extends Record4 {
        @INSPECTED
        public Word arg5;
    }

    public static class Record6 extends Record5 {
        @INSPECTED
        public Word arg6;
    }

    public static class Record7 extends Record6 {
        @INSPECTED
        public Word arg7;
    }

    @INTRINSIC(UNSAFE_CAST) public static native Record1 asRecord1(Record r);
    @INTRINSIC(UNSAFE_CAST) public static native Record2 asRecord2(Record r);
    @INTRINSIC(UNSAFE_CAST) public static native Record3 asRecord3(Record r);
    @INTRINSIC(UNSAFE_CAST) public static native Record4 asRecord4(Record r);
    @INTRINSIC(UNSAFE_CAST) public static native Record5 asRecord5(Record r);
    @INTRINSIC(UNSAFE_CAST) public static native Record6 asRecord6(Record r);
    @INTRINSIC(UNSAFE_CAST) public static native Record7 asRecord7(Record r);

    /**
     * Inspector use only. Includes the record id, and args as an array.
     */
    @HOSTED_ONLY
    public static class HostedRecord extends Record {
        public final long id;
        public final Word[] args;
        public HostedRecord(long id, int header, Word... args) {
            this.id = id;
            this.header = header;
            this.args = args;
        }
    }

    private static final String LOG_SIZE_PROPERTY = "max.vmlog.size";
    private final static int DEFAULT_LOG_SIZE = 8192;

    @INSPECTED
    protected int logSize;
    @INSPECTED
    protected Map<Integer, VMLogger> loggers;

    @INSPECTED
    private static VMLog vmLog = Factory.create();

    @INSPECTED
    protected volatile int nextId;

    protected static final int nextIdOffset = ClassActor.fromJava(VMLog.class).findLocalInstanceFieldActor("nextId").offset();

    /**
     * Invoked on early VM startup.
     */
    public void initialize() {

    }

    public static VMLog vmLog() {
        return vmLog;
    }

    void registerLogger(VMLogger logger) {
        vmLog.loggers.put(logger.loggerId, logger);
    }

    private void checkLogSize() {
        String logSizeProperty = System.getProperty(LOG_SIZE_PROPERTY);
        if (logSizeProperty != null) {
            logSize = Integer.parseInt(logSizeProperty);
        } else {
            logSize = DEFAULT_LOG_SIZE;
        }
    }

    protected VMLog() {
        checkLogSize();
        loggers = new HashMap<Integer, VMLogger>();
    }


    /**
     * Called once the VM is up to check for limitations on logging.
     *
     */
    public static void checkLogOptions() {
        for (VMLogger logger : vmLog.loggers.values()) {
            logger.checkLogOptions();
        }
    }

    protected abstract Record getRecord(int argCount);

}
