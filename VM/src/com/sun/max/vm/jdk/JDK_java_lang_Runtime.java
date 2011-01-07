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
package com.sun.max.vm.jdk;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

/**
 * Implements method substitutions for {@link java.lang.Runtime java.lang.Runtime}.
 *
 * @author Bernd Mathiske
 */
@METHOD_SUBSTITUTIONS(Runtime.class)
public final class JDK_java_lang_Runtime {

    private JDK_java_lang_Runtime() {
    }

    /**
     * Returns the amount of free memory.
     * @see java.lang.Runtime#freeMemory()
     * @return the amount of free memory in bytes
     */
    @SUBSTITUTE
    private long freeMemory() {
        return Heap.reportFreeSpace();
    }

    /**
     * Returns the total amount of memory available to the virtual machine.
     * @see java.lang.Runtime#totalMemory()
     * @return the total amount of memory available to the virtual machine in bytes
     */
    @SUBSTITUTE
    private long totalMemory() {
        // TODO: ask the OS what is available beyond this
        return Heap.maxSize().toLong();
    }

    /**
     * Returns the maximum heap size.
     * @see java.lang.Runtime#maxMemory()
     * @return the maximum heap size in bytes
     */
    @SUBSTITUTE
    private long maxMemory() {
        return Heap.maxSize().toLong();
    }

    /**
     * Request a garbage collection.
     * @see java.lang.Runtime#gc()
     */
    @SUBSTITUTE
    private void gc() {
        if (!Heap.gcDisabled()) {
            Heap.collectGarbage(Size.zero());
        }
    }

    /**
     * Invoke finalizers of garbage collected objects.
     * @see java.lang.Runtime#runFinalization()
     */
    @SUBSTITUTE
    private static void runFinalization0() {
        Heap.runFinalization();
    }

    /**
     * Turn tracing of instructions on or off. Ignored.
     * @param on {@code true} if instructions should be traced; {@code false} otherwise
     */
    @SUBSTITUTE
    private void traceInstructions(boolean on) {
        // do nothing.
    }

    /**
     * Turn tracing of method calls on or off. Ignored.
     * @param on {@code true} if the instructions should be traced; {@code false} otherwise
     */
    @SUBSTITUTE
    private void traceMethodCalls(boolean on) {
        // do nothing.
    }
}
