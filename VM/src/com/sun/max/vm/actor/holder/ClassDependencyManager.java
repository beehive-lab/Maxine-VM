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
import com.sun.max.annotate.*;
import com.sun.max.profile.*;
import com.sun.max.profile.ValueMetrics.IntegerDistribution;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Class maintaining class hierarchy and sub-typing relationships information of currently defined classes.
 * Dynamic compilers may issue queries related to these information and may make assumptions to apply certain optimizations
 * (e.g., devirtualization, type check elimination).
 * A dynamic compiler keeps track of the assumptions it makes when compiling a method in a {@link CiAssumptions} object.
 * The assumptions must be validated by the dependency manager before a target method is installed for uses. If the assumptions are incorrect, because of
 * changes that occurred in the class hierarchy since the assumptions were made, the target method is dropped and a new one must be produced.
 *
 * The dependencies manager is also responsible for recording changes to the class hierarchy and related information that
 * depends on sub-type relationships, and to drive code invalidation (e.g., using deoptimization) when assumptions made by existing code
 * become obsolete because of these changes.
 * The dependency manager must be informed of every changes to the class hierarchy when new classes are defined.
 *
 * @author Laurent Daynes.
 */
public final class ClassDependencyManager {

    private static final int HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK = 0;
    private static final int NO_CONCRETE_SUBTYPE_MARK = NULL_CLASS_ID;

    /**
     * Set accumulating all the methods invalidated during boot image generation.
     * The boot image generator must unlink these, and produce new target methods.
     */
    @HOSTED_ONLY
    public static HashSet<TargetMethod> invalidTargetMethods = new HashSet<TargetMethod>();

    /**
     * Read-write lock used to synchronize modifications to the class hierarchy with validation of
     * compiler assumptions.
     * New class definition must acquire the lock in write mode to exclude all concurrent updates to the class hierarchy,
     * and, more importantly, to exclude all concurrent validations or installations of validated
     * assumptions.
     * Validations and installations of assumptions acquire the lock in read mode to exclude all modifications to
     * class hierarchy information by concurrent class definition. This allows
     * multiple validation to be performed concurrently. Installation of dependencies in the dependency table
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

    private static final ObjectThreadLocal<UniqueConcreteMethodSearch> UCM_SEARCH_HELPER = new ObjectThreadLocal<UniqueConcreteMethodSearch>("UCM_SEARCH_HELPER", "thread local helper for class dependency management") {
        @Override
        public UniqueConcreteMethodSearch initialValue() {
            return new UniqueConcreteMethodSearch();
        }
    };

    /**
     * Helper class for statistics purposes only.
     */
    static class AssumptionCounter {
        int assumption = 0;
        int count = 0;

        AssumptionCounter(int assumption) {
            this.assumption = assumption;
        }

        @Override
        public int hashCode() {
            return assumption;
        }

        private static AssumptionCounter key = new AssumptionCounter(0);

        static void increaseCounter(int assumption, HashMap<AssumptionCounter, AssumptionCounter> counters) {
            key.assumption = assumption;
            AssumptionCounter counter = counters.get(key);
            if (counter == null) {
                counter = new AssumptionCounter(assumption);
                counters.put(counter, counter);
            }
            counter.count++;
        }
    }

    abstract static class ValidAssumptionsProcessor implements AssumptionProcessor {
        abstract void processInvalidated();
    }

    /**
     * An assumption processor that verifies that assumption made on a given type by a method remains valid if a new
     * concrete sub-type is added to the descendant of that type.
     */
    static final class AssumptionChecker extends ValidAssumptionsProcessor {
        /**
         * Type on which the assumption are made.
         */
        private RiType context;
        /**
         *
         */
        private RiType newConcreteSubtype;
        /**
         * Result of the check. If true, all assumptions are valid, otherwise, at least one was invalidated.
         */
        private boolean valid;

        /**
         * Reset the checker for a new verification.
         * @param context
         * @param newConcreteSubtype
         */
        void reset(RiType context, RiType newConcreteSubtype) {
            this.context = context;
            this.newConcreteSubtype = newConcreteSubtype;
            valid = true;
        }

