/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir.interpreter;

import java.lang.reflect.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.hotpath.compiler.Console.*;
import com.sun.max.vm.cps.ir.interpreter.eir.amd64.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.cps.tir.target.*;
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
                if (instruction instanceof EirInfopoint) {
                    final EirInfopoint guardpoint = (EirInfopoint) instruction;
                    if (guardpoint.opcode == Bytecodes.INFO) {
                        descriptors.add(guardpoint.javaFrameDescriptor());
                    }
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
        final AMD64JVMSFrameLayout layout = new AMD64JVMSFrameLayout(executionState.last().method(), 2);

        // Push parameter locals.
        for (int i = 0; i < layout.numberOfParameterSlots(); i++) {
            decrementStackPointer(VMFrameLayout.STACK_SLOT_SIZE);
            cpu().push(arguments[i]);
        }

        // +-------------+
        // | P P P P ... |
        // +-------------+

        // Reserve space for all other frame elments.
        final int offset = layout.localVariableOffset(layout.numberOfParameterSlots() - 1) - layout.localVariableOffset(layout.numberOfParameterSlots());
        decrementStackPointer(offset - AMD64JVMSFrameLayout.JVMS_SLOT_SIZE);

        // +-------------+-------+-------+-----+
        // | P P P P ... | xxxxx | xxxxx | xxx |
        // +-------------+-------+-------+-----+

        // Push remaining slot values.
        for (int i = layout.numberOfParameterSlots(); i < arguments.length; i++) {
            decrementStackPointer(JVMSFrameLayout.STACK_SLOT_SIZE);
            cpu().push(arguments[i]);
        }
    }

    private void decrementStackPointer(int size) {
        cpu().writeStackPointer(cpu().readStackPointer().minus(size));
    }
}
