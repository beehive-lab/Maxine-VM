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
package com.sun.max.vm.compiler.dir;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.vm.compiler.dir.transform.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;

/**
 * A basic block of DIR instructions.
 *
 * Calls do not terminate our kind of basic blocks.
 * Only local control flow instructions do: switch, return, throw.
 *
 * @author Bernd Mathiske
 */
public class DirBlock extends DirValue implements IrBlock {

    private IrBlock.Role _role;
    private final VariableSequence<DirInstruction> _instructions = new ArrayListSequence<DirInstruction>();

    public DirBlock(IrBlock.Role role) {
        _role = role;
    }

    public IrBlock.Role role() {
        return _role;
    }

    public boolean isConstant() {
        return true;
    }

    public Kind kind() {
        return Kind.WORD;
    }

    @Override
    public int hashCode() {
        if (_instructions.isEmpty()) {
            return 0;
        }
        return _instructions.length() ^ _instructions.first().hashCodeForBlock() + _instructions.last().hashCodeForBlock();
    }

    @Override
    public boolean isEquivalentTo(DirValue other, DirBlockEquivalence equivalence) {
        if (other instanceof DirBlock) {
            return equivalence.evaluate(this, (DirBlock) other);
        }
        return super.isEquivalentTo(other, equivalence);
    }

    private static int _nextId = 0;

    private int _id = _nextId++;

    /**
     * @return a unique ID within the compilation unit, for human consumption
     */
    public int id() {
        return _id;
    }

    private int _serial = -1;

    /**
     * All blocks in a compilation unit are ordered and this number can be used to compare the relative position of blocks in the overall sequence.
     */
    public int serial() {
        return _serial;
    }

    public void setSerial(int serial) {
        _serial = serial;
    }

    private GrowableDeterministicSet<DirBlock> _predecessors = new LinkedIdentityHashSet<DirBlock>();

    public GrowableDeterministicSet<DirBlock> predecessors() {
        return _predecessors;
    }

    public void setPredecessors(GrowableDeterministicSet<DirBlock> predecessors) {
        _predecessors = predecessors;
    }

    private GrowableDeterministicSet<DirBlock> _successors = new LinkedIdentityHashSet<DirBlock>();

    public GrowableDeterministicSet<DirBlock> successors() {
        return _successors;
    }

    public void setSuccessors(GrowableDeterministicSet<DirBlock> successors) {
        _successors = successors;
    }

    public VariableSequence<DirInstruction> instructions() {
        return _instructions;
    }

    public void appendInstruction(DirInstruction instruction) {
        _instructions.append(instruction);
    }

    /**
     * @return whether this block only contains a goto and no other instructions
     */
    public boolean isTrivial() {
        return _instructions.length() == 1 && _instructions.first() instanceof DirGoto;
    }

    /**
     * @return whether this block contains no instructions.
     */
    public boolean isEmpty() {
        return _instructions.isEmpty();
    }

    public void printTo(IndentWriter writer) {
        writer.println(this + " {");
        writer.indent();
        for (DirInstruction instruction : _instructions) {
            writer.println(instruction.toString());
        }
        writer.outdent();
        writer.println("}");
    }

    private static final boolean _printindIds = false;

    @Override
    public String toString() {
        if (_printindIds) {
            return "Block_" + _id + "#" + _serial;
        }
        return "Block#" + _serial;
    }

    public void cleanup() {
        _predecessors = null;
        _successors = null;
    }
}