        void reset() {
            valid = true;
        }

        @Override
        public boolean processUniqueConcreteSubtype(RiType context, RiType subtype) {
            // This is called only if assumption on a unique concrete sub-type of the context was recorded.
            // Adding a new concrete sub-type in this case always invalidate this assumption no matter what.
            FatalError.check(this.context == context && subtype != newConcreteSubtype, "can never happens");
            valid = false;
            return false;
        }

        @Override
        public boolean processUniqueConcreteMethod(RiMethod context, RiMethod method) {
            boolean assumptionIsValid = newConcreteSubtype.resolveMethodImpl(context) == method;
            valid = valid && assumptionIsValid;
            return valid;
        }

        @Override
        void processInvalidated() {
            valid = false;
        }

        boolean valid() {
            return valid;
        }
    }

    static final class AssumptionsPrinter extends ValidAssumptionsProcessor {
        final String indent = "    ";
        final PrintStream out;
        AssumptionsPrinter(PrintStream out) {
            this.out = out;
        }

        @Override
        public boolean processUniqueConcreteSubtype(RiType context, RiType subtype) {
            out.println(indent + context + " has unique concrete implementation" + context);
            return true;
        }

        @Override
        public boolean processUniqueConcreteMethod(RiMethod context, RiMethod method) {
            out.println(indent + method + " is a unique concrete method");
            return true;
        }

        @Override
        void processInvalidated() {
            out.println("assumptions have been invalidated");
        }

    }

    /**
     * Valid assumptions made by the compiler and used by a target method.
     * Instances of this class are recorded in {@link ClassDependencyManager#idToValidAssumptions}, and referenced,
     * via their index in that table, in the {@link DependentTargetMethodList} of {@link RiType} instances recorded in
     * {@link ClassDependencyManager#dependentTargetMethodTable}.
     */
    static class ValidAssumptions extends ClassHierarchyAssumptions {
        /**
         * Marker used to invalidate valid assumptions on compiled but not already installed target method.
         */
        static final short [] INVALIDATED = new short[0];

        static final short CLASS_FOLLOWS_FLAG = (short) (1 << 15);

        static boolean class_follows(short assumption) {
            return (assumption & CLASS_FOLLOWS_FLAG) != 0;
        }

        static short methodIndex(short assumption) {
            return (short) (assumption & ~CLASS_FOLLOWS_FLAG);
        }

        /**
         * The target method produced with the validated assumptions.
         * Needed to apply de-optimization should the assumptions be invalidated by some class definition event.
         */
        TargetMethod targetMethod;
        /**
         * Set of assumptions made by the target method, packed into a short array formatted as follows.
         * The first entry specifies the number of classes the target method makes assumptions on.
         * The next entries contains two short per class: the first one hold a class ID, the second one a short bit fields
         * that indicate the type of assumptions and the length of the assumption area of that class.
         * The bit fields position and width are described in {@link ClassAssumptionFlags}.
         */
        volatile short [] assumptions;

        /**
         * Index in {@link ClassDependencyManager#idToValidAssumptions} where this assumption is recorded.
         * Serves as unique identifier of the target method in per-type list of dependent target methods.
         */
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
            final int firstDependencyIndex = 1 + assumptions[0] * 2;
            for (int typeIndex = 1; typeIndex < firstDependencyIndex; typeIndex += 2) {
                dependentTargetMethodTable.removeDependentTargetMethod(id, ClassID.toClassActor(assumptions[typeIndex]));
            }
            // TODO: Revisit the following. the invalidate marker may not be needed if this is done under the write lock ...
            assumptions = INVALIDATED;
        }

