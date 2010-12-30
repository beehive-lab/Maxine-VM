/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.ir.observer.*;

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
     * @return {@code defaultResult} if not supported, otherwise whether the IR in this method references the given builtin
     */
    boolean contains(Builtin builtin, boolean defaultResult);

    /**
     * @return {@code defaultResult} if not supported, otherwise the count of occurrence of builtin in the IR in this method
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
