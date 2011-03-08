/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.actor.holder;

import static com.sun.max.vm.actor.holder.ClassID.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 * Class maintaining class hierarchy and sub-typing relationships information of currently defined classes.
 * Dynamic compilers may issue queries related to these information and may make assumptions to apply certain optimizations
 * (e.g., devirtualization, type check elimination).
 * {@link TargetMethod}s produced by a dynamic compiler keeps track of the assumptions the compiler made in a {@link CiAssumptions} object.
 * This one must be validated by the dependency manager before the code is allowed for uses. If the assumptions are incorrect, because of
 * changed that occured in the class hierarchy since the assumption was made, the code is dropped and a new one must be produced.
 *
 * The dependencies manager is also responsible for recording changes to the class hierarchy and whatever other information that
 * depends on sub-type relationships, and to drive code invalidation (using deoptimization) when assumptions made by existing code
 * become obsolete because of these changes.
 * The dependency manager must be informed of every changes to the class hierarchy when new classes are defined.
 *
 *
 * @author Laurent Daynes.
 */
public final class ClassDependencyManager {

    private static final int HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK = 0;
    private static final int NO_CONCRETE_SUBTYPE_MARK = NULL_CLASS_ID;

    private static final boolean enableDumpOption = false;

    /**
     * Object used for synchronizing modifications to the class hierarchy,
     * and to synchronize validation of target method assumptions.
     */
    private static  final Object classHierarchyLock = new ClassDependencyManager();

    private ClassDependencyManager() {
    }

    private static void dump(ClassActor classActor) {
        Log.print(classActor.id);
        Log.print(", ");
        Log.print(classActor.name());
        Log.print(", ");
        int uct = classActor.uniqueConcreteType;
        if (uct == NO_CONCRETE_SUBTYPE_MARK) {
            Log.print("null, -, ");
        } else if (uct == HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
            Log.print(" multiple, -, ");
        } else {
            Log.print(uct);
            Log.print(", ");
            Log.print(ClassID.toClassActor(uct).name());
        }
        Log.println();
    }

    /**
     * Flush assumptions and activations of code that has been compiled on the assumptions invalidated by {@code newClassActor}.
     *
     * @param newClassActor the newly loaded class that might invalidate assumptions
     */
    private static void flushDependentsOn(ClassActor newClassActor) {
    }

    /**
     * Adds this {@linkplain ClassActor} to the beginning of the list of subclasses of its superclass.
     */
    private static void prependToSiblingList(ClassActor classActor) {
        if (!classActor.isInstanceClass()) {
            // Don't bother for non-instance classes: they all are sub-classes of Objects
            // class hierarchy information can be inferred otherwise.
            return;
        }
        ClassActor superClassActor = classActor.superClassActor;
        if (superClassActor == null) {
            // special case: class "Object"
            return;
        }
        assert !superClassActor.isInterface() : "Superclass cannot be interface.";
        classActor.nextSiblingId = superClassActor.firstSubclassActorId;
        superClassActor.firstSubclassActorId = classActor.id;
    }
    /**
     * Walk up the ancestry, and update the concrete type information.
     * @param ancestor
     * @param uniqueConcreteSubtype
     */
    private static void propagateConcreteSubType(ClassActor ancestor, int uniqueConcreteSubtype) {
        // Update all the ancestors without a concrete sub-type with the unique concrete subtype.
        while (ancestor.uniqueConcreteType == NO_CONCRETE_SUBTYPE_MARK) {
            // No single concrete sub-type has been recorded for this ancestor yet.
            ancestor.uniqueConcreteType = uniqueConcreteSubtype;
            ancestor = ancestor.superClassActor;
        }
        // We reached an ancestor with at least one concrete sub-type (either it is one itself,
        // or one or more of its other children has a concrete sub-type). From here on, we can only
        // have ancestors with some concrete sub-types.
        while (ancestor.uniqueConcreteType != HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
            // Reached an ancestor that had a unique-concrete sub-type.
            // This isn't true anymore, so update the mark.
            ancestor.uniqueConcreteType = HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK;
            ancestor = ancestor.superClassActor;
            if (MaxineVM.isDebug()) {
                FatalError.check(ancestor.uniqueConcreteType != NO_CONCRETE_SUBTYPE_MARK, "must have at least one concrete sub-type");
            }
        }
        // We reached an ancestor with multiple concrete sub types. From here on, all ancestors can only have
        // more than one concrete sub-type. This is a terminal state that will not change until class
        // unloading occurs.
    }

