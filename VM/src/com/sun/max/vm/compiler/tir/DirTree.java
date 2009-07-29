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

package com.sun.max.vm.compiler.tir;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.dir.transform.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.hotpath.compiler.Console;
import com.sun.max.vm.hotpath.compiler.Console.*;


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
            public void visitGuardpoint(DirGuardpoint dirGuardpoint) {
                Console.println(indent + "GUARDPOINT: \n" + dirGuardpoint.javaFrameDescriptor().toMultiLineString());
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
