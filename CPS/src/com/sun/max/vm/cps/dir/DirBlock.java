/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
