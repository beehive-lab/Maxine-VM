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
package com.sun.max.vm.cps.ir.igv.amd64;

import java.lang.reflect.*;

import com.sun.max.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.ir.igv.*;

/**
 * Visiting AMD64 instructions to generate a graph.
 *
 * @author Thomas Wuerthinger
 */
public class AMD64GraphEirInstructionVisitor extends AMD64EirInstructionAdapter {

    private GraphWriter.Node node;

    public AMD64GraphEirInstructionVisitor(GraphWriter.Node node) {
        this.node = node;
    }

    @Override
    public void visitInstruction(EirInstruction eirInstruction) {
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
        this.node.getProperties().setProperty("name", call.function().toString());
        this.node.getProperties().setProperty("result", call.result().toString());
        this.node.getProperties().setProperty("arguments", Utils.toString(call.arguments, ", "));
        this.node.getProperties().setProperty("callerSavedOperands", Utils.toString(call.callerSavedOperands(), ", "));
        this.node.getProperties().setProperty("argumentCount", Integer.toString(call.arguments.length));
        if (call.result() != null) {
            this.node.getProperties().setProperty("result", call.result().toString());
        }
    }

    @Override
    public void visit(EirInfopoint safepoint) {
        super.visit(safepoint);
        this.node.getProperties().setProperty("name", "safepoint");
        this.node.getProperties().setProperty("dump_spec", safepoint.toString());
        this.node.getProperties().setProperty("bci", Integer.toString(safepoint.javaFrameDescriptor().bytecodePosition));

    }
}
