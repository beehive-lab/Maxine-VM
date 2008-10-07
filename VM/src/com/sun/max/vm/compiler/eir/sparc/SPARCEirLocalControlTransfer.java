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
/*VCSID=771b69dd-95fc-47a7-8c2a-f1560deafe98*/
package com.sun.max.vm.compiler.eir.sparc;

import com.sun.max.asm.sparc.*;
import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public abstract class SPARCEirLocalControlTransfer extends SPARCEirOperation implements EirControlTransfer {

    private final EirBlock _target;

    public EirBlock target() {
        return _target;
    }

    private boolean _annulDelaySlot;

    public boolean annulDelaySlot() {
        return _annulDelaySlot;
    }

    public void setAnnulDelaySlot(boolean annulDelaySlot) {
        _annulDelaySlot = annulDelaySlot;
    }

    protected AnnulBit annulBit() {
        return annulDelaySlot() ? AnnulBit.A : AnnulBit.NO_A;
    }

    protected SPARCEirLocalControlTransfer(EirBlock block, EirBlock target) {
        super(block);
        _target = target;
        _target.addPredecessor(block);
        _annulDelaySlot = true;
    }

    @Override
    public void visitSuccessorBlocks(EirBlock.Procedure procedure) {
        procedure.run(_target);
    }

    @Override
    public EirBlock selectSuccessorBlock(PoolSet<EirBlock> eligibleBlocks) {
        if (eligibleBlocks.contains(target())) {
            return target();
        }
        return null;
    }

    public void emitDelayedSlot(SPARCEirTargetEmitter emitter) {
        emitter.assembler().nop();
    }

    @Override
    public String toString() {
        String s = getClass().getSimpleName();
        if (_target != null) {
            s += " -> #" + _target.serial();
        }
        return s;
    }
}
