/*
 * Copyright (c) 2004, 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Maintains the basic information on an object instance encountered in a trace.
 *
 * @author Mick Jordan
 *
 */
public class ObjectRecord {
    /**
     * The unique identifier associated with this object.
     * If {@code id == null} then this record does not denote an object instance; rather it
     * it is used to carry the trace elements for the static fields of the class denoted by {@link #cr}.
     *
     */
    private String id;
    /**
     * The runtime class of this object.
     */
    private ClassRecord cr;

    /**
     * The {@link ThreadRecord} of the thread that created this object.
     */
    private ThreadRecord thread;

    /**
     * The time at which the object was allocated, but before the constructor has executed.
     */
    private long beginCreationTime;

    /**
     * The time at which constructor execution is complete.
     */
    private long endCreationTime;

    /**
     * The time at which the object was known to have been garbage collected.
     * N.B. The trace may not reflect the fact that an object has been collected since
     * there is an inevitable lag between this state and our ability to detect it.
     * I.e., even if this value is non-zero, it may be an arbitrary interval after the
     * object was collected.
     *
     */
    private long deletionTime;

    /**
     * The list of {@link TraceElement trace elements} associated with this object, ordered chronologically.
     */
    private GrowableArray traceElements = GrowableArrayImpl.create();

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

    private int length;  // if an array

    public ObjectRecord(String id, int gcEpoch, ClassRecord cr, ThreadRecord threadRecord, long beginCreationTime) {
        this.id = getMapId(id, gcEpoch);
        this.cr = cr;
        this.thread = threadRecord;
        this.beginCreationTime = beginCreationTime;
    }

    @Override
    public String toString() {
        return getId();
    }

    /**
     * An estimate of the size of an instance of this class.
     * @return
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
     * @return
     */
    public String getId() {
        if (id == null) {
            return "staticTuple";
        } else {
            return id;
        }
    }

    public ThreadRecord getThread() {
        return thread;
    }

    public boolean isStaticTrace() {
        return id == null;
    }

    public ClassRecord getClassRecord() {
        return cr;
    }

    public String getClassName() {
        String result = cr.getName();
        return result;
    }

    public String getClassLoaderId() {
        return cr.getClassLoaderId();
    }

    public long getBeginCreationTime() {
        return beginCreationTime;
    }

    public long getEndCreationTime() {
        return endCreationTime;
    }

    public long getDeletionTime() {
        return deletionTime;
    }

    public GrowableArray getTraceElements() {
        return traceElements;
    }

    public boolean isArray() {
        return cr.isArray();
    }

    /**
     * No more updates, allowing cached results from analysis.
     */
    public void setImmutable() {
        immutable = true;
    }

    /**
     * Only used for objects whose construction we learn about after the
     * constructor has executed, i.e., created by Class.newInstance().
     */
    public void setBeginCreationTime(long beginCreationTime) {
        if (immutable) {
            throw new IllegalAccessError();
        } else {
            this.beginCreationTime = beginCreationTime;
        }
    }

    public void setEndCreationTime(long endCreationTime) {
        if (immutable) {
            throw new IllegalAccessError();
        } else {
            this.endCreationTime = endCreationTime;
        }
    }

    public void setDeletionTime(long deletionTime) {
        if (immutable) {
            throw new IllegalAccessError();
        } else {
            this.deletionTime = deletionTime;
        }
    }

    public void addTraceElement(TraceElement te) {
        if (immutable) {
            throw new IllegalAccessError();
        } else {
            traceElements = traceElements.add(te);
        }
    }

