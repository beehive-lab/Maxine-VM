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

import static com.sun.max.vm.actor.holder.ClassDependencyManager.ClassAssumptionFlags.*;
import static com.sun.max.vm.actor.holder.ClassID.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.profile.*;
import com.sun.max.profile.ValueMetrics.IntegerDistribution;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Class maintaining class hierarchy and sub-typing relationships information of currently defined classes.
 * Dynamic compilers may issue queries related to these information and may make assumptions to apply certain optimizations
 * (e.g., devirtualization, type check elimination).
 * {@link TargetMethod}s produced by a dynamic compiler keeps track of the assumptions the compiler made in a {@link CiAssumptions} object.
 * This one must be validated by the dependency manager before the code is installed for uses. If the assumptions are incorrect, because of
 * changes that occurred in the class hierarchy since the assumptions were made, the target method is dropped and a new one must be produced.
 *
 * The dependencies manager is also responsible for recording changes to the class hierarchy and related information that
 * depends on sub-type relationships, and to drive code invalidation (using deoptimization) when assumptions made by existing code
 * become obsolete because of these changes.
 * The dependency manager must be informed of every changes to the class hierarchy when new classes are defined.
 *
 * @author Laurent Daynes.
 */
public final class ClassDependencyManager {

    private static final int HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK = 0;
    private static final int NO_CONCRETE_SUBTYPE_MARK = NULL_CLASS_ID;

    private static final boolean enableDumpOption = false;

    /**
     * Read-write lock used to synchronize modifications to the class hierarchy with validation of
     * compiler assumptions.
     * New class definition must acquire the lock in write mode to exclude all concurrent updates to the class hierarchy,
     * and, more importantly, to exclude all concurrent validations or installations of validated
     * assumptions.
     * Validations and installations of assumptions acquire the lock in read mode to exclude all modifications to
     * class hierarchy information. Validation doesn't require synchronization on class hierarchy information,
     * multiple validation can be performed concurrently. Installation of dependencies in the dependency table
     * requires additional synchronization as it updates both the table and per class type dependency information.
     */
    private static final ReentrantReadWriteLock classHierarchyLock = new ReentrantReadWriteLock();

    // TODO: factor out with similar code in Class ID ?
    private static final int MINIMAL_DEPENDENT_TARGET_METHOD = 5000;

    /**
     * The table recording target methods compiled with valid assumptions.
     * Such target methods are associated with a {@link ValidAssumptions} instance that
     * records the class ID of the classes upon which assumptions are made. The index to the table slot
     * is used as a unique identifier for the dependent target method, and is used in per-class records
     * of dependent target methods.
     */
    private static VariableLengthArray<ValidAssumptions> idToValidAssumptions = new VariableLengthArray<ValidAssumptions>(MINIMAL_DEPENDENT_TARGET_METHOD);
    private static BitSet usedIDs = new BitSet();

    /**
     * The table mapping class types to their dependent target methods and the assumptions these makes on the class type.
     * The table is updated whenever a target method compiled with assumption is validated, and inspected to re-assess these
     * assumptions whenever the class hierarchy is modified with new types.
     * @see DependentTargetMethodTable for details.
     */
    private static DependentTargetMethodTable dependentTargetMethodTable = new DependentTargetMethodTable();

    private static final ObjectThreadLocal<UniqueConcreteMethodSearch> UCM_SEARCH_HELPER =
        new ObjectThreadLocal<UniqueConcreteMethodSearch>("UCM_SEARCH_HELPER", "thread local helper for class dependency management");


    /**
     * Valid assumptions made by the compiler and used by a target method.
     *
     * @author ldayne
     */
    static class ValidAssumptions extends ClassHierarchyAssumptions {
        /**
         * Marker used to invalidate valid assumptions on compiled but not already installed target method.
         */
        static final short [] INVALIDATED = new short[0];

        /**
         * The target method produced with the validated assumptions.
         * Needed to apply de-optimization should the assumptions be invalidated by some class definition event.
         */
        TargetMethod targetMethod;
        /**
         * Set of assumptions made by the target method, packed into a short array formatted as follows.
         * The first entry specifies the number of classes the target method makes assumptions on.
         * The next entries contains two short per class: the first one hold a class ID, the second one a short bit fields
         * indicate the type of assumption and the length of the assumption area of that class.
         */
        volatile short [] assumptions;

