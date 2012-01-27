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
import static com.sun.max.vm.compiler.deps.DependenciesManager.Logger.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.sun.max.annotate.*;
import com.sun.max.profile.*;
import com.sun.max.profile.ValueMetrics.IntegerDistribution;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deps.Dependencies.DependencyClosure;
import com.sun.max.vm.compiler.deps.DependenciesManager.DependenciesCounter;
import com.sun.max.vm.compiler.deps.DependenciesManager.DependencyChecker;
import com.sun.max.vm.compiler.target.*;

/**
 * Map from a class to the set of {@linkplain Dependencies dependencies}
 * that include the class as a <i>context</i> class. When a class hierarchy change
 * (during class definition) involves a context class of an assumption, the assumption
 * may be invalidated. This map allows efficient discovery of dependencies involving
 * a context class.
 * <p>
 * There is no strong reference (path) from a context class in the dependency map
 * to the assumption objects. Instead, the set of assumption objects are indirectly
 * referenced by their {@linkplain Dependencies#id identifiers}. This
 * prevents a class from being kept alive simply because it is involved in an
 * assumption. This is important for simplifying class unloading.
 */
final class ContextDependents {

    /**
     * A set of {@link Dependencies} identifiers stored in an array. This data structure is designed
     * specifically as the value type in {@link ContextDependents#map}.
     */
    static final class DSet {

        /**
         * Creates a new set with one element.
         * @param depsID
         */
        DSet(int depsID) {
            data = new int[] {depsID};
            size = 1;
        }

        int[] data;
        int size;

        /**
         * Gets the dependency ID at a given index.
         */
        int get(int index) {
            return data[index];
        }

        /**
         * Gets the dependency at a given index.
         */
        Dependencies getDeps(int index) {
            return Dependencies.fromId(get(index));
        }

        /**
         * Adds a value to this set that is not currently in the set.
         *
         * @param depsID the value to add
         */
        void addUnique(int depsID) {
            assert find(depsID) == -1 : depsID + " is already in the set";
            if (size == data.length) {
                data = Arrays.copyOf(data, size * 2);
            }
            data[size++] = depsID;
        }

