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

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;

/**
 * A {@linkplain TargetMethod target method} may have multiple entry points, depending on the calling convention of the
 * compiler(s)/interpreter(s) used by the VM and the optimizations applied by the compilers.
 * {@code CallEntryPoint} enumerates the different possible entry points to a target method. Each entry point denotes an
 * {@linkplain #offset() offset} relative to a target method's
 * {@linkplain TargetMethod#codeStart() first instruction}.
 * <p>
 * Each {@linkplain TargetMethod target method} has a {@linkplain TargetMethod#callEntryPoint fixed} call entry point.
 * This is the entry point used for non-native, non-slow-path-runtime calls made by the target method.
 * Stack walkers and linkers also use this call entry point to determine if an activation
 * frame for a target method is an adapter frame.
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

    public static final List<CallEntryPoint> VALUES = Arrays.asList(values());

    /**
     * The offset of this call entry point in a target method associated (via its
     * {@linkplain TargetMethod#abi() ABI}) with this entry point.
     */
    private int offset = -1;

    /**
     * The offset of this call entry point in a callee of a target method associated (via its
     * {@linkplain TargetMethod#abi() ABI}) with this entry point.
     */
    private int offsetInCallee = -1;

    @HOSTED_ONLY
    public void init(CallEntryPoint cep) {
        init(cep.offset, cep.offsetInCallee);
    }

    @HOSTED_ONLY
    public void init(int offset, int offsetInCallee) {
        assert this.offset == -1 || this.offset == offset : "cannot re-initialize with different value";
        assert this.offsetInCallee == -1 || this.offsetInCallee == offsetInCallee : "cannot re-initialize with different value";
        this.offset = offset;
        this.offsetInCallee = offsetInCallee;
    }

    @HOSTED_ONLY
    public static void initAllToZero() {
        for (CallEntryPoint e : VALUES) {
            e.init(0, 0);
        }
    }

    /**
     * Gets the offset of this call entry point in a target method associated (via its
     * {@linkplain TargetMethod#abi() ABI}) with this entry point.
     *
     * @return the number of bytes that must be added to the address of a target method's first instruction to obtain
     *         the address of this entry point
     */
    @FOLD
    public int offset() {
        return offset;
    }

    /**
     * Gets the address of this call entry point in a given target method.
     */
    public Pointer in(TargetMethod targetMethod) {
        return targetMethod.codeStart().plus(offset());
    }

    /**
     * Gets the offset of this call entry point in a callee of a target method associated (via its
     * {@linkplain TargetMethod#abi() ABI}) with this entry point.
     *
     * @return the number of bytes that must be added to the address of a target method's first instruction to obtain
     *         the address of this entry point
     */
    @FOLD
    public int offsetInCallee() {
        return offsetInCallee;
    }

}
