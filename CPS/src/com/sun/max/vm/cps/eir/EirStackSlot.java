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
