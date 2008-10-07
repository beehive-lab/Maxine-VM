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
/*VCSID=682b2cf2-44be-468b-8d2e-207049019e59*/
package com.sun.max.vm.compiler.ir;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.ir.observer.*;

/**
 * IR representations for methods implement this interface
 * to provide access to the respective method actor.
 *
 * @author Bernd Mathiske
 */
public interface IrMethod {

    /**
     * Gets the Java method actor from which this IR is derived.
     */
    ClassMethodActor classMethodActor();

    /**
     * Whether this IR object has been completely translated.
     */
    boolean isGenerated();

    /**
     * Gets the non-qualified name of the method represented by this IR object.
     * This will typically be equivalent to {@code classMethodActor().name()}.
     */
    String name();

    /**
     * Whether this IR object represents a native method (with a generated JNI stub).
     */
    boolean isNative();

    /**
     * Remove unnecessarily referenced temporary data structures that were only needed during generation.
     */
    void cleanup();

    /**
     * @return 'defaultResult' if not supported, otherwise whether the IR in this method references the given builtin
     */
    boolean contains(Builtin builtin, boolean defaultResult);

    /**
     * @return 'defaultResult' if not supported, otherwise the count of occurrence of builtin in the IR in this method
     */
    int count(Builtin builtin, int defaultResult);

    /**
     * This method gets an entrypoint for this executable method given the specified call
     * entrypoint. For a target method, this will be a machine code address, while for an
     * IR method, this will typically be a method ID.
     * @param callEntryPoint the call entrypoint for which to retrieve the entrypoint
     * @return a word that represents the entrypoint for this methods
     */
    Word getEntryPoint(CallEntryPoint callEntryPoint);

    /**
     * Produces a trace of this IR method as a String. If possible, this string returned by this method should be usable
     * as a "fingerprint" of this IR object. That is, this method should be usable in testing whether or not a compiler
     * is deterministic.
     */
    String traceToString();

    /**
     * Gets the {@link IrTraceObserver} subclass (if any) that is specific to the type of this IR method.
     */
    Class<? extends IrTraceObserver> irTraceObserverType();
}
