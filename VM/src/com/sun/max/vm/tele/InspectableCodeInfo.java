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
package com.sun.max.vm.tele;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;



/**
 * Makes critical state information about code
 * remotely inspectable.
 * <br>
 * Active only when VM is being inspected.
 *
 * @author Michael Van De Vanter
 */
public class InspectableCodeInfo {

    /**
     * Make information inspectable about a just completed method compilation.
     * <br>
     * Should check Inspectable.isVmInspected() here, so that we can avoid the
     * overhead when not being inspected, but we discovered that making this test
     * slows things down slightly, for reasons not understood.
     *
     * @param targetMethod compilation just completed
     * @see Inspectable#isVmInspected()
     */
    public static void notifyCompilationComplete(TargetMethod targetMethod) {
        final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
        compilationFinished(classMethodActor.holder().typeDescriptor.string,
            classMethodActor.name.string,
            classMethodActor.descriptor.string,
            targetMethod);
    }

    /**
     * An empty method whose purpose is to be interrupted by the Inspector when
     * it needs to monitor method compilations in the VM.  The arguments
     * are deliberately made simple so that they can be read with low-level
     * mechanisms in the Inspector.
     *
     * @param holderType type description for class holding the method
     * @param methodName name of the the method
     * @param signature argument type descriptors for the method
     * @param targetMethod the result of the method compilation
     */
    @NEVER_INLINE
    @INSPECTED
    public static void compilationFinished(String holderType, String methodName, String signature, TargetMethod targetMethod) {
    }

}
