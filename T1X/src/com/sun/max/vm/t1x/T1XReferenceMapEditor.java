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
package com.sun.max.vm.t1x;

import static com.sun.max.vm.stack.StackReferenceMapPreparer.*;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.stack.*;

public class T1XReferenceMapEditor implements ReferenceMapInterpreterContext, ReferenceSlotVisitor {
    private final T1XTargetMethod t1xMethod;
    private final JVMSFrameLayout frame;
    private final Object blockFrames;
    private final ExceptionHandler[] exceptionHandlerMap;
    private final BytecodeStopsIterator bytecodeStopsIterator;

    /**
     * The sorted list of basic block starting BCIs.
     * <p>
     * Using a char array (as opposed to a short array) means that {@linkplain Arrays#binarySearch(char[], char) binary search} can be used to
     * find the basic block enclosing a given BCI.
     */
    private final char[] blockBCIs;

    /**
     * Shared non-null global object denoting absence of a valid editor instance.
     */
    public static final T1XReferenceMapEditor SENTINEL = new T1XReferenceMapEditor();

    private T1XReferenceMapEditor() {
        t1xMethod = null;
        frame = null;
        blockFrames = null;
        exceptionHandlerMap = null;
        bytecodeStopsIterator = null;
        blockBCIs = null;
    }

    public T1XReferenceMapEditor(T1XTargetMethod t1xMethod, int numberOfBlocks, boolean[] blockBCIs, BytecodeStopsIterator bytecodeStopsIterator, JVMSFrameLayout frame) {
        assert t1xMethod.numberOfStopPositions() != 0;
        this.t1xMethod = t1xMethod;
        this.exceptionHandlerMap = ExceptionHandler.createHandlerMap(t1xMethod.codeAttribute);
        this.frame = frame;
        this.blockBCIs = new char[numberOfBlocks];
        int blockIndex = 0;
        for (int i = 0; i != blockBCIs.length; ++i) {
            if (blockBCIs[i]) {
                this.blockBCIs[blockIndex++] = (char) i;
            }
        }
        assert blockIndex == numberOfBlocks;
        this.blockFrames = ReferenceMapInterpreter.createFrames(this);
        this.bytecodeStopsIterator = bytecodeStopsIterator;
    }

    public int blockIndexFor(int bci) {
        final int blockIndex = Arrays.binarySearch(blockBCIs, (char) bci);
        if (blockIndex >= 0) {
            return blockIndex;
        }
        return (-blockIndex) - 2;
    }

    public Object blockFrames() {
        return blockFrames;
    }

    public void visitReferenceInLocalVariable(int localVariableIndex) {
        for (int stopIndex = bytecodeStopsIterator.nextStopIndex(true); stopIndex != -1; stopIndex = bytecodeStopsIterator.nextStopIndex(false)) {
            final int offset = stopIndex * t1xMethod.totalRefMapSize();
            final int fpRelativeIndex = frame.localVariableReferenceMapIndex(localVariableIndex);
            ByteArrayBitMap.set(t1xMethod.referenceMaps(), offset, t1xMethod.frameRefMapSize, fpRelativeIndex);
        }
    }

    public void visitReferenceOnOperandStack(int operandStackIndex, boolean parametersPopped) {
        for (int stopIndex = bytecodeStopsIterator.nextStopIndex(true); stopIndex != -1; stopIndex = bytecodeStopsIterator.nextStopIndex(false)) {
            if (parametersPopped != bytecodeStopsIterator.isDirectRuntimeCall()) {
                final int offset = stopIndex * t1xMethod.totalRefMapSize();
                final int fpRelativeIndex = frame.operandStackReferenceMapIndex(operandStackIndex);
                ByteArrayBitMap.set(t1xMethod.referenceMaps(), offset, t1xMethod.frameRefMapSize, fpRelativeIndex);
            }
        }
    }

    public int blockStartBCI(int blockIndex) {
        if (blockIndex == blockBCIs.length) {
            return codeAttribute().code().length;
        }
        return blockBCIs[blockIndex];
    }

