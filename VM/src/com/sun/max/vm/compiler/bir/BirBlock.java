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
package com.sun.max.vm.compiler.bir;

import com.sun.max.collect.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.ir.*;

/**
 * A basic block is a control flow unit, a linear sequence of operations without any control flow.
 *
 * @author Bernd Mathiske
 */
public class BirBlock implements IrBlock {

    private BytecodeBlock bytecodeBlock;
    private IrBlock.Role role;
    private GrowableDeterministicSet<BirBlock> predecessors = new LinkedIdentityHashSet<BirBlock>();
    private GrowableDeterministicSet<BirBlock> successors = new LinkedIdentityHashSet<BirBlock>();
    private boolean hasSafepoint;

    public BytecodeBlock bytecodeBlock() {
        return bytecodeBlock;
    }

    public IrBlock.Role role() {
        return role;
    }

    public void setRole(IrBlock.Role role) {
        this.role = role;
    }

    public boolean hasSafepoint() {
        return hasSafepoint;
    }

    public void haveSafepoint() {
        hasSafepoint = true;
    }

    public BirBlock(BytecodeBlock bytecodeBlock) {
        this.bytecodeBlock = bytecodeBlock;
        this.role = IrBlock.Role.NORMAL;
    }

    @Override
    public int hashCode() {
        return bytecodeBlock.start * bytecodeBlock.end;
    }

    public void addPredecessor(BirBlock predecessor) {
        predecessors.add(predecessor);
    }

    public DeterministicSet<BirBlock> predecessors() {
        return predecessors;
    }

    public void addSuccessor(BirBlock successor) {
        successors.add(successor);
    }

    public DeterministicSet<BirBlock> successors() {
        return successors;
    }

    public int serial() {
        return bytecodeBlock.start;
    }

    @Override
    public String toString() {
        return "<" + role + ": " + bytecodeBlock + ">";
    }

    public boolean isReachable() {
        return !predecessors().isEmpty() || bytecodeBlock.start == 0 || role == IrBlock.Role.EXCEPTION_DISPATCHER;
    }
}
