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
package com.sun.max.vm.compiler.eir.amd64.unix;

import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.EirStackSlot.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.amd64.*;
import com.sun.max.vm.type.*;

/**
 * @author Michael Bebenita
 */
public class UnixAMD64EirTreeABI extends UnixAMD64EirJavaABI {
    public UnixAMD64EirTreeABI(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

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
