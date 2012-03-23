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
import java.util.concurrent.*;

import com.sun.max.vm.actor.holder.*;

/**
 * Map from a class to the set of {@linkplain Dependencies dependencies}
 * that include the class as a <i>context</i> class. When a class hierarchy change
 * (during class definition) involves a context class of an assumption, the assumption
 * may be invalidated. This map allows efficient discovery of dependencies involving
 * a context class.
 * <p>
 * There is no strong reference (path) from a context class in the dependency map
 * to the {@linkplain Dependencies} instances. Instead, the set of {@linkplain Dependencies}
 * objects are indirectly referenced by their {@linkplain Dependencies#id identifiers}.
 * This prevents a class from being kept alive simply because it is involved in a
 * dependency. This is important for simplifying class unloading.
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

    static final ConcurrentHashMap<ClassActor, DSet> map = new ConcurrentHashMap<ClassActor, DSet>(INITIAL_CAPACITY);

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
                    if (dependenciesLogger.enabled()) {
                        deps.logAdd(type);
                    }
                    continue;
                }
            }

            // lost the race - fall back to locking
            synchronized (dset) {
                dset.addUnique(deps.id);
            }
            if (dependenciesLogger.enabled()) {
                deps.logAdd(type);
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
        deps.iterate(new Dependencies.DependencyVisitor() {
            @Override
            public boolean nextContextClass(ClassActor type, ClassActor prev) {
                if (type != null) {
                    DSet dset = map.get(type);
                    if (dset != null) {
                        if (dset.remove(deps.id)) {
                            removed[0]++;
                            if (dependenciesLogger.enabled()) {
                                deps.logRemove(type);
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

}
