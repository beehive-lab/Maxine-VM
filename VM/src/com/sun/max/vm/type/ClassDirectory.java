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

import com.sun.max.vm.actor.holder.*;

/**
 * This class maintains the global class hierarchy and dependencies assumed thereon.
 * TODO explain how this class holds the class hierarchy once data structures, indices etc. are more or less stable
 *
 * @author Michael Duller
 */
public class ClassDirectory {

    /**
     * Adds the class to the class hierarchy. This will also trigger invalidating dependencies and deoptimizing code based thereon.
     *
     * @param classActor the class to be added to the global class hierarchy.
     */
    public static void addToHierarchy(ClassActor classActor) {
        classActor.prependToSiblingList();
        // TODO track interface implementors as well for additional dep. types (...._concrete_subtypes_2 and ...._concrete_methods_2)

        // TODO flush dependents on what is invalidated by the supplied classActor
    }

}
