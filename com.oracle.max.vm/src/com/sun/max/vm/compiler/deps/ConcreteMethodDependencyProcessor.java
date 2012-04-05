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

import static com.sun.max.vm.actor.holder.ClassID.*;
import static com.sun.max.vm.actor.holder.ClassActor.HAS_MULTIPLE_CONCRETE_SUBTYPE_MARK;
import static com.sun.max.vm.actor.holder.ClassActor.NO_CONCRETE_SUBTYPE_MARK;
import static com.sun.max.vm.compiler.deps.DependenciesManager.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAssumptions.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deps.Dependencies.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;

/**
 * {@link DependencyProcessor} for unique concrete method dependency.
 *
 * Statistics from boot image generation showed that the vast majority of target methods have a single concrete
 * method dependency, most of which are on leaf methods, i.e., wherein the context is the holder of the concrete method.
 * The encoding of the dependencies is optimized for this case.
 *
 * The format of the packed data for this dependency is as follows:
 * <pre>
     *     concrete_methods {
     *         short length;        // length of 'deps'
     *         short deps[length];  // array of local_method_dep and non_local_concrete_method_dep structs (defined below)
     *     }
     *
     *     // identifies a concrete method in the same class as 'type'
     *     local_method_dep {
     *         short mindex;  // positive; the member index of the method in 'type'
     *     }
     *
     *     // identifies
     *     non_local_concrete_method_dep {
     *         short mindex;        // negative; (-mindex - 1) is the member index of the implementation/concrete method in 'implHolder'
     *         short implHolder;    // identifier of the class in which the implementation/concrete method is defined
     *         short methodHolder;  // identifier of the class in which the declared method is defined
     *     }
     *
 * </pre>
 */
public class ConcreteMethodDependencyProcessor extends DependencyProcessor {

    /**
     * Essentially the Maxine specific mirror of {@link CiAssumptions.ConcreteMethod}.
     * Implement this interface in a subclass of {@link DependencyVisitor} to
     * process these dependencies.
     */
    public interface ConcreteMethodDependencyProcessorVisitor extends DependencyProcessorVisitor {
        /**
         * Processes a unique concrete method dependency.
         *
         * @param targetMethod the method compiled with this dependency
         * @param method a virtual or interface method
         * @param impl the method assumed to be the unique concrete implementation of {@code method}
         * @param context class context
         * @return {@code true} to continue the iteration, {@code false} to terminate it
         */
        boolean doConcreteMethod(TargetMethod targetMethod, MethodActor method, MethodActor impl, ClassActor context);

    }

    static class ToStringConcreteMethodDependencyProcessorVisitor extends ToStringDependencyProcessorVisitor implements ConcreteMethodDependencyProcessorVisitor {
        public boolean doConcreteMethod(TargetMethod targetMethod, MethodActor method, MethodActor impl, ClassActor context) {
            sb.append(" UCM[").append(method);
            if (method != impl && impl.holder() != context) {
                sb.append(",").append(context);
                sb.append(",").append(impl);
            }
            sb.append(']');
            return true;
        }
    }

    static final ToStringConcreteMethodDependencyProcessorVisitor toStringConcreteDependencyProcessorVisitor = new ToStringConcreteMethodDependencyProcessorVisitor();

    @Override
    protected ToStringDependencyProcessorVisitor getToStringDependencyProcessorVisitor() {
        return toStringConcreteDependencyProcessorVisitor;
    }

    private static final ConcreteMethodDependencyProcessor singleton = new ConcreteMethodDependencyProcessor();

    private static ThreadLocal<UniqueConcreteMethodSearch> ucms = new ThreadLocal<UniqueConcreteMethodSearch>() {
        @Override
        public UniqueConcreteMethodSearch initialValue() {
            return new UniqueConcreteMethodSearch();
        }
    };

    private ConcreteMethodDependencyProcessor() {
        super(CiAssumptions.ConcreteMethod.class);
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

    @Override
    protected boolean validate(Assumption assumption, ClassDeps classDeps) {
        ClassActor contextClassActor = (ClassActor) ((ContextAssumption) assumption).context;
        ConcreteMethod cm = (ConcreteMethod) assumption;
        if (ucms.get().doIt(contextClassActor, (MethodActor) cm.dependee) != cm.dependee) {
            return false;
        }

        MethodActor impl = (MethodActor) cm.dependee;
        MethodActor method = (MethodActor) cm.method;
        short mIndex = Dependencies.getMIndex(impl);
        if (impl == method && impl.holder() == contextClassActor) {
            classDeps.add(this, mIndex);
        } else {
            classDeps.add(this, (short) -(mIndex + 1));
            classDeps.add(this, (short) impl.holder().id);
            classDeps.add(this, (short) method.holder().id);
        }
        return true;
    }

    @Override
    protected DependencyProcessorVisitor match(DependencyVisitor dependencyVisitor) {
        return dependencyVisitor instanceof ConcreteMethodDependencyProcessorVisitor ? (ConcreteMethodDependencyProcessorVisitor) dependencyVisitor : null;
    }

    @Override
    protected int visit(DependencyProcessorVisitor dependencyProcessorVisitor, ClassActor context, Dependencies dependencies, int index) {
        ConcreteMethodDependencyProcessorVisitor ucmVisitor = (ConcreteMethodDependencyProcessorVisitor) dependencyProcessorVisitor;
        int i = index;
        int mindex = dependencies.packed[i++];
        MethodActor impl = null;
        MethodActor method = null;
        if (mindex >= 0) {
            impl = ucmVisitor == null ? null : MethodID.toMethodActor(MethodID.fromWord(MemberID.create(context.id, mindex)));
            method = impl;
        } else {
            int implHolder = dependencies.packed[i++];
            int methodHolderID = dependencies.packed[i++];
            if (ucmVisitor != null) {
                impl = MethodID.toMethodActor(MethodID.fromWord(MemberID.create(implHolder, -mindex - 1)));
                ClassActor methodHolder = ClassID.toClassActor(methodHolderID);
                method = methodHolder.findLocalMethodActor(impl.name, impl.descriptor());
            }
        }
        if (ucmVisitor != null) {
            if (!ucmVisitor.doConcreteMethod(dependencies.targetMethod, method, impl, context)) {
                return -1;
            }
        }
        return i;
    }

    public static MethodActor getUniqueConcreteMethod(ClassActor declaredType, MethodActor method) {
        // Default is to return null. See sub-classes of ClassActor for specific details.
        assert declaredType.isSubtypeOf(method.holder());
        classHierarchyLock.readLock().lock();
        try {
            return new ConcreteMethodDependencyProcessor.UniqueConcreteMethodSearch().doIt(declaredType, method);
        } finally {
            classHierarchyLock.readLock().unlock();
        }
    }


}
