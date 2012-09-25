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
package com.sun.max.vm.ext.jvmti;

import static com.sun.max.vm.compiler.deps.DependenciesManager.*;

import com.sun.cri.ci.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deps.*;
import com.sun.max.vm.compiler.deps.ContextDependents.DSet;
import com.sun.max.vm.compiler.deps.Dependencies.*;
import com.sun.max.vm.compiler.target.*;


/**
 * Encodes that a {@link TargetMethod} depends on (was compiled with) a specific set of JVMTI events and breakpoints.
 */
public class JVMTI_DependencyProcessor extends DependencyProcessor {

    private static class JVMTI_Assumption extends CiAssumptions.ContextAssumption {
        private long eventSettings;
        private long[] breakpoints;

        JVMTI_Assumption(ClassActor context, long eventSettings, long[] breakpoints) {
            super(context);
            this.eventSettings = eventSettings;
            this.breakpoints = breakpoints;
        }
    }

    public interface JVMTI_DependencyProcessorVisitor extends DependencyProcessorVisitor {
        boolean doCheckSettings(TargetMethod targetMethod, long eventSettings, long[] breakpoints);
    }

    public static class ToStringJVMTI_DependencyProcessorVisitor extends ToStringDependencyProcessorVisitor implements JVMTI_DependencyProcessorVisitor {
        @Override
        public boolean doCheckSettings(TargetMethod targetMethod, long eventSettings, long[] breakpoints) {
            sb.append("JVMTI[");
            sb.append(Long.toHexString(eventSettings));
            for (long b : breakpoints) {
                sb.append(' ');
                sb.append(b);
            }
            sb.append(']');
            return true;
        }

    }

    public static final ToStringJVMTI_DependencyProcessorVisitor toStringJVMTI_DependencyProcessorVisitor = new ToStringJVMTI_DependencyProcessorVisitor();

    @Override
    protected ToStringDependencyProcessorVisitor getToStringDependencyProcessorVisitor(StringBuilder sb) {
        return toStringJVMTI_DependencyProcessorVisitor;
    }

    private static final JVMTI_DependencyProcessor singleton = new JVMTI_DependencyProcessor();

    public static Dependencies recordInstrumentation(ClassActor context, long eventSettings, long[] breakpoints) {
        CiAssumptions assumptions = new CiAssumptions();
        assumptions.record(new JVMTI_Assumption(context, eventSettings, breakpoints));
        return Dependencies.validateDependencies(assumptions);
    }

    JVMTI_DependencyProcessor() {
        super(JVMTI_Assumption.class);
    }

    @Override
    protected boolean validate(CiAssumptions.Assumption assumption, ClassDeps classDeps) {
        JVMTI_Assumption jvmtiAssumption = (JVMTI_Assumption) assumption;
        classDeps.add(this, jvmtiAssumption.eventSettings);
        int length = jvmtiAssumption.breakpoints == null ? 0 : jvmtiAssumption.breakpoints.length;
        classDeps.add(this, length);
        for (int i = 0; i < length; i++) {
            classDeps.add(this, jvmtiAssumption.breakpoints[i]);
        }
        return true;
    }

    @Override
    protected DependencyProcessorVisitor match(DependencyVisitor dependencyVisitor) {
        return (dependencyVisitor instanceof JVMTI_DependencyProcessorVisitor) ? (JVMTI_DependencyProcessorVisitor) dependencyVisitor : null;
    }

    @Override
    protected int visit(DependencyProcessorVisitor dependenciesProcessorVisitor, ClassActor context, Dependencies dependencies, int index) {
        JVMTI_DependencyProcessorVisitor jvmtiVisitor = (JVMTI_DependencyProcessorVisitor) dependenciesProcessorVisitor;
        int i = index;
        long eventSettings = dependencies.getLong(i);
        i += 4;
        int length = dependencies.getInt(i);
        i += 2;
        if (jvmtiVisitor != null) {
            long[] breakpoints = length == 0 ? null : new long[length];
            for (int b = 0; b < length; b++) {
                breakpoints[b] = dependencies.getLong(i);
                i += 4;
            }
            jvmtiVisitor.doCheckSettings(dependencies.targetMethod(), eventSettings, breakpoints);
        } else {
            i += length * 4;
        }
        return i;
    }

    private static class Visitor extends DependencyVisitor implements JVMTI_DependencyProcessorVisitor {
        boolean matches;
        final TargetMethod targetMethodToMatch;
        final long eventSettingsToMatch;
        final long[] breakpointsToMatch;

        Visitor(ClassMethodActor classMethodActor, long eventSettings) {
            this.targetMethodToMatch = classMethodActor.currentTargetMethod();
            this.eventSettingsToMatch = eventSettings;
            this.breakpointsToMatch = JVMTIBreakpoints.getBreakpoints(classMethodActor);
        }

        public boolean doCheckSettings(TargetMethod targetMethod, long eventSettings, long[] breakpoints) {
            if (targetMethod == targetMethodToMatch) {
                matches = check(eventSettings, breakpoints);
                // terminate visit
                return false;
            }
            // continue searching
            return true;
        }

        private boolean check(long eventSettings, long[] breakpoints) {
            if ((eventSettingsToMatch & eventSettings) == eventSettingsToMatch) {
                if (breakpointsToMatch == null) {
                    return true;
                }
                if (breakpoints == null) {
                    return false;
                }
                for (long rb : breakpointsToMatch) {
                    boolean match = false;
                    for (long thisRb : breakpoints) {
                        if (rb == thisRb) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Check event/breakpoint settings for {@linkplain ClassMethodActor#currentTargetMethod()}.
     * @param classMethodActor
     * @param eventSettings
     * @return {@code} true iff settings satisfied.
     */
    public static boolean checkSettings(ClassMethodActor classMethodActor, long eventSettings) {
        Visitor visitor = new Visitor(classMethodActor, eventSettings);
        classHierarchyLock.readLock().lock();
        try {
            DSet dset = ContextDependents.map.get(classMethodActor.holder());
            synchronized (dset) {
                for (int i = 0; i < dset.size(); i++) {
                    final Dependencies deps = dset.getDeps(i);
                    visitor.matches = false;
                    deps.visit(visitor);
                    if (visitor.matches) {
                        return visitor.matches;
                    }
                }
            }
        } finally {
            classHierarchyLock.readLock().unlock();
        }
        return false;
    }

}
