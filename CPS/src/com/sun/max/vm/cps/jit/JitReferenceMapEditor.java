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
package com.sun.max.vm.cps.jit;

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

public class JitReferenceMapEditor implements ReferenceMapInterpreterContext, ReferenceSlotVisitor {
    private final JitTargetMethod targetMethod;
    private final JVMSFrameLayout stackFrameLayout;
    private final Object blockFrames;
    private final ExceptionHandler[] exceptionHandlerMap;
    private final BytecodeStopsIterator bytecodeStopsIterator;

    /**
     * The sorted list of basic block starting positions.
     * <p>
     * Using a char array (as opposed to a short array) means that {@linkplain Arrays#binarySearch(char[], char) binary search} can be used to
     * find the basic block enclosing a given bytecode position.
     */
    private final char[] blockStartBytecodePositions;

    /**
     * Shared non-null global object denoting absence of a valid {@code JitReferenceMapEditor} instance.
     */
    public static final JitReferenceMapEditor SENTINEL = new JitReferenceMapEditor();

    private JitReferenceMapEditor() {
        targetMethod = null;
        stackFrameLayout = null;
        blockFrames = null;
        exceptionHandlerMap = null;
        bytecodeStopsIterator = null;
        blockStartBytecodePositions = null;
    }

    public JitReferenceMapEditor(JitTargetMethod targetMethod, int numberOfBlocks, boolean[] blockStarts, BytecodeStopsIterator bytecodeStopsIterator, JVMSFrameLayout jitStackFrameLayout) {
        assert targetMethod.numberOfStopPositions() != 0;
        final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
        this.targetMethod = targetMethod;
        this.exceptionHandlerMap = ExceptionHandler.createHandlerMap(classMethodActor.codeAttribute());
        this.stackFrameLayout = jitStackFrameLayout;
        this.blockStartBytecodePositions = new char[numberOfBlocks];
        int blockIndex = 0;
        for (int i = 0; i != blockStarts.length; ++i) {
            if (blockStarts[i]) {
                blockStartBytecodePositions[blockIndex++] = (char) i;
            }
        }
        assert blockIndex == numberOfBlocks;
        this.blockFrames = ReferenceMapInterpreter.createFrames(this);
        this.bytecodeStopsIterator = bytecodeStopsIterator;
    }

    public int blockIndexFor(int bytecodePosition) {
        final int blockIndex = Arrays.binarySearch(blockStartBytecodePositions, (char) bytecodePosition);
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
            final int offset = stopIndex * targetMethod.frameReferenceMapSize();
            final int fpRelativeIndex = stackFrameLayout.localVariableReferenceMapIndex(localVariableIndex);
            ByteArrayBitMap.set(targetMethod.referenceMaps(), offset, targetMethod.frameReferenceMapSize(), fpRelativeIndex);
        }
    }

    public void visitReferenceOnOperandStack(int operandStackIndex, boolean parametersPopped) {
        for (int stopIndex = bytecodeStopsIterator.nextStopIndex(true); stopIndex != -1; stopIndex = bytecodeStopsIterator.nextStopIndex(false)) {
            if (parametersPopped != bytecodeStopsIterator.isDirectRuntimeCall()) {
                final int offset = stopIndex * targetMethod.frameReferenceMapSize();
                final int fpRelativeIndex = stackFrameLayout.operandStackReferenceMapIndex(operandStackIndex);
                ByteArrayBitMap.set(targetMethod.referenceMaps(), offset, targetMethod.frameReferenceMapSize(), fpRelativeIndex);
            }
        }
    }

    public int blockStartBCI(int blockIndex) {
        if (blockIndex == blockStartBytecodePositions.length) {
            return codeAttribute().code().length;
        }
        return blockStartBytecodePositions[blockIndex];
    }

    public CodeAttribute codeAttribute() {
        return classMethodActor().codeAttribute();
    }

    public ClassMethodActor classMethodActor() {
        return targetMethod.classMethodActor();
    }

    public ExceptionHandler exceptionHandlersActiveAt(int bytecodePosition) {
        if (exceptionHandlerMap == null) {
            return null;
        }
        return exceptionHandlerMap[bytecodePosition];
    }

    public int numberOfBlocks() {
        return blockStartBytecodePositions.length;
    }

    public JVMSFrameLayout stackFrameLayout() {
        return stackFrameLayout;
    }

    public void fillInMaps() {
        if (traceStackRootScanning()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Finalizing JIT reference maps for ");
            Log.printMethod(classMethodActor(), true);
            Log.unlock(lockDisabledSafepoints);
        }

        final ReferenceMapInterpreter interpreter = ReferenceMapInterpreter.from(blockFrames);
        interpreter.finalizeFrames(this);
        interpreter.interpretReferenceSlots(this, this, bytecodeStopsIterator);

        if (traceStackRootScanning()) {
            final boolean lockDisabledSafepoints = Log.lock();
            bytecodeStopsIterator.reset();
            final CodeAttribute codeAttribute = targetMethod.classMethodActor().codeAttribute();
            for (int bcp = bytecodeStopsIterator.bci(); bcp != -1; bcp = bytecodeStopsIterator.next()) {
                for (int stopIndex = bytecodeStopsIterator.nextStopIndex(true); stopIndex != -1; stopIndex = bytecodeStopsIterator.nextStopIndex(false)) {
                    final int offset = stopIndex * targetMethod.frameReferenceMapSize();
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
                    Log.print(targetMethod.stopPosition(stopIndex));
                    if (bytecodeStopsIterator.isDirectRuntimeCall()) {
                        Log.print('*');
                    }
                    if (interpreter.isFrameInitialized(blockIndexFor(bcp))) {
                        Log.print(", locals={");
                        for (int localVariableIndex = 0; localVariableIndex < codeAttribute.maxLocals; ++localVariableIndex) {
                            final int fpRelativeIndex = stackFrameLayout.localVariableReferenceMapIndex(localVariableIndex);
                            if (ByteArrayBitMap.isSet(targetMethod.referenceMaps(), offset, targetMethod.frameReferenceMapSize(), fpRelativeIndex)) {
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
                            final int fpRelativeIndex = stackFrameLayout.operandStackReferenceMapIndex(operandStackIndex);
                            if (ByteArrayBitMap.isSet(targetMethod.referenceMaps(), offset, targetMethod.frameReferenceMapSize(), fpRelativeIndex)) {
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

            Log.print("Finalized JIT reference maps for ");
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
