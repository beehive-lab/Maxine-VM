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
/*VCSID=66266411-395f-48a9-93dd-4973d3f5714f*/
package com.sun.max.vm.bytecode.graft;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * 
 * @author Doug Simon
s */
public final class SynchronizedMethodPreprocessor extends BytecodeAssembler {

    public SynchronizedMethodPreprocessor(ConstantPoolEditor constantPoolEditor, MethodActor classMethodActor, CodeAttribute codeAttribute) {
        super(constantPoolEditor, 0, codeAttribute.maxStack(), codeAttribute.maxLocals());

        _codeStream = new SeekableByteArrayOutputStream();

        final SynchronizedMethodTransformer synchronizedMethodTransformer = classMethodActor.isStatic() ?
            new StaticSynchronizedMethodTransformer(this, constantPoolEditor.indexOf(PoolConstantFactory.createClassConstant(classMethodActor.holder().toJava()))) :
            new VirtualSynchronizedMethodTransformer(this, allocateLocal(Kind.REFERENCE));
        synchronizedMethodTransformer.acquireMonitor();
        _trackingStack = false;
        final OpcodePositionRelocator relocator = synchronizedMethodTransformer.transform(new BytecodeBlock(codeAttribute.code()));
        _trackingStack = true;

        final Kind resultKind = classMethodActor.resultKind();
        setStack(resultKind.stackSlots());
        synchronizedMethodTransformer.releaseMonitorAndReturn(resultKind);

        final int monitorExitHandlerAddress = currentAddress();
        setStack(1);
        synchronizedMethodTransformer.releaseMonitorAndRethrow();

        _trackingStack = false;
        final byte[] code = code();
        final Sequence<ExceptionHandlerEntry> exceptionHandlerTable = fixupExceptionHandlerTable(
                        monitorExitHandlerAddress,
                        code,
                        codeAttribute.exceptionHandlerTable(),
                        relocator);
        _result = new CodeAttribute(
                        constantPool(),
                        code,
                        (char) maxStack(),
                        (char) maxLocals(),
                        exceptionHandlerTable,
                        codeAttribute.lineNumberTable().relocate(relocator),
                        codeAttribute.localVariableTable().relocate(relocator),
                        null);

    }

    private boolean _trackingStack = true;

    @Override
    public void setStack(int depth) {
        if (_trackingStack) {
            super.setStack(depth);
        }
    }

    private final SeekableByteArrayOutputStream _codeStream;
    private final CodeAttribute _result;

    @Override
    protected void setWritePosition(int position) {
        _codeStream.seek(position);
    }

    @Override
    protected void writeByte(byte b) {
        _codeStream.write(b);
    }

    public CodeAttribute codeAttribute() {
        return _result;
    }

    @Override
    public byte[] code() {
        fixup();
        return _codeStream.toByteArray();
    }

    /**
     * Adjusts the exception handler table for a synchronized method to account for the change in bytecode addresses.
     * Also, any code range previously not covered by an exception handler is now covered the monitor exit handler.
     * 
     * @param monitorExitHandlerAddress
     *                the entry address of the monitor exit handler (i.e. an exception handler that tries to release the
     *                monitor for the synchronized method before re-throwing the original exception)
     * @param code
     *                the modified bytecode
     * @param exceptionHandlerTable
     *                the exception handler table for the original, unmodified bytecode
     * @param relocator
     * @return the fixed up exception handler table
     */
    private Sequence<ExceptionHandlerEntry> fixupExceptionHandlerTable(int monitorExitHandlerAddress,
                                                                       byte[] code,
                                                                       Sequence<ExceptionHandlerEntry> exceptionHandlerTable,
                                                                       OpcodePositionRelocator relocator) {
        final int codeLength = code.length;
        final int relocatedCodeStartAddress = relocator.relocate(0);
        assert Bytecode.from(code[codeLength - 1]) == Bytecode.ATHROW;
        if (exceptionHandlerTable.isEmpty()) {
            return new ArraySequence<ExceptionHandlerEntry>(new ExceptionHandlerEntry(relocatedCodeStartAddress, codeLength - 1, monitorExitHandlerAddress, 0));
        }

        final AppendableSequence<ExceptionHandlerEntry> updatedExceptionHandlerTable = new ArrayListSequence<ExceptionHandlerEntry>();
        int previousEntryEndAddress = relocatedCodeStartAddress;
        for (ExceptionHandlerEntry entry : exceptionHandlerTable) {
            final ExceptionHandlerEntry relocatedEntry = entry.relocate(relocator);
            if (previousEntryEndAddress < relocatedEntry.startPosition()) {
                // There's a gap between the previous catch range and the current catch range. Insert a range with whose handler is the monitor exit handler.
                updatedExceptionHandlerTable.append(new ExceptionHandlerEntry(previousEntryEndAddress, relocatedEntry.startPosition(), monitorExitHandlerAddress, 0));
            }
            updatedExceptionHandlerTable.append(relocatedEntry);
            previousEntryEndAddress = relocatedEntry.endPosition();
        }

        // Cover the range of any generated exception dispatchers as well as the monitor exit exception handler itself *except for the very last athrow*
        if (previousEntryEndAddress < monitorExitHandlerAddress) {
            updatedExceptionHandlerTable.append(new ExceptionHandlerEntry(previousEntryEndAddress, codeLength - 1, monitorExitHandlerAddress, 0));
        }
        return updatedExceptionHandlerTable;
    }
}
