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

import java.util.*;
import java.io.*;

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
 * @author Mick Jordan
 *
 */

public class TraceRun {
    /**
     * Uniquely identifies the trace.
     */
    private String name;

    private Map<String, ObjectRecord> objects;

    private Map<String, SortedMap<String, ClassRecord>> classLoaders;

    private Map<String, ObjectRecord> missingConstructors;

    private ArrayList<GCEpoch> garbageCollections;

    private long objectCount = 0;

    private long arrayCount = 0;

    private int missingConstructorCount;

    private long lastTime;

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

    public TraceRun(String name, Map<String, ObjectRecord>  objects, Map<String, SortedMap<String, ClassRecord>> classLoaders,
            Map<String, ObjectRecord> missingConstructors, long objectCount, long arrayCount,
            int missingConstructorCount, ArrayList<GCEpoch> garbageCollections, long lastTime) {
        this.name = name;
        this.objects = objects;
        this.classLoaders = classLoaders;
        this.missingConstructors = missingConstructors;
        this.objectCount = objectCount;
        this.arrayCount = arrayCount;
        this.missingConstructorCount = missingConstructorCount;
        this.garbageCollections = garbageCollections;
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

        Iterator<ClassRecord> iter = getClassesIterator();
        while (iter.hasNext()) {
            ClassRecord cr = iter.next();
            ObjectRecord std = cr.getObjects().get(0);
            std.setImmutable();
        }
    }

    public String getName() {
        return name;
    }

    public Map<String, ObjectRecord> getObjects() {
        return objects;
    }

    /**
     * Gets the class loaders map. The key is the id of the class loader instance
     * (zero for the bootstrap class loader) and the value is a map from class names
     * to {@link ClassRecord class records}, denoting all the classes loaded
     * by that class loader.
     * @return
     */
    public Map<String, SortedMap<String, ClassRecord>> getClassLoaders() {
        return classLoaders;
    }

    public long getObjectCount() {
        return objectCount;
    }

    public long getArrayCount() {
        return arrayCount;
    }

    public Map<String, ObjectRecord> getMissingConstructors() {
        return missingConstructors;
    }

    public int getMissingConstructorCount() {
        return missingConstructorCount;
    }

    public long getLastTime() {
        return lastTime;
    }

    public ArrayList<GCEpoch> getGarbageCollections()  {
        return garbageCollections;
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
        Iterator<ObjectRecord> iter = getObjects().values().iterator();
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