        /**
         * Iterate over the assumptions made on the specified class.
         * @param classID identifier of the class
         * @param processor processor the assumptions are fed to
         */
        void iterate(int classID, ValidAssumptionsProcessor processor) {
            if (assumptions == INVALIDATED) {
                processor.processInvalidated();
                return;
            }
            final int numTypes = assumptions[0];
            final int firstDependencyIndex = 1 + numTypes * 2;
            int dependencyIndex = firstDependencyIndex;
            for (int i = 1; i < firstDependencyIndex; i += 2) {
                if (assumptions[i] == classID) {
                    final ClassActor classActor = ClassID.toClassActor(classID);
                    final short assumptionFlags = assumptions[i + 1];
                    if (CLASS_HAS_UCT.isBooleanFlagSet(assumptionFlags)) {
                        processor.processUniqueConcreteSubtype(classActor, ClassID.toClassActor(classActor.uniqueConcreteType));
                    }
                    if (CLASS_HAS_UCM.isBooleanFlagSet(assumptionFlags)) {
                        final int endAssumptions = dependencyIndex + CLASS_ASSUMPTIONS_LENGTH.getFlag(assumptionFlags);
                        boolean processorCarriesOn = true;
                        if (CLASS_LOCAL_UCM_ONLY.isBooleanFlagSet(assumptionFlags)) {
                            while (dependencyIndex < endAssumptions && processorCarriesOn) {
                                short methodIndex = assumptions[dependencyIndex++];
                                MethodActor method = classActor.localVirtualMethodActors()[methodIndex];
                                processorCarriesOn = processor.processUniqueConcreteMethod(method, method);
                            }
                            return;
                        }
                        while (dependencyIndex < endAssumptions && processorCarriesOn) {
                            short assumption = assumptions[dependencyIndex++];
                            if (class_follows(assumption)) {
                                final int methodIndex = methodIndex(assumption);
                                final int concreteTypeClassID = assumptions[dependencyIndex++];
                                final ClassActor concreteMethodHolder = ClassID.toClassActor(concreteTypeClassID);
                                MethodActor method = concreteMethodHolder.localVirtualMethodActors()[methodIndex];
                                MethodActor contextMethod = classActor.findLocalMethodActor(method.name, method.descriptor());
                                processorCarriesOn = processor.processUniqueConcreteMethod(contextMethod, method);
                            } else {
                                MethodActor method = classActor.localVirtualMethodActors()[assumption];
                                processorCarriesOn = processor.processUniqueConcreteMethod(method, method);
                            }
                        }
                    }
                    return;
                }
                dependencyIndex += CLASS_ASSUMPTIONS_LENGTH.getFlag(assumptions[i + 1]);
            }
            ClassActor classActor = ClassID.toClassActor(classID);
            FatalError.unexpected(classActor + "(id = " + classID + ") should be in valid assumptions (#" + id + ") for target method " + targetMethod);
        }

        // Stats support