    /**
     * Returns the time period from the end of the object construction to its
     * deletion, if known, else to lastTime.
     */
    public long getLifeTime(long lastTime) {
        return ((deletionTime == 0) ? lastTime : deletionTime)
                - endCreationTime;
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

        lastModifyTime = endCreationTime;
        int s = traceElements.size();
        if (s > 0) {
            for (int i = s - 1; i >= 0; i--) {
                TraceElement te = traceElements.get(i);
                if (te instanceof WriteTraceElement) {
                    lastModifyTime = te.accessTime;
                    break;
                }
            }
        }

        if (lastModifyTime <= endCreationTime) {
            return 0;
        } else {
            return lastModifyTime - endCreationTime;
        }
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    /**
     * Represents an event on a particular object instance.
     *
     */
    public static abstract class TraceElement {
        // pseudo field name to indicate entire array update
        public static final String ARRAYCOPY = "ARRAYCOPY";
        public static final String LITERAL = "LITERAL";

        ClassRecord classRecord;
        /**
         * If the access was to a field, the name of the field, if to an array the symbolic name of the index.
         */
        private FieldRecord field;
        /**
         * Thread on which the event occurred.
         */
        private ThreadRecord thread;
        /**
         * The time the event occurred.
         */
        protected long accessTime;

        protected TraceElement(ClassRecord classRecord, FieldRecord field, ThreadRecord thread, long accessTime) {
            this.classRecord = classRecord;
            this.field = field;
            this.thread = thread;
            this.accessTime = accessTime;
        }

        public FieldRecord getField() {
            return field;
        }

        /**
         * For forward reference fixup.
         * @param name
         */
        public void setField(ClassRecord classRecord, FieldRecord field) {
            this.classRecord = classRecord;
            this.field = field;
        }

        public ThreadRecord getThread() {
            return thread;
        }

        public long getAccessTime() {
            return accessTime;
        }

        public static int getSize() {
            return 32;
        }

        public ClassRecord getClassRecord() {
            return classRecord;
        }

        public abstract String name();
    }


    public static abstract class WriteTraceElement extends TraceElement {
        public WriteTraceElement(ClassRecord classRecord, FieldRecord field, ThreadRecord thread, long writeTime) {
            super(classRecord, field, thread, writeTime);
        }

        public long getWriteTime() {
            return accessTime;
        }

        public static int getSize() {
            return TraceElement.getSize();
        }

        @Override
        public String name() {
            return "write";
        }

    }

    public static class NullWriteTraceElement extends WriteTraceElement {
        public NullWriteTraceElement(ClassRecord classRecord, FieldRecord field, ThreadRecord thread, long writeTime) {
            super(classRecord, field, thread, writeTime);
        }

    }

    public static class ObjectWriteTraceElement extends WriteTraceElement {
        private ObjectRecord value;

        public ObjectWriteTraceElement(ClassRecord classRecord, FieldRecord field, ThreadRecord thread, long writeTime, ObjectRecord value) {
            super(classRecord, field, thread, writeTime);
            this.value = value;
        }

        public ObjectRecord getValue() {
            return value;
        }
    }

    public static class LongWriteTraceElement extends WriteTraceElement {
        private long value;

        public LongWriteTraceElement(ClassRecord classRecord, FieldRecord field, ThreadRecord thread, long writeTime, long value) {
            super(classRecord, field, thread, writeTime);
            this.value = value;
        }

        public long getValue() {
            return value;
        }
    }

    public static class FloatWriteTraceElement extends WriteTraceElement {
        private float value;

        public FloatWriteTraceElement(ClassRecord classRecord, FieldRecord field, ThreadRecord thread, long writeTime, float value) {
            super(classRecord, field, thread, writeTime);
            this.value = value;
        }

        public float getValue() {
            return value;
        }
    }

    public static class DoubleWriteTraceElement extends WriteTraceElement {
        private double value;

        public DoubleWriteTraceElement(ClassRecord classRecord, FieldRecord field, ThreadRecord thread, long writeTime, double value) {
            super(classRecord, field, thread, writeTime);
            this.value = value;
        }

        public double getValue() {
            return value;
        }
    }

    /**
     * Denotes a read of an object instance.
     *
     */
    public static class ReadTraceElement extends TraceElement {
        public ReadTraceElement(ClassRecord classRecord, FieldRecord field, ThreadRecord thread, long readTime) {
            super(classRecord, field, thread, readTime);
        }

        public long getReadTime() {
            return accessTime;
        }

        @Override
        public String name() {
            return "read";
        }
    }

    /**
     * Denotes an array copy.
     * The inherited state, i.e., {@link #field array index} refers to the destination array.
     *
     */
    public static class ArrayCopyTraceElement extends TraceElement {
        private ObjectRecord srcTd;
        private int destPos;
        private int srcPos;
        private int length;

        public ArrayCopyTraceElement(ThreadRecord thread, long writeTime, int destPos, ObjectRecord srcTd,
                int srcPos, int length) {

            super(null, null/*ARRAYCOPY*/, thread, writeTime); // TODO fix this
            this.srcTd = srcTd;
            this.srcPos = srcPos;
            this.destPos = destPos;
            this.length = length;
        }

        public ObjectRecord getSrcTd() {
            return srcTd;
        }

        public int getSrcPos() {
            return srcPos;
        }

        public int getDestPos() {
            return destPos;
        }

        public int getLength() {
            return length;
        }

        @Override
        public String name() {
            return "arraycopy";
        }
    }

    /**
     * Traversal (visitor) support.
     */
    public static abstract class Visitor {
        public abstract void visit(TraceRun traceRun, ObjectRecord td,
                TraceElement te, PrintStream ps, Object[] args);

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
        for (int i = 0; i < td.getTraceElements().size(); i++) {
            TraceElement te = td.getTraceElements().get(i);
            visitor.visit(traceRun, td, te, ps, args);
        }
        return visitor.getResult();
    }

}
