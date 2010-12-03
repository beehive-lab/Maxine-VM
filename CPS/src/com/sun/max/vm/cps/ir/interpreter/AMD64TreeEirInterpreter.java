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
package com.sun.max.vm.cps.ir.interpreter;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.ir.interpreter.eir.amd64.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.cps.tir.target.*;
import com.sun.max.vm.hotpath.compiler.*;
import com.sun.max.vm.hotpath.compiler.Console.Color;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.amd64.*;
import com.sun.max.vm.value.*;

/**
 * Incomplete!
 */
public class AMD64TreeEirInterpreter extends AMD64EirInterpreter {
    private BirState executionState;
    private TirTree tree;

    public AMD64TreeEirInterpreter() {
        super(eirGeneratorScheme().eirGenerator());
    }

    public TirGuard execute(TirTree tree, BirState state) {
        final TargetTree targetTree = tree.targetTree();

        executionState = state;
        this.tree = tree;

        final Value[] arguments = state.getSlots(0, tree.entryState().length());
        try {
            return (TirGuard) execute(targetTree.treeEirMethod(), arguments).asObject();
        } catch (InvocationTargetException e) {
            throw ProgramError.unexpected(e);
        }
    }

    private List<EirJavaFrameDescriptor> getGuardpointDescriptors(EirMethod method) {
        final List<EirJavaFrameDescriptor> descriptors = new ArrayList<EirJavaFrameDescriptor>();
        for (EirBlock block : method.blocks()) {
            for (EirInstruction instruction : block.instructions()) {
                if (instruction instanceof EirGuardpoint) {
                    final EirGuardpoint guardpoint = (EirGuardpoint) instruction;
                    descriptors.add(guardpoint.javaFrameDescriptor());
                }
            }
        }
        return descriptors;
    }

    private void writeback(TirGuard guard) {

        final List<EirJavaFrameDescriptor> descriptors = getGuardpointDescriptors(tree.targetTree().treeEirMethod());

        final EirJavaFrameDescriptor descriptor = descriptors.get(tree.getNumber(guard));
        Console.println("EXITED AT: " + descriptor.toMultiLineString());

        for (int i = 0; i < descriptor.locals.length; i++) {
            Console.println(Color.TEAL, "SLOT: " + i + " <== " + cpu().read(descriptor.locals[i].location()) + " from " + descriptor.locals[i].location());
            executionState.store(i, cpu().read(descriptor.locals[i].location()));
        }

        for (int i = 0; i < descriptor.stackSlots.length; i++) {
            Console.println(Color.TEAL, "SLOT: " + (i + descriptor.locals.length) + " <== " + cpu().read(descriptor.stackSlots[i].location()) + " from " + descriptor.stackSlots[i].location());
            executionState.store(i + descriptor.locals.length, cpu().read(descriptor.stackSlots[i].location()));
        }

        executionState.println();
    }

    private TirGuard exitGuard;

    @Override
    public void visit(EirAssignment assignment) {
        final Value read = cpu().read(assignment.sourceOperand().location());
        if (read.kind().isReference) {
            exitGuard = (TirGuard) read.asObject();
        }
        super.visit(assignment);
    }

    @Override
    public void visit(EirCall call) throws InvocationTargetException {
        writeback(exitGuard);
        super.visit(call);
    }

    private static AMD64EirGeneratorScheme eirGeneratorScheme() {
        return (AMD64EirGeneratorScheme) CPSCompiler.Static.compiler();
    }

    /**
     * Map tree parameters onto their original JIT frame locations, such that no adapter frames are required when
     * executing a tree. The tree frame takes over the JIT frame.
     *
     * +-------------+-------+-------+-----+-------+--------+
     * | P P P P ... | RA FP | xxxxx | T T | L L L | S S .. |
     * +-------------+-------+-------+-----+-------+--------+
     *
     *                   |------ Frame Size -------|
     *
     * P  = parameter local slot
     * RA = caller's return address
     * FP = caller's frame pointer
     * xx = frame alignment
     * T  = template slot
     * L  = non-parameter local slot
     * S  = stack slot
     *
     */

    @Override
    protected void setupParameters(EirLocation[] locations, Value... arguments) {
        final AMD64JitStackFrameLayout layout = new AMD64JitStackFrameLayout(executionState.last().method(), 2);

        // Push parameter locals.
        for (int i = 0; i < layout.numberOfParameterSlots(); i++) {
            decrementStackPointer(CompiledStackFrameLayout.STACK_SLOT_SIZE);
            cpu().push(arguments[i]);
        }

        // +-------------+
        // | P P P P ... |
        // +-------------+

        // Reserve space for all other frame elments.
        final int offset = layout.localVariableOffset(layout.numberOfParameterSlots() - 1) - layout.localVariableOffset(layout.numberOfParameterSlots());
        decrementStackPointer(offset - AMD64JitStackFrameLayout.JIT_SLOT_SIZE);

        // +-------------+-------+-------+-----+
        // | P P P P ... | xxxxx | xxxxx | xxx |
        // +-------------+-------+-------+-----+

        // Push remaining slot values.
        for (int i = layout.numberOfParameterSlots(); i < arguments.length; i++) {
            decrementStackPointer(JitStackFrameLayout.STACK_SLOT_SIZE);
            cpu().push(arguments[i]);
        }
    }

    private void decrementStackPointer(int size) {
        cpu().writeStackPointer(cpu().readStackPointer().minus(size));
    }
}
