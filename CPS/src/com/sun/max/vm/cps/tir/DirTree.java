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

package com.sun.max.vm.cps.tir;

import com.sun.cri.bytecode.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.dir.transform.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.hotpath.compiler.Console.*;

public class DirTree extends DirMethod {
    private final TirTree tree;

    public DirTree(TirTree tree, ClassMethodActor classMethodActor) {
        super(classMethodActor);
        this.tree = tree;
    }

    public void print() {
        Console.printDivider("DIR TREE: " + NameMap.nameOf(tree));
        Console.print("entry state: ");
        tree.entryState().println();
        Console.print("parameters: ");
        for (DirVariable parameter : parameters()) {
            Console.print(Color.LIGHTGREEN, parameter + " ");
        }
        Console.println();
        final String indent = "    ";

        final DirAdapter nameAdapter = new DirAdapter() {
            @Override
            public void visitInstruction(DirInstruction dirInstruction) {
                Console.println(indent + dirInstruction.toString());
            }

            @Override
            public void visitInfopoint(DirInfopoint infopoint) {
                if (infopoint.opcode == Bytecodes.INFO) {
                    Console.println(indent + "GUARDPOINT: \n" + infopoint.javaFrameDescriptor().toMultiLineString());
                }
            }
        };

        if (blocks() != null) {
            for (DirBlock block : blocks()) {
                Console.printThinDivider("BLOCK: " + block.serial());
                for (DirInstruction instruction : block.instructions()) {
                    instruction.acceptVisitor(nameAdapter);
                }
            }
        }

        Console.printThinDivider();

        /*
        String parametersAsString = " ";
        if (_parameters == null) {
            parametersAsString = "null";
        } else {
            for (DirVariable parameter : _parameters) {
                parametersAsString += parameter + " ";
            }
        }
        final CharArrayWriter charArrayWriter = new CharArrayWriter();
        final IndentWriter writer = new IndentWriter(charArrayWriter);
        writer.println("DIR(" + parametersAsString + ")");
        if (_blocks != null) {
            writer.indent();
            for (DirBlock block : _blocks) {
                block.trace(writer);
            }
            writer.outdent();
        }
        return charArrayWriter.toString();
        */

    }
}
