/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.actor.holder.ClassID.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Manages class hierarchy changes and its side effects in terms of assumptions
 * made on the class hierarchy by the compiler(s). Such an assumption is
 * a <i>dependency</i> of a compiled method.
 * <p>
 * Compilers issue queries against the class hierarchy and encode the answers as dependencies
 * which enable speculative optimizations (e.g., de-virtualization, type check elimination).
 * A dynamic compiler aggregates dependencies when compiling a method.
 * The dependencies must be validated before a target method is installed.
 * If validation fails (because of changes in the class hierarchy since the assumptions
 * were made), the target method is discarded and the compilation is repeated.
 * <p>
 * The dependency manager is also responsible for recording changes to the
 * class hierarchy and invalidating target methods whose dependencies are
 * invalidated as a result of the change to the class hierarchy. As such,
 * the dependency manager is notified of a class hierarchy change just before
 * it is published to the rest of the system during class
 * {@linkplain ClassRegistry#define(ClassActor) definition}.
 */
public final class DependenciesManager {

    private static final int HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK = 0;
    private static final int NO_CONCRETE_SUBTYPE_MARK = NULL_CLASS_ID;

    /**
     * Read-write lock used to synchronize modifications to the class hierarchy with validation of dependencies.
     * New class definition must acquire the lock in write mode to exclude all concurrent updates to the class hierarchy,
     * and, more importantly, to exclude all concurrent validations or installations of validated dependencies.
     * Validation and installation of dependencies acquire the lock in read mode to exclude all modifications to
     * class hierarchy information by concurrent class definition. This allows
     * multiple validation to be performed concurrently. Installation of dependencies in the dependency table
     * requires additional synchronization as it updates both the table and per class type dependency information.
     */
    public static final ReentrantReadWriteLock classHierarchyLock = new ReentrantReadWriteLock();

    /**
     * The data structure mapping classes to their dependents.
     */
    public static final ContextDependents contextDependents = new ContextDependents();

    /**
     * Helper class for statistics purposes only.
     */
    static class DependenciesCounter {
        int assumption = 0;
        int count = 0;

        DependenciesCounter(int assumption) {
            this.assumption = assumption;
        }

        @Override
        public int hashCode() {
            return assumption;
        }

        private static DependenciesCounter key = new DependenciesCounter(0);

        static void incCounter(int assumption, HashMap<DependenciesCounter, DependenciesCounter> counters) {
            key.assumption = assumption;
            DependenciesCounter counter = counters.get(key);
            if (counter == null) {
                counter = new DependenciesCounter(assumption);
                counters.put(counter, counter);
            }
            counter.count++;
        }
    }

    /**
     * Verifies dependencies on a given type when a concrete sub-type is added to the descendants of the type.
     */
    static final class DependencyChecker extends Dependencies.DependencyClosure {
        /**
         * Type on which the assumption are made.
         */
        private ClassActor context;

        /**
         * New concrete subtype of the context type being added to the class hierarchy.
         */
        private ClassActor concreteSubtype;

        /**
         * Result of the check. If true, all dependencies are valid, otherwise, at least one was invalidated.
         */
        private boolean valid;

        /**
         * Reset the checker for a new verification.
         * @param context
         * @param concreteSubtype
         */
        void reset(ClassActor context, ClassActor concreteSubtype) {
            this.classID = context.id;
            this.context = context;
            this.concreteSubtype = concreteSubtype;
            valid = true;
        }

        void reset() {
            valid = true;
        }

        @Override
        public boolean doConcreteSubtype(TargetMethod targetMethod, ClassActor context, ClassActor subtype) {
            // This is called only if assumption on a unique concrete sub-type of the context was recorded.
            // Adding a new concrete sub-type in this case always invalidate this assumption no matter what.
            assert this.context == context && subtype != concreteSubtype : "can never happen";
            valid = false;
            if (dependenciesLogger.enabled()) {
                dependenciesLogger.logInvalidateUCT(targetMethod, context, subtype);
            }

            return false;
        }

        @Override
        public boolean doConcreteMethod(TargetMethod targetMethod, MethodActor method, MethodActor impl, ClassActor context) {
            RiMethod newImpl = concreteSubtype.resolveMethodImpl(method);
            if (newImpl != impl) {
                valid = false;
                if (dependenciesLogger.enabled()) {
                    dependenciesLogger.logInvalidateUCM(targetMethod, context, method, impl);
                }
            }
            return valid;
        }

        @Override
        public void doInvalidated() {
            valid = false;
        }

        boolean valid() {
            return valid;
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
     * Validates a given set of assumptions and returns them encoded in a {@link Dependencies} object
     * if validation succeeds. If validation fails, {@link Dependencies#INVALID} is returned instead.
     * If {@code assumptions == null}, then {@code null} is returned.
     */
    public static Dependencies validateDependencies(CiAssumptions assumptions) {
        if (assumptions == null) {
            return null;
        }
        return Dependencies.validate(assumptions);
    }

    /**
     * Returns all the {@link TargetMethod} instances that inlined {@code inlinee}.
     * @param inlinee
     * @return
     */
    public static ArrayList<TargetMethod> getInliners(ClassMethodActor inlinee) {
        return contextDependents.getInliners(inlinee);
    }

    private static void dump(ClassActor classActor) {
        Log.print(classActor.id);
        Log.print(", ");
        Log.print(classActor.name());
        Log.print(", ");
        Log.print(classActor.superClassActor.id);
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
     * Propagates changes resulting from adding a new sub-type to a type up the ancestry of that type.
     * The ancestry of the type is traversed, dependencies on sub-type relationships are re-evaluated, those that
     * became invalid are removed, and the unique concrete sub-type information is updated.
     *
     * @param concreteType a new defined type that is (currently) a concrete type
     * @param ancestor a super class (direct or indirect) of {@code concreteType}
     * @param the dependencies already invalidated by this update
     * @return the dependencies invalidated by this update
     */
    private static ArrayList<Dependencies> propagateConcreteSubType(ClassActor concreteType, ClassActor ancestor, ArrayList<Dependencies> invalidated) {
        // Update all the ancestors without a concrete sub-type with the unique concrete sub-type.
        while (ancestor.uniqueConcreteType == NO_CONCRETE_SUBTYPE_MARK) {
            // No single concrete sub-type has been recorded for this ancestor yet.
            ancestor.uniqueConcreteType = concreteType.id;
            ancestor = ancestor.superClassActor;
        }
        // We reached an ancestor with at least one concrete sub-type (either it is one itself,
        // or one or more of its other children has a concrete sub-type). From here on, we can only
        // have ancestors with some concrete sub-types.
        while (ancestor.uniqueConcreteType != HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
            invalidated = contextDependents.flushInvalidDependencies(ancestor, concreteType, invalidated);
            // Reached an ancestor that had a unique-concrete sub-type.
            // This isn't true anymore, so update the mark.
            ancestor.uniqueConcreteType = HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK;
            ancestor = ancestor.superClassActor;
            if (ancestor == null) {
                break;
            }
            assert ancestor.uniqueConcreteType != NO_CONCRETE_SUBTYPE_MARK : "must have at least one concrete sub-type";
        }
        // We reached an ancestor with multiple concrete sub types. From here on, all ancestors can only have
        // more than one concrete sub-type. This is a terminal state that will not change until class
        // unloading occurs.
        // However, there might still be unique concrete method assumptions invalidated by the new concrete
        // sub-type. For example, consider A, super-class of concrete type B and C. The unique concrete method foo may be A.foo.
        // If a new type D, sub-type of C is added to the hierarchy and such that D overrides method foo, then any assumptions
        // made on foo being a unique concrete method of A should be invalidated.
        // Hence this loop.
        while (ancestor != null) {
            invalidated = contextDependents.flushInvalidDependencies(ancestor, concreteType, invalidated);
            ancestor = ancestor.superClassActor;
        }
        return invalidated;
    }

    private static ArrayList<Dependencies> propagateConcreteSubType(ClassActor concreteSubType, InterfaceActor superType, ArrayList<Dependencies> invalidated) {
        if (superType.uniqueConcreteType == NO_CONCRETE_SUBTYPE_MARK) {
            // No single concrete sub-type has been recorded for this ancestor yet.
            superType.uniqueConcreteType = concreteSubType.id;
        } else {
            if (superType.uniqueConcreteType != HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
                superType.uniqueConcreteType = HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK;
            }
            invalidated = contextDependents.flushInvalidDependencies(superType, concreteSubType, invalidated);
        }
        return invalidated;
    }

    /**
     * Initializes the unique concrete type for a class currently being {@linkplain ClassRegistry#define(ClassActor) defined}.
     * This also updates the unique concrete types recorded for all of the class's ancestors.
     *
     * @param classActor a class being defined
     * @return any dependencies invalidated by this update (this array may contain duplicates)
     */
    private static ArrayList<Dependencies> recordUniqueConcreteSubtype(ClassActor classActor) {
        ArrayList<Dependencies> invalidated = null;
        if (classActor.isInstanceClass()) {
            FatalError.check(!classActor.hasSubclass(), "must be leaf at class definition time");
            // If new class is abstract, the unique concrete sub-type table relationship doesn't change.
            if (!classActor.isAbstract()) {
                // Recording is made at class definition time, when the class hasn't any sub-types yet.
                // So the unique concrete sub-type is itself.
                classActor.uniqueConcreteType = classActor.id;
                if (classActor.superClassActor != null) {
                    // Next, update unique concrete sub-type information of super-classes.
                    invalidated = propagateConcreteSubType(classActor, classActor.superClassActor, invalidated);

                    for (InterfaceActor iface : classActor.getAllInterfaceActors()) {
                        invalidated = propagateConcreteSubType(classActor, iface, invalidated);
                    }
                }
            }
        } else if (classActor.isPrimitiveClassActor()) {
            // Primitive types are leaves, rooted directly at the Object type.
            // Nothing to propagate.
            classActor.uniqueConcreteType = classActor.id;
        } else if (classActor.isArrayClass()) {
            // Arrays are concrete types, regardless of whether their element type is a concrete type or not.
            // (i.e., one can create instance of T[] even if T is abstract).
            // Further,  T[] > S[] if  T > S.
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
        // everything else is abstract and therefore (i) doesn't have any concrete sub-type yet,
        // and (ii), cannot change the unique concrete sub-type of their super-types.
        return invalidated;
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

    /**
     * Utility to walk a type tree and find concrete method implementation for a given signature.
     */
    static final class UniqueConcreteMethodSearch {
        private MethodActor firstConcreteMethod;
        private boolean hasMoreThanOne;

        private boolean setConcreteMethod(MethodActor concreteMethod) {
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
        private boolean shouldSearchSubTypes(ClassActor root, MethodActor method) {
            final int uct = root.uniqueConcreteType;
            if (uct == NO_CONCRETE_SUBTYPE_MARK) {
                // No concrete type, no need to search sub-types.
                return false;
            }
            if (uct != HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK) {
                ClassActor concreteType = ClassID.toClassActor(uct);
                // This is the only concrete sub-type for the current context. The concrete method
                // is whatever concrete method is used by this concrete type.
                setConcreteMethod((MethodActor) concreteType.resolveMethodImpl(method));
                // found the single concrete method for this class actor. No need to search sub-types.
                return false;
            }
            // There is multiple concrete sub-type. Need to search them to determine unique concrete method.
            return true;
        }

        /**
         * Search the instance class tree rooted by the specified class actor for concrete implementations
         * of the specified method. Result of the search can be obtained via {{@link #uniqueConcreteMethod()}.
         *
         * @param root a tuple or hybrid class actor
         * @param method the method concrete implementation of are being searched
         */
        private void searchInstanceClassTree(ClassActor root, MethodActor method) {
            // Iterate over all concrete sub-types and determines if they all used the same method.
            assert root.isInstanceClass() : "must be an hybrid or tuple class actor";
            assert root.firstSubclassActorId != NULL_CLASS_ID : "must have at least one sub-class";
            assert firstConcreteMethod == null || !hasMoreThanOne;

            setConcreteMethod((MethodActor) root.resolveMethodImpl(method));
            if (hasMoreThanOne) {
                return;
            }
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
                classId = subType.nextSibling();
            } while(classId != NULL_CLASS_ID);
        }

        MethodActor uniqueConcreteMethod() {
            return hasMoreThanOne ? null : firstConcreteMethod;
        }

        MethodActor doIt(ClassActor root, MethodActor method) {
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

    public static MethodActor getUniqueConcreteMethod(ClassActor declaredType, MethodActor method) {
        // Default is to return null. See sub-classes of ClassActor for specific details.
        assert declaredType.isSubtypeOf(method.holder());
        classHierarchyLock.readLock().lock();
        try {
            return new UniqueConcreteMethodSearch().doIt(declaredType, method);
        } finally {
            classHierarchyLock.readLock().unlock();
        }
    }

    /**
     * Adds the class to the class hierarchy.
     * This checks dependencies on the type hierarchy and invalidates all target methods whose dependencies are no longer valid.
     *
     * @param classActor the class to be added to the global class hierarchy
     */
    public static void addToHierarchy(ClassActor classActor) {
        classHierarchyLock.writeLock().lock();
        try {
            classActor.prependToSiblingList();
            ArrayList<Dependencies> invalidated = recordUniqueConcreteSubtype(classActor);
            invalidateDependencies(invalidated, classActor);
        } finally {
            classHierarchyLock.writeLock().unlock();
        }
    }

    /**
     * Processes a list of invalidated dependencies, triggering deopt as necessary.
     *
     * @param invalidated the head of a {@link Dependencies} list (which may contain duplicates)
     * @param classActor the class to be added to the global class hierarchy
     */
    static void invalidateDependencies(ArrayList<Dependencies> invalidated, ClassActor classActor) {
        if (invalidated == null) {
            return;
        }
        if (dependenciesLogger.enabled()) {
            dependenciesLogger.logInvalidateDeps(classActor);
            for (Dependencies deps : invalidated) {
                deps.logInvalidated();
            }
        }

        ArrayList<TargetMethod> methods = new ArrayList<TargetMethod>(invalidated.size());
        for (Dependencies deps : invalidated) {
            if (deps.invalidate()) {
                if (MaxineVM.isHosted()) {
                    CompiledPrototype.invalidateTargetMethod(deps.targetMethod);
                }
                methods.add(deps.targetMethod);
            }
        }
        if (MaxineVM.isHosted()) {
            return;
        } else if (!methods.isEmpty()) {
            new Deoptimization(methods).go();
        }
    }

    /**
     * Dump the content of the dependent target method table to the specified {@link PrintStream}.
     * @param out output stream where to print the dump.
     */
    @HOSTED_ONLY
    public static void dump(PrintStream out) {
        classHierarchyLock.readLock().lock();
        try {
            contextDependents.dump(out);
            contextDependents.printStatistics(out);

        } finally {
            classHierarchyLock.readLock().unlock();
        }
    }

    /**
     * Dump class hierarchy Information to Log.
     */
    @HOSTED_ONLY
    public static void dumpClassHierarchy() {
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
                Log.println("class id, class name, parent class id, concrete subtype, concrete subtype class id");
            }
            final int largestClassId = ClassID.largestClassId();
            while (classId <= largestClassId) {
                ClassActor classActor;
                // Skip unused ids
                do {
                    classActor = ClassID.toClassActor(classId++);
                } while(classActor == null && classId <= largestClassId);
                if (classId > largestClassId) {
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

    // Logging

    @HOSTED_ONLY
    @VMLoggerInterface
    private interface DependenciesLoggerInterface {
        void add(
                        @VMLogParam(name = "targetMethod") TargetMethod targetMethod,
                        @VMLogParam(name = "id") int id,
                        @VMLogParam(name = "type") ClassActor type);

        void remove(
                        @VMLogParam(name = "targetMethod") TargetMethod targetMethod,
                        @VMLogParam(name = "id") int id,
                        @VMLogParam(name = "type") ClassActor type);

        void register(
                        @VMLogParam(name = "targetMethod") TargetMethod targetMethod,
                        @VMLogParam(name = "id") int id);

        void invalidateDeps(
                        @VMLogParam(name = "type") ClassActor type);

        void invalidated(
                        @VMLogParam(name = "targetMethod") TargetMethod targetMethod,
                        @VMLogParam(name = "id") int id);

        void invalidateUCT(
                        @VMLogParam(name = "targetMethod") TargetMethod targetMethod,
                        @VMLogParam(name = "context") ClassActor context,
                        @VMLogParam(name = "subtype") ClassActor subtype);

        void invalidateUCM(
                        @VMLogParam(name = "targetMethod") TargetMethod targetMethod,
                        @VMLogParam(name = "context") ClassActor context,
                        @VMLogParam(name = "method") MethodActor method,
                        @VMLogParam(name = "impl") MethodActor impl);
    }


    public static final DependenciesLogger dependenciesLogger = new DependenciesLogger();


    public static final class DependenciesLogger extends DependenciesLoggerAuto {
        DependenciesLogger() {
            super("Deps", "compilation dependencies.");
        }

        @Override
        protected void traceAdd(TargetMethod targetMethod, int id, ClassActor type) {
            traceAddRemove(id, type, "Added");
        }

        @Override
        protected void traceRegister(TargetMethod targetMethod, int id) {
            Dependencies deps = Dependencies.fromId(id);
            printPrefix();
            Log.println("Register " + deps.toString(true));
        }

        @Override
        protected void traceRemove(TargetMethod targetMethod, int id, ClassActor type) {
            traceAddRemove(id, type, "Removed");
        }

        @Override
        protected void traceInvalidated(TargetMethod targetMethod, int id) {
            printPrefix();
            Dependencies deps = Dependencies.fromId(id);
            Log.println("   " + deps);
        }

        @Override
        protected void traceInvalidateDeps(ClassActor type) {
            printPrefix();
            Log.println("adding " + type + " to the hierarchy invalidates:");
        }

        @Override
        protected void traceInvalidateUCT(TargetMethod targetMethod, ClassActor context, ClassActor subtype) {
            StringBuilder sb = invalidateSB(targetMethod, "UCT[").append(context);
            if (context != subtype) {
                sb.append(",").append(subtype);
            }
            sb.append(']');
            Log.println(sb.toString());
        }

        @Override
        protected void traceInvalidateUCM(TargetMethod targetMethod, ClassActor context, MethodActor method, MethodActor impl) {
            StringBuilder sb = invalidateSB(targetMethod, "UCM[").append(method);
            if (method != impl) {
                sb.append(",").append(impl);
            }
            sb.append("]");
            Log.println(sb.toString());
        }

        private static void traceAddRemove(int id, ClassActor type, String kind) {
            printPrefix();
            Dependencies deps = Dependencies.fromId(id);
            Log.println(kind + " dependency from " + deps + " to " + type);
        }

        private static StringBuilder invalidateSB(TargetMethod targetMethod, String iKind) {
            return new StringBuilder("DEPS: invalidated ").append(targetMethod).append(", invalid dep: ").append(iKind);
        }

        private static void printPrefix() {
            Log.print("DEPS: ");
        }
    }

// START GENERATED CODE
    private static abstract class DependenciesLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            Add, Register, Remove,
            Invalidated, InvalidateDeps, InvalidateUCT, InvalidateUCM;

            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = new int[] {0x1, 0x1, 0x1, 0x1, 0x0, 0x1, 0x1};

        protected DependenciesLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logAdd(TargetMethod targetMethod, int id, ClassActor type) {
            log(Operation.Add.ordinal(), objectArg(targetMethod), intArg(id), classActorArg(type));
        }
        protected abstract void traceAdd(TargetMethod targetMethod, int id, ClassActor type);

        @INLINE
        public final void logRegister(TargetMethod targetMethod, int id) {
            log(Operation.Register.ordinal(), objectArg(targetMethod), intArg(id));
        }
        protected abstract void traceRegister(TargetMethod targetMethod, int id);

        @INLINE
        public final void logRemove(TargetMethod targetMethod, int id, ClassActor type) {
            log(Operation.Remove.ordinal(), objectArg(targetMethod), intArg(id), classActorArg(type));
        }
        protected abstract void traceRemove(TargetMethod targetMethod, int id, ClassActor type);

        @INLINE
        public final void logInvalidated(TargetMethod targetMethod, int id) {
            log(Operation.Invalidated.ordinal(), objectArg(targetMethod), intArg(id));
        }
        protected abstract void traceInvalidated(TargetMethod targetMethod, int id);

        @INLINE
        public final void logInvalidateDeps(ClassActor type) {
            log(Operation.InvalidateDeps.ordinal(), classActorArg(type));
        }
        protected abstract void traceInvalidateDeps(ClassActor type);

        @INLINE
        public final void logInvalidateUCT(TargetMethod targetMethod, ClassActor context, ClassActor subtype) {
            log(Operation.InvalidateUCT.ordinal(), objectArg(targetMethod), classActorArg(context), classActorArg(subtype));
        }
        protected abstract void traceInvalidateUCT(TargetMethod targetMethod, ClassActor context, ClassActor subtype);

        @INLINE
        public final void logInvalidateUCM(TargetMethod targetMethod, ClassActor context, MethodActor method, MethodActor impl) {
            log(Operation.InvalidateUCM.ordinal(), objectArg(targetMethod), classActorArg(context), objectArg(method), objectArg(impl));
        }
        protected abstract void traceInvalidateUCM(TargetMethod targetMethod, ClassActor context, MethodActor method, MethodActor impl);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //Add
                    traceAdd(toTargetMethod(r, 1), toInt(r, 2), toClassActor(r, 3));
                    break;
                }
                case 1: { //Register
                    traceRegister(toTargetMethod(r, 1), toInt(r, 2));
                    break;
                }
                case 2: { //Remove
                    traceRemove(toTargetMethod(r, 1), toInt(r, 2), toClassActor(r, 3));
                    break;
                }
                case 3: { //Invalidated
                    traceInvalidated(toTargetMethod(r, 1), toInt(r, 2));
                    break;
                }
                case 4: { //InvalidateDeps
                    traceInvalidateDeps(toClassActor(r, 1));
                    break;
                }
                case 5: { //InvalidateUCT
                    traceInvalidateUCT(toTargetMethod(r, 1), toClassActor(r, 2), toClassActor(r, 3));
                    break;
                }
                case 6: { //InvalidateUCM
                    traceInvalidateUCM(toTargetMethod(r, 1), toClassActor(r, 2), toMethodActor(r, 3), toMethodActor(r, 4));
                    break;
                }
            }
        }
    }

// END GENERATED CODE

}
