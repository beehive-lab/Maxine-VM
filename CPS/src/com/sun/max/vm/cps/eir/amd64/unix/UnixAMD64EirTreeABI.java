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
package com.sun.max.vm.cps.eir.amd64.unix;

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.EirStackSlot.Purpose;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.amd64.*;
import com.sun.max.vm.type.*;

/**
 * @author Michael Bebenita
 */
public class UnixAMD64EirTreeABI extends UnixAMD64EirJavaABI {

    @Override
    public EirLocation[] getParameterLocations(Purpose stackSlotPurpose, Kind... kinds) {
        ProgramError.unexpected();
        return null;
    }

    /**
     * Map tree parameters onto their original JIT frame locations, such that no adapter frames are required when
     * executing a tree. The tree frame takes over the JIT frame.
     *
     * +-------------+-------+-------+-----+-------+--------+
     * | P P P P ... | RA FP | xxxxx | T T | L L L | S S .. |
     * +-------------+-------+-------+-----+-------+--------+
     *
     *                   |------ Frame Size -------|
     *
     * P  = parameter local slot
     * RA = caller's return address
     * FP = caller's frame pointer
     * xx = frame alignment
     * T  = template slot
     * L  = non-parameter local slot
     * S  = stack slot
     *
     */
    @Override
    public EirLocation[] getParameterLocations(ClassMethodActor classMethodActor, Purpose purpose, Kind[] kinds) {
        final AMD64JitStackFrameLayout layout = new AMD64JitStackFrameLayout(classMethodActor, 2);
        final EirLocation[] parameterLocations = new EirLocation[kinds.length];
        final int stackSlots = kinds.length - layout.numberOfLocalSlots();
        final int offset = 0 - layout.operandStackOffset(stackSlots) - JitStackFrameLayout.JIT_SLOT_SIZE;
        for (int slotIndex = 0; slotIndex < kinds.length; slotIndex++) {
            parameterLocations[slotIndex] = new EirStackSlot(purpose, layout.localVariableOffset(slotIndex) + offset);
        }
        return parameterLocations;
    }

    @Override
    public EirLocation getResultLocation(Kind kind) {
        return AMD64EirRegister.General.RAX;
    }
}