        int id;

        ValidAssumptions(AssumptionValidator validator) {
            FatalError.check(classHierarchyLock.getReadHoldCount() > 0, "Must hold class hierarchy lock");
            registerValidAssumptions(this);
            assumptions = validator.packAssumptions();
            dependentTargetMethodTable.addDependentTargetMethod(id, validator.typeAssumptions.keySet());
        }

        @Override
        public boolean isValid() {
            // is valid as long as some changes in the class hierarchy haven't invalidated it.
            return assumptions != INVALIDATED;
        }

        /**
         * Indicates whether a target method was installed with this validated set of assumptions.
         * If true, deoptimization is needed to invalidate this set.
         * @return
         */
        boolean hasTargetMethod() {
            return targetMethod != null;
        }

        void setTargetMethod(TargetMethod targetMethod) {
            FatalError.check(classHierarchyLock.getReadHoldCount() > 0, "Must hold class hierarchy lock");
            this.targetMethod = targetMethod;
        }

        /**
         * Invalidate this set of assumptions. Must be done only once all references to this record is cleared from the {@link DependentTargetMethodTable}.
         */
        void invalidate() {
            // Called only when modifying the class hierarchy.
            FatalError.check(classHierarchyLock.isWriteLocked(), "Must hold class hierarchy lock in write mode");
            assumptions = INVALIDATED;
        }
    }

    /**
     * Register the target method produced with a set of validated assumptions.
     * If assumptions were invalidated in the meantime, the assumptions are dropped and the
     * compiler that made the assumption must recompile the target method.
     *
     * @param assumptions a set of assumptions
     * @param targetMethod the target methods to associate with the assumptions
     * @return true if the assumptions are still valid, false if recompilation is required.
     */
    public static boolean registerValidatedTarget(ClassHierarchyAssumptions assumptions, TargetMethod targetMethod) {
        if (assumptions == ClassHierarchyAssumptions.noAssumptions) {
            return true;
        }
        ValidAssumptions validAssumptions = (ValidAssumptions) assumptions;
        classHierarchyLock.readLock().lock();
        try {
            if (validAssumptions.isValid()) {
                validAssumptions.setTargetMethod(targetMethod);
                return true;
            }
        } finally {
            classHierarchyLock.readLock().unlock();
        }
        // Drop it !
        clearValidAssumptions(validAssumptions);
        return false;
    }

    public static ClassHierarchyAssumptions validateAssumptions(CiAssumptions ciAssumptions) {
        if (ciAssumptions != null) {
            final AssumptionValidator validator = new AssumptionValidator();
            classHierarchyLock.readLock().lock();
            try {
                ciAssumptions.visit(validator);
                if (!validator.validated) {
                    return ClassHierarchyAssumptions.invalidAssumptions;
                }
                ValidAssumptions result = new ValidAssumptions(validator);
                return result;
            } finally {
                classHierarchyLock.readLock().unlock();
            }
        }
        return ClassHierarchyAssumptions.noAssumptions;
    }

    static int registerValidAssumptions(ValidAssumptions validAssumptions) {
        synchronized (usedIDs) {
            final int id = usedIDs.nextClearBit(0);
            validAssumptions.id = id;
            usedIDs.set(id);
            idToValidAssumptions.set(id, validAssumptions);
            return id;
        }
    }

    static void clearValidAssumptions(ValidAssumptions validAssumptions) {
        synchronized (usedIDs) {
            idToValidAssumptions.set(validAssumptions.id, null);
            usedIDs.clear(validAssumptions.id);
        }
    }

    static class DependentTargetMethodList {
        int [] dependentLists;
        DependentTargetMethodList(int dependent) {
            dependentLists = new int[] {2, dependent };
        }

        synchronized void add(int dependent) {
            int nextSlot = dependentLists[0];
            if (nextSlot == dependentLists.length) {
                // extend first.
                int [] newDependentLists = new int[nextSlot << 1];
                for (int i = 1; i < nextSlot; i++) {
                    newDependentLists[i] = dependentLists[i];
                }
                dependentLists = newDependentLists;
            }
            dependentLists[nextSlot++] = dependent;
            dependentLists[0] = nextSlot;
        }

