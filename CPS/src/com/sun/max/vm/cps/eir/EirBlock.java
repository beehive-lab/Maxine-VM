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
package com.sun.max.vm.cps.eir;

import java.util.*;

import com.sun.max.*;
import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.ir.*;

/**
 * @author Bernd Mathiske
 * @author Thomas Wuerthinger
 */
public class EirBlock extends EirValue implements IrBlock, PoolObject {

    private final EirMethod method;

    private int loopNestingDepth;
    private boolean isMoveResolverBlock;

    private PoolSet<EirVariable> liveGen;
    private PoolSet<EirVariable> liveKill;
    private PoolSet<EirVariable> inverseLiveKill;
    private PoolSet<EirVariable> liveIn;
    private PoolSet<EirVariable> liveOut;

    public void setLiveKill(PoolSet<EirVariable> liveKill) {
        this.liveKill = liveKill;

        inverseLiveKill = PoolSet.allOf(liveKill.pool());
        for (EirVariable variable : liveKill) {
            inverseLiveKill.remove(variable);
        }
    }

    public void setLiveGen(PoolSet<EirVariable> liveGen) {
        this.liveGen = liveGen;
    }

    public PoolSet<EirVariable> liveGen() {
        return liveGen;
    }

    public void setLiveIn(PoolSet<EirVariable> liveIn) {
        this.liveIn = liveIn;
    }

    public void setLiveOut(PoolSet<EirVariable> liveOut) {
        this.liveOut = liveOut;
    }

    public PoolSet<EirVariable> liveIn() {
        return liveIn;
    }

    public PoolSet<EirVariable> liveOut() {
        return liveOut;
    }

    public EirMethod method() {
        return method;
    }

    private Role role;

    public Role role() {
        return role;
    }

    public void setRole(Role r) {
        role = r;
    }

    private int serial;

    public int serial() {
        return serial;
    }

    public void setSerial(int serial) {
        this.serial = serial;
    }

    public EirBlock(EirMethod method, Role role, int serial) {
        this.method = method;
        this.role = role;
        this.serial = serial;
        fixLocation(new Location(this));
    }

    public final class Location extends EirLocation {

        private final EirBlock block;

        public EirBlock block() {
            return block;
        }

        private Location(EirBlock block) {
            this.block = block;
        }

        @Override
        public EirLocationCategory category() {
            return EirLocationCategory.BLOCK;
        }

        @Override
        public String toString() {
            return "<block#" + block.serial + ">";
        }

        @Override
        public TargetLocation toTargetLocation() {
            try {
                return new TargetLocation.Block(label.position());
            } catch (AssemblyException assemblyException) {
                throw ProgramError.unexpected();
            }
        }
    }

    private LinkedIdentityHashSet<EirBlock> predecessors = new LinkedIdentityHashSet<EirBlock>();

    public LinkedIdentityHashSet<EirBlock> predecessors() {
        return predecessors;
    }

    public void addPredecessor(EirBlock block) {
        block.clearSuccessorCache();
        predecessors.add(block);
    }

    public void clearPredecessors() {
        for (EirBlock pred : predecessors) {
            pred.clearSuccessorCache();
        }
        predecessors.clear();
    }

    public boolean isAdjacentSuccessorOf(EirBlock other) {
        return serial() == other.serial() + 1;
    }

    public interface Procedure {

        void run(EirBlock block);
    }

    public void visitSuccessors(Procedure procedure) {
        if (instructions.size() > 0) {
            instructions.get(instructions.size() - 1).visitSuccessorBlocks(procedure);
        }
    }

    private List<EirBlock> cachedNormalSuccessors;
    private List<EirBlock> cachedAllSuccessors;

    /**
     * (tw) Returns normal unique successors of a block without exception successors.
     * @return
     */
    public List<EirBlock> normalUniqueSuccessors() {

        if (cachedNormalSuccessors != null) {
            return cachedNormalSuccessors;
        }

        final IdentityHashSet<EirBlock> blocks = new LinkedIdentityHashSet<EirBlock>();
        final List<EirBlock> result = new ArrayList<EirBlock>(3);

        final Procedure filterProcedure = new Procedure() {
            public void run(EirBlock block) {
                if (!blocks.contains(block)) {
                    blocks.add(block);
                    result.add(block);
                }
            }
        };

        Utils.last(instructions()).visitSuccessorBlocks(filterProcedure);

        cachedNormalSuccessors = result;
        return result;
    }

    private void clearSuccessorCache() {
        cachedAllSuccessors = null;
        cachedNormalSuccessors = null;
    }

