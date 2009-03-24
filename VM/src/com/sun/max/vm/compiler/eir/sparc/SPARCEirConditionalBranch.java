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
package com.sun.max.vm.compiler.eir.sparc;

import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * @author Laurent Daynes
 */
public abstract class SPARCEirConditionalBranch extends SPARCEirLocalControlTransfer {

    private final EirBlock _next;

    public EirBlock next() {
        return _next;
    }

    public SPARCEirConditionalBranch(EirBlock block, EirBlock target, EirBlock next) {
        super(block, target);
        _next = next;
        next.addPredecessor(block);
    }

    @Override
    public EirBlock selectSuccessorBlock(PoolSet<EirBlock> eligibleBlocks) {
        if (eligibleBlocks.contains(_next)) {
            return _next;
        }
        return super.selectSuccessorBlock(eligibleBlocks);
    }

    /**
     * Emit jump to next block if neither it nor the target block is adjacent to the current block.
     * Never fill the delay slot.
     * @param emitter
     */
    public void emitJumpToNext(SPARCEirTargetEmitter emitter) {
        if (!_next.isAdjacentSuccessorOf(emitter.currentEirBlock())) {
            emitter.assembler().ba(AnnulBit.A, _next.asLabel());
            emitter.assembler().nop();
        }
    }

    public abstract void emitConditionalBranch(SPARCEirTargetEmitter emitter, Label label);
    public abstract void emitReverseConditionalBranch(SPARCEirTargetEmitter emitter, Label label);

    @Override
    public void emit(SPARCEirTargetEmitter emitter) {
        if (target().isAdjacentSuccessorOf(emitter.currentEirBlock())) {
            emitReverseConditionalBranch(emitter, next().asLabel());
            emitDelayedSlot(emitter);
        } else {
            emitConditionalBranch(emitter, target().asLabel());
            emitDelayedSlot(emitter);
            emitJumpToNext(emitter);
        }
    }

    @Override
    public String toString() {
        String s = super.toString();
        if (_next != null) {
            s += " | #" + _next.serial();
        }
        return s;
    }
}
