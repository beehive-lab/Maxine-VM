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
package com.sun.max.vm.stack;

import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.sparc.*;


/**
 * The ABI of some platforms requires the use of a stack bias. The bias is a constant offset that needs to be added to the registers holding the stack pointer
 * to access elements of the stack frame.
 * The only example of this so far is the Solaris SPARC V9 (64-bits platform. The stack bias helps the operating system identifying easily
 * whether it runs in 64-bits address mode when hitting a stack overflow signal.
 * That is, the registers that hold, respectively, the stack and frame pointers are set to the corresponding pointer minus a bias.
 * Accessing a slot of the stack frame requires adding the bias, e.g.,  ld [ %i6 + STACK_BIAS + x]  loads the content
 * of the slot at offset x from the frame pointer.
 *
 * @author Laurent Daynes
 */
public enum STACK_BIAS {
    /**
     * Default case: no bias. Use this for all platforms who ABI doesn't use any stack bias.
     */
    NONE,

    /**
     * The standard Solaris / SPARC V9 STACK BIAS.
     */
    SPARC_V9 {
        @Override
        public int stackBias() {
            return SPARCStackFrameLayout.STACK_BIAS;
        }

        @Override
        public boolean isStackPointerBiased() {
            return true;
        }

        @Override
        public boolean isFramePointerBiased() {
            return true;
        }
    },

    /**
     * Variant of the standard SPARC V9 used by the JIT. The JIT doesn't use the native frame pointer register to address its frame,
     * so only its stack pointer needs biasing.
     */
    JIT_SPARC_V9 {
        @Override
        public int stackBias() {
            return SPARCStackFrameLayout.STACK_BIAS;
        }

        @Override
        public boolean isStackPointerBiased() {
            return true;
        }
    };

    public Pointer unbias(Pointer biasedPointer) {
        return biasedPointer.plus(stackBias());
    }

    public Pointer bias(Pointer unbiasedPointer) {
        return  unbiasedPointer.minus(stackBias());
    }

    public int stackBias() {
        return 0;
    }

    public boolean isStackPointerBiased() {
        return false;
    }

    public boolean isFramePointerBiased() {
        return false;
    }
}
