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
package com.sun.max.vm.cps.dir;

import java.util.*;

import com.sun.max.io.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.dir.transform.*;
import com.sun.max.vm.cps.ir.*;
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

    private final IrBlock.Role role;
    private final ArrayList<DirInstruction> instructions = new ArrayList<DirInstruction>();

    public DirBlock(IrBlock.Role role) {
        this.role = role;
    }

    public IrBlock.Role role() {
        return role;
    }

    public boolean isConstant() {
        return true;
    }

    public Kind kind() {
        return Kind.WORD;
    }

    @Override
    public int hashCode() {
        if (instructions.isEmpty()) {
            return 0;
        }
        return instructions.size() ^ instructions.get(0).hashCodeForBlock() + instructions.get(instructions.size() - 1).hashCodeForBlock();
    }

    @Override
    public boolean isEquivalentTo(DirValue other, DirBlockEquivalence equivalence) {
        if (other instanceof DirBlock) {
            return equivalence.evaluate(this, (DirBlock) other);
        }
        return super.isEquivalentTo(other, equivalence);
    }

    private static int nextId = 0;

    private int id = nextId++;

    /**
     * @return a unique ID within the compilation unit, for human consumption
     */
    public int id() {
        return id;
    }

    private int serial = -1;

    /**
     * All blocks in a compilation unit are ordered and this number can be used to compare the relative position of blocks in the overall sequence.
     */
    public int serial() {
        return serial;
    }

    public void setSerial(int serial) {
        this.serial = serial;
    }

    private LinkedIdentityHashSet<DirBlock> predecessors = new LinkedIdentityHashSet<DirBlock>();

    public LinkedIdentityHashSet<DirBlock> predecessors() {
        return predecessors;
    }

    public void setPredecessors(LinkedIdentityHashSet<DirBlock> predecessors) {
        this.predecessors = predecessors;
    }

    private LinkedIdentityHashSet<DirBlock> successors = new LinkedIdentityHashSet<DirBlock>();

    public LinkedIdentityHashSet<DirBlock> successors() {
        return successors;
    }

    public void setSuccessors(LinkedIdentityHashSet<DirBlock> successors) {
        this.successors = successors;
    }

    public ArrayList<DirInstruction> instructions() {
        return instructions;
    }

    public void appendInstruction(DirInstruction instruction) {
        instructions.add(instruction);
    }

    /**
     * @return whether this block only contains a goto and no other instructions
     */
    public boolean isTrivial() {
        return instructions.size() == 1 && instructions.get(0) instanceof DirGoto;
    }

    /**
     * @return whether this block contains no instructions.
     */
    public boolean isEmpty() {
        return instructions.isEmpty();
    }

    public void printTo(IndentWriter writer) {
        writer.println(this + " {");
        writer.indent();
        for (DirInstruction instruction : instructions) {
            writer.println(instruction.toString());
        }
        writer.outdent();
        writer.println("}");
    }

    private static final boolean printindIds = false;

    @Override
    public String toString() {
        if (printindIds) {
            return "Block_" + id + "#" + serial;
        }
        return "Block#" + serial;
    }

    public void cleanup() {
        predecessors = null;
        successors = null;
    }
}
