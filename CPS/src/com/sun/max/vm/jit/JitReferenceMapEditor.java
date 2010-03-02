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
package com.sun.max.vm.jit;

import java.util.Arrays;

import com.sun.c1x.bytecode.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.stack.*;

public class JitReferenceMapEditor implements ReferenceMapInterpreterContext, ReferenceSlotVisitor {
    private final JitTargetMethod targetMethod;
    private final JitStackFrameLayout stackFrameLayout;
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

    public JitReferenceMapEditor(JitTargetMethod targetMethod, int numberOfBlocks, boolean[] blockStarts, BytecodeStopsIterator bytecodeStopsIterator, JitStackFrameLayout jitStackFrameLayout) {
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

    public int blockStartBytecodePosition(int blockIndex) {
        if (blockIndex == blockStartBytecodePositions.length) {
            return classMethodActor().codeAttribute().code().length;
        }
        return blockStartBytecodePositions[blockIndex];
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

    public JitStackFrameLayout stackFrameLayout() {
        return stackFrameLayout;
    }

    public void fillInMaps(int[] bytecodeToTargetCodePositionMap) {
        if (Heap.traceRootScanning()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Finalizing JIT reference maps for ");
            Log.printMethod(classMethodActor(), true);
            Log.unlock(lockDisabledSafepoints);
        }

        final ReferenceMapInterpreter interpreter = ReferenceMapInterpreter.from(blockFrames);
        interpreter.finalizeFrames(this);
        interpreter.interpretReferenceSlots(this, this, bytecodeStopsIterator);

        if (Heap.traceRootScanning()) {
            final boolean lockDisabledSafepoints = Log.lock();
            bytecodeStopsIterator.reset();
            final CodeAttribute codeAttribute = targetMethod.classMethodActor().codeAttribute();
            for (int bcp = bytecodeStopsIterator.bytecodePosition(); bcp != -1; bcp = bytecodeStopsIterator.next()) {
                for (int stopIndex = bytecodeStopsIterator.nextStopIndex(true); stopIndex != -1; stopIndex = bytecodeStopsIterator.nextStopIndex(false)) {
                    final int offset = stopIndex * targetMethod.frameReferenceMapSize();
                    Log.print(bcp);
                    Log.print(":");
                    int opc = codeAttribute.code()[bcp] & 0xff;
                    final String opcode = Bytecodes.nameOf(opc);
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