    private static void recordInstanceClassActor(ClassActor classActor) {
        FatalError.check(!classActor.hasSubclass(), "must be leaf at class definition time");
        // If new class is abstract, the unique concrete sub-type table relationship doesn't change.
        if (!classActor.isAbstract()) {
            // Recording is made at class definition time, when the class hasn't any sub-type yet.
            // So the unique concrete sub-type is one self.
            final int uniqueConcreteSubtype = classActor.id;
            classActor.uniqueConcreteType = uniqueConcreteSubtype;
            ClassActor ancestor = classActor.superClassActor;
            if (ancestor == null) {
                // Can only be the class actor for java.lang.Object
                return;
            }
            // Next, update unique concrete sub-type information of super-classes.
            propagateConcreteSubType(ancestor, uniqueConcreteSubtype);

            // Last, update the unique concrete sub-type of the interfaces the class implements.
            for (ClassActor implemented : classActor.localInterfaceActors()) {
                propagateConcreteSubType(implemented, uniqueConcreteSubtype);
            }
        }
    }

    private static void recordUniqueConcreteSubtype(ClassActor classActor) {
        if (classActor.isInstanceClass()) {
            recordInstanceClassActor(classActor);
        } else if (classActor.isPrimitiveClassActor()) {
            // Primitive types are leaves, rooted directly at the Object type.
            // Nothing to propagate.
            classActor.uniqueConcreteType = classActor.id;
        } else if (classActor.isArrayClass()) {
            // Arrays are concrete types, regardless of whether their element type is a concrete type or not.
            // (i.e., one can create instance of T [] even if T is abstract).
            // Further,  T [] > S [] if  T > S.
            // Therefore, an array has a unique concrete type if and only if
            // its element type is a leaf in the class hierarchy.
            // We can only infer that for final classes.
            ClassActor elementClassActor = classActor.elementClassActor();
            if (elementClassActor.isPrimitiveClassActor() || elementClassActor.isFinal()) {
                classActor.uniqueConcreteType = classActor.id;
            }
            // We leave the unique concrete type to the NULL_CLASS_ID for all other cases as it
            // can be inferred from the element type.
        }
        // everything else is a abstract and therefore (i) doesn't have any concrete sub-type yet,
        // and (ii), cannot change the unique concrete sub-type of their super-types.
    }

    public static ClassActor getUniqueConcreteSubtype(ClassActor classActor) {
        if (!classActor.isArrayClass()) {
            synchronized (classHierarchyLock) {
                int uct = classActor.uniqueConcreteType;
                if (uct <= HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
                    // Encoding of uct is such that
                    // NO_MARK < HAS_MULTIPLE_MARK < CLASS_ID for all ClassActor != ClassActor.from(Object.class)
                    // So the test above filters that either has no concrete or multiple concrete sub-types.
                    return null;
                }
                return ClassID.toClassActor(uct);
            }
        }
        // Should we care about being less conservative for class array?
        // i.e., we should return the array class id if the element type is a leaf
        // (i.e., has no sub-classes, or has no implementation if an interface).
        return ClassID.toClassActor(classActor.uniqueConcreteType);
    }

    /*
     * Utility to walk a type tree and find concrete method implementation for a given signature.
     */
    static class UniqueConcreteMethodSearch {
        private RiMethod firstConcreteMethod = null;
        private boolean hasMoreThanOne = false;

        private boolean setConcreteMethod(RiMethod concreteMethod) {
            assert concreteMethod != null;
            if (concreteMethod != firstConcreteMethod) {
                if (firstConcreteMethod == null) {
                    firstConcreteMethod = concreteMethod;
                } else {
                    hasMoreThanOne = true;
                }
            }
            return hasMoreThanOne;
        }

        RiMethod uniqueConcreteMethod() {
            return hasMoreThanOne ? null : firstConcreteMethod;
        }


        /**
         *
         * @param root
         * @param method
         * @return true if sub-type needs to be walked over to find concrete implementation
         */
        boolean shouldSearchSubTypes(ClassActor root, RiMethod method) {
            final int uct = root.uniqueConcreteType;
            if (uct == NO_CONCRETE_SUBTYPE_MARK) {
                // No concrete type, no need to search. No need to search sub-types.
                return false;
            }
            if (uct != HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
                ClassActor concreteType = ClassID.toClassActor(uct);
                // This is the only concrete sub-type for the current context. The concrete method
                // is whatever concrete method is used by this concrete type.
                setConcreteMethod(concreteType.resolveMethodImpl(method));
                // found the single concrete method for this class actor. No need to search sub-types.
                return false;
            }
            // There is multiple concrete sub-type. Need to search them to determine unique concrete method.
            return true;
        }

