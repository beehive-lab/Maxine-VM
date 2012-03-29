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

import java.util.*;
import java.util.concurrent.locks.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.log.VMLog.*;
import com.sun.max.vm.log.hosted.*;

/**
 * The {@linkplain DependenciesManager} is the central point of control for the
 * management of dependencies in {@link TargetMethod compiled code}.
 * <p>
 * Compilers issue queries against the class hierarchy and encode the answers as {@link Dependencies dependencies}
 * which enable speculative optimizations (e.g., de-virtualization, type check elimination).
 * A compiler aggregates dependencies when compiling a method.
 * The dependencies must be validated before a target method is installed.
 * If validation fails (because of changes in the class hierarchy since the assumptions
 * were made), the target method is discarded and the compilation is repeated.
 * <p>
 * The set of assumptions/dependencies is open ended and each is managed by a {@link DependencyProcessor}.
 * The manager is responsible for recording the set of {@linkplain DependencyProcessor dependency processors},
 * and providing locking and logging support.
 * <p>
 * Dependencies can be invalidated for a number of reasons, the most common being
 * the addition of a new class to the system. The {@link #addToHierarchy(ClassActor)} method
 * is the method that should be called by the class definition system of the VM to report
 * the addition of a class.
 */
public final class DependenciesManager {
    /**
     * The collection of {@link DependencyProcessor} objects that handle specific types of {@link CiAssumptions.Assumption}.
     * This is immutable after image build and we keep an array of the values for fast, allocation free, iteration.
     */
    public static final Map<Class<? extends CiAssumptions.Assumption>, DependencyProcessor> dependencyProcessors = new HashMap<Class<? extends CiAssumptions.Assumption>, DependencyProcessor>();

    /**
     * The current packed encoding limits the number of {@linkplain DependencyProcessor} instances.
     */
    public static final int MAX_DEPENDENCY_PROCESSORS = 16;

    /**
     * A bit number that uniquely identifies the {@linkplain DependencyProcessor}.
     */
    private static int nextDependencyProcessorId;

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
     * Used during registration to accumulate {@linkplain DependencyProcessor} instances.
     */
    @HOSTED_ONLY
    private static final ArrayList<DependencyProcessor> dependencyProcessorList = new ArrayList<DependencyProcessor>();

    private static class InitializationCompleteCallback implements JavaPrototype.InitializationCompleteCallback {

        @Override
        public void initializationComplete() {
            dependencyProcessorsArray = new DependencyProcessor[dependencyProcessorList.size()];
            dependencyProcessorList.toArray(dependencyProcessorsArray);
        }
    }

    static {
        JavaPrototype.registerInitializationCompleteCallback(new InitializationCompleteCallback());
    }

    /**
     * The dependency processors, ordered by their id number.
     */
    @CONSTANT_WHEN_NOT_ZERO
    static DependencyProcessor[] dependencyProcessorsArray;

    /**
     * The data structure mapping classes to their dependents.
     */
    public static final ContextDependents contextDependents = new ContextDependents();

    /**
     * Registration of a new {@linkplain DependencyProcessor}.
     * @param dependencyProcessor the {@linkplain DependencyProcessor}
     * @param assumptionClass the associated subclass of {@linkplain CiAssumptions.Assumption}
     * @return the unique id for the processor
     */
    @HOSTED_ONLY
    static synchronized int registerDependencyProcessor(DependencyProcessor dependencyProcessor,
                    Class< ? extends CiAssumptions.Assumption> assumptionClass) {
        ProgramError.check(dependencyProcessors.put(assumptionClass, dependencyProcessor) == null);
        ProgramError.check(nextDependencyProcessorId < MAX_DEPENDENCY_PROCESSORS);
        dependencyProcessorList.add(dependencyProcessor);
        return nextDependencyProcessorId++;
    }

    /**
     * Adds a class to the class hierarchy.
     * This checks dependencies on the type hierarchy and invalidates all target methods whose dependencies are no longer valid.
     *
     * @param classActor the class to be added to the global class hierarchy
     */
    public static void addToHierarchy(ClassActor classActor) {
        classHierarchyLock.writeLock().lock();
        try {
            classActor.prependToSiblingList();
            ArrayList<Dependencies> invalidated = ConcreteTypeDependencyProcessor.recordUniqueConcreteSubtype(classActor);
            ConcreteTypeDependencyProcessor.invalidateDependencies(invalidated, classActor);
        } finally {
            classHierarchyLock.writeLock().unlock();
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
            Add, InvalidateDeps, InvalidateUCM,
            InvalidateUCT, Invalidated, Register, Remove;

            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = new int[] {0x1, 0x0, 0x1, 0x1, 0x1, 0x1, 0x1};

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
        public final void logInvalidateDeps(ClassActor type) {
            log(Operation.InvalidateDeps.ordinal(), classActorArg(type));
        }
        protected abstract void traceInvalidateDeps(ClassActor type);

        @INLINE
        public final void logInvalidateUCM(TargetMethod targetMethod, ClassActor context, MethodActor method, MethodActor impl) {
            log(Operation.InvalidateUCM.ordinal(), objectArg(targetMethod), classActorArg(context), methodActorArg(method), methodActorArg(impl));
        }
        protected abstract void traceInvalidateUCM(TargetMethod targetMethod, ClassActor context, MethodActor method, MethodActor impl);

        @INLINE
        public final void logInvalidateUCT(TargetMethod targetMethod, ClassActor context, ClassActor subtype) {
            log(Operation.InvalidateUCT.ordinal(), objectArg(targetMethod), classActorArg(context), classActorArg(subtype));
        }
        protected abstract void traceInvalidateUCT(TargetMethod targetMethod, ClassActor context, ClassActor subtype);

        @INLINE
        public final void logInvalidated(TargetMethod targetMethod, int id) {
            log(Operation.Invalidated.ordinal(), objectArg(targetMethod), intArg(id));
        }
        protected abstract void traceInvalidated(TargetMethod targetMethod, int id);

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

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //Add
                    traceAdd(toTargetMethod(r, 1), toInt(r, 2), toClassActor(r, 3));
                    break;
                }
                case 1: { //InvalidateDeps
                    traceInvalidateDeps(toClassActor(r, 1));
                    break;
                }
                case 2: { //InvalidateUCM
                    traceInvalidateUCM(toTargetMethod(r, 1), toClassActor(r, 2), toMethodActor(r, 3), toMethodActor(r, 4));
                    break;
                }
                case 3: { //InvalidateUCT
                    traceInvalidateUCT(toTargetMethod(r, 1), toClassActor(r, 2), toClassActor(r, 3));
                    break;
                }
                case 4: { //Invalidated
                    traceInvalidated(toTargetMethod(r, 1), toInt(r, 2));
                    break;
                }
                case 5: { //Register
                    traceRegister(toTargetMethod(r, 1), toInt(r, 2));
                    break;
                }
                case 6: { //Remove
                    traceRemove(toTargetMethod(r, 1), toInt(r, 2), toClassActor(r, 3));
                    break;
                }
            }
        }
    }

// END GENERATED CODE

}
