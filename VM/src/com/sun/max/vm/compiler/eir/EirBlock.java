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
package com.sun.max.vm.compiler.eir;

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.target.*;

/**
 * @author Bernd Mathiske
 * @author Thomas Wuerthinger
 */
public class EirBlock extends EirValue implements IrBlock, PoolObject {

    private final EirMethod _method;

    private int _loopNestingDepth;
    private boolean _isMoveResolverBlock;

    private PoolSet<EirVariable> _liveGen;
    private PoolSet<EirVariable> _liveKill;
    private PoolSet<EirVariable> _inverseLiveKill;
    private PoolSet<EirVariable> _liveIn;
    private PoolSet<EirVariable> _liveOut;

    public void setLiveKill(PoolSet<EirVariable> liveKill) {
        _liveKill = liveKill;

        _inverseLiveKill = PoolSet.allOf(liveKill.pool());
        for (EirVariable variable : liveKill) {
            _inverseLiveKill.remove(variable);
        }
    }

    public void setLiveGen(PoolSet<EirVariable> liveGen) {
        _liveGen = liveGen;
    }

    public PoolSet<EirVariable> liveGen() {
        return _liveGen;
    }

    public void setLiveIn(PoolSet<EirVariable> liveIn) {
        _liveIn = liveIn;
    }

    public void setLiveOut(PoolSet<EirVariable> liveOut) {
        _liveOut = liveOut;
    }

    public PoolSet<EirVariable> liveIn() {
        return _liveIn;
    }

    public PoolSet<EirVariable> liveOut() {
        return _liveOut;
    }

    public EirMethod method() {
        return _method;
    }

    private Role _role;

    public Role role() {
        return _role;
    }

    public void setRole(Role r) {
        _role = r;
    }

    private int _serial;

    public int serial() {
        return _serial;
    }

    public void setSerial(int serial) {
        _serial = serial;
    }

    public EirBlock(EirMethod method, Role role, int serial) {
        _method = method;
        _role = role;
        _serial = serial;
        fixLocation(new Location(this));
    }

    public final class Location extends EirLocation {

        private final EirBlock _block;

        public EirBlock block() {
            return _block;
        }

        private Location(EirBlock block) {
            super();
            _block = block;
        }

        @Override
        public EirLocationCategory category() {
            return EirLocationCategory.BLOCK;
        }

        @Override
        public String toString() {
            return "<block#" + _block._serial + ">";
        }

        @Override
        public TargetLocation toTargetLocation() {
            try {
                return new TargetLocation.Block(_label.position());
            } catch (AssemblyException assemblyException) {
                throw ProgramError.unexpected();
            }
        }
    }

    private VariableDeterministicSet<EirBlock> _predecessors = new LinkedIdentityHashSet<EirBlock>();

    public DeterministicSet<EirBlock> predecessors() {
        return _predecessors;
    }

    public void addPredecessor(EirBlock block) {
        block.clearSuccessorCache();
        _predecessors.add(block);
    }

    public void clearPredecessors() {
        for (EirBlock pred : _predecessors) {
            pred.clearSuccessorCache();
        }
        _predecessors.clear();
    }

    public boolean isAdjacentSuccessorOf(EirBlock other) {
        return serial() == other.serial() + 1;
    }

    public interface Procedure {

        void run(EirBlock block);
    }

    public void visitSuccessors(Procedure procedure) {
        if (_instructions.length() > 0) {
            _instructions.last().visitSuccessorBlocks(procedure);
        }
    }

    private Sequence<EirBlock> _cachedNormalSuccessors;
    private Sequence<EirBlock> _cachedAllSuccessors;

    /**
     * (tw) Returns normal unique successors of a block without exception successors.
     * @return
     */
    public Sequence<EirBlock> normalUniqueSuccessors() {

        if (_cachedNormalSuccessors != null) {
            return _cachedNormalSuccessors;
        }

        final IdentityHashSet<EirBlock> blocks = new LinkedIdentityHashSet<EirBlock>();
        final AppendableSequence<EirBlock> result = new ArrayListSequence<EirBlock>(3);

        final Procedure filterProcedure = new Procedure() {
            public void run(EirBlock block) {
                if (!blocks.contains(block)) {
                    blocks.add(block);
                    result.append(block);
                }
            }
        };

        instructions().last().visitSuccessorBlocks(filterProcedure);

        _cachedNormalSuccessors = result;
        return result;
    }

