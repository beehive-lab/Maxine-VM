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

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAssumptions.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deps.Dependencies.DependencyVisitor;
import com.sun.max.vm.compiler.deps.ContextDependents.*;
import com.sun.max.vm.compiler.deps.Dependencies.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.ti.*;

/**
 * {@link DependencyProcessor} for inlined methods.
 *
 * The format of the packed data for this dependency is as follows:
 * <pre>
     *     inlined_methods {
     *         short length;        // length of 'deps'
     *         short deps[length];  // array of local_inlined_method_dep and non_local_inlined_method_dep structs (defined below)
     *                           // the context class is always the holder of an inlining method, and the {@link ClassMethodActor}
     *                           // of the {@link TargetMethod} always denotes the inlining method, so it does not need to be recorded
     *                           // in the dependency data. N.B. This means that the inlining method is lost
     *                           // until the TargetMethod is {@link Dependencies#registerValidatedTarget(Dependencies, TargetMethod) set}
     *     }
     *
     *     // identifies an inlined method in same class as inliner
     *     local_inlined_method_dep {
     *         short inlinee_mindex // positive; inlinee_mindex is the member index of the inlinee method in same class
     *     }
     *
     *     // identifies an inlined method in different class to inliner
     *     non_local_inlined_method_dep {
     *         short inlinee_mindex // negative; (-mindex - 1) is the member index of the inlinee method in inlineHolder
     *         short inlineHolder;  // identifier of the class in which the inlinee method is defined
     *     }
 * </pre>
 */
public class InlinedMethodDependencyProcessor extends DependencyProcessor {

    /**
     * Essentially the Maxine specific mirror of {@link InlinedMethod}.
     * Implement this interface in a subclass of {@link DependencyVisitor} to
     * process these dependencies.
     */
    public interface InlinedMethodDependencyProcessorVisitor extends DependencyProcessorVisitor {
        /**
         * Process an inlined method dependency.
         * @param targetMethod the method compiled with this dependency
         * @param method the inlining method
         * @param inlinee the inlined method (inlinee)
         * @param context class context
         * @return {@code true} to continue the iteration, {@code false} to terminate it
         */
        boolean doInlinedMethod(TargetMethod targetMethod, ClassMethodActor method, ClassMethodActor inlinee, ClassActor context);

    }

    static class ToStringInlinedMethodDependencyProcessorVisitor extends ToStringDependencyProcessorVisitor implements InlinedMethodDependencyProcessorVisitor {
        @Override
        public boolean doInlinedMethod(TargetMethod targetMethod, ClassMethodActor method, ClassMethodActor inlinee, ClassActor context) {
            sb.append(" INL[").append(inlinee).append(']');
            return true;
        }
    }

    static final ToStringInlinedMethodDependencyProcessorVisitor toStringInlinedMethodDependencyProcessorVisitor = new ToStringInlinedMethodDependencyProcessorVisitor();

    @Override
    protected ToStringDependencyProcessorVisitor getToStringDependencyProcessorVisitor(StringBuilder sb) {
        return toStringInlinedMethodDependencyProcessorVisitor.setStringBuilder(sb);
    }

    private static final InlinedMethodDependencyProcessor singleton = new InlinedMethodDependencyProcessor();

    private InlinedMethodDependencyProcessor() {
        super(CiAssumptions.InlinedMethod.class);
    }

    @Override
    protected boolean validate(Assumption assumption, ClassDeps classDeps) {
        ClassActor contextClassActor = (ClassActor) ((ContextAssumption) assumption).context;
        InlinedMethod inlineMethod = (InlinedMethod) assumption;
        ClassMethodActor inlinee = (ClassMethodActor) inlineMethod.dependee;
        if (VMTI.handler().hasBreakpoints(inlinee)) {
            return false;
        }
        short inlineeMIndex = Dependencies.getMIndex(inlinee);
        if (inlinee.holder() == contextClassActor) {
            // inlinee in same class, use shorter form
            classDeps.add(this, inlineeMIndex);
        } else {
            classDeps.add(this, (short) -(inlineeMIndex + 1));
            classDeps.add(this, (short) inlinee.holder().id);
        }
        return true;
    }

