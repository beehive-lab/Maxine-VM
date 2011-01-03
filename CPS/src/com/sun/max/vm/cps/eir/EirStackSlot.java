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
package com.sun.max.vm.cps.eir;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.target.*;

/**
 * @author Bernd Mathiske
 * @author Paul Caprioli
 */
public final class EirStackSlot extends EirLocation {

    public enum Purpose {
        /**
         * A slot for an incoming argument. The offset for this type of slot is relative to the address of the first (stack based) incoming argument.
         */
        PARAMETER,

        /**
         * A slot allocated or pinned by the register allocator.
         */
        LOCAL,

        /**
         * The first (i.e. lowest address) slot of a block on the frame allocated by {@link StackAllocate}.
         */
        BLOCK
    }

    /**
     * The offset of this stack slot within the stack memory for slot of the same {@linkplain #purpose type}.
     * The platform specific backend maps this offset to an address on the stack.
     */
    public final int offset;

    public final Purpose purpose;

    @Override
    public EirStackSlot asStackSlot() {
        return this;
    }

    public EirStackSlot(Purpose purpose, int offset) {
        this.purpose = purpose;
        this.offset = offset;
    }

    @Override
    public EirLocationCategory category() {
        return EirLocationCategory.STACK_SLOT;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof EirStackSlot) {
            final EirStackSlot stackSlot = (EirStackSlot) other;
            return offset == stackSlot.offset && purpose == stackSlot.purpose;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return offset ^ purpose.ordinal();
    }

    @Override
    public String toString() {
        return purpose.name().toLowerCase() + ":" + offset;
    }

    @Override
    public TargetLocation toTargetLocation() {
        switch (purpose) {
            case PARAMETER:
                return new TargetLocation.ParameterStackSlot(Unsigned.idiv(offset, Word.size()));
            case LOCAL:
                return new TargetLocation.LocalStackSlot(Unsigned.idiv(offset, Word.size()));
            default:
                throw ProgramError.unknownCase();
        }
    }

}
