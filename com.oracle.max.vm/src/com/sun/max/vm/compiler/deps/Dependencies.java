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
package com.sun.max.vm.compiler.deps;

import static com.sun.max.vm.compiler.deps.DependenciesManager.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAssumptions.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.Actor;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deps.DependencyProcessor.DependencyProcessorVisitor;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.jni.MemberID;
import com.sun.max.vm.type.*;

/**
 * Encodes the {@link CiAssumptions.Assumption assumptions} made when compiling a target method.
 * The assumptions, which initially are specified using {@link Actor} subtypes,
 * are encoded in an efficient, packed, format using {@link MemberID member ids}.
 * Once the assumptions are validated initially, they are associated with
 * the {@link TargetMethod} that resulted from the compilation.
 * <p>
 * An assumption always involves a {@link ClassActor class}, which is referred to
 * as the <i>context</i> class. All the {@link Dependencies dependencies} with
 * the same context class are kept together in {@link ContextDependents}.
 * <p>
 * Changes in the VM may necessitate validation of the {@link Dependencies dependencies}.
 * E.g., every time a new class is {@linkplain ClassRegistry#define(ClassActor) defined},
 * the classes that are ancestors of the new class must be checked
 * to see if any of their dependent assumptions are invalidated as a result
 * of adding the new class to the hierarchy.
 */
public final class Dependencies {

    /**
     * Client for {@linkplain Dependencies#visit(DependencyVisitor) iterating}
     * over the basic structure of a {@link Dependencies} object.
     * <p>
     * Note that there are no method definitions related to {@linkplain DependencyProcessor}
     * here as their parameters are processor specific. A visitor that wants to
     * visit a specific dependency extends this class and implements the appropriate
     * {@linkplain DependencyProcessorVisitor}.
     * <p>
     * Two constructors are provided, one that visits all dependencies regardless of context class,
     * and one that only visits dependencies of a specific context class.
     *
     * An invalidated {@linkplain Dependencies} causes {@link #doInvalidated()} to be invoked.
     * Otherwise the data associated with each {@linkplain DependencyProcessor} is visited, possibly filtered by context class.
     * It is the responsibility of the {@linkplain DependencyProcessor} to process this data, invoking
     * the {@linkplain DependencyProcessorVisitor} if the visitor subclass implements the related subclass
     * of {@linkplain DependencyProcessorVisitor}.
     */
    public static abstract class DependencyVisitor {
        public DependencyVisitor() {
            this(ClassID.NULL_CLASS_ID);
        }

        public DependencyVisitor(int classID) {
            this.classID = classID;
        }

        /**
         * Only the dependencies for the context class whose identifier matches this field are
         * iterated. If this field is {@link ClassID#NULL_CLASS_ID}, then all dependencies
         * are iterated.
         */
        protected int classID;

        /**
         * Notifies this visitor of a new class context during iteration.
         *
         * @param c the class context of subsequent dependencies or {@code null} if there are no more contexts
         * @param prev the previous class context or {@code null} if {@code c} is the first class context
         * @return {@code false} if this visitor wants to terminate the iteration
         */
        protected boolean nextContextClass(ClassActor c, ClassActor prev) {
            return true;
        }

        /**
         * Invoked when an invalidated {@link Dependencies} instance is encountered.
         */
        protected void doInvalidated() {
        }

        /**
         * Generic visit to a single dependency within the data controlled by a {@linkplain DependencyProcessor}.
         * The default implementation just calls back to the {@linkplain DependencyProcessor} to invoke
         * any {@linkplain DependencyProcessorVisitor type-friendly callback} that this visitor implements,
         * and, <b>important</b> to step {@code index} to the next dependency.
         * @param dependencies the {@linkplain Dependencies} instance
         * @param context the context {@linkplain ClassActor} this dependency belongs to
         * @param dependencyProcessor the processor that manages this dependency
         * @param index of the dependency data in {@code dependencies.packed}.
         * @return the index of the next dependency or {@code -1} to terminate the visit.
         */
        protected int visit(Dependencies dependencies, ClassActor context, DependencyProcessor dependencyProcessor, int index) {
            return dependencyProcessor.visit(dependencyProcessor.match(this), context, dependencies, index);
        }
    }