    /**
     * (tw) Returns all unique successors of a block.
     * @return
     */
    public List<EirBlock> allUniqueSuccessors() {

        if (cachedAllSuccessors != null) {
            return cachedAllSuccessors;
        }

        final IdentityHashSet<EirBlock> blocks = new LinkedIdentityHashSet<EirBlock>();
        final List<EirBlock> result = new ArrayList<EirBlock>(3);

        final Procedure filterProcedure = new Procedure() {
            public void run(EirBlock block) {
                if (!blocks.contains(block)) {
                    blocks.add(block);
                    result.add(block);
                }
            }
        };

        for (EirInstruction instruction : instructions) {
            instruction.visitSuccessorBlocks(filterProcedure);
        }

        cachedAllSuccessors = result;
        return result;
    }

    public int loopNestingDepth() {
        return loopNestingDepth;
    }

    public void setLoopNestingDepth(int loopNestingDepth) {
        assert loopNestingDepth >= 0;
        this.loopNestingDepth = loopNestingDepth;
    }

    private final ArrayList<EirInstruction> instructions = new ArrayList<EirInstruction>();

    public List<EirInstruction> instructions() {
        return instructions;
    }

    public void setInstruction(int index, EirInstruction instruction) {
        instruction.setIndex(index);
        instructions.set(index, instruction);
    }

    public void appendInstruction(EirInstruction instruction) {
        instruction.setIndex(instructions.size());
        instructions.add(instruction);
    }

    private void updateIndices(int startIndex) {
        for (int i = startIndex; i < instructions.size(); i++) {
            instructions.get(i).setIndex(i);
        }
    }

    public void insertInstruction(int index, EirInstruction instruction) {
        instructions.add(index, instruction);
        updateIndices(index);
    }

    public void removeInstruction(int index) {
        instructions.remove(index);
        updateIndices(index);
    }

    private Label label = new Label();

    public Label asLabel() {
        return label;
    }

    public <EirTargetEmitter_Type extends EirTargetEmitter> void emit(EirTargetEmitter_Type emitter) {
        emitter.setCurrentEirBlock(this);
        emitter.assembler().bindLabel(asLabel());
        for (EirInstruction<?, EirTargetEmitter_Type> instruction : instructions()) {
            instruction.emit(emitter);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + serial();
    }

    @Override
    public void cleanup() {
        for (EirInstruction instruction : instructions()) {
            instruction.cleanup();
        }
    }

    public void cleanupAfterEmitting() {
        for (EirInstruction instruction : instructions()) {
            instruction.cleanupAfterEmitting();
        }
    }

    public static void printTo(IndentWriter writer, Iterable<EirBlock> eirBlocks) {
        for (EirBlock eirBlock : eirBlocks) {
            eirBlock.printTo(writer);
            writer.println();
            writer.flush();
        }
    }

    private void printSet(IndentWriter writer, String name, PoolSet<EirVariable> set) {

        writer.print(name + "={");

        for (EirVariable variable : set) {
            writer.print(variable.toString() + "; ");
        }

        writer.print("}");
    }

    public void printTo(IndentWriter writer) {
        writer.print(toString());

        if (this.isMoveResolverBlock()) {
            writer.print("(MOVE_RESOLVER)");
        }

        if (liveIn != null) {
            printSet(writer, "liveIn", liveIn);
        }

        if (liveOut != null) {
            printSet(writer, "liveOut", liveOut);
        }

        if (liveKill != null) {
            printSet(writer, "liveKill", liveKill);
        }

        if (liveGen != null) {
            printSet(writer, "liveGen", liveGen);
        }

        writer.println();
        writer.indent();
        int i = 0;
        while (i < instructions.size()) {
            writer.println(":" + Integer.toString(i) + " (" + Integer.toString(instructions.get(i).number()) + ") " + instructions.get(i).toString());
            i++;
        }
        writer.outdent();
        writer.flush();
    }

    public PoolSet<EirVariable> inverseLiveKill() {
        return inverseLiveKill;
    }

    private int beginNumber;
    private int endNumber;

    public int beginNumber() {
        return beginNumber;
    }

    public void setNumbers(int begin, int end) {
        assert begin <= end;
        beginNumber = begin;
        endNumber = end;
    }

    public int endNumber() {
        return endNumber;
    }

    /**
     * Substitutes all predecessor blocks based on a set of substitution pairs.
     * @param mapping a mapping object containing the substitution pairs
     */
    public void substitutePredecessorBlocks(Mapping<EirBlock, EirBlock> mapping) {
        for (EirBlock block : mapping.keys()) {
            if (predecessors.contains(block)) {
                final EirBlock substitute = mapping.get(block);
                predecessors.remove(block);
                predecessors.add(substitute);
                substitute.clearSuccessorCache();
                block.clearSuccessorCache();
            }
        }

    }

    /**
     * Sets whether this block was only inserted in order to resolve moves during register allocation.
     */
    public void setMoveResolverBlock(boolean b) {
        isMoveResolverBlock = b;
    }

    /**
     * Checks whether this block was only inserted in order to resolve moves during register allocation.
     */
    public boolean isMoveResolverBlock() {
        return isMoveResolverBlock;
    }

}