    @Override
    protected DependencyProcessorVisitor match(DependencyVisitor dependencyVisitor) {
        return dependencyVisitor instanceof InlinedMethodDependencyProcessorVisitor ? (InlinedMethodDependencyProcessorVisitor) dependencyVisitor : null;
    }

    @Override
    protected int visit(DependencyProcessorVisitor dependencyProcessorVisitor, ClassActor context, Dependencies dependencies, int index) {
        InlinedMethodDependencyProcessorVisitor inlineVisitor = (InlinedMethodDependencyProcessorVisitor) dependencyProcessorVisitor;
        int i = index;
        int mindex = dependencies.packed[i++];
        int inlineeMIndex = mindex;
        ClassActor inlineeHolder;
        if (inlineeMIndex >= 0) {
            inlineeHolder = context;
        } else {
            inlineeMIndex = -inlineeMIndex - 1;
            int inlineeHolderID = dependencies.packed[i++];
            inlineeHolder = inlineVisitor != null ? ClassIDManager.toClassActor(inlineeHolderID) : null;
        }
        if (inlineVisitor != null) {
            ClassMethodActor inliningMethod = dependencies.targetMethod.classMethodActor;
            ClassMethodActor inlineeMethod = (ClassMethodActor) MethodID.toMethodActor(MethodID.fromWord(MemberID.create(inlineeHolder.id, inlineeMIndex)));
            if (!inlineVisitor.doInlinedMethod(dependencies.targetMethod, inliningMethod, inlineeMethod, context)) {
                return -1;
            }
        }
        return i;
    }

    private static final ArrayList<TargetMethod> EMPTY = new ArrayList<TargetMethod>(0);

    /**
     * Returns all the {@link TargetMethod} instances that inlined {@code inlineeToCheck}.
     *
     * @param inlineeToCheck
     */
    public static ArrayList<TargetMethod> getInliners(final ClassMethodActor inlineeToCheck) {
        if (inlineeToCheck.isDeclaredNeverInline()) {
            return EMPTY;
        }
        final ArrayList<TargetMethod> result = new ArrayList<TargetMethod>();
        classHierarchyLock.readLock().lock();
        try {
            for (DSet dset : ContextDependents.map.values()) {
                synchronized (dset) {
                    int i = 0;
                    while (i < dset.size()) {
                        final Dependencies deps = dset.getDeps(i);
                        final int fi = i;
                        deps.visit(new FindInliners() {

                            @Override
                            public boolean doInlinedMethod(TargetMethod targetMethod, ClassMethodActor method, ClassMethodActor inlinee, ClassActor context) {
                                if (inlinee == inlineeToCheck) {
                                    inlineLogger.logDoInlinedMethod(fi, deps, targetMethod, method, inlinee, context);
                                    add(targetMethod);
                                }
                                return true;
                            }

                            private boolean add(TargetMethod targetMethod) {
                                for (TargetMethod tm : result) {
                                    if (tm == targetMethod) {
                                        return false;
                                    }
                                }
                                result.add(targetMethod);
                                return true;
                            }
                        });
                        i++;
                    }
                }
            }
            return result;
        } finally {
            classHierarchyLock.readLock().unlock();
        }
    }

    private static abstract class FindInliners extends Dependencies.DependencyVisitor implements InlinedMethodDependencyProcessor.InlinedMethodDependencyProcessorVisitor {

    }

    @VMLoggerInterface(noTrace = true)
    private interface InlineLoggerInterface {
        void doInlinedMethod(int i, Dependencies deps, TargetMethod targetMethod, ClassMethodActor method, ClassMethodActor inlinee, ClassActor context);
    }

    private static final InlineLogger inlineLogger = new InlineLogger();

    private static class InlineLogger extends InlineLoggerAuto {
        InlineLogger() {
            super("GetInliners", "");
        }
    }


// START GENERATED CODE
    private static abstract class InlineLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            DoInlinedMethod;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = new int[] {0x6};

        protected InlineLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logDoInlinedMethod(int arg1, Dependencies arg2, TargetMethod arg3, ClassMethodActor arg4, ClassMethodActor arg5,
                ClassActor arg6) {
            log(Operation.DoInlinedMethod.ordinal(), intArg(arg1), objectArg(arg2), objectArg(arg3), methodActorArg(arg4), methodActorArg(arg5),
                classActorArg(arg6));
        }
    }

// END GENERATED CODE
}
