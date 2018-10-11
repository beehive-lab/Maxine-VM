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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.vm.log.*;

/**
 * Access to the VM's singleton object that manages the {@linkplain VMLog log}.
 * <p>
 * Truncates the deep copy.
 *
 * @see VMLog
 */
public class TeleVMLog extends TeleTupleObject {

    private static TeleVMLog vmLog = null;

    /**
     * Gets the singleton object in the VM that manages the log.
     */
    public static TeleVMLog getVMLog(TeleVM vm) {
        if (vmLog == null) {
            final RemoteReference vmLogRef = vm.fields().VMLog_vmLog.readRemoteReference(vm);
            if (vmLogRef != null) {
                vmLog = (TeleVMLog) vm.objects().makeTeleObject(vmLogRef);
            }
        }
        return vmLog;
    }

    private final int logEntries;

    private int nextID;

    /**
     * The deep-copied set of {@link VMLogger} instances, used for operation/argument customization.
     */
    private VMLogger[] loggers = null;

    protected TeleVMLog(TeleVM vm, RemoteReference vmLogReference) {
        super(vm, vmLogReference);
        this.logEntries = fields().VMLog_logEntries.readInt(vmLogReference);
        this.nextID = fields().VMLog_nextId.readInt(vmLogReference);
    }

    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        this.nextID = fields().VMLog_nextId.readInt(reference());
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Truncate deep copy.
     */
    @Override
    public Object createDeepCopy(DeepCopier context) {
        return null;
    }

    /**
     * The value of {@link VMLog} {@code logEntries}, which is set at image build time.
     * For implementations with a shared global buffer and fixed size log records
     * this value is the largest number of records that can be in existence.
     * However, for per-thread buffers and/or variable size log records,
     * it may be an underestimate.
     */
    public int logEntries() {
        return logEntries;
    }

    /**
     * Monotonically increasing global unique id for a log record.
     * Incremented every time {@link VMLog}{@code .getRecord()} is invoked.
     *
     * @see VMLog
     */
    public int nextID() {
        return nextID;
    }

    public VMLogger getLogger(int id) {
        for (VMLogger logger : loggers()) {
            if (logger != null && logger.loggerId == id) {
                return logger;
            }
        }
        return null;
    }

    private VMLogger[] loggers() {
        if (loggers == null) {
            RemoteReference loggersRef = fields().VMLog_loggers.readRemoteReference(reference());
            TeleArrayObject teleLoggersArray = (TeleArrayObject) objects().makeTeleObject(loggersRef);
            loggers = (VMLogger[]) teleLoggersArray.deepCopy();
        }
        return loggers;
    }

}
