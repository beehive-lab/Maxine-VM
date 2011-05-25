/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.stack;

import static com.sun.max.platform.Platform.*;

import com.sun.max.unsafe.*;

/**
 * The ABI of some platforms requires the use of a stack bias. The bias is a constant offset that needs to be added to the registers holding the
 * stack pointer to access elements of the stack frame.
 * The only example of this so far is the Solaris SPARC V9 (64-bits platform. The stack bias helps the operating system identifying easily
 * whether it runs in 64-bits address mode when hitting a stack overflow signal.
 * That is, the registers that hold, respectively, the stack and frame pointers are set to the corresponding pointer minus a bias.
 * Accessing a slot of the stack frame requires adding the bias, e.g.,  ld [%i6 + STACK_BIAS + x]  loads the contents
 * of the slot at offset x from the frame pointer.
 */
public enum StackBias {
    /**
     * Default case: no bias. Use this for all platforms whose ABI doesn't use any stack bias.
     */
    NONE,

    /**
     * The standard Solaris / SPARC V9 STACK BIAS.
     */
    SPARC_V9 {
        @Override
        public boolean isStackPointerBiased() {
            return true;
        }

        @Override
        public boolean isFramePointerBiased() {
            return true;
        }
    };

    public Pointer unbias(Pointer biasedPointer) {
        return biasedPointer.plus(stackBias());
    }

    public Pointer bias(Pointer unbiasedPointer) {
        return unbiasedPointer.minus(stackBias());
    }

    public final int stackBias() {
        return platform().stackBias;
    }

    public boolean isStackPointerBiased() {
        return false;
    }

    public boolean isFramePointerBiased() {
        return false;
    }
}
