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
package com.sun.max.vm.compiler.cps.bir;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.cps.ir.*;

public class BirMethod extends AbstractIrMethod {

    private int maxStack;
    private int maxLocals;
    private byte[] code;
    private IndexedSequence<BirBlock> blocks;
    private BirBlock[] blockMap;

    private Sequence<ExceptionHandlerEntry> exceptionDispatcherTable;

    public BirMethod(ClassMethodActor classMethodActor) {
        super(classMethodActor);
    }

    public boolean isGenerated() {
        return code != null;
    }

    public int maxLocals() {
        return maxLocals;
    }

    public int maxStack() {
        return maxStack;
    }

    public byte[] code() {
        return code;
    }

    public Sequence<BirBlock> blocks() {
        return blocks;
    }

    public BirBlock[] blockMap() {
        return blockMap;
    }

    public BirBlock getBlockAt(int bytecodeAddress) {
        return blockMap[bytecodeAddress];
    }

    public Sequence<ExceptionHandlerEntry> exceptionDispatcherTable() {
        return exceptionDispatcherTable;
    }

    public void setGenerated(byte[] code, int maxStack, int maxLocals, IndexedSequence<BirBlock> blocks, BirBlock[] blockMap, Sequence<ExceptionHandlerEntry> exceptionDispatcherTable) {
        this.code = code;
        this.maxStack = maxStack;
        this.maxLocals = maxLocals;
        this.blocks = blocks;
        this.blockMap = blockMap;
        this.exceptionDispatcherTable = exceptionDispatcherTable;

        for (ExceptionHandlerEntry exceptionDispatcherEntry : exceptionDispatcherTable()) {
            final BirBlock birBlock = blockMap[exceptionDispatcherEntry.handlerPosition()];
            birBlock.setRole(IrBlock.Role.EXCEPTION_DISPATCHER);
        }
    }

    public String traceToString() {
        final CharArrayWriter charArrayWriter = new CharArrayWriter();
        final IndentWriter writer = new IndentWriter(charArrayWriter);
        writer.println("BIR: " + name());
        if (blocks != null && !blocks.isEmpty()) {
            writer.indent();
            writer.println("maxStack: " + maxStack);
            writer.println("maxLocals: " + maxLocals);
            writer.outdent();
            if (blocks != null) {
                for (BirBlock block : blocks) {
                    final ConstantPool constantPool = classMethodActor().codeAttribute().constantPool;
                    writer.print(BytecodePrinter.toString(constantPool, block.bytecodeBlock()));
                }
                for (BirBlock block : blocks) {
                    writer.println(block.toString());
                }
            }
            if (!exceptionDispatcherTable.isEmpty()) {
                writer.println("Exception handlers:");
                for (ExceptionHandlerEntry entry : exceptionDispatcherTable) {
                    writer.println(entry.toString());
                }
            }
        }
        return charArrayWriter.toString();
    }

}
