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

import static com.sun.max.vm.actor.holder.ClassActor.HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK;
import static com.sun.max.vm.actor.holder.ClassActor.NO_CONCRETE_SUBTYPE_MARK;
import static com.sun.max.vm.compiler.deps.DependenciesManager.*;
import static com.sun.max.vm.compiler.deps.ContextDependents.map;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAssumptions.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.deps.ContextDependents.*;
import com.sun.max.vm.compiler.deps.Dependencies.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * {@linkplain DependencyProcessor} for {@link ConcreteSubtype}.
 *
 * This dependency has no additional data in the packed format.
 */
public class ConcreteTypeDependencyProcessor extends DependencyProcessor {

    /**
     * Essentially the Maxine specific mirror of {@link ConcreteSubtype}.
     * Implement this interface in a subclass of {@link DependencyVisitor} to
     * process these dependencies.
     */
    public interface ConcreteTypeDependencyProcessorVisitor extends DependencyProcessorVisitor {
        /**
         * Processes a unique concrete subtype dependency.
         *
         * @param targetMethod the method compiled with this dependency
         * @param context
         * @param subtype the subtype assumed to be the unique concrete subtype of {@code context}
         *
         * @return {@code true} to continue the iteration, {@code false} to terminate it
         */
        boolean doConcreteSubtype(TargetMethod targetMethod, ClassActor context, ClassActor subtype);
    }

    static class ToStringConcreteTypeDependencyProcessorVisitor extends ToStringDependencyProcessorVisitor implements ConcreteTypeDependencyProcessorVisitor {
        @Override
        public boolean doConcreteSubtype(TargetMethod targetMethod, ClassActor context, ClassActor subtype) {
            sb.append(" UCT[").append(context);
            if (context != subtype) {
                sb.append(",").append(subtype);
            }
            sb.append(']');
            return true;
        }
    }

    static final ToStringConcreteTypeDependencyProcessorVisitor toStringConcreteTypeDependencyProcessorVisitor = new ToStringConcreteTypeDependencyProcessorVisitor();

    @Override
    protected ToStringDependencyProcessorVisitor getToStringDependencyProcessorVisitor(StringBuilder sb) {
        return toStringConcreteTypeDependencyProcessorVisitor.setStringBuilder(sb);
    }

    private static final ConcreteTypeDependencyProcessor singleton = new ConcreteTypeDependencyProcessor();

    private ConcreteTypeDependencyProcessor() {
        super(CiAssumptions.ConcreteSubtype.class, false);
    }

    @Override
    protected boolean validate(CiAssumptions.Assumption assumption, ClassDeps packedDeps) {
        ClassActor contextClassActor = (ClassActor) ((ContextAssumption) assumption).context;
        ConcreteSubtype cs = (ConcreteSubtype) assumption;
        final ClassActor subtype = (ClassActor) cs.subtype;
        return contextClassActor.uniqueConcreteType == subtype.id;
    }

    @Override
    protected DependencyProcessorVisitor match(DependencyVisitor dependencyVisitor) {
        return dependencyVisitor instanceof ConcreteTypeDependencyProcessorVisitor ? (ConcreteTypeDependencyProcessorVisitor) dependencyVisitor : null;
    }

    @Override
    protected int visit(DependencyProcessorVisitor dependencyProcessorVisitor, ClassActor context, Dependencies dependencies, int index) {
        ConcreteTypeDependencyProcessorVisitor uctVisitor = (ConcreteTypeDependencyProcessorVisitor) dependencyProcessorVisitor;
        if (uctVisitor != null) {
            if (!uctVisitor.doConcreteSubtype(dependencies.targetMethod, context, ClassID.toClassActor(context.uniqueConcreteType))) {
                return -1;
            }
        }
        return index; // no data to process
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
     * Initializes the unique concrete type for a class currently being {@linkplain ClassRegistry#define(ClassActor) defined}.
     * This also updates the unique concrete types recorded for all of the class's ancestors.
     *
     * @param classActor a class being defined
     * @return any dependencies invalidated by this update (this array may contain duplicates)
     */
    static ArrayList<Dependencies> recordUniqueConcreteSubtype(ClassActor classActor) {
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

    /**
     * Propagates changes resulting from adding a new sub-type to a type up the ancestry of that type.
     * The ancestry of the type is traversed, dependencies on sub-type relationships are re-evaluated, those that
     * became invalid are removed, and the unique concrete sub-type information is updated.
     *
     * @param concreteType a new defined type that is (currently) a concrete type
     * @param ancestor a super class (direct or indirect) of {@code concreteType}
     * @param invalidated the dependencies already invalidated by this update
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
            invalidated = flushInvalidDependencies(ancestor, concreteType, invalidated);
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
            invalidated = flushInvalidDependencies(ancestor, concreteType, invalidated);
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
            invalidated = flushInvalidDependencies(superType, concreteSubType, invalidated);
        }
        return invalidated;
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
     * Verifies dependencies on a given type when a concrete sub-type is added to the descendants of the type.
     */
    static final class DependencyChecker extends Dependencies.DependencyVisitor
            implements ConcreteMethodDependencyProcessor.ConcreteMethodDependencyProcessorVisitor, ConcreteTypeDependencyProcessor.ConcreteTypeDependencyProcessorVisitor {
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

    private static final DependencyChecker checker = new DependencyChecker();

    /**
     * Removes any dependencies that are invalidated by a class hierarchy change.
     *
     * @param ancestor a type for which dependencies need to be re-validated
     * @param concreteType the new sub-type causing the hierarchy change
     * @param invalidated a list of invalidated dependencies (may be null)
     * @return the invalidated dependencies
     */
    static ArrayList<Dependencies> flushInvalidDependencies(ClassActor ancestor, ClassActor concreteType, ArrayList<Dependencies> invalidated) {
        assert classHierarchyLock.isWriteLockedByCurrentThread() : "must hold the class hierarchy lock in write mode";
        // We hold the classHierarchyLock in write mode.
        // This means there cannot be any concurrent modifications to the dependencies.
        DSet dset = map.get(ancestor);
        if (dset == null) {
            return invalidated;
        }
        checker.reset(ancestor, concreteType);
        int i = 0;
        while (i < dset.size()) {
            Dependencies deps = dset.getDeps(i);
            checker.reset();
            deps.visit(checker);
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
        if (dset.size() == 0) {
            map.remove(ancestor);
        }

        return invalidated;
    }


}