    /**
     * Data structure used while encoding the dependencies for a class into a {@code short[]}.
     * {@code records[n]} holds the records for the n'th dependency type.
     *
     * The unique ids for {@link ClassActor} and {@link MethodActor} are used in the array to save space. The
     * {@linkplain ClassActor context} id of the dependencies is always the first element of the array. The encoding is
     * extensible to new dependencies through a "flags" field, which specifies which dependencies are present. The
     * additional data for the dependencies follows the "flags" field, in the order specified by the bit number
     * of the associated {@link DependencyProcessor dependency processor}. If a dependency is present,
     * and it has associated data, the length of its data follows the "flags" field, otherwise the length field is absent.
     * I.e. there is <b>not</b> a length field of zero for processors that do not encode extra data.
     * This knowledge is encoded in {@linkplain DependencyProcessor#hasData }.
     * <p>
     * The common part of the packed structure is as follows:
     * <pre>
     *         short type;            // identifier of the context class
     *         short flags            // bit mask specifying which dependencies are present
     *         short length           // length of processor-specific data (absent if none encoded)
     * </pre>
     */
    public static class ClassDeps {
        class Records {
            short count;
            short[] buf;

            void add(short s) {
                if (buf == null) {
                    buf = new short[initialCapacity];
                }
                if (count == buf.length) {
                    buf = Arrays.copyOf(buf, count * 2);
                }
                buf[count++] = s;
            }
        }
        short flags;
        Records[] records;
        int initialCapacity;

        public ClassDeps(int initialCapacity) {
            this.initialCapacity = initialCapacity;
            records = new Records[dependencyProcessorsArray.length];
        }

        private Records get(DependencyProcessor dp) {
            int id = dp.id;
            if (records[id] == null) {
                records[id] = new Records();
            }
            return records[id];
        }

        /**
         * Add a piece of dependency data for the {@linkplain DependencyProcessor}.
         * @param dp
         * @param val
         */
        public void add(DependencyProcessor dp, short val) {
            get(dp).add(val);
        }

        public void add(DependencyProcessor dp, int val) {
            Records r = get(dp);
            r.add((short) (val >> 16));
            r.add((short) (val & 0xffff));
        }

        public void add(DependencyProcessor dp, long val) {
            add(dp, (int) (val >> 32));
            add(dp, (int) (val & 0xffffffff));
        }

    }

    public static final int CLASSID_INDEX = 0;
    public static final int FLAGS_INDEX = 1;
    public static final int DATA_OFFSET = 2;

    /**
     * Sentinel instance for compilations with dependencies that failed their validation phase.
     */
    public static final Dependencies INVALID = new Dependencies();

    /**
     * Marker used to denote invalidated dependencies.
     */
    static final short[] INVALIDATED = {};

    /**
     * The target method compiled with these dependencies.
     */
    TargetMethod targetMethod;

    /**
     * Unique identifier for these dependencies. Allocated from {@link #idMap}.
     */
    public final int id;

    /**
     * Set of dependencies, packed into a short array.
     */
    volatile short[] packed;

    @HOSTED_ONLY
    private Dependencies() {
        packed = INVALIDATED;
        id = -1;
    }

    private Dependencies(short[] packed) {
        id = idMap.allocate(this);
        this.packed = packed;
    }

    void setTargetMethod(TargetMethod targetMethod) {
        FatalError.check(classHierarchyLock.getReadHoldCount() > 0, "Must hold class hierarchy lock");
        this.targetMethod = targetMethod;
    }

    public TargetMethod targetMethod() {
        return targetMethod;
    }

    private static ClassDeps get(HashMap<ClassActor, ClassDeps> dependencies, ClassActor type) {
        ClassDeps buf = dependencies.get(type);
        if (buf == null) {
            buf = new ClassDeps(4);
            dependencies.put(type, buf);
        }
        return buf;
    }

    public int getInt(int index) {
        return (packed[index] << 16) | packed[index + 1];
    }

    public long getLong(int index) {
        return (getInt(index) << 32) | getInt(index + 2);
    }

    static short getMIndex(MethodActor methodActor) {
        int mindex = methodActor.memberIndex();
        FatalError.check(mindex <= Short.MAX_VALUE && mindex >= 0, "method index range not supported");
        return (short) mindex;
    }