        synchronized void remove(int dependent) {
            int end = dependentLists[0];
            int i = 1;
            while (i < end) {
                if (dependentLists[i] == dependent) {
                    int newEnd = end - 1;
                    if (i < newEnd) {
                        dependentLists[i] = dependentLists[newEnd];
                    }
                    dependentLists[0] = newEnd;
                    // should we trim the array here if poorly occupied ?
                    return;
                }
                i++;
            }

        }
    }
    /**
     * Table mappings class types to target method that made assumption on them.
     * Each class types are assigned
     */
    static class DependentTargetMethodTable {
        /**
         * Initial capacity of the table. Based on statistics gathered over boot image generation and VM startup.
         * Needs to be adjusted depending on the dynamic compilation scheme.
         */
        static final int INITIAL_CAPACITY = 600;

        final ConcurrentHashMap<RiType, DependentTargetMethodList> typeToDependentTargetMethods =
            new ConcurrentHashMap<RiType, DependentTargetMethodList>(INITIAL_CAPACITY);

        void addDependentTargetMethod(int dependentID, Set<RiType> dependsOn) {
            for (RiType type : dependsOn) {
                DependentTargetMethodList list = typeToDependentTargetMethods.get(type);
                if (list == null) {
                    DependentTargetMethodList prev = typeToDependentTargetMethods.put(type, new DependentTargetMethodList(dependentID));
                    FatalError.check(prev == null, "race condition detected !?");
                } else {
                    list.add(dependentID);
                }
            }
        }
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
            int uct = classActor.uniqueConcreteType;
            if (uct <= HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
                // Encoding of uct is such that
                // NO_MARK < HAS_MULTIPLE_MARK < CLASS_ID for all ClassActor != ClassActor.from(Object.class)
                // So the test above filters that either has no concrete or multiple concrete sub-types.
                return null;
            }
            return ClassID.toClassActor(uct);
        }
        // Should we care about being less conservative for class array?
        // i.e., we should return the array class id if the element type is a leaf
        // (i.e., has no sub-classes, or has no implementation if an interface).
        return ClassID.toClassActor(classActor.uniqueConcreteType);
    }

    /*
     * Utility to walk a type tree and find concrete method implementation for a given signature.
     */
    static final class UniqueConcreteMethodSearch {
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

        /**
         *
         * @param root
         * @param method
         * @return true if sub-type needs to be walked over to find concrete implementation
         */
        private boolean shouldSearchSubTypes(ClassActor root, RiMethod method) {
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

        /**
         * Search the instance class tree rooted by the specified class actor for concrete implementations
         * of the specified method. Result of the search can be obtained via {{@link #uniqueConcreteMethod()}.
         * @param root a tuple or hybrid class actor
         * @param method the method concrete implementation of are being searched
         */
        private void searchInstanceClassTree(ClassActor root, RiMethod method) {
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

        RiMethod uniqueConcreteMethod() {
            return hasMoreThanOne ? null : firstConcreteMethod;
        }

        RiMethod uniqueConcreteMethod(ClassActor root, RiMethod method) {
            // Reset before initiating the search.
            hasMoreThanOne = false;
            firstConcreteMethod = null;
            if (shouldSearchSubTypes(root, method)) {
                if (root.isInterface()) {
                    // Don't bother for now. Assume can't find concrete method implementation.
                    return null;
                }
                searchInstanceClassTree(root, method);
            }
            return uniqueConcreteMethod();
        }
    }

    public static RiMethod getUniqueConcreteMethod(ClassActor declaredType, RiMethod method) {
        // Default is to return null. See sub-classes of ClassActor for specific details.
        assert declaredType.isSubtypeOf(method.holder());
        classHierarchyLock.readLock().lock();
        try {
            return UCM_SEARCH_HELPER.get().uniqueConcreteMethod(declaredType, method);
        } finally {
            classHierarchyLock.readLock().unlock();
        }
    }

    /**
     * Adds the class to the class hierarchy. This will also trigger invalidating dependencies and deoptimizing code based thereon.
     *
     * @param classActor the class to be added to the global class hierarchy.
     */
    public static void addToHierarchy(ClassActor classActor) {
        classHierarchyLock.writeLock().lock();
        try {
            prependToSiblingList(classActor);
            recordUniqueConcreteSubtype(classActor);
            flushDependentsOn(classActor);
        } finally {
            classHierarchyLock.writeLock().unlock();
        }
    }

    enum ClassAssumptionFlags {
        CLASS_ASSUMPTIONS_LENGTH(0, 13),
        CLASS_HAS_UCM(13, 1),
        CLASS_LOCAL_UCM_ONLY(14, 1),
        CLASS_HAS_UCT(15, 1);

        final int mask;
        final int leftmostBitPos;

        short setFlag(short flagHolder, short flag) {
            return (short) (flagHolder | (flag << leftmostBitPos));
        }

        short getFlag(short flagHolder) {
            return (short) ((flagHolder & mask) >> leftmostBitPos);
        }

        short setBooleanFlag(short flagHolder) {
            return (short) (flagHolder | mask);
        }

        short clearBooleanFlag(short flagHolder) {
            return (short) (flagHolder & ~mask);
        }

        boolean isBooleanFlagSet(short flagHolder) {
            return (flagHolder & mask) != 0;
        }

        int bitWidth() {
            return Integer.bitCount(mask);
        }

        ClassAssumptionFlags(int leftmostBitPos, int numBits) {
            this.leftmostBitPos = leftmostBitPos;
            this.mask = ((~0) & ((1 << numBits) - 1)) << leftmostBitPos;
        }
    }

    /**
     * Validate assumptions for a single compiled method and build lists of assumptions made on class type, one list per type.
     * Each type list of assumptions are pre-formatted during validation in a format that'll ease updating the global
     * dependency table in case assumptions are valid.
     * All lists are dropped on first invalid assumption met.
     *
     * Initial statistics on boot image generation shows that the vast majority of target methods have a single concrete
     * method dependencies, and less
     * than 10 % have some unique concrete type dependencies and typically a single one.
     * Further, most single concrete method dependencies are on leaf methods, i.e., wherein the context method is the concrete method.
     * So an encoding of the dependencies should optimized for these cases.
     */
    static class AssumptionValidator implements CiAssumptions.AssumptionProcessor {

        // AssumptionList recorded per type are made of methodID with a tag stored in the highest two bits,
        // so that:
        // assumption == 0 => UCT dependencies, no method ID
        // assumption > 0  => local only UCM, methodID == assumption
        // assumption < 0  => non local UCM, methodID = assumption &~ UNIQUE_CONCRETE_METHOD_DEP
        static final int UNIQUE_CONCRETE_TYPE_DEP = 0 << 30;
        static final int LEAF_CONCRETE_METHOD_DEP = 1 << 30;
        static final int UNIQUE_CONCRETE_METHOD_DEP = 2 << 30;

        static final int [] canonicalizedSingleUCT = new int[] {2, UNIQUE_CONCRETE_TYPE_DEP};

        /**
         * Maps of class types to assumptions made about them.
         */
        private final HashMap<RiType, int []> typeAssumptions = new HashMap<RiType, int []>(10);

        /**
         * Result of the validation.
         */
        private boolean validated = true;

        private int totalLocal = 0;
        private int totalNonLocal = 0;

        private int [] grow(RiType context, int [] encodedDependencies) {
            int length = encodedDependencies.length;
            int [] newEncodedDependencies = new int[length << 1];
            System.arraycopy(encodedDependencies, 0, newEncodedDependencies, 0, length);
            typeAssumptions.put(context, newEncodedDependencies);
            return newEncodedDependencies;
        }

        private boolean isUniqueConcreteMethod(RiMethod context, RiMethod method) {
            final ClassActor declaredType = (ClassActor) context.holder();
            return UCM_SEARCH_HELPER.get().uniqueConcreteMethod(declaredType, method) == method;
        }

        private static final short DEFAULT_SUMMARY_FLAG = CLASS_LOCAL_UCM_ONLY.setBooleanFlag(CLASS_HAS_UCM.setBooleanFlag((short) 0));
        private static final int MAX_ASSUMPTION_LENGTH = 1 << CLASS_ASSUMPTIONS_LENGTH.bitWidth();

        public short [] packAssumptions() {
            FatalError.check(ClassID.largestClassId() < Short.MAX_VALUE, "Support for 1 << 16 number of classes not supported yet");
            // Pre-compute size of the dependencies arrays:
            final int numClasses = typeAssumptions.size();
            int size = totalLocal + 2 * totalNonLocal + 2 * numClasses + 1;
            short [] assumptions = new short[size];
            assumptions[0] = (short) numClasses;
            int classIndex = 1;
            int dependenciesIndex = 2 * numClasses + 1;
            for (RiType type : typeAssumptions.keySet()) {
                assumptions[classIndex++] = (short) ((ClassActor) type).id;
                int [] assumptionList = typeAssumptions.get(type);
                int i = 1;
                short assumptionsSummary = DEFAULT_SUMMARY_FLAG;
                final int firstDependencyIndex = dependenciesIndex;
                final int end = assumptionList[0];
                while (i < end) {
                    int assumption = assumptionList[i++];
                    FatalError.check(assumption >= Short.MAX_VALUE, "Not supported");
                    if (assumption > 0) {
                        // A local only unique concrete method.
                        assumptions[dependenciesIndex++] = (short) assumption;
                    } else if (assumption < 0) {
                        assumptions[dependenciesIndex++] = (short) assumption;
                        assumptions[dependenciesIndex++] = (short) assumptionList[i++];
                        assumptionsSummary = CLASS_LOCAL_UCM_ONLY.clearBooleanFlag(assumptionsSummary);
                    } else {
                        assumptionsSummary = CLASS_HAS_UCT.setBooleanFlag(assumptionsSummary);
                    }
                }
                int assumptionLength = dependenciesIndex - firstDependencyIndex;
                FatalError.check(assumptionLength < MAX_ASSUMPTION_LENGTH, "too many assumptions.");
                assumptions[classIndex++] = CLASS_ASSUMPTIONS_LENGTH.setFlag(assumptionsSummary, (short) assumptionLength);
            }
            return assumptions;
        }

        @Override
        public boolean processUniqueConcreteSubtype(RiType context, RiType subtype) {
            boolean valid = true;
            if (context != subtype) {
                final ClassActor classActor = (ClassActor) context;
                final ClassActor subClassActor =  (ClassActor) subtype;
                valid = classActor.uniqueConcreteType == subClassActor.id;
            }
            if (valid) {
                int [] encodedDependencies = typeAssumptions.get(context);
                if (encodedDependencies == null) {
                    typeAssumptions.put(context, canonicalizedSingleUCT);
                } else if (encodedDependencies != canonicalizedSingleUCT) {
                    int end = encodedDependencies[0];
                    if (end == encodedDependencies.length) {
                        encodedDependencies = grow(context, encodedDependencies);
                    }
                    encodedDependencies[end++] = AssumptionValidator.UNIQUE_CONCRETE_TYPE_DEP;
                    encodedDependencies[0] = end;
                } // otherwise: nothing to do as there can be only one single concrete type.
                return true;
            }
            validated = false;
            // Drop whatever was built so far.
            typeAssumptions.clear();
            return false;
        }

        @Override
        public boolean processUniqueConcreteMethod(RiMethod context, RiMethod method) {
            if (!isUniqueConcreteMethod(context, method)) {
                // Drop whatever was built so far.
                typeAssumptions.clear();
                validated = false;
                return false;
            }
            final RiType contextHolder = context.holder();
            int [] encodedDependencies = typeAssumptions.get(contextHolder);
            if (encodedDependencies == null) {
                encodedDependencies = new int[4];
                encodedDependencies[0] = 1;
                typeAssumptions.put(contextHolder, encodedDependencies);
            }
            int end = encodedDependencies[0];
            int contextMethodIndex = ((MethodActor) method).memberIndex();
            if (context == method) {
                totalLocal++;
                if (end + 2 >= encodedDependencies.length) {
                    encodedDependencies = grow(contextHolder, encodedDependencies);
                }
                encodedDependencies[end++] = AssumptionValidator.LEAF_CONCRETE_METHOD_DEP;
                encodedDependencies[end++] = contextMethodIndex;
            } else {
                totalNonLocal++;
                if (end + 3 >= encodedDependencies.length) {
                    encodedDependencies = grow(contextHolder, encodedDependencies);
                }
                encodedDependencies[end++] = AssumptionValidator.UNIQUE_CONCRETE_METHOD_DEP;
                encodedDependencies[end++] = contextMethodIndex;
                encodedDependencies[end++] = ((ClassActor) method.holder()).id;
            }
            encodedDependencies[0] = end;
            return true;
        }
    }

    static class AssumptionStatsGatherer implements CiAssumptions.AssumptionProcessor {
        int totalDeps = 0;
        int numDep = 0;
        int numUCT = 0;
        int numUCM = 0;
        int selfUCT = 0;
        int selfUCM = 0;
        HashSet<RiType> typeContextsPerCompiledMethod = new HashSet<RiType>(10);
        HashSet<RiMethod> methodContextsPerCompiledMethod = new HashSet<RiMethod>(10);
        HashSet<RiType> typeContexts = new HashSet<RiType>(50);
        HashSet<RiMethod> methodContexts = new HashSet<RiMethod>(50);

        IntegerDistribution numTypeContextsPerMethod = ValueMetrics.newIntegerDistribution("numTypeContextPerMethod", 0, 10);
        IntegerDistribution numMethodContextsPerMethod = ValueMetrics.newIntegerDistribution("numMethodContextPerMethod", 0, 10);
        IntegerDistribution numUCTPerMethods = ValueMetrics.newIntegerDistribution("numUCTPerMethods", 0, 10);
        IntegerDistribution numUCMPerMethods = ValueMetrics.newIntegerDistribution("numUCMPerMethods", 0, 30);
        IntegerDistribution numDepPerMethods = ValueMetrics.newIntegerDistribution("numDepPerMethods", 0, 40);

        long numTargetMethods                 = 0;
        long numTargetMethodWithAssumptions  = 0;

        void summarize() {
            numTargetMethods++;
            if (numDep > 0) {
                totalDeps += numDep;
                numTargetMethodWithAssumptions++;
                numDepPerMethods.record(numDep);
                numTypeContextsPerMethod.record(typeContextsPerCompiledMethod.size());
                numMethodContextsPerMethod.record(methodContextsPerCompiledMethod.size());
                numUCTPerMethods.record(numUCT);
                numUCMPerMethods.record(numUCM);
                typeContextsPerCompiledMethod.clear();
                methodContextsPerCompiledMethod.clear();

                numUCT = 0;
                numUCM = 0;
                numDep = 0;
            }
        }

        @Override
        public boolean processUniqueConcreteSubtype(RiType context, RiType subtype) {
            if (context.equals(subtype)) {
                selfUCT++;
            }
            numDep++;
            numUCT++;
            typeContextsPerCompiledMethod.add(context);
            typeContexts.add(context);
            return true;
        }

        @Override
        public boolean processUniqueConcreteMethod(RiMethod context, RiMethod method) {
            if (context.equals(method)) {
                selfUCM++;
            }
            numDep++;
            numUCM++;
            methodContextsPerCompiledMethod.add(context);
            methodContexts.add(context);
            typeContextsPerCompiledMethod.add(context.holder());
            typeContexts.add(context.holder());
            return true;
        }

        public void report() {
            PrintStream out = System.out;
            out.println("CiAssumptions statistics");
            out.println("#target methods                " + numTargetMethods);
            out.println("#target methods w/ assumptions " + numTargetMethodWithAssumptions);
            out.println("#assumptions                   " + totalDeps);
            out.println("#type contexts                 " + typeContexts.size());
            out.println("#method contexts               " + methodContexts.size());
            out.println("#self UCT                      " + selfUCT);
            out.println("#self UCM                      " + selfUCM);
            numDepPerMethods.report("# deps / methods", out);
            numTypeContextsPerMethod.report("# type contexts / methods", out);
            numMethodContextsPerMethod.report("# method contexts / methods", out);
            numUCTPerMethods.report("# uct deps / methods", out);
            numUCMPerMethods.report("# ucm deps / methods", out);
        }
    }

    private static AssumptionStatsGatherer assumptionStatsGatherer = new AssumptionStatsGatherer();

    /**
     * Dump the table in the log.
     */
    public static void dump() {
        assumptionStatsGatherer.report();

        if (!enableDumpOption) {
            return;
        }
        classHierarchyLock.readLock().lock();

        try {
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
        } finally {
            classHierarchyLock.readLock().unlock();
        }
    }

}
