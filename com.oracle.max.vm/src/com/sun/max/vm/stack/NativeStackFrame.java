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
package com.sun.max.vm.stack;

import com.sun.max.unsafe.*;
import com.sun.max.vm.jni.*;

/**
 * Abstracts over one or more stack frames for non-Java code. The reason that a
 * single object may represent multiple native frames is that it is impossible
 * to know the stack layout employed by the compiler that generated the code.
 * The only thing we can know for these frames is where the stack pointer and
 * return instruction is Java code that transitioned the call stack from Java
 * code into native code. This is known because all such transitions occur
 * through a {@linkplain NativeStubGenerator native stub} which records the current
 * {@link JavaFrameAnchor}.
 */
public final class NativeStackFrame extends StackFrame {

    public NativeStackFrame(StackFrame callee, Pointer instructionPointer, Pointer framePointer, Pointer stackPointer) {
        super(callee, instructionPointer, stackPointer, framePointer);
    }

    /**
     * {@inheritDoc}
     *
     * Two native frames are the same iff their stack and frame pointers are identical.
     */
    @Override
    public boolean isSameFrame(StackFrame stackFrame) {
        if (stackFrame instanceof NativeStackFrame) {
            return stackFrame.sp.equals(sp) && stackFrame.fp.equals(fp);
        }
        return false;
    }

    @Override
    public String toString() {
        return "<native@" + ip.toHexString() + ">";
    }
}