    /**
     * Validates a given set of assumptions and returns them encoded in a {@link Dependencies} object
     * if validation succeeds. If validation fails, {@link Dependencies#INVALID} is returned instead.
     * If {@code assumptions == null}, then {@code null} is returned.
     */
    public static Dependencies validateDependencies(CiAssumptions assumptions) {
        if (assumptions == null) {
            return null;
        }
        if (MaxineVM.isHosted()) {
            DependenciesManager.checkDependencyProcessorLoaded(assumptions);
        }
        classHierarchyLock.readLock().lock();
        try {
            FatalError.check(ClassID.largestClassId() <= Short.MAX_VALUE, "Support for 1 << 16 number of classes not supported yet");
            HashMap<ClassActor, ClassDeps> packedDeps = new HashMap<ClassActor, ClassDeps>(10);
            for (Assumption a : assumptions) {
                ClassActor contextClassActor = (ClassActor) ((ContextAssumption) a).context;
                ClassDeps classDeps = get(packedDeps, contextClassActor);
                DependencyProcessor dependencyProcessor = DependenciesManager.dependencyProcessors.get(a.getClass());
                if (dependencyProcessor != null) {
                    classDeps.flags |= dependencyProcessor.bitMask;
                    if (!dependencyProcessor.validate(a, classDeps)) {
                        return Dependencies.INVALID;
                    }
                } else {
                    assert false : "unhandled subtype of CiAssumptions: " + a.getClass().getName();
                }

            }

            // Calculate the size of the array needed for all the dependencies
            int size = 0;
            for (Map.Entry<ClassActor, ClassDeps> e : packedDeps.entrySet()) {
                ClassDeps classDeps = e.getValue();
                size += 2; // context type and flags

                for (int d = 0; d < dependencyProcessorsArray.length; d++) {
                    DependencyProcessor dp = dependencyProcessorsArray[d];
                    ClassDeps.Records records = classDeps.records[dp.id];
                    if (records != null && records.count > 0) {
                        size += 1 + records.count;
                    }
                }
            }

            short[] packed = new short[size];
            int i = 0;
            for (Map.Entry<ClassActor, ClassDeps> e : packedDeps.entrySet()) {
                ClassActor classActor = e.getKey();
                ClassDeps classDeps = e.getValue();
                packed[i++] = (short) classActor.id;
                packed[i++] = classDeps.flags;
                for (int d = 0; d < dependencyProcessorsArray.length; d++) {
                    DependencyProcessor dp = dependencyProcessorsArray[d];
                    ClassDeps.Records records = classDeps.records[dp.id];
                    if (records != null && records.count > 0) {
                        assert i < packed.length;
                        packed[i++] = records.count;
                        assert i + records.count <= packed.length;
                        System.arraycopy(records.buf, 0, packed, i, records.count);
                        i += records.count;
                    }
                }
            }
            assert i == packed.length;

            Dependencies deps = new Dependencies(packed);
            contextDependents.addDependencies(deps, packedDeps.keySet());
            return deps;
        } finally {
            classHierarchyLock.readLock().unlock();
        }
    }

    /**
     * Register the target method produced with a set of validated dependencies.
     *
     * @param deps a set of validated dependencies
     * @param targetMethod the target method to associate with the dependencies
     */
    public static void registerValidatedTarget(final Dependencies deps, final TargetMethod targetMethod) {
        classHierarchyLock.readLock().lock();
        try {
            deps.setTargetMethod(targetMethod);
        } finally {
            classHierarchyLock.readLock().unlock();
        }
        if (dependenciesLogger.enabled()) {
            deps.logRegister();
        }
    }

    /**
     * Invalidates this set of dependencies.
     *
     * @return {@code true} if this set of dependencies was not already invalidated
     */
    boolean invalidate() {
        // Called only when modifying the class hierarchy.
        FatalError.check(classHierarchyLock.isWriteLocked(), "Must hold class hierarchy lock in write mode");

        // The above lock makes an atomic CAS unnecessary for 'packed'
        if (packed == INVALIDATED) {
            return false;
        }

        // Remove all other mappings from context types not involved in the current class hierarchy change
        contextDependents.removeDependencies(this);

        idMap.free(this);

        // Prevent multiple invalidations
        packed = INVALIDATED;
        return true;
    }

