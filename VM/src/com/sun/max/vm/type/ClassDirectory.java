/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.type;

import java.util.*;

import com.sun.cri.ri.*;
import com.sun.max.vm.actor.holder.*;

/**
 * This class maintains the global class hierarchy and dependencies assumed thereon.
 * TODO explain how this class holds the class hierarchy once data structures, indices etc. are more or less stable
 *
 * @author Michael Duller
 */
public class ClassDirectory {

    private static final Map<RiType, Set<RiMethod>> leafMethodAssumptionByHolder = new HashMap<RiType, Set<RiMethod>>();

    /**
     * Adds the class to the class hierarchy. This will also trigger invalidating dependencies and deoptimizing code based thereon.
     *
     * @param classActor the class to be added to the global class hierarchy.
     */
    public static void addToHierarchy(ClassActor classActor) {
        classActor.prependToSiblingList();
        // TODO track interface implementors as well for future additional dependency types (...._concrete_subtypes_2 and ...._concrete_methods_2)?
        flushDependentsOn(classActor);
        UniqueConcreteSubtypeTable.recordClassActor(classActor);
    }

    public static boolean recordLeafMethodAssumption(RiMethod method) {
        //Log.print("recordLeafMethodAssumption for ");
        //Log.println(method);
        Set<RiMethod> holderMethods = leafMethodAssumptionByHolder.get(method.holder());
        if (holderMethods == null) {
            holderMethods = new HashSet<RiMethod>();
            leafMethodAssumptionByHolder.put(method.holder(), holderMethods);
        }
        holderMethods.add(method);
        // FIXME return true to enable speculative optimizations
        return false;
    }

    /**
     * Flush assumptions and activations of code that has been compiled on the assumptions invalidated by {@code newClassActor}.
     *
     * @param newClassActor the newly loaded class that might invalidate assumptions
     */
    private static void flushDependentsOn(ClassActor newClassActor) {
        Set<RiMethod> methodCandidates = leafMethodAssumptionByHolder.get(newClassActor.superClassActor);
        if (methodCandidates != null) {
            //Log.print("just now defined class ");
            //Log.print(newClassActor);
            //Log.print(" extends and thus invalidates leaf assumptions for ");
            //Log.println(newClassActor.superClassActor);
            // TODO further checks if actual method is overridden, if so, trigger deopt for activations of all affected methods
        }
    }

}
