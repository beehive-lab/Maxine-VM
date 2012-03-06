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
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deps.DependenciesManager.DependenciesCounter;
import com.sun.max.vm.compiler.deps.DependenciesManager.UniqueConcreteMethodSearch;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.jvmti.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Encodes the {@link CiAssumptions assumptions} made when compiling a target method.
 * The assumptions, which initially are specified using {@link Actor} subtypes,
 * are encoded in an efficient, packed, format using {@link MemberID member ids}.
 * Once the assumptions are validated initially, they are associated with
 * the {@link TargetMethod} that resulted from the compilation.
 * <p>
 * An assumption always involves a {@link ClassActor class}, which is referred to
 * as the <i>context</i> class. All the {@link Dependencies dependencies} with
 * the same context class are kept together in {@link ContextDependents}.
 * <p>
 * Changes in the VM may necessitate validation that the {@link Dependencies dependencies}
 * are still valid. E.g., every time a new class is {@linkplain ClassRegistry#define(ClassActor) defined},
 * the classes that are ancestors of the new class must be checked
 * to see if any of their dependent assumptions are invalidated as a result
 * of adding the new class to the hierarchy.
 */
public final class Dependencies {

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
     * <p>
     * The unique ids for {@ClassActor} and {@link MethodActor} are used in the array to save space. The
     * {@link ClassActor context} id of the dependencies is always the first element of the array. The encoding is
     * extensible to new dependencies through a "flags" field, which specifies which dependencies are present. The
     * additional data for the dependencies follows the "flags" field, in the order specified by the bit number
     * associated with the dependency. If a dependency is not present, neither is its data (i.e., there is no
     * "zero-length" data). The concrete subtype dependency has no additional data.
     * <p>
     * Statistics from boot image generation showed that the vast majority of target methods have a single concrete
     * method dependency, and less than 10 % have some unique concrete type dependencies and typically a single one.
     * Furthermore, most single concrete method dependencies are on leaf methods, i.e., wherein the context is the
     * holder of the concrete method. The encoding of the dependencies is optimized for these cases.
     * <p>
     * The packed data structure is described below:
     *
     * <pre>
     *     {
     *         s2 type;            // identifier of the context class
     *         s2 flags            // bit mask specifying which dependencies are present
     *         concrete_methods cmDeps; // present iff (flags & CONCRETE_METHOD_MASK) != 0
     *         inlined_methods imDeps;  // present iff (flags & INLINED_METHOD_MASK) != 0
     *     }[] packed;
     *
     *     concrete_methods {
     *         s2 length;        // length of 'deps'
     *         s2 deps[length];  // array of local_method_dep and non_local_concrete_method_dep structs (defined below)
     *     }
     *
     *     inlined_methods {
     *         s2 length;        // length of 'deps'
     *         s2 deps[length];  // array of local_inlined_method_dep and non_local_inlined_method_dep structs (defined below)
     *                           // the context class is always the holder of an inlining method, and the ClassMethodActor
     *                           // of the TargetMethod always denotes the inlining method, so it does not need to be recorded
     *                           // in the dependency data. N.B. This means that the inlining method is lost
     *                           // until the TargetMethod is {@link DependenciesManager#registerValidatedTarget(Dependencies, TargetMethod) set}
     *     }
     *
     *     // identifies a concrete method in the same class as 'type'
     *     local_method_dep {
     *         s2 mindex;  // positive; the member index of the method in 'type'
     *     }
     *
     *     // identifies
     *     non_local_concrete_method_dep {
     *         s2 mindex;        // negative; (-mindex - 1) is the member index of the implementation/concrete method in 'implHolder'
     *         s2 implHolder;    // identifier of the class in which the implementation/concrete method is defined
     *         s2 methodHolder;  // identifier of the class in which the declared method is defined
     *     }
     *
     *     // identifies an inlined method in same class as inliner
     *     local_inlined_method_dep {
     *         s2 inlinee_mindex // positive; inlinee_mindex is the member index of the inlinee method in same class
     *     }
     *
     *     // identifies an inlined method in different class to inliner
     *     non_local_inlined_method_dep {
     *         s2 inlinee_mindex // negative; (-mindex - 1) is the member index of the inlinee method in inlineHolder
     *         s2 inlineHolder;  // identifier of the class in which the inlinee method is defined
     *     }
     * </pre>
     */
    volatile short[] packed;

    /**
     * The kinds of dependency supported.
     */
    private static enum DependencyKind {
        CONCRETE_SUBTYPE,
        CONCRETE_METHOD {
            @Override
            int skip(short[] packed, int index) {
                return skipArray(packed, index);
            }

        },
        INLINED_METHOD {
            @Override
            int skip(short[] packed, int index) {
                return skipArray(packed, index);
            }
        };

        /**
         * Skip over dependency specific data,
         * Default is no additional data.
         */
        int skip(short[] packed, int index) {
            return index;
        }

        /**
         * Skip the amount of data indicated by the length, which is at {@code packed[index]}.
         */
        int skipArray(short[] packed, int index) {
            return index + 1 + packed[index];
        }

        // Get these computed at boot image creation
        private static final DependencyKind[] VALUES = values();
        private static short[] MASKS = setMasks();
        private static final int CONCRETE_SUBTYPE_MASK = MASKS[CONCRETE_SUBTYPE.ordinal()];
        private static final int CONCRETE_METHOD_MASK = MASKS[CONCRETE_METHOD.ordinal()];
        private static final int INLINED_METHOD_MASK = MASKS[INLINED_METHOD.ordinal()];

        @HOSTED_ONLY
        static short[] setMasks() {
            short[] masks = new short[VALUES.length];
            for (DependencyKind depKind : DependencyKind.VALUES) {
                masks[depKind.ordinal()] = (short) (1 << depKind.ordinal());
            }
            return masks;
        }

    }

    @HOSTED_ONLY
    private Dependencies() {
        packed = INVALIDATED;
        id = -1;
    }

    private Dependencies(short[] packed) {
        id = idMap.allocate(this);
        this.packed = packed;
    }

    /**
     * Data structure used while encoding the dependencies for a class into a {@code short[]}.
     * {@code records[n]} holds the records for the n'th dependency type
     */
    static class ClassDeps {
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
            records = new Records[DependencyKind.values().length];
        }

        void add(DependencyKind kind, short s) {
            int kindIndex = kind.ordinal();
            if (records[kindIndex] == null) {
                records[kindIndex] = new Records();
            }
            records[kindIndex].add(s);
        }
    }

    /**
     * Validates a given set of assumptions and returns them encoded in a {@link Dependencies} object
     * if validation succeeds. If validation fails, {@link Dependencies#INVALID} is returned instead.
     */
    static Dependencies validate(CiAssumptions assumptions) {
        classHierarchyLock.readLock().lock();
        try {
            FatalError.check(ClassID.largestClassId() <= Short.MAX_VALUE, "Support for 1 << 16 number of classes not supported yet");
            HashMap<ClassActor, ClassDeps> packedDeps = new HashMap<ClassActor, ClassDeps>(10);
            UniqueConcreteMethodSearch ucms = null;
            for (Assumption a : assumptions) {
                ClassActor contextClassActor = (ClassActor) ((ContextAssumption) a).context;
                if (a instanceof ConcreteMethod) {
                    ConcreteMethod cm = (ConcreteMethod) a;
                    if (ucms == null) {
                        ucms = new UniqueConcreteMethodSearch();
                    }
                    if (ucms.doIt(contextClassActor, (MethodActor) cm.dependee) != cm.dependee) {
                        return Dependencies.INVALID;
                    }

                    MethodActor impl = (MethodActor) cm.dependee;
                    MethodActor method = (MethodActor) cm.method;
                    short mIndex = getMIndex(impl);
                    ClassDeps classDeps = get(packedDeps, contextClassActor);
                    classDeps.flags |= DependencyKind.CONCRETE_METHOD_MASK;
                    if (impl == method && impl.holder() == contextClassActor) {
                        classDeps.add(DependencyKind.CONCRETE_METHOD, mIndex);
                    } else {
                        classDeps.add(DependencyKind.CONCRETE_METHOD, (short) -(mIndex + 1));
                        classDeps.add(DependencyKind.CONCRETE_METHOD, (short) impl.holder().id);
                        classDeps.add(DependencyKind.CONCRETE_METHOD, (short) method.holder().id);
                    }
                } else if (a instanceof ConcreteSubtype) {
                    ConcreteSubtype cs = (ConcreteSubtype) a;
                    final ClassActor subtype = (ClassActor) cs.subtype;
                    if (contextClassActor.uniqueConcreteType != subtype.id) {
                        return Dependencies.INVALID;
                    }
                    ClassDeps classDeps = get(packedDeps, contextClassActor);
                    classDeps.flags |= DependencyKind.CONCRETE_SUBTYPE_MASK;
                } else if (a instanceof InlinedMethod) {
                    InlinedMethod inlineMethod = (InlinedMethod) a;
                    ClassMethodActor inlinee = (ClassMethodActor) inlineMethod.dependee;
                    if (JVMTIBreakpoints.hasBreakpoints(inlinee)) {
                        return Dependencies.INVALID;
                    }
                    ClassDeps classDeps = get(packedDeps, contextClassActor);
                    classDeps.flags |= DependencyKind.INLINED_METHOD_MASK;
                    short inlineeMIndex = getMIndex(inlinee);
                    if (inlinee.holder() == contextClassActor) {
                        // inlinee in same class, use shorter form
                        classDeps.add(DependencyKind.INLINED_METHOD, inlineeMIndex);
                    } else {
                        classDeps.add(DependencyKind.INLINED_METHOD, (short) -(inlineeMIndex + 1));
                        classDeps.add(DependencyKind.INLINED_METHOD, (short) inlinee.holder().id);
                    }
                } else {
                    assert false : "unhandled subtype of CiAssumptions";
                }

            }

            int size = 0;
            for (Map.Entry<ClassActor, ClassDeps> e : packedDeps.entrySet()) {
                ClassDeps classDeps = e.getValue();
                size += 2; // context type and flags
                for (DependencyKind depKind : DependencyKind.VALUES) {
                    ClassDeps.Records records = classDeps.records[depKind.ordinal()];
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
                for (DependencyKind depKind : DependencyKind.VALUES) {
                    ClassDeps.Records records = classDeps.records[depKind.ordinal()];
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

    private static short getMIndex(MethodActor methodActor) {
        int mindex = methodActor.memberIndex();
        FatalError.check(mindex <= Short.MAX_VALUE && mindex >= 0, "method index range not supported");
        return (short) mindex;

    }

    private static ClassDeps get(HashMap<ClassActor, ClassDeps> dependencies, ClassActor type) {
        ClassDeps buf = dependencies.get(type);
        if (buf == null) {
            buf = new ClassDeps(4);
            dependencies.put(type, buf);
        }
        return buf;
    }

    void setTargetMethod(TargetMethod targetMethod) {
        FatalError.check(classHierarchyLock.getReadHoldCount() > 0, "Must hold class hierarchy lock");
        this.targetMethod = targetMethod;
    }

    public TargetMethod targetMethod() {
        return targetMethod;
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
     * Client for {@linkplain Dependencies#iterate(DependencyClosure) iterating}
     * over each dependency in a {@link Dependencies} object.
     */
    public static class DependencyClosure {
        public DependencyClosure() {
            this(ClassID.NULL_CLASS_ID);
        }

        public DependencyClosure(int classID) {
            this.classID = classID;
        }

        /**
         * Only the dependencies for the context class whose identifier matches this field are
         * iterated. If this field is {@link ClassID#NULL_CLASS_ID}, then all dependencies
         * are iterated.
         */
        protected int classID;

        /**
         * Notifies this closure of a new class context during iteration.
         *
         * @param c the class context of subsequent dependencies or {@code null} if there are no more contexts
         * @param prev the previous class context or {@code null} if {@code c} is the first class context
         * @return {@code false} if this closure wants to terminate the iteration
         */
        public boolean nextContextClass(ClassActor c, ClassActor prev) {
            return true;
        }

        /**
         * Processes a unique concrete subtype dependency.
         *
         * @param targetMethod the method compiled with this dependency
         * @param context
         * @param subtype the subtype assumed to be the unique concrete subtype of {@code context}
         *
         * @return {@code true} to continue the iteration, {@code false} to terminate it
         */
        public boolean doConcreteSubtype(TargetMethod targetMethod, ClassActor context, ClassActor subtype) {
            return true;
        }

        /**
         * Processes a unique concrete method dependency.
         *
         * @param targetMethod the method compiled with this dependency
         * @param method a virtual or interface method
         * @param impl the method assumed to be the unique concrete implementation of {@code method}
         * @param context class contxt
         * @return {@code true} to continue the iteration, {@code false} to terminate it
         */
        public boolean doConcreteMethod(TargetMethod targetMethod, MethodActor method, MethodActor impl, ClassActor context) {
            return true;
        }

        /**
         * Process an inlined method dependency.
         * @param targetMethod the method compiled with this dependency
         * @param method the inlining method
         * @param inlinee the inlined method (inlinee)
         * @param context class context
         * @return {@code true} to continue the iteration, {@code false} to terminate it
         */
        public boolean doInlinedMethod(TargetMethod targetMethod, ClassMethodActor method, ClassMethodActor inlinee, ClassActor context) {
            return true;
        }

        public void doInvalidated() {
        }
    }

    /**
     * Iterates over all the dependencies.
     *
     * @param dc closure the dependencies are fed to
     */
    public void iterate(DependencyClosure dc) {
        if (packed == INVALIDATED) {
            dc.doInvalidated();
            return;
        }
        int i = 0;
        ClassActor prev = null;
        while (i < packed.length) {
            int contextClassID = packed[i++];
            int flags = packed[i++];
            final ClassActor contextClassActor = ClassID.toClassActor(contextClassID);
            if (dc.classID == ClassID.NULL_CLASS_ID || dc.classID == contextClassID) {
                if (!dc.nextContextClass(contextClassActor, prev)) {
                    return;
                }
                for (DependencyKind depKind : DependencyKind.VALUES) {
                    if ((flags & DependencyKind.MASKS[depKind.ordinal()]) != 0) {
                        switch (depKind) {
                            case CONCRETE_SUBTYPE: {
                                if (!dc.doConcreteSubtype(targetMethod, contextClassActor, ClassID.toClassActor(contextClassActor.uniqueConcreteType))) {
                                    return;
                                }
                                break;
                            }

                            case CONCRETE_METHOD:
                            case INLINED_METHOD: {
                                short length = packed[i++];
                                int end = i + length;
                                while (i < end) {
                                    int mindex = packed[i++];
                                    if (depKind == DependencyKind.CONCRETE_METHOD) {
                                        MethodActor impl = null;
                                        MethodActor method = null;
                                        if (mindex >= 0) {
                                            impl = MethodID.toMethodActor(MethodID.fromWord(MemberID.create(contextClassID, mindex)));
                                            assert impl != null;
                                            method = impl;
                                        } else {
                                            int implHolder = packed[i++];
                                            impl = MethodID.toMethodActor(MethodID.fromWord(MemberID.create(implHolder, -mindex - 1)));
                                            int methodHolderID = packed[i++];
                                            ClassActor methodHolder = ClassID.toClassActor(methodHolderID);
                                            method = methodHolder.findLocalMethodActor(impl.name, impl.descriptor());
                                            assert method != null : impl;
                                        }
                                        if (!dc.doConcreteMethod(targetMethod, method, impl, contextClassActor)) {
                                            return;
                                        }
                                    } else {
                                        // INLINED_METHOD
                                        int inlineeMIndex = mindex;
                                        ClassActor inlineeHolder;
                                        if (inlineeMIndex >= 0) {
                                            inlineeHolder = contextClassActor;
                                        } else {
                                            inlineeMIndex = -inlineeMIndex - 1;
                                            int inlineeHolderID = packed[i++];
                                            inlineeHolder = ClassID.toClassActor(inlineeHolderID);
                                        }
                                        ClassMethodActor inliningMethod = targetMethod.classMethodActor;
                                        ClassMethodActor inlineeMethod = (ClassMethodActor) MethodID.toMethodActor(MethodID.fromWord(MemberID.create(inlineeHolder.id, inlineeMIndex)));
                                        if (!dc.doInlinedMethod(targetMethod, inliningMethod, inlineeMethod, contextClassActor)) {
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // if we were just iterating one context and this was it we are done
                if (dc.classID == contextClassID) {
                    return;
                }
            } else {
                // skip over all records for this context
                for (DependencyKind depKind : DependencyKind.VALUES) {
                    if ((flags & DependencyKind.MASKS[depKind.ordinal()]) != 0) {
                        i = depKind.skip(packed, i);
                    }
                }
            }
            prev = contextClassActor;

        }
        dc.nextContextClass(null, prev);
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
            iterate(new DependencyClosure() {
                @Override
                public boolean doConcreteSubtype(TargetMethod method, ClassActor context, ClassActor subtype) {
                    sb.append(" UCT[").append(context);
                    if (context != subtype) {
                        sb.append(",").append(subtype);
                    }
                    sb.append(']');
                    return true;
                }
                @Override
                public boolean doConcreteMethod(TargetMethod targetMethod, MethodActor method, MethodActor impl, ClassActor context) {
                    sb.append(" UCM[").append(method);
                    if (method != impl && impl.holder() != context) {
                        sb.append(",").append(context);
                        sb.append(",").append(impl);
                    }
                    sb.append(']');
                    return true;
                }
                @Override
                public boolean doInlinedMethod(TargetMethod targetMethod, ClassMethodActor method, ClassMethodActor inlinee, ClassActor context) {
                    sb.append(" INL[").append(inlinee).append(']');
                    return true;
                }
            });
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

    // Stats support

    @HOSTED_ONLY
    void countAssumptionsPerType(int classID, final HashMap<DependenciesCounter, DependenciesCounter> counters) {
        iterate(new DependencyClosure(classID) {
            @Override
            public boolean doConcreteSubtype(TargetMethod method, ClassActor context, ClassActor subtype) {
                DependenciesCounter.incCounter(0, counters);
                return true;
            }
            @Override
            public boolean doConcreteMethod(TargetMethod targetMethod, MethodActor method, MethodActor impl, ClassActor context) {
                DependenciesCounter.incCounter(id, counters);
                return true;
            }
        });
    }
}
