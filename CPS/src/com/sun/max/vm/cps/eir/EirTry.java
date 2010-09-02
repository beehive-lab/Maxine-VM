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

import com.sun.max.collect.*;

/**
 * This pseudo-instruction (aka directive) directs the control flow of subsequent exceptions towards a given catch
 * block.
 *
 * @author Bernd Mathiske
 */
public class EirTry<EirInstructionVisitor_Type extends EirInstructionVisitor, EirTargetEmitter_Type extends EirTargetEmitter> extends EirInstruction<EirInstructionVisitor_Type, EirTargetEmitter_Type> {

    private EirBlock catchBlock;

    public EirBlock catchBlock() {
        return catchBlock;
    }

    public EirTry(EirBlock block, EirBlock catchBlock) {
        super(block);
        this.catchBlock = catchBlock;
    }

    @Override
    public String toString() {
        if (catchBlock == null) {
            return "try";
        }
        return "try -> #" + catchBlock.serial();
    }

    @Override
    public boolean isRedundant() {
        for (int i = index() - 1; i >= 0; i--) {
            final EirInstruction instruction = block().instructions().get(i);
            if (instruction instanceof EirCall) {
                return false;
            }
            if (instruction instanceof EirTry) {
                final EirTry other = (EirTry) instruction;
                return other.catchBlock == catchBlock;
            }
        }
        return false;
    }

    @Override
    public void visitSuccessorBlocks(EirBlock.Procedure procedure) {
        super.visitSuccessorBlocks(procedure);
        if (catchBlock != null) {
            procedure.run(catchBlock);
        }
    }

    @Override
    public void substituteSuccessorBlocks(Mapping<EirBlock, EirBlock> map) {
        super.substituteSuccessorBlocks(map);
        if (catchBlock != null && map.containsKey(catchBlock)) {
            catchBlock = map.get(catchBlock);
        }
    }

    @Override
    public void acceptVisitor(EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void emit(EirTargetEmitter emitter) {
        emitter.addCatching(catchBlock);
    }

}
