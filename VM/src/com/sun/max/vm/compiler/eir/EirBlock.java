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
/*VCSID=3c62b031-c305-41fb-ba8e-22d2292de3ec*/
package com.sun.max.vm.compiler.eir;

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.target.*;

/**
 * @author Bernd Mathiske
 */
public class EirBlock extends EirValue implements IrBlock, PoolObject {

    private final EirMethod _method;

    public EirMethod method() {
        return _method;
    }

    private Role _role;

    public Role role() {
        return _role;
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
        _predecessors.add(block);
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

    private int _loopNestingDepth;

    public int loopNestingDepth() {
        return _loopNestingDepth;
    }

    public void setLoopNestingDepth(int loopNestingDepth) {
        _loopNestingDepth = loopNestingDepth;
    }

    public void incrementLoopNestingDepth() {
        _loopNestingDepth++;
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

    public void printTo(IndentWriter writer) {
        writer.println(toString());
        writer.indent();
        int i = 0;
        while (i < _instructions.length()) {
            writer.println(":" + Integer.toString(i) + "   " + _instructions.get(i).toString());
            i++;
        }
        writer.outdent();
    }

}
