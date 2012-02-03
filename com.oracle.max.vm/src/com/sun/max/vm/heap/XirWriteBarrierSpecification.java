/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap;

import com.sun.cri.xir.*;
import com.sun.cri.xir.CiXirAssembler.XirOperand;
import com.sun.max.util.*;

/**
 * Reference write-barrier specification. Interface between HeapScheme and Xir.
 * WORK IN PROGRESS
 *
 * Barrier generators only take two arguments: An CiXirAssembler and an array of XirOperand.
 * The arguments needed are specified via an argument specification, so the generator can take the
 * requirement into account to factor some operations (mostly, computing a field address), and to pass only the necessary arguments.
 * This would requires some parsing of the argument specifications though.
 */
public interface XirWriteBarrierSpecification extends WriteBarrierSpecification {

    /**
     * A generic write barrier generator interface to XIR.
     */
    public interface XirWriteBarrierGenerator {
        /**
         * Generate XIR assembly code for a write barrier according to precise specification.
         * Because different barrier implementation takes different arguments both in types and their numbers, the
         * interface takes an arguments array.
         * @param asm
         * @param operands
         */
        void genWriteBarrier(CiXirAssembler asm, XirOperand ... operands);
    }

    XirWriteBarrierGenerator NULL_WRITE_BARRIER_GEN = new XirWriteBarrierGenerator() {

        public void genWriteBarrier(CiXirAssembler asm, XirOperand... operands) {
        }
    };

    /**
     * Return a XIR write-barrier generator that implements the specification encoded in a bit set whose elements correspond to enum-based flags.
     *
     * @param writeBarrierSpec a bit set encoding a write barrier specification.
     * @return
     */
    XirWriteBarrierGenerator barrierGenerator(IntBitSet<WriteBarrierSpecification.WriteBarrierSpec> writeBarrierSpec);
}
