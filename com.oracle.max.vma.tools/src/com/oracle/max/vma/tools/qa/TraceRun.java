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

import java.util.*;
import java.io.*;

import com.oracle.max.vma.tools.qa.TransientVMAdviceHandlerTypes.*;

/**
 * This is essentially the raw data read by {@link ProcessLog} organized
 * differently. The {@link #objects} map contains all objects present in the trace,
 * including those whose construction was "unseen". The value in the map is the
 * {@link ObjectRecord} for that object.
 *
 * To facilitate querying by class or classloader an additional data structure
 * is maintained, rooted in a {@link Map} keyed by the ids of classloader objects. The
 * associated value is a {@link SortedMap} of class names to {@link ClassRecords}.
 *
 * In principle, there can be multiple instances of this class corresponding to different traces.
 * In practice, that feature was used mostly in the historic version that worked with the
 * output from multiple "isolates" in the (MVM) multi-tasking virtual machine.
 *
 */

public class TraceRun {
    /**
     * Uniquely identifies the trace.
     */
    public final String name;

    public final ArrayList<AdviceRecord> adviceRecordList;

    /**
     * The objects map. The key is the id, qualified by the allocation, aka {@link ObjectRecord#id}.
     */
    public final Map<String, ObjectRecord> objects;

    public final Map<String, ThreadRecord> threads;

    /**
     * The class loaders map. The key is the id of the class loader instance
     *  and the value is a map from class names
     * to {@link ClassRecord class records}, denoting all the classes loaded
     * by that class loader.
     */
    public final Map<String, SortedMap<String, ClassRecord>> classLoaders;

    public final Map<String, ObjectRecord> missingConstructors;

    public final ArrayList<AllocationEpoch> allocationEpochs;

    public final long objectCount;

    public final long arrayCount;

    public final int missingConstructorCount;

    public final long lastTime;
    public final long startTime;

    /**
     * The following fields are caches for performance reasons. A value of -1
     * indicates an unset cache value.
     */

    private static long immutableObjectCount = -1;

    private static long immutableArrayCount = -1;

    private static class CheckCounts {
        long checkArrayCount;
        long checkObjCount;
    }

    public TraceRun(String name, ArrayList<AdviceRecord> adviceRecordList, Map<String, ObjectRecord> objects, Map<String, ThreadRecord> threads, Map<String, SortedMap<String, ClassRecord>> classLoaders,
            Map<String, ObjectRecord> missingConstructors, long objectCount, long arrayCount,
            int missingConstructorCount, ArrayList<AllocationEpoch> allocationEpochs, long startTime, long lastTime) {
        this.name = name;
        this.adviceRecordList = adviceRecordList;
        this.objects = objects;
        this.threads = threads;
        this.classLoaders = classLoaders;
        this.missingConstructors = missingConstructors;
        this.objectCount = objectCount;
        this.arrayCount = arrayCount;
        this.missingConstructorCount = missingConstructorCount;
        this.allocationEpochs = allocationEpochs;
        this.startTime = startTime;
        this.lastTime = lastTime;

        Visitor visitor = new Visitor() {
            CheckCounts checkCounts = new CheckCounts();
            @Override
            public void visit(TraceRun traceRun, ObjectRecord td,
                    PrintStream ps, Object[] args) {
                if (td.isArray()) {
                    checkCounts.checkArrayCount++;
                } else {
                    checkCounts.checkObjCount++;
                }
                td.setImmutable();
            }

            @Override
            public Object getResult() {
                return checkCounts;
            }
        };

        visit(visitor, null, null);
        CheckCounts checkCounts = (CheckCounts) visitor.getResult();
        assert checkCounts.checkArrayCount == arrayCount;
        assert checkCounts.checkObjCount == objectCount;
    }

    public long relTime(long time) {
        return time - startTime;
    }

    /**
     * This is a convenience method for backwards compatibility with queries
     * that assume that it is possible to iterate over all the loaded classes
     * (in all classloaders).
     */
    public Iterator<ClassRecord> getClassesIterator() {
        return new ClassIterator(classLoaders.values().iterator());
    }

    public long getImmutableObjectCount() {
        if (immutableObjectCount < 0) {
            Visitor visitor = new Visitor() {
                private long count = 0;

                @Override
                public void visit(TraceRun traceRun, ObjectRecord td,
                        PrintStream ps, Object[] args) {
                    long mlt = td.getModifyLifeTime();
                    if (mlt == 0) {
                        if (!(td.getClassRecord().isArray())) {
                            count++;
                        }
                    }
                }

                @Override
                public Object getResult() {
                    return new Long(count);
                }
            };
            immutableObjectCount = ((Long) visit(visitor, null, null)).longValue();
        }
        return immutableObjectCount;
    }

    public long getImmutableArrayCount() {
        if (immutableArrayCount < 0) {
            Visitor visitor = new Visitor() {
                private long count = 0;

                @Override
                public void visit(TraceRun traceRun, ObjectRecord td,
                        PrintStream ps, Object[] args) {
                    long mlt = td.getModifyLifeTime();
                    if (mlt == 0) {
                        if (td.getClassRecord().isArray()) {
                            count++;
                        }
                    }
                }

                @Override
                public Object getResult() {
                    return new Long(count);
                }
            };
            immutableArrayCount = ((Long) visit(visitor, null, null)).longValue();
        }
        return immutableArrayCount;
    }

    public ClassRecord getClassRecord(String classLoaderId, String className) {
        SortedMap<String, ClassRecord> classes = classLoaders.get(classLoaderId);
        if (classes == null) {
            return null;
        } else {
            ClassRecord cr = classes.get(className);
            return cr;
        }
    }

    /*
     * Traversal (visitor) support for "objects" map
     */
    public static abstract class Visitor {
        public abstract void visit(TraceRun traceRun, ObjectRecord td,
                PrintStream ps, Object[] args);

        public Object getResult() {
            return null;
        }
    }

    public Object visit(Visitor visitor, PrintStream ps, Object[] args) {
        Iterator<ObjectRecord> iter = objects.values().iterator();
        while (iter.hasNext()) {
            ObjectRecord td = iter.next();
            visitor.visit(this, td, ps, args);
        }
        return visitor.getResult();
    }

    static class ClassIterator implements Iterator<ClassRecord> {
        private Iterator<SortedMap<String, ClassRecord>> clIter; // underlying iterator on classLoaders map

        private Iterator<ClassRecord> classesIter; // current iterator on current classes map

        ClassIterator(Iterator<SortedMap<String, ClassRecord>> clIter) {
            this.clIter = clIter;
            nextClassesIter();
        }

        public boolean hasNext() {
            if (classesIter == null) {
                return false;
            } else {
                if (classesIter.hasNext()) {
                    return true;
                } else {
                    nextClassesIter();
                    return (classesIter != null) && classesIter.hasNext();
                }
            }
        }

        public ClassRecord next() {
            if (classesIter.hasNext()) {
                return classesIter.next();
            } else {
                // move on to next classloader if any
                nextClassesIter();
                if ((classesIter != null) && classesIter.hasNext()) {
                    return classesIter.next();
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void nextClassesIter() {
            if (clIter.hasNext()) {
                classesIter = clIter.next().values().iterator();
            } else {
                classesIter = null;
            }
        }
    }
}