    /**
     * Visits all the dependencies in the packed form.
     *
     * @param visitor visitor the dependencies are fed to
     */
    public void visit(DependencyVisitor visitor) {
        if (packed == INVALIDATED) {
            visitor.doInvalidated();
            return;
        }
        int i = 0;
        ClassActor prev = null;
        while (i < packed.length) {
            int contextClassID = packed[i++];
            int flags = packed[i++];
            final ClassActor contextClassActor = ClassID.toClassActor(contextClassID);
            if (visitor.classID == ClassID.NULL_CLASS_ID || visitor.classID == contextClassID) {
                if (!visitor.nextContextClass(contextClassActor, prev)) {
                    return;
                }
                // scan every set dependencies' data
                for (int b = 0; b < DependenciesManager.dependencyProcessorsArray.length; b++) {
                    int mask = 1 << b;
                    if ((flags & mask) != 0) {
                        DependencyProcessor dependencyProcessor = DependenciesManager.dependencyProcessorsArray[b];
                        short length = dependencyProcessor.hasData ? packed[i++] : 0;
                        int end = i + length;
                        do {
                            i = visitor.visit(this, contextClassActor, dependencyProcessor, i);
                            if (i < 0) {
                                return;
                            }
                        } while (i < end);
                    }
                }

                // if we were just iterating one context and this was it we are done
                if (visitor.classID == contextClassID) {
                    return;
                }
            } else {
                // skip over all records for this context
                for (int b = 0; b < DependenciesManager.dependencyProcessorsArray.length; b++) {
                    int mask = 1 << b;
                    if ((flags & mask) != 0) {
                        DependencyProcessor dependencyProcessor = DependenciesManager.dependencyProcessorsArray[b];
                        if (dependencyProcessor.hasData) {
                            i += 1 + packed[i];
                        }
                    }
                }
            }
            prev = contextClassActor;

        }
        visitor.nextContextClass(null, prev);
        assert i == packed.length;
    }

    void logAdd(ClassActor type) {
        dependenciesLogger.logAdd(targetMethod, id, type);
    }

    void logRemove(ClassActor type) {
        dependenciesLogger.logRemove(targetMethod, id, type);
    }

    void logRegister() {
        dependenciesLogger.logRegister(targetMethod, id);
    }

    void logInvalidated() {
        dependenciesLogger.logInvalidated(targetMethod, id);
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean verbose) {
        String value;
        if (targetMethod == null) {
            value = String.valueOf(id);
        } else {
            value = id + "#" + targetMethod;
        }
        if (!verbose) {
            return value;
        } else {
            final StringBuilder sb = new StringBuilder(value + Arrays.toString(packed));
            visit(new AllDependencyVisitors.ToStringDependencyVisitor(sb));
            return sb.toString();
        }
    }

    // TODO (ld): factor out with similar code in Class ID ?
    private static final int MINIMAL_DEPENDENT_TARGET_METHOD = 5000;

    /**
     * Map used to allocate and reclaim unique identifiers for {@link Dependencies} objects.
     */
    private static class IDMap extends LinearIDMap<Dependencies> {
        final BitSet usedIDs;
        public IDMap(int initialCapacity) {
            super(initialCapacity);
            usedIDs = new BitSet();
        }

        synchronized int allocate(Dependencies deps) {
            int id = usedIDs.nextClearBit(0);
            usedIDs.set(id);
            set(id, deps);
            return id;
        }

        synchronized void free(Dependencies deps) {
            assert get(deps.id) == deps : deps + " != " + get(deps.id);
            set(deps.id, null);
            usedIDs.clear(deps.id);
        }
    }

    /**
     * Map used to allocate and reclaim unique identifiers for {@link Dependencies} objects.
     */
    public static final IDMap idMap = new IDMap(MINIMAL_DEPENDENT_TARGET_METHOD);

    static Dependencies fromId(int depsID) {
        Dependencies deps = idMap.get(depsID);
        assert deps != null : "invalid dependencies id: " + depsID;
        return deps;
    }

    @HOSTED_ONLY
    DependencyProcessor[] getDependencyProcessors(int flags) {
        int count = 0;
        for (int b = 0; b < MAX_DEPENDENCY_PROCESSORS; b++) {
            int mask = 1 << b;
            if ((flags & mask) != 0) {
                count++;
            }
        }
        DependencyProcessor[] result = new DependencyProcessor[count];
        count = 0;
        for (int b = 0; b < MAX_DEPENDENCY_PROCESSORS; b++) {
            int mask = 1 << b;
            if ((flags & mask) != 0) {
                DependencyProcessor dependencyProcessor = DependenciesManager.dependencyProcessorsArray[b];
                result[count++] = dependencyProcessor;
            }
        }
        return result;
    }

}
