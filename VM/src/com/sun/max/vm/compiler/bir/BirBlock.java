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
 * A basic block is a control flow unit,
 * a linear sequence of operations without explicit intermittent control flow change.
 *
 * @author Bernd Mathiske
 */
public class BirBlock implements IrBlock {

    private BytecodeBlock _bytecodeBlock;

    public BytecodeBlock bytecodeBlock() {
        return _bytecodeBlock;
    }

    private IrBlock.Role _role;

    public IrBlock.Role role() {
        return _role;
    }

    public void setRole(IrBlock.Role role) {
        _role = role;
        if (role == IrBlock.Role.EXCEPTION_DISPATCHER) {
            haveSafepoint();
        }
    }

    private boolean _hasSafepoint;

    public boolean hasSafepoint() {
        return _hasSafepoint;
    }

    public void haveSafepoint() {
        _hasSafepoint = true;
    }

    public BirBlock(BytecodeBlock bytecodeBlock) {
        _bytecodeBlock = bytecodeBlock;
        _role = IrBlock.Role.NORMAL;
    }

    @Override
    public int hashCode() {
        return _bytecodeBlock.start() * _bytecodeBlock.end();
    }

    private GrowableDeterministicSet<BirBlock> _predecessors = new LinkedIdentityHashSet<BirBlock>();

    public void addPredecessor(BirBlock predecessor) {
        _predecessors.add(predecessor);
    }

    public DeterministicSet<BirBlock> predecessors() {
        return _predecessors;
    }

    private GrowableDeterministicSet<BirBlock> _successors = new LinkedIdentityHashSet<BirBlock>();

    public void addSuccessor(BirBlock successor) {
        _successors.add(successor);
    }

    public DeterministicSet<BirBlock> successors() {
        return _successors;
    }

    public int serial() {
        return _bytecodeBlock.start();
    }

    @Override
    public String toString() {
        return "<" + _role + ": " + _bytecodeBlock + ">";
    }

    public boolean isReachable() {
        return !predecessors().isEmpty() || _bytecodeBlock.start() == 0 || _role == IrBlock.Role.EXCEPTION_DISPATCHER;
    }
}
