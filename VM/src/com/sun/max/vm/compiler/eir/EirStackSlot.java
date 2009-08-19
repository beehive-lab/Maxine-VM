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
package com.sun.max.vm.compiler.eir;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;

/**
 * @author Bernd Mathiske
 * @author Paul Caprioli
 */
public final class EirStackSlot extends EirLocation {

    public enum Purpose {
        PARAMETER, // incoming parameter of the method we are compiling
        LOCAL      // reusable
    }

    // TODO: this should be an index, not an offset
    /**
     * The logical offset of this stack slot. If this stack slot represents a {@linkplain #isParameter() parameter},
     * then the value is relative to the address of the stack slot holding the first stack based parameter.
     * Otherwise, the value is relative to the value of the {@linkplain EirABI#stackPointer() stack pointer}
     * after execution of the enclosing method's prologue.
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
