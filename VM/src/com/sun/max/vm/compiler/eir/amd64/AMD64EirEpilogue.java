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
package com.sun.max.vm.compiler.eir.amd64;

import com.sun.max.annotate.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64EirEpilogue extends EirEpilogue<AMD64EirInstructionVisitor, AMD64EirTargetEmitter> implements AMD64EirInstruction {

    public AMD64EirEpilogue(EirBlock block, EirMethod eirMethod,
                            EirValue[] calleeSavedValues, EirLocation[] calleeSavedRegisters,
                            EirLocation resultLocation) {
        super(block, eirMethod, calleeSavedValues, calleeSavedRegisters, resultLocation);
    }

    @Override
    public void emit(AMD64EirTargetEmitter emitter) {
        if (!eirMethod().isTemplate()) {
            final int frameSize = eirMethod().frameSize();
            if (eirMethod().classMethodActor() instanceof TrampolineMethodActor) {
                final TrampolineMethodActor trampoline = (TrampolineMethodActor) eirMethod().classMethodActor();
                if (trampoline.invocation() != TRAMPOLINE.Invocation.STATIC) {
                    // This is a trampoline that returns the call entry point of the compiled/resolved method.
                    // The epilogue routes to that entry point by pushing the entry point on top of the stack so it looks like a RIP
                    // and performs a return. This will make the activation record looking like it was called by the trampoline's caller.
                    // Note that the original arguments are preserved in the registers,
                    // and the RIP to the trampoline caller is already on the stack.
                    // Also, note that this works too with an adapter frame. In that case, the trampoline returns the adapter frame's RIP and
                    // the call entry point of the method was stored directly on the adapter frame.

                    // Last, we manipulate the frame pointer only once, and at the end, so as not minimize the work for stack walkers.
                    final AMD64GeneralRegister64 newRipRegister = ((AMD64EirRegister.General) emitter.abi().getResultLocation(Kind.WORD)).as64();
                    if (frameSize == 0) {
                        emitter.assembler().push(newRipRegister);
                    } else {
                        assert frameSize >= Kind.WORD.size();
                        final int offsetToNewRip = frameSize - Kind.WORD.size();
                        emitter.assembler().mov(offsetToNewRip, emitter.framePointer().indirect(), newRipRegister);
                        if (offsetToNewRip > 0) {
                            emitter.assembler().addq(emitter.framePointer(), offsetToNewRip);
                        }
                    }
                    return;
                }
            }
            if (frameSize != 0) {
                emitter.assembler().addq(emitter.framePointer(), frameSize);
            }
        }
    }

    @Override
    public void acceptVisitor(AMD64EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

}
