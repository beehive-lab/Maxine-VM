/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vma.tools.qa;

import java.io.PrintStream;

import com.oracle.max.vma.tools.qa.TransientVMAdviceHandlerTypes.*;

/**
 * Maintains the basic information on an object instance encountered in a trace.
 *
 */
public class ObjectRecord {
    /**
     * The unique identifier associated with this object.
     * If {@code id == null} then this record does not denote an object instance; rather it
     * it is used to carry the trace elements for the static fields of the class denoted by {@link #klass}.
     *
     */
    public final String id;
    /**
     * The runtime class of this object.
     */
    public final ClassRecord klass;

    /**
     * The {@link ThreadRecord} of the thread that created this object.
     */
    public final ThreadRecord thread;

    /**
     * The list of {@link AdviceRecord instances} manipulating this object.
     */
    private GrowableArray adviceRecords = GrowableArrayImpl.create();

    /**
     * The record in the trace that corresponds to the creation of this object.
     */
    public AdviceRecord beginCreationRecord;

    public AdviceRecord endCreationRecord;

    public AdviceRecord removalRecord;

    /**
     * Indicates that the no more changes will be made to this record and
     * that the values of {@link #modifyLifeTime} and {@link #lastModifyTime} are valid.
     */
    private boolean immutable = false;

    /*
     * The following fields are caches for performance reasons, and are only set
     * after immutable == true. A value of -1 indicates an unset cache value.
     */

    /**
     * The length of time that this object was mutable.
     * I.e., the time from its creation until the {@link #lastModifyTime}.
     */
    private long modifyLifeTime = -1;

    /**
     * The last time that this object instance was modified.
     */
    private long lastModifyTime = -1;

    public int traceOccurrences = 1;  // number of times this id appears in the trace

    public ObjectRecord(String id, int gcEpoch, ClassRecord cr, ThreadRecord threadRecord, AdviceRecord beginCreationRecord) {
        this.id = getMapId(id, gcEpoch);
        this.klass = cr;
        this.thread = threadRecord;
        this.beginCreationRecord = beginCreationRecord;
    }

    @Override
    public String toString() {
        return "(" + klass.getName() + ") " + getId();
    }

    public String toString(TraceRun traceRun, boolean showClass, boolean showThread, boolean showCt, boolean showEc,
                    boolean showLt, boolean showMlt) {
        StringBuilder result = new StringBuilder(id);
        if (showClass) {
            result.append(", ");
            result.append(klass.name);
        }
        if (showThread) {
            result.append(", th ");
            result.append(thread);
        }
        if (showCt) {
            result.append(", ct ");
            result.append(TimeFunctions.formatTime(getBeginCreationTime() - traceRun.startTime));
        }
        if (showEc) {
            result.append(", ec ");
            result.append(TimeFunctions.formatTime(getEndCreationTime() - traceRun.startTime));
        }
        if (showLt) {
            result.append(", lt ");
            result.append(TimeFunctions.formatTime(getLifeTime(traceRun.lastTime)));
        }
        if (showLt) {
            result.append(", mlt ");
            result.append(TimeFunctions.formatTime(getModifyLifeTime()));
        }
        result.append(removalRecord == null ? ", alive" : ", dead");
        return result.toString();
    }

    /**
     * An estimate of the size of an instance of this class.
     */
    public static int getSize() {
        return 80;
    }

    /**
     * Gets the id form that is stored in the map.
     * @param id
     * @return unique id
     */
    public static String getMapId(String id, int gcEpoch) {
        return id == null ? null : id + ":" + gcEpoch;
    }

    /**
     * This returns the full id, i.e. including the subId, even for the root.
     */
    public String getId() {
        if (id == null) {
            return "staticTuple";
        } else {
            return id;
        }
    }

    public ClassRecord getClassRecord() {
        return klass;
    }

    public String getClassName() {
        String result = klass.getName();
        return result;
    }

    public String getClassLoaderId() {
        return klass.getClassLoaderId();
    }

    /**
     * The time at which the object was allocated, but before the constructor has executed.
     */
    public long getBeginCreationTime() {
        return beginCreationRecord.time;
    }

    /**
     * The time at which constructor execution is complete.
     */
    public long getEndCreationTime() {
        return endCreationRecord.time;
    }

