/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.runtime;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;

/**
 * Counts the bytecodes that are executed. This should give the same results
 * as the hard-wired counting enabled by {@code -T1X:+PrintBytecodeHistogram}.
 */
public class BytecodeCounter extends NullVMAdviceHandler {
    private static long[] counts = new long[256];

    @NEVER_INLINE
    public static void inc(int tag) {
        counts[tag]++;
    }

    @Override
    public void initialise(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.TERMINATING) {
            for (int b = 0; b < counts.length; b++) {
                long count = counts[b];
                if (count > 0) {
                    System.out.format("%s: %d%n", Bytecodes.nameOf(b), count);
                }
            }
        }
    }

}
