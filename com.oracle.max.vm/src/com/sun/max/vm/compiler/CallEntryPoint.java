/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler;

import static com.sun.max.vm.MaxineVM.*;

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
 */
public enum CallEntryPoint {

    /**
     * Denotes the address stored in vtables and itables. In the absence of virtual call-site optimization (e.g., inline
     * caching), this call entry point is typically chosen to be the same as {@link #OPTIMIZED_ENTRY_POINT}, so as to
     * avoid extra computation in virtual method dispatch from optimized code.
     */
    VTABLE_ENTRY_POINT,

    /**
     * Denotes the entry address used by the baseline compiler when compiling a call.
     */
    BASELINE_ENTRY_POINT,

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
     * The offset of this call entry point in a target method associated with this entry point.
     */
    private int offset = -1;

    /**
     * The offset of this call entry point in a callee of a target method associated with this entry point.
     */
    private int offsetInCallee = -1;

    /**
     * Gets the offset of this call entry point in a target method associated with this entry point.
     *
     * @return the number of bytes that must be added to the address of a target method's first instruction to obtain
     *         the address of this entry point
     */
    @Fold
    public int offset() {
        return offset;
    }

    /**
     * Gets the address of this call entry point in a given target method.
     */
    public CodePointer in(TargetMethod targetMethod) {
        return targetMethod.codeAt(offset());
    }

    /**
     * Gets the offset of this call entry point in a callee of a target method associated with this entry point.
     *
     * @return the number of bytes that must be added to the address of a target method's first instruction to obtain
     *         the address of this entry point
     */
    @Fold
    public int offsetInCallee() {
        return offsetInCallee;
    }

    @HOSTED_ONLY
    private void init(CallEntryPoint cep) {
        init(cep.offset, cep.offsetInCallee);
    }

    @HOSTED_ONLY
    private void init(int offset, int offsetInCallee) {
        assert this.offset == -1 || this.offset == offset : "cannot re-initialize with different value";
        assert this.offsetInCallee == -1 || this.offsetInCallee == offsetInCallee : "cannot re-initialize with different value";
        this.offset = offset;
        this.offsetInCallee = offsetInCallee;
    }

    @HOSTED_ONLY
    private static void initAllToZero() {
        for (CallEntryPoint e : VALUES) {
            e.init(0, 0);
        }
    }

    static {
        if (vm().compilationBroker.needsAdapters()) {
            OPTIMIZED_ENTRY_POINT.init(8, 8);
            BASELINE_ENTRY_POINT.init(0, 0);
            VTABLE_ENTRY_POINT.init(OPTIMIZED_ENTRY_POINT);
            // Calls made from a C_ENTRY_POINT method link to the OPTIMIZED_ENTRY_POINT of the callee
            C_ENTRY_POINT.init(0, OPTIMIZED_ENTRY_POINT.offset());
        } else {
            CallEntryPoint.initAllToZero();
        }
    }
}
