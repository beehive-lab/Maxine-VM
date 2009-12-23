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
package com.sun.max.vm.compiler.cps.eir;

/**
 * This instruction is used to effectively delete another instruction by overwriting it,
 * sparing the effort to restructure the block it is placed into.
 * No filler is supposed to have any effect.
 *
 * @author Bernd Mathiske
 */
public final class EirFiller extends EirInstruction {

    public EirFiller(EirBlock block) {
        super(block);
    }

    @Override
    public boolean isRedundant() {
        return true;
    }

    @Override
    public void addLiveVariable(EirVariable variable) {
        // Do nothing: this type of instruction does not record variable liveness as it is simply a place holder for an
        // instruction deleted by the register allocator
    }

    @Override
    public void removeLiveVariable(EirVariable variable) {
        // Do nothing: this type of instruction does not record variable liveness as it is simply a place holder for an
        // instruction deleted by the register allocator
    }

    @Override
    public void emit(EirTargetEmitter emitter) {
    }

    @Override
    public void acceptVisitor(EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "<filler>";
    }

}