        void countAssumptionsPerType(int classID, HashMap<AssumptionCounter, AssumptionCounter> assumptionCounters) {
            final int numAssumptions = assumptions[0];
            final int firstDependencyIndex = 1 + numAssumptions * 2;
            int dependencyIndex = firstDependencyIndex;
            for (int i = 1; i < firstDependencyIndex; i += 2) {
                if (assumptions[i] == classID) {
                    final short assumptionFlags = assumptions[i + 1];
                    if (CLASS_HAS_UCT.isBooleanFlagSet(assumptionFlags)) {
                        AssumptionCounter.increaseCounter(0, assumptionCounters);
                    }
                    final int endAssumptions = dependencyIndex + CLASS_ASSUMPTIONS_LENGTH.getFlag(assumptionFlags);
                    while (dependencyIndex < endAssumptions) {
                        AssumptionCounter.increaseCounter(assumptions[dependencyIndex++], assumptionCounters);
                    }
                    return;
                }
                dependencyIndex += CLASS_ASSUMPTIONS_LENGTH.getFlag(assumptions[i + 1]);
            }
            FatalError.unexpected("class ID should be in valid assumptions");
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

    static class DependentListIterator implements Iterator<ValidAssumptions> {
        int [] dependentLists;
        int current;
        int lastDependentIndex;

        DependentListIterator() {

        }

        void reset(DependentTargetMethodList list) {
            dependentLists = list.dependentLists;
            current = list.getFirstDependentIndex();
            lastDependentIndex = list.getLastDependentIndex();
        }

        @Override
        public boolean hasNext() {
            return current <= lastDependentIndex;
        }

        @Override
        public ValidAssumptions next() {
            return idToValidAssumptions.get(dependentLists[current++]);
        }

        @Override
        public void remove() {
            // rollback to the last returned.
            current--;
            if (current < lastDependentIndex) {
                // Move the last element at the freed position.
                dependentLists[current] = dependentLists[lastDependentIndex];
            }
            // Otherwise, current == lastDependentIndex and the following act the removal while updating the length of the list.
            dependentLists[0] = lastDependentIndex--;
        }
    }


    /**
     * List of dependent target methods recorded in the concurrent hash map backing a {@link DependentTargetMethodTable}.
     * Just a wrapper around an array of identifiers of {@link ValidAssumptions}.
     * These arrays aren't stored directly in the concurrent hash map to simplify concurrency when multiple
     * threads update the array and a resizing is needed, which would also requires the array to be replaced in the hash map.
     *
     * TargetMethod compilations add dependents to a list, but only class definers remove and iterate over them.
     * Note that synchronizations on the {@link ClassDependencyManager#classHierarchyLock} allows multiple concurrent addition
     * to a list, but guarantees exclusive access when removing or iterating over a list. Hence the sparse use of synchronization
     * here (only the {@link DependentTargetMethodList#add(int)} is synchronized.
     */
    static final class DependentTargetMethodList {
        /**
         * Array of valid assumptions identifier. The first entry records the effective size of the array,
         * which is always smaller or equal to that length of the array.
         */
        int [] dependentLists;
        DependentTargetMethodList(int dependent) {
            dependentLists = new int[] {2, dependent };
        }

        int numDependents() {
            return dependentLists[0] - 1;
        }

        int getFirstDependentIndex() {
            return 1;
        }

        int getLastDependentIndex() {
            return dependentLists[0] - 1;
        }

        /**
         * Addition of a dependent to the list.
         * This operation may be performed concurrently.
         * @param dependent
         */
        synchronized void add(int dependent) {
            if (MaxineVM.isDebug()) {
                FatalError.check(classHierarchyLock.getReadHoldCount() > 0, "must hold the class hierarchy lock in read mode");
            }
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

        void remove(int dependent) {
            if (MaxineVM.isDebug()) {
                FatalError.check(classHierarchyLock.isWriteLockedByCurrentThread(), "must hold the class hierarchy lock in read mode");
            }
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
    static final class DependentTargetMethodTable {
        /**
         * Initial capacity of the table. Based on statistics gathered over boot image generation and VM startup.
         * Needs to be adjusted depending on the dynamic compilation scheme.
         */
        static final int INITIAL_CAPACITY = 600;

        /**
         * Iterator over dependent list.
         */

        final ConcurrentHashMap<RiType, DependentTargetMethodList> typeToDependentTargetMethods =
            new ConcurrentHashMap<RiType, DependentTargetMethodList>(INITIAL_CAPACITY);

        void addDependentTargetMethod(int dependentID, Set<RiType> dependsOn) {
            for (RiType type : dependsOn) {
                DependentTargetMethodList list = typeToDependentTargetMethods.get(type);
                if (list == null) {
                    list = typeToDependentTargetMethods.putIfAbsent(type, new DependentTargetMethodList(dependentID));
                    if (list == null) {
                        trace(dependentID, type, 1);
                        return;
                    }
                    // We've lost a race with another concurrent thread adding a list for the same type.
                    // Fall off to add to that list.
                }
                list.add(dependentID);
                trace(dependentID, type, 2);
            }
        }

        void removeDependentTargetMethod(int dependentID, RiType type) {
            DependentTargetMethodList list = typeToDependentTargetMethods.get(type);
            if (list != null) {
                list.remove(dependentID);
                trace(dependentID, type, 3);
                if (list.numDependents() == 0) {
                    boolean removed = typeToDependentTargetMethods.remove(type, list);
                    if (MaxineVM.isDebug()) {
                        FatalError.check(removed, "dependent target method list removal should always succeed");
                    }
                    trace(dependentID, type, 4);
                }
                return;
            }
            FatalError.unexpected("dependent ID  should have been in the list");
        }

        private static int traceAtLevel = 1;
        private static void trace(int dependentID, RiType type, int action) {
            if (MaxineVM.isHosted()) {
                switch(action) {
                    case 1:
                        Trace.line(traceAtLevel, "*** Created DependentTargetList for " + type + " with dependent " + dependentID);
                        break;
                    case 2:
                        Trace.line(traceAtLevel, "*** Added dependent " + dependentID + " for " + type);
                        break;
                    case 3:
                        Trace.line(traceAtLevel, "*** Removed dependent " + dependentID + " for " + type);
                        break;
                    case 4:
                        Trace.line(traceAtLevel, "*** Deleted DependentTargetList for " + type);
                        break;
                }
            }
        }

        /**
         * Set that accumulates the target methods whose assumptions are invalidated by the current modification to the class hierarchy.
         */
        private HashSet<ValidAssumptions> invalidationSet = null;

        private final DependentListIterator dependentListIterator = new DependentListIterator();

        private final AssumptionChecker assumptionsChecker = new AssumptionChecker();

        /**
         * Removes any dependents on a type whose assumptions becomes invalid when a new sub-type is added to the type's descendants.
         * The {@link ValidAssumptions} instances representing the invalid dependents are accumulated in an invalidation set that must
         * be cleared from other types to complete the flushing of invalid dependents from a {@link DependentTargetMethodTable}.
         * @see DependentTargetMethodTable#clearInvalidatedAssumptions()
         * @param type the type whose dependents' assumptions need to be re-validated
         * @param concreteSubTypeID
         * @return
         */
        void flushInvalidDependentAssumptions(RiType type, RiType newConcreteSubType) {
            // We hold the classHierarchyLock in write mode.
            // This means there cannot be any concurrent modifications to the
            // typeToDependentTargetMethods and the DependentTargetMethodList
            // recorded in it.
            DependentTargetMethodList list = typeToDependentTargetMethods.get(type);
            if (list == null) {
                return;
            }
            final int classID = ((ClassActor) type).id;
            assumptionsChecker.reset(type, newConcreteSubType);
            dependentListIterator.reset(list);
            try {
                // Otherwise, iterate over the dependents and check their assumptions.
                while (dependentListIterator.hasNext()) {
                    ValidAssumptions validAssumptions = dependentListIterator.next();
                    validAssumptions.iterate(classID, assumptionsChecker);
                    if (!assumptionsChecker.valid()) {
                        dependentListIterator.remove();
                        if (invalidationSet == null) {
                            invalidationSet = new HashSet<ValidAssumptions>(4);
                        }
                        invalidationSet.add(validAssumptions);
                        assumptionsChecker.reset();
                    }
                }
            } catch (Throwable t) {
                FatalError.unexpected("DEBUG ME");
            }
        }

        /**
         * Clear any records of the invalidated target method accumulated in the invalidation set so far.
         *
         * @return an array holding all the invalidated TargetMethods for further processing, or null if none if
         * the invalidation set is empty.
         */
        TargetMethod [] clearInvalidatedAssumptions() {
            // Return an array of target method.
            if (invalidationSet == null || invalidationSet.isEmpty()) {
                return null;
            }
            TargetMethod [] invalidatedMethods = new TargetMethod[invalidationSet.size()];
            int i = 0;
            for (ValidAssumptions validAssumptions : invalidationSet) {
                invalidatedMethods[i++] = validAssumptions.targetMethod;
                validAssumptions.invalidate();
                clearValidAssumptions(validAssumptions);
            }
            invalidationSet = null;
            return invalidatedMethods;
        }

        /**
         * Dump statistics.
         */
        void printStatistics() {
            final AssumptionCounter uctCounter = new AssumptionCounter(0);
            IntegerDistribution numDistinctAssumptionsPerType = ValueMetrics.newIntegerDistribution("numDistinctAssumptionsPerType", 0, 20);
            IntegerDistribution numUCTAssumptionsPerType = ValueMetrics.newIntegerDistribution("numUCTAssumptionsPerType", 0, 20);
            IntegerDistribution numAssumptionsPerType = ValueMetrics.newIntegerDistribution("numAssumptionsPerType", 0, 20);
            IntegerDistribution numDependentsPerType = ValueMetrics.newIntegerDistribution("numDependentPerType", 0, 20);
            HashMap<AssumptionCounter, AssumptionCounter> assumptionsCounters = new HashMap<AssumptionCounter, AssumptionCounter>(20);

            for (RiType type : typeToDependentTargetMethods.keySet()) {
                final int classID = ((ClassActor) type).id;
                final DependentTargetMethodList list = typeToDependentTargetMethods.get(type);
                final int numDependents = list.numDependents();
                numDependentsPerType.record(numDependents);
                for (int i = 1; i <= numDependents; i++) {
                    idToValidAssumptions.get(list.dependentLists[i]).countAssumptionsPerType(classID, assumptionsCounters);
                }
                AssumptionCounter c = assumptionsCounters.remove(uctCounter);
                numUCTAssumptionsPerType.record(c != null ? c.count : 0);
                numDistinctAssumptionsPerType.record(assumptionsCounters.size());
                int totalTypeAssumptions = 0;
                for (AssumptionCounter co : assumptionsCounters.values()) {
                    totalTypeAssumptions += co.count;
                }
                numAssumptionsPerType.record(totalTypeAssumptions);
            }

            final PrintStream out = System.out;
            out.println("# types with dependent methods: " + typeToDependentTargetMethods.size());
            numDependentsPerType.report("# dependents / types", out);
            numAssumptionsPerType.report("# total assumptions / type", out);
            numDistinctAssumptionsPerType.report("# distinct assumptions / type", out);
            numUCTAssumptionsPerType.report("# UCT assumption / type, stream", out);
        }

        AssumptionsPrinter printer = new AssumptionsPrinter(System.out);

        void printDependents(RiType type, PrintStream out) {
            final int classID = ((ClassActor) type).id;
            DependentTargetMethodList list = typeToDependentTargetMethods.get(type);
            out.print("class " + type + " (id = " + classID + ") has ");
            if (list == null) {
                out.println("no dependents");
                return;
            }
            final int numDependents = list.numDependents();
            out.println(numDependents + " dependents");
            for (int i = 1; i <= numDependents; i++) {
                idToValidAssumptions.get(list.dependentLists[i]).iterate(classID, printer);
            }
        }

        void dump(PrintStream out) {
            out.println("================================================================");
            out.println("DependentTargetMethodTable has " + typeToDependentTargetMethods.size() + " entries");
            for (RiType type : typeToDependentTargetMethods.keySet()) {
                printDependents(type, out);
            }
            out.println("================================================================");
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

    private static void addLocalInterfaces(ClassActor classActor, HashSet<InterfaceActor> set) {
        for (InterfaceActor implemented : classActor.localInterfaceActors()) {
            set.add(implemented);
        }
    }
    /**
     * Propagate changes resulting from adding a new sub-type to a type up the ancestry of that type.
     * The ancestry of the type is walked up, assumptions made on sub-type relationships are re-evaluated, those that
     * became invalid are removed, and the unique concrete sub-type information is updated.
     * @param superType
     * @param newConcreteSubtype
     */
    private static void propagateConcreteSubType(ClassActor newConcreteSubType, ClassActor superType) {
        final int concreteSubtypeID = newConcreteSubType.id;
        ClassActor ancestor = superType;
        // Update all the ancestors without a concrete sub-type with the unique concrete sub-type.
        while (ancestor.uniqueConcreteType == NO_CONCRETE_SUBTYPE_MARK) {
            // No single concrete sub-type has been recorded for this ancestor yet.
            ancestor.uniqueConcreteType = concreteSubtypeID;
            ancestor = ancestor.superClassActor;
        }
        // We reached an ancestor with at least one concrete sub-type (either it is one itself,
        // or one or more of its other children has a concrete sub-type). From here on, we can only
        // have ancestors with some concrete sub-types.
        while (ancestor.uniqueConcreteType != HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
            dependentTargetMethodTable.flushInvalidDependentAssumptions(ancestor, newConcreteSubType);
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
        // However, there might still be unique concrete method assumptions that may be invalidated by the new concrete
        // sub-type. For example, consider A, super-class of concrete type B and C. The unique concrete method foo may be A.foo.
        // If a new type D, sub-type of C is added to the hierarchy and such that D overrides method foo, then any assumptions
        // made on foo being a unique concrete method of A should be invalidated.
        // Hence this loop.
        while (ancestor != null) {
            dependentTargetMethodTable.flushInvalidDependentAssumptions(ancestor, newConcreteSubType);
            ancestor = ancestor.superClassActor;
        }
    }

    private static void propagateConcreteSubType(ClassActor newConcreteSubType, InterfaceActor superType) {
        if (superType.uniqueConcreteType == NO_CONCRETE_SUBTYPE_MARK) {
            // No single concrete sub-type has been recorded for this ancestor yet.
            superType.uniqueConcreteType = newConcreteSubType.id;
        } else {
            if (superType.uniqueConcreteType != HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
                superType.uniqueConcreteType = HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK;
            }
            dependentTargetMethodTable.flushInvalidDependentAssumptions(superType, newConcreteSubType);
        }
    }

    private static void recordInstanceClassActor(ClassActor classActor) {
        FatalError.check(!classActor.hasSubclass(), "must be leaf at class definition time");
        // If new class is abstract, the unique concrete sub-type table relationship doesn't change.
        if (!classActor.isAbstract()) {
            // Recording is made at class definition time, when the class hasn't any sub-type yet.
            // So the unique concrete sub-type is oneself.
            classActor.uniqueConcreteType = classActor.id;
            ClassActor ancestor = classActor.superClassActor;
            if (ancestor == null) {
                // Can only be the class actor for java.lang.Object
                return;
            }
            HashSet<InterfaceActor> implementedInterfaces = classActor.getAllInterfaceActors();
            // Next, update unique concrete sub-type information of super-classes.
            propagateConcreteSubType(classActor, ancestor);

            // Last, update the unique concrete sub-type of the interfaces the class implements.
            for (ClassActor implemented : classActor.localInterfaceActors()) {
                propagateConcreteSubType(classActor, implemented);
            }

            for (InterfaceActor implemented : implementedInterfaces) {
                propagateConcreteSubType(classActor, implemented);
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
     * Adds the class to the class hierarchy.
     * This verifies that assumptions previously made by the compiler on the type hierarchy and invalidate
     * any target methods whose assumptions aren't valid anymore.
     *
     * @param classActor the class to be added to the global class hierarchy.
     */
    public static void addToHierarchy(ClassActor classActor) {
        classHierarchyLock.writeLock().lock();
        try {
            prependToSiblingList(classActor);
            recordUniqueConcreteSubtype(classActor);
            TargetMethod [] invalidatedTargetMethods = dependentTargetMethodTable.clearInvalidatedAssumptions();
            if (invalidatedTargetMethods != null) {
                invalidateTargetMethods(invalidatedTargetMethods);
            }
        } finally {
            classHierarchyLock.writeLock().unlock();
        }
    }

    static void invalidateTargetMethods(TargetMethod [] invalidatedTargetMethods) {
        if (MaxineVM.isHosted()) {
            for (TargetMethod targetMethod : invalidatedTargetMethods) {
                invalidTargetMethods.add(targetMethod);
                Trace.line(1, "*** Invalid target method: " + targetMethod);
            }
            return;
        }
        FatalError.unexpected("Invalidation of target methods with invalid assumptions not implemented");
    }


    /**
     * Flags stored at dedicated bit location within the entry in the {@link ValidAssumptions#assumptions} short array
     * describing a target method assumptions.
     * See {@link ValidAssumptions#assumptions}.
     */
    enum ClassAssumptionFlags {
        /**
         * Length of the area describing the assumptions made on the context class.
         */
        CLASS_ASSUMPTIONS_LENGTH(0, 13),
        /**
         * Flag indicating if some unique concrete methods are assumed for the context class.
         */
        CLASS_HAS_UCM(13, 1),
        /**
         * Flag indicating if all unique concrete method assumptions are local to the context class.
         */
        CLASS_LOCAL_UCM_ONLY(14, 1),
        /**
         * Flag indicating if the context class is assumed a unique concrete sub-type.
         */
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
    static class AssumptionValidator implements AssumptionProcessor {

        // AssumptionList recorded per type are made of methodID with a tag stored in the highest two bits,
        // so that:
        // assumption == 0 => UCT dependencies, no method ID
        // assumption > 0  => local only UCM, methodID == assumption
        // assumption < 0  => non local UCM, methodID = assumption &~ UNIQUE_CONCRETE_METHOD_DEP
        static final int UNIQUE_CONCRETE_TYPE_DEP = 0 << 30;
        static final int LEAF_CONCRETE_METHOD_DEP = 1 << 30;
        static final int UNIQUE_CONCRETE_METHOD_DEP = 2 << 30;
        static final int TAG_MASK = ~(3 << 30);

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
        private static final short UCT_ASSUMPTION_ONLY = CLASS_HAS_UCT.setBooleanFlag((short) 0);
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
                    if (assumption > 0) {
                        final int untaggedAssumption = assumption & TAG_MASK;
                        if (untaggedAssumption >= Short.MAX_VALUE) {
                            FatalError.unexpected("method index too large for packed assumptions");
                        }
                        // A local only unique concrete method.
                        assumptions[dependenciesIndex++] = (short) untaggedAssumption;
                    } else if (assumption < 0) {
                        final int untaggedAssumption = assumption & TAG_MASK;
                        if (untaggedAssumption >= Short.MAX_VALUE) {
                            FatalError.unexpected("method index too large for packed assumptions");
                        }
                        assumptions[dependenciesIndex++] = (short) (untaggedAssumption | ValidAssumptions.CLASS_FOLLOWS_FLAG);
                        assumptions[dependenciesIndex++] = (short) assumptionList[i++];
                        assumptionsSummary = CLASS_LOCAL_UCM_ONLY.clearBooleanFlag(assumptionsSummary);
                    } else {
                        assumptionsSummary = CLASS_HAS_UCT.setBooleanFlag(assumptionsSummary);
                    }
                }
                int assumptionLength = dependenciesIndex - firstDependencyIndex;
                FatalError.check(assumptionLength < MAX_ASSUMPTION_LENGTH, "too many assumptions.");
                if (assumptionLength == 0) {
                    assumptions[classIndex++] = UCT_ASSUMPTION_ONLY;
                } else {
                    assumptions[classIndex++] = CLASS_ASSUMPTIONS_LENGTH.setFlag(assumptionsSummary, (short) assumptionLength);
                }
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
            final int contextMethodIndex = ((MethodActor) method).memberIndex();
            if (context == method) {
                totalLocal++;
                if (end + 1 >= encodedDependencies.length) {
                    encodedDependencies = grow(contextHolder, encodedDependencies);
                }
                encodedDependencies[end++] = contextMethodIndex | AssumptionValidator.LEAF_CONCRETE_METHOD_DEP;
            } else {
                totalNonLocal++;
                if (end + 2 >= encodedDependencies.length) {
                    encodedDependencies = grow(contextHolder, encodedDependencies);
                }
                encodedDependencies[end++] = contextMethodIndex | AssumptionValidator.UNIQUE_CONCRETE_METHOD_DEP;
                encodedDependencies[end++] = ((ClassActor) method.holder()).id;
                if (MaxineVM.isHosted()) {
                    // Check that I can retrieve back the original assumption information.
                    int i = end - 2;
                    FatalError.check(ClassID.toClassActor(encodedDependencies[i + 1]).localVirtualMethodActors()[encodedDependencies[i] & TAG_MASK] == method,
                                    "incorrect encoding");
                }
            }
            encodedDependencies[0] = end;
            return true;
        }
    }

    static class CiAssumptionStatsGatherer implements AssumptionProcessor {
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

    /**
     * Dump the table in the log.
     */
    public static void dump() {
        classHierarchyLock.readLock().lock();
        dependentTargetMethodTable.dump(System.out);
        dependentTargetMethodTable.printStatistics();
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