        void searchTypeGraph(ClassActor root, RiMethod method) {
            if (shouldSearchSubTypes(root, method)) {
                if (root.isInterface()) {
                    // Don't bother for now. Assume can't find concrete method implementation.
                    return;
                }
                searchInstanceClassTree(root, method);
            }
        }

        /**
         * Search the instance class tree rooted by the specified class actor for concrete implementations
         * of the specified method. Result of the search can be obtained via {{@link #uniqueConcreteMethod()}.
         * @param root a tuple or hybrid class actor
         * @param method the method concrete implementation of are being searched
         */
        void searchInstanceClassTree(ClassActor root, RiMethod method) {
            // Iterate over all concrete sub-types and determines if they all used the same method.
            assert root.isInstanceClass() : "must be an hybrid or tuple class actor";
            assert root.firstSubclassActorId != NULL_CLASS_ID : "must have at least one sub-class";
            assert firstConcreteMethod == null || !hasMoreThanOne;

            int classId = root.firstSubclassActorId;
            do {
                ClassActor subType = ClassID.toClassActor(classId);
                if (shouldSearchSubTypes(subType, method)) {
                    searchInstanceClassTree(subType, method);
                }
                if (hasMoreThanOne) {
                    // no need to search further.
                    return;
                }
                classId = subType.nextSiblingId;
            } while(classId != NULL_CLASS_ID);
        }

        void reset() {
            hasMoreThanOne = false;
            firstConcreteMethod = null;
        }
    }

    private static UniqueConcreteMethodSearch uniqueConcreteMethodSearch = new UniqueConcreteMethodSearch();

    public static RiMethod getUniqueConcreteMethod(ClassActor declaredType, RiMethod method) {
        // Default is to return null. See sub-classes of ClassActor for specific details.
        assert declaredType.isSubtypeOf(method.holder());
        synchronized (classHierarchyLock) {
            uniqueConcreteMethodSearch.reset();
            uniqueConcreteMethodSearch.searchTypeGraph(declaredType, method);
            return uniqueConcreteMethodSearch.uniqueConcreteMethod();
        }
    }

    /**
     * Adds the class to the class hierarchy. This will also trigger invalidating dependencies and deoptimizing code based thereon.
     *
     * @param classActor the class to be added to the global class hierarchy.
     */
    public static void addToHierarchy(ClassActor classActor) {
        synchronized (classHierarchyLock) {
            prependToSiblingList(classActor);
            recordUniqueConcreteSubtype(classActor);
            flushDependentsOn(classActor);
        }
    }

    /**
     * Validate a set of assumptions made by a dynamic compiler.
     *
     * @param ciAssumptions
     * @return
     */
    public static boolean validateAssumptions(CiAssumptions ciAssumptions) {
        return true;
    }

    /**
     * Dump the table in the log.
     */
    public static void dump() {
        if (!enableDumpOption) {
            return;
        }
        synchronized (classHierarchyLock) {
            int classId = 0;
            int totalClasses = 0;
            int totalAbstractClasses = 0;
            int totalLeaves = 0;
            int totalUCP = 0;
            int totalClassesWithUCP = 0;
            int totalClassesWithMCP = 0;

            boolean printDetails = false;
            if (printDetails) {
                Log.println("class id, class name, concrete subtype, concrete subtype class id");
            }
            final int length = ClassID.largestClassId();
            while (classId < length) {
                ClassActor classActor;
                // Skip unused ids
                do {
                    classActor = ClassID.toClassActor(classId++);
                } while(classActor == null && classId < length);
                if (classId >= length) {
                    break;
                }
                totalClasses++;
                if (classActor.isAbstract()) {
                    totalAbstractClasses++;
                }
                if (classActor.firstSubclassActorId == NULL_CLASS_ID) {
                    totalLeaves++;
                }

                if (classActor.uniqueConcreteType == HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
                    totalClassesWithMCP++;
                } else {
                    totalClassesWithUCP++;
                    if (classActor.uniqueConcreteType == classActor.id) {
                        totalUCP++;
                    }
                }
                if (printDetails) {
                    dump(classActor);
                }
            }

            Log.print("# classes            :");
            Log.println(totalClasses);
            Log.print("# abstract classes   :");
            Log.println(totalAbstractClasses);
            Log.print("# leaves             :");
            Log.println(totalLeaves);
            Log.print("# UCP                :");
            Log.println(totalUCP);
            Log.print("# classes with UCP   :");
            Log.println(totalClassesWithUCP);
            Log.print("# classes with MCP   :");
            Log.println(totalClassesWithMCP);
        }
    }

}
