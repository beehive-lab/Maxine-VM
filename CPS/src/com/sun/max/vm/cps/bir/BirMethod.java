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
package com.sun.max.vm.cps.bir;

import java.io.*;
import java.util.*;

import com.sun.max.io.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.cps.ir.*;

public class BirMethod extends AbstractIrMethod {

    private int maxStack;
    private int maxLocals;
    private byte[] code;
    private List<BirBlock> blocks;
    private BirBlock[] blockMap;

    private ExceptionHandlerEntry[] exceptionDispatcherTable;

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

    public List<BirBlock> blocks() {
        return blocks;
    }

    public BirBlock[] blockMap() {
        return blockMap;
    }

    public BirBlock getBlockAt(int bytecodeAddress) {
        return blockMap[bytecodeAddress];
    }

    public ExceptionHandlerEntry[] exceptionDispatcherTable() {
        return exceptionDispatcherTable;
    }

    public void setGenerated(byte[] code, int maxStack, int maxLocals, List<BirBlock> blocks, BirBlock[] blockMap, ExceptionHandlerEntry[] exceptionDispatcherTable) {
        this.code = code;
        this.maxStack = maxStack;
        this.maxLocals = maxLocals;
        this.blocks = blocks;
        this.blockMap = blockMap;
        this.exceptionDispatcherTable = exceptionDispatcherTable;

        for (ExceptionHandlerEntry exceptionDispatcherEntry : exceptionDispatcherTable()) {
            final BirBlock birBlock = blockMap[exceptionDispatcherEntry.handlerBCI()];
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
                    final ConstantPool constantPool = classMethodActor().codeAttribute().cp;
                    writer.print(BytecodePrinter.toString(constantPool, block.bytecodeBlock()));
                }
                for (BirBlock block : blocks) {
                    writer.println(block.toString());
                }
            }
            if (exceptionDispatcherTable.length != 0) {
                writer.println("Exception handlers:");
                for (ExceptionHandlerEntry entry : exceptionDispatcherTable) {
                    writer.println(entry.toString());
                }
            }
        }
        return charArrayWriter.toString();
    }

}
