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
package com.sun.max.vm.bytecode.graft;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.io.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.type.*;

/**
 *
 * @author Doug Simon
 */
public final class SynchronizedMethodPreprocessor extends BytecodeAssembler {

    /**
     * Specifies whether the current runtime requires this preprocessing.
     */
    public static boolean REQUIRED = CPSCompiler.Static.compiler() != null;

    public SynchronizedMethodPreprocessor(ConstantPoolEditor constantPoolEditor, MethodActor classMethodActor, CodeAttribute codeAttribute) {
        super(constantPoolEditor, 0, codeAttribute.maxStack, codeAttribute.maxLocals);

        codeStream = new SeekableByteArrayOutputStream();

        final SynchronizedMethodTransformer synchronizedMethodTransformer = classMethodActor.isStatic() ?
            new StaticSynchronizedMethodTransformer(this, constantPoolEditor.indexOf(PoolConstantFactory.createClassConstant(classMethodActor.holder()))) :
            new VirtualSynchronizedMethodTransformer(this, allocateLocal(Kind.REFERENCE));
        synchronizedMethodTransformer.acquireMonitor();
        trackingStack = false;
        final OpcodeBCIRelocator relocator = synchronizedMethodTransformer.transform(new BytecodeBlock(codeAttribute.code()));
        trackingStack = true;

        final Kind resultKind = classMethodActor.resultKind();
        setStack(resultKind.stackSlots);
        Label returnBlockLabel = synchronizedMethodTransformer.returnBlockLabel;
        int returnInstructionAddress = -1;
        if (returnBlockLabel != null) {
            returnBlockLabel.bind();
            synchronizedMethodTransformer.releaseMonitor();
            returnInstructionAddress = currentAddress();
            return_(resultKind);
        }

        final int monitorExitHandlerAddress = currentAddress();
        setStack(1);
        synchronizedMethodTransformer.releaseMonitor();
        final int monitorExitHandlerEndAddress = currentAddress();
        athrow();

        trackingStack = false;
        final byte[] code = code();
        final ExceptionHandlerEntry[] exceptionHandlerTable = fixupExceptionHandlerTable(
                        returnInstructionAddress,
                        monitorExitHandlerAddress,
                        monitorExitHandlerEndAddress,
                        code, codeAttribute.exceptionHandlerTable(), relocator);
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
    protected void setWriteBCI(int bci) {
        codeStream.seek(bci);
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
     * @param returnInstructionAddress TODO
     * @param monitorExitHandlerAddress the entry address of the monitor exit handler (i.e. an exception handler that
     *            tries to release the monitor for the synchronized method before re-throwing the original exception)
     * @param monitorExitHandlerEndAddress
     * @param code the modified bytecode
     * @param exceptionHandlerTable the exception handler table for the original, unmodified bytecode
     * @param relocator
     *
     * @return the fixed up exception handler table
     */
    private ExceptionHandlerEntry[] fixupExceptionHandlerTable(int returnInstructionAddress,
                                                                       int monitorExitHandlerAddress,
                                                                       int monitorExitHandlerEndAddress,
                                                                       byte[] code, ExceptionHandlerEntry[] exceptionHandlerTable, OpcodeBCIRelocator relocator) {
        final int codeLength = code.length;
        final int relocatedCodeStartAddress = relocator.relocate(0);
        assert (code[codeLength - 1] & 0xff) == Bytecodes.ATHROW;
        if (exceptionHandlerTable.length == 0) {
            if (returnInstructionAddress == -1) {
                return new ExceptionHandlerEntry[] {new ExceptionHandlerEntry(relocatedCodeStartAddress, codeLength - 1, monitorExitHandlerAddress, 0)};
            }
            return new ExceptionHandlerEntry[] {
                new ExceptionHandlerEntry(relocatedCodeStartAddress, returnInstructionAddress, monitorExitHandlerAddress, 0),
                new ExceptionHandlerEntry(returnInstructionAddress + 1, monitorExitHandlerEndAddress, monitorExitHandlerAddress, 0)
            };
        }

        final ArrayList<ExceptionHandlerEntry> table = new ArrayList<ExceptionHandlerEntry>();
        int previousEntryEndAddress = relocatedCodeStartAddress;
        for (ExceptionHandlerEntry entry : exceptionHandlerTable) {
            final ExceptionHandlerEntry relocatedEntry = entry.relocate(relocator);
            if (previousEntryEndAddress < relocatedEntry.startBCI()) {
                // There's a gap between the previous catch range and the current catch range. Insert a range with whose handler is the monitor exit handler.
                table.add(new ExceptionHandlerEntry(previousEntryEndAddress, relocatedEntry.startBCI(), monitorExitHandlerAddress, 0));
            }
            table.add(relocatedEntry);
            previousEntryEndAddress = relocatedEntry.endBCI();
        }

        if (returnInstructionAddress != -1) {
            if (previousEntryEndAddress < returnInstructionAddress) {
                table.add(new ExceptionHandlerEntry(previousEntryEndAddress, returnInstructionAddress, monitorExitHandlerAddress, 0));
            }
            previousEntryEndAddress = returnInstructionAddress + 1;
        }

        // Cover the range of any generated exception dispatchers as well as the monitor exit exception handler itself
        if (previousEntryEndAddress < monitorExitHandlerAddress) {
            table.add(new ExceptionHandlerEntry(previousEntryEndAddress, monitorExitHandlerEndAddress, monitorExitHandlerAddress, 0));
        }
        return table.toArray(new ExceptionHandlerEntry[table.size()]);
    }
}
