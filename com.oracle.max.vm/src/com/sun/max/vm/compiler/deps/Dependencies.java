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
package com.sun.max.vm.compiler.deps;

import static com.sun.max.vm.compiler.deps.DependenciesManager.*;

import java.util.*;

import com.sun.cri.ci.CiAssumptions.Assumption;
import com.sun.cri.ci.CiAssumptions.ConcreteMethod;
import com.sun.cri.ci.CiAssumptions.ConcreteSubtype;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deps.DependenciesManager.DependenciesCounter;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Assumptions on one or more classes made when compiling a target method.
 * Each class in a set of assumptions is a <i>context</i> class. Every time
 * a new class is {@linkplain ClassRegistry#define(ClassActor) defined},
 * the context classes that are ancestors of the new class must be checked
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
     * Statistics from boot image generation showed that the vast majority of target methods
     * have a single concrete method dependency, and less than 10 % have some unique concrete type
     * dependencies and typically a single one. Furthermore, most single concrete method dependencies
     * are on leaf methods, i.e., wherein the context is the holder of the concrete method.
     * The encoding of the dependencies is optimized for these cases.
     * <p>
     * The packed data structure is described below:
     * <pre>
     *     {
     *         s2 type;            // identifier of a context class
     *         s2 length;          // absolute value gives the length of 'deps';
     *                             // if negative, there is a unique concrete type dependency based on 'type'
     *         s2 deps[|length|];  // array of compact_method_dep and non_compact_method_dep structs (defined below)
     *     }[] packed;
     *
     *     compact_method_dep {
     *         s2 mindex;  // positive; the member index of the method in 'type'
     *     }
     *
     *     non_compact_method_dep {
     *         s2 mindex;        // negative; (-mindex - 1) is the member index of the implementation/concrete method in 'holder'
     *         s2 implHolder;    // identifier of the class in which the implementation/concrete method is defined
     *         s2 methodHolder;  // identifier of the class in which the declared method is defined
     *     }
     * </pre>
     */
    volatile short[] packed;

    @HOSTED_ONLY
    private Dependencies() {
        packed = INVALIDATED;
        id = -1;
    }

    /**
     * Creates a dependencies object encapsulating all the dependencies for a single target method.
     *
     * @param dependencies a set of validated dependencies
     * @param compactCMs the number of compact {@linkplain ConcreteMethod concrete method} dependencies. A compact
     *            concrete method dependency is one where {@code cm.impl == cm.method && cm.impl.holder() == cm.context}
     * @param nonCompactCMs the number of non-compact {@linkplain ConcreteMethod concrete method} dependencies
     */
    Dependencies(HashMap<ClassActor, ArrayList<Assumption>> dependencies, int compactCMs, int nonCompactCMs) {
        FatalError.check(classHierarchyLock.getReadHoldCount() > 0, "Must hold class hierarchy lock");
        id = idMap.allocate(this);
        this.packed = pack(dependencies, compactCMs, nonCompactCMs);
        contextDependents.addDependencies(this, dependencies.keySet());
    }

    private static short[] pack(HashMap<ClassActor, ArrayList<Assumption>> deps, int localUCMs, int nonLocalUCMs) {
        FatalError.check(ClassID.largestClassId() <= Short.MAX_VALUE, "Support for 1 << 16 number of classes not supported yet");

        // Pre-compute size of the dependencies arrays:
        final int numClasses = deps.size();
        int size = (numClasses * 2) + localUCMs + (nonLocalUCMs * 3);
        short[] packed = new short[size];
        int i = 0;
        for (Map.Entry<ClassActor, ArrayList<Assumption>> e : deps.entrySet()) {
            ClassActor context = e.getKey();
            ArrayList<Assumption> depsForContext = e.getValue();
            packed[i++] = (short) context.id;
            int lengthIndex = i++;
            boolean uct = false;
            for (Assumption a : depsForContext) {
                if (a instanceof ConcreteMethod) {
                    ConcreteMethod cm = (ConcreteMethod) a;
                    MethodActor impl = (MethodActor) cm.impl;
                    MethodActor method = (MethodActor) cm.method;
                    int mindex = impl.memberIndex();
                    FatalError.check(mindex <= Short.MAX_VALUE && mindex >= 0, "method index range not supported");
                    if (impl == method && impl.holder() == context) {
                        packed[i++] = (short) mindex;
                    } else {
                        packed[i++] = (short) -(mindex + 1);
                        packed[i++] = (short) impl.holder().id;
                        packed[i++] = (short) method.holder().id;
                    }
                } else {
                    assert a instanceof ConcreteSubtype : "unexpected assumption: " + a;
                    assert !uct : "can be at most one UCT per context type";
                    uct = true;
                }
            }

            int length = i - lengthIndex - 1;
            FatalError.check(length <= Short.MAX_VALUE && length >= 0, "length not supported");
            if (uct) {
                length = -length;
            }
            packed[lengthIndex] = (short) length;
        }
        assert i == packed.length;
        return packed;
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
         * @param c the class context of all subsequent dependencies or {@code null} if there are no more dependencies
         * @param prev the previous class context or {@code null} if {@code c} is the first class context
         * @return {@code false} if this closure wants to stop traversing the dependencies
         */
        public boolean nextClass(ClassActor c, ClassActor prev) {
            return true;
        }

        /**
         * Processes a unique concrete subtype dependency.
         *
         * @param targetMethod the method compiled with this dependency
         * @param context
         * @param subtype the subtype assumed to be the unique concrete subtype of {@code context}
         *
         * @return {@code true} to continue the iteration, {@code false} to stop it
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
         * @param context TODO
         * @return {@code true} to continue the iteration, {@code false} to stop it
         */
        public boolean doConcreteMethod(TargetMethod targetMethod, MethodActor method, MethodActor impl, ClassActor context) {
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
            int holder = packed[i++];
            int length = packed[i++];
            final ClassActor classActor = ClassID.toClassActor(holder);
            if (dc.classID == ClassID.NULL_CLASS_ID || dc.classID == holder) {
                boolean stop = !dc.nextClass(classActor, prev);
                if (length < 0) {
                    if (!stop) {
                        stop = !dc.doConcreteSubtype(targetMethod, classActor, ClassID.toClassActor(classActor.uniqueConcreteType));
                    }
                    length = -length;
                }
                while (!stop && length != 0) {
                    int mindex = packed[i++];
                    length--;
                    MethodActor impl;
                    MethodActor method;
                    if (mindex >= 0) {
                        impl = MethodID.toMethodActor(MethodID.fromWord(MemberID.create(holder, mindex)));
                        assert impl != null;
                        method = impl;
                    } else {
                        int implHolder = packed[i++];
                        length--;
                        impl = MethodID.toMethodActor(MethodID.fromWord(MemberID.create(implHolder, -mindex - 1)));
                        int methodHolderID = packed[i++];
                        length--;
                        ClassActor methodHolder = ClassID.toClassActor(methodHolderID);
                        method = methodHolder.findLocalMethodActor(impl.name, impl.descriptor());
                        assert method != null : impl;
                    }
                    stop = !dc.doConcreteMethod(targetMethod, method, impl, classActor);
                }
                assert length >= 0;
                if (dc.classID == holder) {
                    return;
                }
            } else {
                if (length < 0) {
                    length = -length;
                }
                i += length;
            }
            prev = classActor;

        }
        dc.nextClass(null, prev);
        assert i == packed.length;
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
