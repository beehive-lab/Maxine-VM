/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.tele;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 */
public final class TargetBreakpoint {

    @HOSTED_ONLY
    public static byte[] createBreakpointCode(ISA isa) {
        switch (isa) {
            case AMD64:
            case IA32: {
                return new byte[] {(byte) 0xCC};
            }
            default: {
                FatalError.unimplemented();
                break;
            }
        }
        return null;
    }

    public static final byte[] breakpointCode = createBreakpointCode(Platform.platform().isa);

    private final Pointer instructionPointer;
    private byte[] originalCode;

    private TargetBreakpoint(Address instructionPointer) {
        this.instructionPointer = instructionPointer.asPointer();
    }

    public boolean isEnabled() {
        return originalCode != null;
    }

    private void enable() {
        if (originalCode == null) {
            originalCode = new byte[breakpointCode.length];
            // TODO (mlvdv) Record original code into the history, key instruction pointer.
            // Pick a data structure that can be read easily by the interpreter,
            // a hash table with overwriting because the interpreter is slow
            Memory.readBytes(instructionPointer, originalCode);
            Memory.writeBytes(breakpointCode, instructionPointer);
        }
    }

    private void disable() {
        if (originalCode != null) {
            Memory.writeBytes(originalCode, instructionPointer);
            originalCode = null;
        }
    }

    /**
     * This data structure is easy to interpret remotely.
     */
    private static final SortedLongArrayMapping<TargetBreakpoint> targetBreakpoints = new SortedLongArrayMapping<TargetBreakpoint>();

    // make another array like this for deleted TargetBreakpoints
    // add another method to look them up by address

    @INSPECTED
    public static byte[] findOriginalCode(long instructionPointer) {
        final TargetBreakpoint targetBreakpoint = targetBreakpoints.get(instructionPointer);
        if (targetBreakpoint != null) {
            return targetBreakpoint.originalCode;
        }
        return null;
    }

    public static synchronized void make(Address instructionPointer) {
        TargetBreakpoint targetBreakpoint = targetBreakpoints.get(instructionPointer.toLong());
        if (targetBreakpoint == null) {
            targetBreakpoint = new TargetBreakpoint(instructionPointer);
            targetBreakpoints.put(instructionPointer.toLong(), targetBreakpoint);
        }
        targetBreakpoint.enable();
    }

    public static synchronized void delete(Address instructionPointer) {
        final TargetBreakpoint targetBreakpoint = targetBreakpoints.get(instructionPointer.toLong());
        if (targetBreakpoint != null) {
            targetBreakpoint.disable();
            targetBreakpoints.remove(instructionPointer.toLong());
        }
    }
}
