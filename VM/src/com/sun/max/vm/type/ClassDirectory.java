/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.type;

import java.util.*;

import com.sun.cri.ri.*;
import com.sun.max.vm.*;
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
