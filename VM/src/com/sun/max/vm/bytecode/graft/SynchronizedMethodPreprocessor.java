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
package com.sun.max.vm.bytecode.graft;

import java.util.*;

import com.sun.cri.bytecode.*;
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
        super(constantPoolEditor, 0, codeAttribute.maxStack, codeAttribute.maxLocals);

        codeStream = new SeekableByteArrayOutputStream();

        final SynchronizedMethodTransformer synchronizedMethodTransformer = classMethodActor.isStatic() ?
            new StaticSynchronizedMethodTransformer(this, constantPoolEditor.indexOf(PoolConstantFactory.createClassConstant(classMethodActor.holder()))) :
            new VirtualSynchronizedMethodTransformer(this, allocateLocal(Kind.REFERENCE));
        synchronizedMethodTransformer.acquireMonitor();
        trackingStack = false;
        final OpcodePositionRelocator relocator = synchronizedMethodTransformer.transform(new BytecodeBlock(codeAttribute.code()));
        trackingStack = true;

        final Kind resultKind = classMethodActor.resultKind();
        setStack(resultKind.stackSlots);
        Label returnBlockLabel = synchronizedMethodTransformer.returnBlockLabel;
        if (returnBlockLabel != null) {
            returnBlockLabel.bind();
            synchronizedMethodTransformer.releaseMonitor();
            return_(resultKind);
        }

        final int monitorExitHandlerAddress = currentAddress();
        setStack(1);
        pop();
        synchronizedMethodTransformer.releaseMonitor();
        final int monitorExitHandlerEndAddress = currentAddress();
        invokestatic(ExceptionDispatcher.safepointAndLoadExceptionObject, 0, 1);
        athrow();

        trackingStack = false;
        final byte[] code = code();
        final ExceptionHandlerEntry[] exceptionHandlerTable = fixupExceptionHandlerTable(
                        monitorExitHandlerAddress,
                        monitorExitHandlerEndAddress,
                        code,
                        codeAttribute.exceptionHandlerTable(), relocator);
        result = new CodeAttribute(
                        constantPool(),
                        code,
                        (char) maxStack(),
                        (char) maxLocals(),
                        exceptionHandlerTable,
                        codeAttribute.lineNumberTable().relocate(relocator),
                        codeAttribute.localVariableTable().relocate(relocator),
                        null);

    }

    private boolean trackingStack = true;

    @Override
    public void setStack(int depth) {
        if (trackingStack) {
            super.setStack(depth);
        }
    }

    private final SeekableByteArrayOutputStream codeStream;
    private final CodeAttribute result;

    @Override
    protected void setWritePosition(int position) {
        codeStream.seek(position);
    }

    @Override
    protected void writeByte(byte b) {
        codeStream.write(b);
    }

    public CodeAttribute codeAttribute() {
        return result;
    }

    @Override
    public byte[] code() {
        fixup();
        return codeStream.toByteArray();
    }

    /**
     * Adjusts the exception handler table for a synchronized method to account for the change in bytecode addresses.
     * Also, any code range previously not covered by an exception handler is now covered the monitor exit handler.
     *
     * @param monitorExitHandlerAddress the entry address of the monitor exit handler (i.e. an exception handler that
     *            tries to release the monitor for the synchronized method before re-throwing the original exception)
     * @param monitorExitHandlerEndAddress
     * @param code the modified bytecode
     * @param exceptionHandlerTable the exception handler table for the original, unmodified bytecode
     * @param relocator
     * @return the fixed up exception handler table
     */
    private ExceptionHandlerEntry[] fixupExceptionHandlerTable(int monitorExitHandlerAddress,
                                                                       int monitorExitHandlerEndAddress,
                                                                       byte[] code,
                                                                       ExceptionHandlerEntry[] exceptionHandlerTable, OpcodePositionRelocator relocator) {
        final int codeLength = code.length;
        final int relocatedCodeStartAddress = relocator.relocate(0);
        assert (code[codeLength - 1] & 0xff) == Bytecodes.ATHROW;
        if (exceptionHandlerTable.length == 0) {
            return new ExceptionHandlerEntry[] {new ExceptionHandlerEntry(relocatedCodeStartAddress, codeLength - 1, monitorExitHandlerAddress, 0)};
        }

        final ArrayList<ExceptionHandlerEntry> table = new ArrayList<ExceptionHandlerEntry>();
        int previousEntryEndAddress = relocatedCodeStartAddress;
        for (ExceptionHandlerEntry entry : exceptionHandlerTable) {
            final ExceptionHandlerEntry relocatedEntry = entry.relocate(relocator);
            if (previousEntryEndAddress < relocatedEntry.startPosition()) {
                // There's a gap between the previous catch range and the current catch range. Insert a range with whose handler is the monitor exit handler.
                table.add(new ExceptionHandlerEntry(previousEntryEndAddress, relocatedEntry.startPosition(), monitorExitHandlerAddress, 0));
            }
            table.add(relocatedEntry);
            previousEntryEndAddress = relocatedEntry.endPosition();
        }

        // Cover the range of any generated exception dispatchers as well as the monitor exit exception handler itself
        if (previousEntryEndAddress < monitorExitHandlerAddress) {
            table.add(new ExceptionHandlerEntry(previousEntryEndAddress, monitorExitHandlerEndAddress, monitorExitHandlerAddress, 0));
        }
        return table.toArray(new ExceptionHandlerEntry[table.size()]);
    }
}