    /**
     * The time at which the object was known to have been garbage collected.
     * N.B. The trace may not reflect the fact that an object has been collected since
     * there is an inevitable lag between this state and our ability to detect it.
     * I.e., even if this value is non-zero, it may be an arbitrary interval after the
     * object was collected.
     *
     */
    public long getDeletionTime() {
        return removalRecord == null ? 0 : removalRecord.time;
    }

    public boolean isArray() {
        return klass.isArray();
    }

    /**
     * No more updates, allowing cached results from analysis.
     */
    public void setImmutable() {
        immutable = true;
    }

    /**
     * Only used for objects whose construction we learn about after the
     * constructor has executed, i.e, those whose allocation was not logged..
     */
    public void setBeginCreationRecord(AdviceRecord beginCreationRecord) {
        if (immutable) {
            throw new IllegalAccessError();
        } else {
            this.beginCreationRecord = beginCreationRecord;
        }
    }

    public void setEndCreationRecord(AdviceRecord endCreationRecord) {
        if (immutable) {
            throw new IllegalAccessError();
        } else {
            this.endCreationRecord = endCreationRecord;
        }
    }

    public void setRemovalRecord(AdviceRecord removalRecord) {
        if (immutable) {
            throw new IllegalAccessError();
        } else {
            this.removalRecord = removalRecord;
        }
    }

    public void addTraceElement(AdviceRecord adviceRecord) {
        if (immutable) {
            throw new IllegalAccessError();
        } else {
            adviceRecords = adviceRecords.add(adviceRecord);
        }
    }

    public GrowableArray getAdviceRecords() {
        return adviceRecords;
    }

    /**
     * Returns the time period from the end of the object construction to its
     * removal, if known, else to lastTime.
     */
    public long getLifeTime(long lastTime) {
        long result =  ((removalRecord == null) ? lastTime : removalRecord.time)
                - endCreationRecord.time;
//        if (result < 0) {
//            System.console();
//        }
        return result;
    }

    public long getLastModifyTime() {
        if (immutable && (lastModifyTime >= 0)) {
            return lastModifyTime;
        }
        getModifyLifeTime();
        return lastModifyTime;
    }

    /**
     * Returns the time period from the end of the object construction to the
     * last time the object was modified. If no updates took place after
     * construction, returns 0. Also sets lastModifyTime.
     */
    public long getModifyLifeTime() {
        // traces are ordered by time
        if (immutable && (modifyLifeTime >= 0)) {
            return modifyLifeTime;
        }

        lastModifyTime = endCreationRecord.time;
        int s = adviceRecords.size();
        if (s > 0) {
            for (int i = s - 1; i >= 0; i--) {
                AdviceRecord ar = adviceRecords.get(i);
                if (RecordType.MODIFY_OPERATIONS.contains(ar.getRecordType())) {
                    lastModifyTime = ar.time;
                    break;
                }
            }
        }

        if (lastModifyTime <= endCreationRecord.time) {
            return 0;
        } else {
            return lastModifyTime - endCreationRecord.time;
        }
    }

    public int getLength() {
        assert klass.isArray();
        return beginCreationRecord.getPackedValue();
    }

    /**
     * Traversal (visitor) support.
     */
    public static abstract class Visitor {
        public abstract void visit(TraceRun traceRun, ObjectRecord td,
                AdviceRecord te, PrintStream ps, Object[] args);

        public abstract Object getResult();
    }

    /**
     * Visit all the trace elements associated with object instance {@code td}.
     * @param traceRun identifies the specific trace associated with this object instance
     * @param td the object instance whose traces are to be visited
     * @param visitor the visitor object to apply to each trace element
     * @param ps any output should be written to this stream
     * @param args any additional arguments to pass to {@code visitor}
     * @return the value from {@link Visitor#getResult()}
     */
    public Object visit(TraceRun traceRun, ObjectRecord td, Visitor visitor,
            PrintStream ps, Object[] args) {
        for (int i = 0; i < td.adviceRecords.size(); i++) {
            AdviceRecord te = td.adviceRecords.get(i);
            visitor.visit(traceRun, td, te, ps, args);
        }
        return visitor.getResult();
    }

}
