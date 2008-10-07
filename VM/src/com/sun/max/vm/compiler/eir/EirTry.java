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
/*VCSID=8b277448-0b03-490f-b0be-cba2835b6b83*/
package com.sun.max.vm.compiler.eir;

/**
 * This pseudo-instruction (aka directive) directs the control flow
 * of subsequent exceptions towards a given catch block.
 * 
 * @author Bernd Mathiske
 */
public class EirTry extends EirInstruction {

    private final EirBlock _catchBlock;

    public EirBlock catchBlock() {
        return _catchBlock;
    }

    public EirTry(EirBlock block, EirBlock catchBlock) {
        super(block);
        _catchBlock = catchBlock;
    }

    @Override
    public String toString() {
        if (_catchBlock == null) {
            return "try";
        }
        return "try -> #" + _catchBlock.serial();
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
                return other._catchBlock == _catchBlock;
            }
        }
        return false;
    }

    @Override
    public void visitSuccessorBlocks(EirBlock.Procedure procedure) {
        if (_catchBlock != null) {
            procedure.run(_catchBlock);
        }
    }

    @Override
    public void acceptVisitor(EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void emit(EirTargetEmitter emitter) {
        emitter.addCatching(_catchBlock);
    }

}
