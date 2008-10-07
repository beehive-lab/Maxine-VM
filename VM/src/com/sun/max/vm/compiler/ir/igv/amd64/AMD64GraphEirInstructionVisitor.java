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
/*VCSID=a4d5c02a-a89c-4fb4-8160-c2bda8028024*/
package com.sun.max.vm.compiler.ir.igv.amd64;

import java.lang.reflect.*;

import com.sun.max.lang.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.compiler.ir.igv.*;

/**
 * Visiting AMD64 instructions to generate a graph.
 *
 * @author Thomas Wuerthinger
 */
public class AMD64GraphEirInstructionVisitor extends AMD64EirInstructionAdapter {

    private GraphWriter.Node _node;

    public AMD64GraphEirInstructionVisitor(GraphWriter.Node node) {
        _node = node;
    }

    @Override
    public void visitInstruction(EirInstruction eirInstruction) {
        final GraphWriter.Node node = _node;
        super.visitInstruction(eirInstruction);
        node.getProperties().setProperty("name", eirInstruction.toString());
        node.getProperties().setProperty("class", eirInstruction.getClass().toString());

        for (AMD64EirRegister register : AMD64EirRegister.pool()) {
            if (eirInstruction.hasRegister(register)) {
                node.getProperties().setProperty(register.toString(), "true");
            }
        }
    }

    @Override
    public void visit(EirAssignment instruction) {
        super.visit(instruction);
        final GraphWriter.Node node = _node;
        node.getProperties().setProperty("destination", instruction.destinationOperand().toString());
    }

    @Override
    public void visit(EirBreakpoint instruction) {
        // TODO Auto-generated method stub
        super.visit(instruction);
    }

    @Override
    public void visit(EirCatch instruction) {
        // TODO Auto-generated method stub
        super.visit(instruction);
    }

    @Override
    public void visit(EirEpilogue instruction) {
        // TODO Auto-generated method stub
        super.visit(instruction);
    }

    @Override
    public void visit(EirFiller instruction) {
        // TODO Auto-generated method stub
        super.visit(instruction);
    }

    @Override
    public void visit(EirPrologue instruction) {
        // TODO Auto-generated method stub
        super.visit(instruction);
    }

    @Override
    public void visit(EirTry instruction) {
        // TODO Auto-generated method stub
        super.visit(instruction);
    }

    @Override
    public void visit(EirCall call) throws InvocationTargetException {
        super.visit(call);
        final GraphWriter.Node node = _node;

        node.getProperties().setProperty("name", call.function().toString());
        node.getProperties().setProperty("result", call.result().toString());
        node.getProperties().setProperty("arguments", Arrays.toString(call.arguments()));
        node.getProperties().setProperty("callerSavedOperands", Arrays.toString(call.callerSavedOperands()));
        node.getProperties().setProperty("argumentCount", Integer.toString(call.arguments().length));
        if (call.result() != null) {
            node.getProperties().setProperty("result", call.result().toString());
        }
    }

    @Override
    public void visit(EirSafepoint safepoint) {
        super.visit(safepoint);
        final GraphWriter.Node node = _node;
        node.getProperties().setProperty("name", "safepoint");
        node.getProperties().setProperty("dump_spec", safepoint.toString());
        node.getProperties().setProperty("bci", Integer.toString(safepoint.javaFrameDescriptor().bytecodeLocation().position()));

    }
}