    public CodeAttribute codeAttribute() {
        return t1xMethod.codeAttribute;
    }

    public ClassMethodActor classMethodActor() {
        return t1xMethod.classMethodActor();
    }

    public ExceptionHandler exceptionHandlersActiveAt(int bci) {
        if (exceptionHandlerMap == null) {
            return null;
        }
        return exceptionHandlerMap[bci];
    }

    public int numberOfBlocks() {
        return blockBCIs.length;
    }

    public JVMSFrameLayout stackFrameLayout() {
        return frame;
    }

    public void fillInMaps() {
        if (traceStackRootScanning()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Finalizing T1X reference maps for ");
            Log.printMethod(classMethodActor(), true);
            Log.unlock(lockDisabledSafepoints);
        }

        final ReferenceMapInterpreter interpreter = ReferenceMapInterpreter.from(blockFrames);
        interpreter.finalizeFrames(this);
        interpreter.interpretReferenceSlots(this, this, bytecodeStopsIterator);

        if (traceStackRootScanning()) {
            final boolean lockDisabledSafepoints = Log.lock();
            bytecodeStopsIterator.reset();
            final CodeAttribute codeAttribute = t1xMethod.classMethodActor().codeAttribute();
            for (int bcp = bytecodeStopsIterator.bci(); bcp != -1; bcp = bytecodeStopsIterator.next()) {
                for (int stopIndex = bytecodeStopsIterator.nextStopIndex(true); stopIndex != -1; stopIndex = bytecodeStopsIterator.nextStopIndex(false)) {
                    final int offset = stopIndex * t1xMethod.totalRefMapSize();
                    Log.print(bcp);
                    Log.print(":");
                    int opc = codeAttribute.code()[bcp] & 0xff;
                    final String opcode = Bytecodes.baseNameOf(opc);
                    Log.print(opcode);
                    int chars = Ints.sizeOfBase10String(bcp) + 1 + opcode.length();
                    while (chars++ < 20) {
                        Log.print(' ');
                    }
                    Log.print(" stop[");
                    Log.print(stopIndex);
                    Log.print("]@");
                    Log.print(t1xMethod.stopPosition(stopIndex));
                    if (bytecodeStopsIterator.isDirectRuntimeCall()) {
                        Log.print('*');
                    }
                    if (interpreter.isFrameInitialized(blockIndexFor(bcp))) {
                        Log.print(", locals={");
                        for (int localVariableIndex = 0; localVariableIndex < codeAttribute.maxLocals; ++localVariableIndex) {
                            final int fpRelativeIndex = frame.localVariableReferenceMapIndex(localVariableIndex);
                            if (ByteArrayBitMap.isSet(t1xMethod.referenceMaps(), offset, t1xMethod.frameRefMapSize, fpRelativeIndex)) {
                                Log.print(' ');
                                Log.print(localVariableIndex);
                                Log.print("[fp+");
                                Log.print(fpRelativeIndex * Word.size());
                                Log.print("]");
                            }
                        }
                        Log.print(" }");
                        Log.print(", stack={");
                        for (int operandStackIndex = 0; operandStackIndex < codeAttribute.maxStack; ++operandStackIndex) {
                            final int fpRelativeIndex = frame.operandStackReferenceMapIndex(operandStackIndex);
                            if (ByteArrayBitMap.isSet(t1xMethod.referenceMaps(), offset, t1xMethod.frameRefMapSize, fpRelativeIndex)) {
                                Log.print(' ');
                                Log.print(operandStackIndex);
                                Log.print("[fp+");
                                Log.print(fpRelativeIndex * Word.size());
                                Log.print("]");
                            }
                        }
                        Log.println(" }");
                    } else {
                        Log.println(", *unreachable*");
                    }
                }
            }

            Log.print("Finalized T1X reference maps for ");
            Log.printMethod(classMethodActor(), true);
            Log.unlock(lockDisabledSafepoints);
        }

    }

    @Override
    public String toString() {
        if (this == SENTINEL) {
            return "SENTINEL";
        }
        return getClass().getSimpleName() + "[" + classMethodActor() + "]";
    }
}