    private void clearSuccessorCache() {
        _cachedAllSuccessors = null;
        _cachedNormalSuccessors = null;
    }

    /**
     * (tw) Returns all unique successors of a block.
     * @return
     */
    public Sequence<EirBlock> allUniqueSuccessors() {

        if (_cachedAllSuccessors != null) {
            return _cachedAllSuccessors;
        }

        final IdentityHashSet<EirBlock> blocks = new LinkedIdentityHashSet<EirBlock>();
        final AppendableSequence<EirBlock> result = new ArrayListSequence<EirBlock>(3);

        final Procedure filterProcedure = new Procedure() {
            public void run(EirBlock block) {
                if (!blocks.contains(block)) {
                    blocks.add(block);
                    result.append(block);
                }
            }
        };

        for (EirInstruction instruction : _instructions) {
            instruction.visitSuccessorBlocks(filterProcedure);
        }

        _cachedAllSuccessors = result;
        return result;
    }

    public int loopNestingDepth() {
        return _loopNestingDepth;
    }

    public void setLoopNestingDepth(int loopNestingDepth) {
        assert loopNestingDepth >= 0;
        _loopNestingDepth = loopNestingDepth;
    }

    private final VariableSequence<EirInstruction> _instructions = new ArrayListSequence<EirInstruction>();

    public IndexedSequence<EirInstruction> instructions() {
        return _instructions;
    }

    public void setInstruction(int index, EirInstruction instruction) {
        instruction.setIndex(index);
        _instructions.set(index, instruction);
    }

    public void appendInstruction(EirInstruction instruction) {
        instruction.setIndex(_instructions.length());
        _instructions.append(instruction);
    }

    private void updateIndices(int startIndex) {
        for (int i = startIndex; i < _instructions.length(); i++) {
            _instructions.get(i).setIndex(i);
        }
    }

    public void insertInstruction(int index, EirInstruction instruction) {
        _instructions.insert(index, instruction);
        updateIndices(index);
    }

    public void removeInstruction(int index) {
        _instructions.remove(index);
        updateIndices(index);
    }

    private Label _label = new Label();

    public Label asLabel() {
        return _label;
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

        if (_liveIn != null) {
            printSet(writer, "liveIn", _liveIn);
        }

        if (_liveOut != null) {
            printSet(writer, "liveOut", _liveOut);
        }

        if (_liveKill != null) {
            printSet(writer, "liveKill", _liveKill);
        }

        if (_liveGen != null) {
            printSet(writer, "liveGen", _liveGen);
        }

        writer.println();
        writer.indent();
        int i = 0;
        while (i < _instructions.length()) {
            writer.println(":" + Integer.toString(i) + " (" + Integer.toString(_instructions.get(i).number()) + ") " + _instructions.get(i).toString());
            i++;
        }
        writer.outdent();
        writer.flush();
    }

    public PoolSet<EirVariable> inverseLiveKill() {
        return _inverseLiveKill;
    }

    private int _beginNumber;
    private int _endNumber;

    public int beginNumber() {
        return _beginNumber;
    }

    public void setNumbers(int begin, int end) {
        assert begin <= end;
        _beginNumber = begin;
        _endNumber = end;
    }

    public int endNumber() {
        return _endNumber;
    }

    /**
     * Substitutes all predecessor blocks based on a set of substitution pairs.
     * @param mapping a mapping object containing the substitution pairs
     */
    public void substitutePredecessorBlocks(VariableMapping<EirBlock, EirBlock> mapping) {
        for (EirBlock block : mapping.keys()) {
            if (_predecessors.contains(block)) {
                final EirBlock substitute = mapping.get(block);
                _predecessors.remove(block);
                _predecessors.add(substitute);
                substitute.clearSuccessorCache();
                block.clearSuccessorCache();
            }
        }

    }

    /**
     * Sets whether this block was only inserted in order to resolve moves during register allocation.
     */
    public void setMoveResolverBlock(boolean b) {
        _isMoveResolverBlock = b;
    }

    /**
     * Checks whether this block was only inserted in order to resolve moves during register allocation.
     */
    public boolean isMoveResolverBlock() {
        return _isMoveResolverBlock;
    }

}
