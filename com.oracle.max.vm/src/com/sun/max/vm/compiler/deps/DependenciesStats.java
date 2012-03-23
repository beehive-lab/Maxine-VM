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

import static com.sun.max.vm.actor.holder.ClassActor.*;
import static com.sun.max.vm.actor.holder.ClassID.*;
import static com.sun.max.vm.compiler.deps.DependenciesManager.classHierarchyLock;
import static com.sun.max.vm.compiler.deps.ContextDependents.map;

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.profile.*;
import com.sun.max.profile.ValueMetrics.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.deps.Dependencies.DependencyVisitor;
import com.sun.max.vm.compiler.deps.DependencyProcessor.ToStringDependencyProcessorVisitor;
import com.sun.max.vm.compiler.deps.ContextDependents.*;

/**
 * Statistics gathering for dependencies to aid in tuning.
 * Currently hosted only, so assumes the VM is itself representative of real applications.
 */
@HOSTED_ONLY
public class DependenciesStats {
    static class Counter {
        int count;
    }

    /**
     * Dump the content of the {@linkplain ContextDependents} map to the specified {@link PrintStream}.
     * @param out output stream where to print the dump.
     */
    @HOSTED_ONLY
    public static void dump(PrintStream out) {
        classHierarchyLock.readLock().lock();
        try {
            dumpContextDependentsMap(out);
            printStatistics(out);

        } finally {
            classHierarchyLock.readLock().unlock();
        }
    }

    /**
     * Dump statistics.
     */
    @HOSTED_ONLY
    private static void printStatistics(PrintStream out) {
        IntegerDistribution numDistinctAssumptionsPerType = ValueMetrics.newIntegerDistribution("numDistinctAssumptionsPerType", 0, 20);
        IntegerDistribution numDependenciesPerType = ValueMetrics.newIntegerDistribution("numDependenciesPerType", 0, 20);
        IntegerDistribution numDependentsPerType = ValueMetrics.newIntegerDistribution("numDependentPerType", 0, 20);
        HashMap<DependencyProcessor, Counter> dependencyCounters = new HashMap<DependencyProcessor, Counter>(20);
        HashMap<DependencyProcessor, IntegerDistribution> numSpecificDependenciesPerType = new HashMap<DependencyProcessor, IntegerDistribution>();
        for (DependencyProcessor dependencyProcessor : DependenciesManager.dependencyProcessorsArray) {
            dependencyCounters.put(dependencyProcessor, new Counter());
            numSpecificDependenciesPerType.put(dependencyProcessor, ValueMetrics.newIntegerDistribution(dependencyProcessor.getClass().getName()));
        }

        for (ClassActor type : map.keySet()) {
            DSet dset = map.get(type);
            numDependentsPerType.record(dset.size);

            for (int i = 0; i < dset.size; i++) {
                final Dependencies deps = dset.getDeps(i);
                countDependenciesPerType(deps, type.id, dependencyCounters);
            }

            int numDistinctAssumptionsPerTypeCounter = 0;
            int totalTypeAssumptions = 0;
            for (Map.Entry<DependencyProcessor, Counter> entrySet : dependencyCounters.entrySet()) {
                DependencyProcessor dependencyProcessor = entrySet.getKey();
                Counter counter = entrySet.getValue();

                totalTypeAssumptions += counter.count;
                if (counter.count > 0) {
                    numDistinctAssumptionsPerTypeCounter++;
                }
                numSpecificDependenciesPerType.get(dependencyProcessor).record(counter.count);
                // zero out counter for next class
                counter.count = 0;
            }
            numDistinctAssumptionsPerType.record(numDistinctAssumptionsPerTypeCounter);
            numDependenciesPerType.record(totalTypeAssumptions);
        }

        out.println("# types with dependent methods: " + map.size());
        numDependentsPerType.report("# dependents / types", out);
        numDependenciesPerType.report("# total assumptions / type", out);
        numDistinctAssumptionsPerType.report("# distinct assumptions / type", out);
        for (Map.Entry<DependencyProcessor, IntegerDistribution> entrySet : numSpecificDependenciesPerType.entrySet()) {
            DependencyProcessor dependencyProcessor = entrySet.getKey();
            IntegerDistribution integerDistribution = entrySet.getValue();
            integerDistribution.report("# " + dependencyProcessor.getClass().getName() + " dependency / type", out);
        }
    }

    @HOSTED_ONLY
    private static void printDependents(ClassActor type, final PrintStream out) {
        DSet dset = map.get(type);
        int numDependents = dset.size;
        out.println("\nclass " + type + " (id = " + type.id + ") has " + numDependents + " dependents");
        for (int i = 0; i < dset.size; i++) {
            final Dependencies deps = dset.getDeps(i);
            out.println("  " + deps);
            deps.iterate(new DependencyVisitor(type.id) {
                @Override
                protected int visit(Dependencies dependencies, ClassActor context, DependencyProcessor dependencyProcessor, int index) {
                    StringBuilder sb = new StringBuilder();
                    ToStringDependencyProcessorVisitor v = dependencyProcessor.getToStringDependencyProcessorVisitor();
                    v.setStringBuilder(sb);
                    dependencyProcessor.visit(v, context, dependencies, index);
                    out.println("    " + sb.toString());
                    // step the index
                    return dependencyProcessor.visit(null, context, dependencies, index);
                }

                @Override
                public void doInvalidated() {
                    out.println("assumptions have been invalidated");
                }
            });
        }
    }

    @HOSTED_ONLY
    private static void countDependenciesPerType(final Dependencies deps, int classID, final HashMap<DependencyProcessor, Counter> counters) {
        deps.iterate(new DependencyVisitor(classID) {
            @Override
            protected int visit(Dependencies dependencies, ClassActor context, DependencyProcessor dependencyProcessor, int index) {
                counters.get(dependencyProcessor).count++;
                // step the index
                return dependencyProcessor.visit(null, context, dependencies, index);
            }
        });
    }

    @HOSTED_ONLY
    private static void dumpContextDependentsMap(PrintStream out) {
        out.println("================================================================");
        out.println("ContextDependents map has " + map.size() + " entries");
        for (ClassActor type : map.keySet()) {
            printDependents(type, out);
        }
        out.println("================================================================");
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


}
