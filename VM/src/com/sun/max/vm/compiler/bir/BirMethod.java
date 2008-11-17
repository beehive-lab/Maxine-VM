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
package com.sun.max.vm.compiler.bir;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.ir.*;

public class BirMethod extends AbstractIrMethod {

    private int _maxStack;
    private int _maxLocals;
    private byte[] _code;
    private IndexedSequence<BirBlock> _blocks;
    private BirBlock[] _blockMap;

    private Sequence<ExceptionHandlerEntry> _exceptionDispatcherTable;

    public BirMethod(ClassMethodActor classMethodActor) {
        super(classMethodActor);
    }

    public boolean isGenerated() {
        return _code != null;
    }

    public int maxLocals() {
        return _maxLocals;
    }

    public int maxStack() {
        return _maxStack;
    }

    public byte[] code() {
        return _code;
    }

    public Sequence<BirBlock> blocks() {
        return _blocks;
    }

    public BirBlock[] blockMap() {
        return _blockMap;
    }

    public BirBlock getBlockAt(int bytecodeAddress) {
        return _blockMap[bytecodeAddress];
    }

    public Sequence<ExceptionHandlerEntry> exceptionDispatcherTable() {
        return _exceptionDispatcherTable;
    }

    public void setGenerated(byte[] code, int maxStack, int maxLocals, IndexedSequence<BirBlock> blocks, BirBlock[] blockMap, Sequence<ExceptionHandlerEntry> exceptionDispatcherTable) {
        _code = code;
        _maxStack = maxStack;
        _maxLocals = maxLocals;
        _blocks = blocks;
        _blockMap = blockMap;
        _exceptionDispatcherTable = exceptionDispatcherTable;

        for (ExceptionHandlerEntry exceptionDispatcherEntry : exceptionDispatcherTable()) {
            final BirBlock birBlock = _blockMap[exceptionDispatcherEntry.handlerPosition()];
            birBlock.setRole(IrBlock.Role.EXCEPTION_DISPATCHER);
        }
    }

    public String traceToString() {
        final CharArrayWriter charArrayWriter = new CharArrayWriter();
        final IndentWriter writer = new IndentWriter(charArrayWriter);
        writer.println("BIR: " + name());
        if (_blocks != null && !_blocks.isEmpty()) {
            writer.indent();
            writer.println("maxStack: " + _maxStack);
            writer.println("maxLocals: " + _maxLocals);
            writer.outdent();
            if (_blocks != null) {
                for (BirBlock block : _blocks) {
                    final ConstantPool constantPool = classMethodActor().codeAttribute().constantPool();
                    writer.print(BytecodePrinter.toString(constantPool, block.bytecodeBlock()));
                }
                for (BirBlock block : _blocks) {
                    writer.println(block.toString());
                }
            }
        }
        return charArrayWriter.toString();
    }

}
