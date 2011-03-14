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
import com.sun.max.vm.compiler.target.*;

/**
 * An adapter enables calls across code compiled by different compilers with different calling convention. The adapter
 * takes care of setting up a caller activation frame as expected by the callee. A {@code AdapterStackFrame} object
 * abstracts the activation frame of such adapters.
 *
 * @author Laurent Daynes
 */
public class AdapterStackFrame extends VMStackFrame {

    public AdapterStackFrame(StackFrame callee, AdapterStackFrameLayout layout, TargetMethod targetMethod, Pointer instructionPointer, Pointer framePointer, Pointer stackPointer) {
        super(callee, layout, targetMethod, instructionPointer, framePointer, stackPointer);
    }

    /**
     * Two adapter frames are the same iff they are of the same concrete type and their stack and frame pointers are identical.
     */
    @Override
    public boolean isSameFrame(StackFrame stackFrame) {
        if (stackFrame.getClass().equals(getClass())) {
            return stackFrame.sp.equals(sp) && stackFrame.fp.equals(fp);
        }
        return false;
    }
}