        /**
         * Determines if a given value is in this set.
         *
         * @param depsID the value to search for
         * @return {@code true} iff {@code value} is in this set
         */
        int find(int depsID) {
            for (int i = 0; i < size; i++) {
                if (depsID == data[i]) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Removes an element from this set at a known index.
         *
         * @param index the index of the element to remove
         * @return the removed element
         */
        int removeAt(int index) {
            int id = data[index];
            if (index == size - 1) {
                data[index] = 0;
            } else {
                // Replace with last element
                data[index] = data[size - 1];
            }
            size--;
            return id;
        }

        /**
         * Removes an element from this set.
         *
         * @param depsID the element to remove
         * @return {@code true} iff the element was removed
         */
        boolean remove(int depsID) {
            for (int i = 0; i < size; i++) {
                if (depsID == data[i]) {
                    int removed = removeAt(i);
                    assert removed == depsID;
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(getDeps(i));
            }
            return sb.append('}').toString();
        }
    }

    /**
     * Initial capacity of the map. Based on statistics gathered over boot image generation and VM startup.
     * Needs to be adjusted depending on the dynamic compilation scheme.
     */
    static final int INITIAL_CAPACITY = 600;

    private final ConcurrentHashMap<ClassActor, DSet> map = new ConcurrentHashMap<ClassActor, DSet>(INITIAL_CAPACITY);

    /**
     * Adds a mapping from each context type in a dependencies object to the dependency object.
     */
    void addDependencies(Dependencies deps, Set<ClassActor> typesInDeps) {
        assert classHierarchyLock.getReadHoldCount() > 0 : "must hold the class hierarchy lock in read mode";
        for (ClassActor type : typesInDeps) {
            DSet dset = map.get(type);
            if (dset == null) {
                dset = map.putIfAbsent(type, new DSet(deps.id));
                if (dset == null) {
                    // won the race to add the first dependency
                    if (logger.enabled()) {
                        deps.logAddRemove(Operation.Add, type);
                    }
                    continue;
                }
            }

            // lost the race - fall back to locking
            synchronized (dset) {
                dset.addUnique(deps.id);
            }
            if (logger.enabled()) {
                deps.logAddRemove(Operation.Add, type);
            }
        }
    }

    /**
     * Removes all mappings in which a given dependencies object is a value.
     * That is, the mapping for each context type in {@code deps} is updated to
     * remove {@code deps}.
     *
     * @return the number references to {@code deps} in this map that were removed
     */
    int removeDependencies(final Dependencies deps) {
        final int[] removed = {0};
        deps.iterate(new DependencyClosure() {
            @Override
            public boolean nextContextClass(ClassActor type, ClassActor prev) {
                if (type != null) {
                    DSet dset = map.get(type);
                    if (dset != null) {
                        if (dset.remove(deps.id)) {
                            removed[0]++;
                            if (logger.enabled()) {
                                deps.logAddRemove(Operation.Remove, type);
//                                Log.println("DEPS: Removed dependency from " + deps.toString() + " to " + type);
                            }
                        }
                        if (dset.size == 0) {
                            map.remove(type);
                        }
                    }
                }
                return true;
            }
        });
        return removed[0];
    }

    private final DependencyChecker checker = new DependencyChecker();

    /**
     * Removes any dependencies that are invalidated by a class hierarchy change.
     *
     * @param ancestor a type for which dependencies need to be re-validated
     * @param concreteType the new sub-type causing the hierarchy change
     * @param a list of invalidated dependencies (may be null)
     * @return the invalidated dependencies
     */
    ArrayList<Dependencies> flushInvalidDependencies(ClassActor ancestor, ClassActor concreteType, ArrayList<Dependencies> invalidated) {
        assert classHierarchyLock.isWriteLockedByCurrentThread() : "must hold the class hierarchy lock in read mode";
        // We hold the classHierarchyLock in write mode.
        // This means there cannot be any concurrent modifications to the dependencies.
        DSet dset = map.get(ancestor);
        if (dset == null) {
            return invalidated;
        }
        checker.reset(ancestor, concreteType);
        int i = 0;
        while (i < dset.size) {
            Dependencies deps = dset.getDeps(i);
            checker.reset();
            deps.iterate(checker);
            if (!checker.valid()) {
                if (invalidated == null) {
                    invalidated = new ArrayList<Dependencies>();
                }
                invalidated.add(deps);
                dset.removeAt(i);
            } else {
                i++;
            }
        }
        if (dset.size == 0) {
            map.remove(ancestor);
        }

        return invalidated;
    }

    /**
     * Dump statistics.
     */
    @HOSTED_ONLY
    void printStatistics(PrintStream out) {
        final DependenciesCounter uctCounter = new DependenciesCounter(0);
        IntegerDistribution numDistinctAssumptionsPerType = ValueMetrics.newIntegerDistribution("numDistinctAssumptionsPerType", 0, 20);
        IntegerDistribution numUCTAssumptionsPerType = ValueMetrics.newIntegerDistribution("numUCTAssumptionsPerType", 0, 20);
        IntegerDistribution numAssumptionsPerType = ValueMetrics.newIntegerDistribution("numDependenciesPerType", 0, 20);
        IntegerDistribution numDependentsPerType = ValueMetrics.newIntegerDistribution("numDependentPerType", 0, 20);
        HashMap<DependenciesCounter, DependenciesCounter> assumptionsCounters = new HashMap<DependenciesCounter, DependenciesCounter>(20);

        for (ClassActor type : map.keySet()) {
            DSet dset = map.get(type);
            int numDependents = dset.size;
            for (int i = 0; i < dset.size; i++) {
                final Dependencies deps = dset.getDeps(i);
                deps.countAssumptionsPerType(type.id, assumptionsCounters);
            }

            numDependentsPerType.record(numDependents);
            DependenciesCounter c = assumptionsCounters.remove(uctCounter);
            numUCTAssumptionsPerType.record(c != null ? c.count : 0);
            numDistinctAssumptionsPerType.record(assumptionsCounters.size());
            int totalTypeAssumptions = 0;
            for (DependenciesCounter co : assumptionsCounters.values()) {
                totalTypeAssumptions += co.count;
            }
            numAssumptionsPerType.record(totalTypeAssumptions);
        }

        out.println("# types with dependent methods: " + map.size());
        numDependentsPerType.report("# dependents / types", out);
        numAssumptionsPerType.report("# total assumptions / type", out);
        numDistinctAssumptionsPerType.report("# distinct assumptions / type", out);
        numUCTAssumptionsPerType.report("# UCT assumption / type, stream", out);
    }

    @HOSTED_ONLY
    private void printDependents(ClassActor type, final PrintStream out) {
        DSet dset = map.get(type);
        int numDependents = dset.size;
        out.println("class " + type + " (id = " + type.id + ") has " + numDependents + " dependents");
        for (int i = 0; i < dset.size; i++) {
            final Dependencies deps = dset.getDeps(i);
            deps.iterate(new DependencyClosure(type.id) {
                @Override
                public boolean doConcreteSubtype(TargetMethod method, ClassActor context, ClassActor subtype) {
                    out.println("    " + context + " has unique concrete implementation" + subtype + " [" + deps + "]");
                    return true;
                }
                @Override
                public boolean doConcreteMethod(TargetMethod targetMethod, MethodActor method, MethodActor impl, ClassActor context) {
                    out.println("    " + impl + " is a unique concrete method [" + deps + "]");
                    return true;
                }
                @Override
                public void doInvalidated() {
                    out.println("assumptions have been invalidated");
                }
            });
        }
    }

    @HOSTED_ONLY
    void dump(PrintStream out) {
        out.println("================================================================");
        out.println("DependentTargetMethodTable has " + map.size() + " entries");
        for (ClassActor type : map.keySet()) {
            printDependents(type, out);
        }
        out.println("================================================================");
    }
}
