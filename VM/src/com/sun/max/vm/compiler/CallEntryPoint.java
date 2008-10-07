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
package com.sun.max.vm.compiler;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;

/**
 * A {@linkplain TargetMethod target method} may have multiple entry points, depending on the calling convention of the
 * compiler(s)/interpreter(s) used by the VM and the optimizations applied by the compilers.
 * {@code CallEntryPoint} enumerates the different possible entry points to a target method. Each entry point denotes an
 * {@linkplain #offsetFromCodeStart() offset} relative to a target method's
 * {@linkplain TargetMethod#codeStart() first instruction}.
 * <p>
 * Each {@linkplain TargetMethod target method} is associated with a {@linkplain TargetABI target ABI} which specifies a
 * {@linkplain TargetABI#callEntryPoint() call entry point}. Stack walkers and linkers can use this call entry point to
 * determine if an activation frame for a target method is an adapter frame, and to determine what entry point to
 * use for statically linked calls.
 *
 * @author Laurent Daynes
 */
public enum CallEntryPoint {

    /**
     * Denotes the address stored in vtables and itables. In the absence of virtual call-site optimization (e.g., inline
     * caching), this call entry point is typically chosen to be the same as {@link #OPTIMIZED_ENTRY_POINT}, so as to
     * avoid extra computation in virtual method dispatch from optimized code.
     */
    VTABLE_ENTRY_POINT,

    /**
     * Denotes the entry address used by the JIT compiler when compiling a call.
     */
    JIT_ENTRY_POINT,

    /**
     * Denotes the entry address used by the optimizing compiler when compiling a call.
     */
    OPTIMIZED_ENTRY_POINT,

    /**
     * Denotes the address used by native code when calling a {@linkplain JniFunctions JNI function} or some other
     * {@linkplain C_FUNCTION VM entry point}. These methods have no adapter frame and are always compiled with the
     * optimizing compiler.
     */
    C_ENTRY_POINT;

    public static final IndexedSequence<CallEntryPoint> VALUES = new ArraySequence<CallEntryPoint>(values());

    /**
     * Gets the offset of this call entry point relative to the address of a target method's
     * {@linkplain TargetMethod#codeStart() first instruction}. That is, given a target method {@code targetMethod} and
     * a call entry point {@code callEntryPoint}, the address of the {@code callEntryPoint} in {@code targetMethod} is
     * computed as {@code targetMethod.codeStart().plus(callEntryPoint.offsetFromCodeStart)}.
     *
     * @return the number of bytes that must be added to the address of a target method's first instruction to obtain
     *         the address of this entry point
     */
    @FOLD
    public int offsetFromCodeStart() {
        return VMConfiguration.target().offsetToCallEntryPoints()[ordinal()];
    }

    /**
     * Gets the address of this call entry point in a given target method.
     */
    public Pointer in(TargetMethod targetMethod) {
        return targetMethod.codeStart().plus(offsetFromCodeStart());
    }

    /**
     * Gets the offset of this call entry point relative to the address of a callee target method's
     * {@linkplain TargetMethod#codeStart() first instruction}. That is, given a target method {@code targetMethod} and
     * a call entry point {@code callEntryPoint}, the address of the {@code callEntryPoint} in {@code targetMethod} is
     * computed as {@code targetMethod.codeStart().plus(callEntryPoint.offsetFromCalleeCodeStart)}.
     *
     * @return the number of bytes that must be added to the address of a target method's first instruction to obtain
     *         the address of this entry point
     */
    @FOLD
    public int offsetFromCalleeCodeStart() {
        return VMConfiguration.target().offsetsToCalleeEntryPoints()[ordinal()];
    }

}
