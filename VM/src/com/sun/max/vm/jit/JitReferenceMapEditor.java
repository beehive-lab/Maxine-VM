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

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.stack.*;

public class JitReferenceMapEditor implements ReferenceMapInterpreterContext, ReferenceSlotVisitor {
    private final JitTargetMethod _targetMethod;
    private final JitStackFrameLayout _stackFrameLayout;
    private final Object _blockFrames;
    private final ExceptionHandler[] _exceptionHandlerMap;
    private final BytecodeStopsIterator _bytecodeStopsIterator;

    /**
     * The sorted list of basic block starting positions.
     * <p>
     * Using a char array (as opposed to a short array) means that {@linkplain Arrays#binarySearch(char[], char) binary search} can be used to
     * find the basic block enclosing a given bytecode position.
     */
    private final char[] _blockStartBytecodePositions;

    public JitReferenceMapEditor(JitTargetMethod targetMethod, int numberOfBlocks, boolean[] blockStarts, BytecodeStopsIterator bytecodeStopsIterator, JitStackFrameLayout jitStackFrameLayout) {
        assert targetMethod.numberOfStopPositions() != 0;
        final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
        _targetMethod = targetMethod;
        _exceptionHandlerMap = ExceptionHandler.createHandlerMap(classMethodActor.rawCodeAttribute());
        _stackFrameLayout = jitStackFrameLayout;
        _blockStartBytecodePositions = new char[numberOfBlocks];
        int blockIndex = 0;
        for (int i = 0; i != blockStarts.length; ++i) {
            if (blockStarts[i]) {
                _blockStartBytecodePositions[blockIndex++] = (char) i;
            }
        }
        assert blockIndex == numberOfBlocks;
        _blockFrames = ReferenceMapInterpreter.createFrames(this);
        _bytecodeStopsIterator = bytecodeStopsIterator;
    }

    public int blockIndexFor(int bytecodePosition) {
        final int blockIndex = Arrays.binarySearch(_blockStartBytecodePositions, (char) bytecodePosition);
        if (blockIndex >= 0) {
            return blockIndex;
        }
        return (-blockIndex) - 2;
    }

    @Override
    public Object blockFrames() {
        return _blockFrames;
    }

    @Override
    public void visitReferenceInLocalVariable(int localVariableIndex) {
        for (int stopIndex = _bytecodeStopsIterator.nextStopIndex(true); stopIndex != -1; stopIndex = _bytecodeStopsIterator.nextStopIndex(false)) {
            final int offset = stopIndex * _targetMethod.frameReferenceMapSize();
            final int fpRelativeIndex = _stackFrameLayout.localVariableReferenceMapIndex(localVariableIndex);
            ByteArrayBitMap.set(_targetMethod.referenceMaps(), offset, _targetMethod.frameReferenceMapSize(), fpRelativeIndex);
        }
    }

    @Override
    public void visitReferenceOnOperandStack(int operandStackIndex, boolean parametersPopped) {
        for (int stopIndex = _bytecodeStopsIterator.nextStopIndex(true); stopIndex != -1; stopIndex = _bytecodeStopsIterator.nextStopIndex(false)) {
            if (parametersPopped != _bytecodeStopsIterator.isDirectRuntimeCall()) {
                final int offset = stopIndex * _targetMethod.frameReferenceMapSize();
                final int fpRelativeIndex = _stackFrameLayout.operandStackReferenceMapIndex(operandStackIndex);
                ByteArrayBitMap.set(_targetMethod.referenceMaps(), offset, _targetMethod.frameReferenceMapSize(), fpRelativeIndex);
            }
        }
    }

    @Override
    public int blockStartBytecodePosition(int blockIndex) {
        if (blockIndex == _blockStartBytecodePositions.length) {
            return classMethodActor().rawCodeAttribute().code().length;
        }
        return _blockStartBytecodePositions[blockIndex];
    }

    @Override
    public ClassMethodActor classMethodActor() {
        return _targetMethod.classMethodActor();
    }

    @Override
    public ExceptionHandler exceptionHandlersActiveAt(int bytecodePosition) {
        if (_exceptionHandlerMap == null) {
            return null;
        }
        return _exceptionHandlerMap[bytecodePosition];
    }

    @Override
    public int numberOfBlocks() {
        return _blockStartBytecodePositions.length;
    }

    public JitStackFrameLayout stackFrameLayout() {
        return _stackFrameLayout;
    }

    public void fillInMaps(int[] bytecodeToTargetCodePositionMap) {
        if (Heap.traceGCRootScanning()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Finalizing JIT reference maps for ");
            Log.printMethodActor(classMethodActor(), true);
            Log.unlock(lockDisabledSafepoints);
        }

        final ReferenceMapInterpreter interpreter = ReferenceMapInterpreter.from(_blockFrames);
        interpreter.finalizeFrames(this);
        interpreter.interpretReferenceSlots(this, this, _bytecodeStopsIterator);

        if (Heap.traceGCRootScanning()) {
            final boolean lockDisabledSafepoints = Log.lock();
            _bytecodeStopsIterator.reset();
            final CodeAttribute codeAttribute = _targetMethod.classMethodActor().codeAttribute();
            for (int bcp = _bytecodeStopsIterator.bytecodePosition(); bcp != -1; bcp = _bytecodeStopsIterator.next()) {
                for (int stopIndex = _bytecodeStopsIterator.nextStopIndex(true); stopIndex != -1; stopIndex = _bytecodeStopsIterator.nextStopIndex(false)) {
                    final int offset = stopIndex * _targetMethod.frameReferenceMapSize();
                    Log.print(bcp);
                    Log.print(":");
                    final String opcode = Bytecode.from(codeAttribute.code()[bcp]).name();
                    Log.print(opcode);
                    int chars = Ints.sizeOfBase10String(bcp) + 1 + opcode.length();
                    while (chars++ < 20) {
                        Log.print(' ');
                    }
                    Log.print(" stop[");
                    Log.print(stopIndex);
                    Log.print("]@");
                    Log.print(_targetMethod.stopPosition(stopIndex));
                    if (_bytecodeStopsIterator.isDirectRuntimeCall()) {
                        Log.print('*');
                    }
                    Log.print(", locals={");
                    for (int localVariableIndex = 0; localVariableIndex < codeAttribute.maxLocals(); ++localVariableIndex) {
                        final int fpRelativeIndex = _stackFrameLayout.localVariableReferenceMapIndex(localVariableIndex);
                        if (ByteArrayBitMap.isSet(_targetMethod.referenceMaps(), offset, _targetMethod.frameReferenceMapSize(), fpRelativeIndex)) {
                            Log.print(' ');
                            Log.print(localVariableIndex);
                            Log.print("[fp+");
                            Log.print(fpRelativeIndex * Word.size());
                            Log.print("]");
                        }
                    }
                    Log.print(" }");
                    Log.print(", stack={");
                    for (int operandStackIndex = 0; operandStackIndex < codeAttribute.maxStack(); ++operandStackIndex) {
                        final int fpRelativeIndex = _stackFrameLayout.operandStackReferenceMapIndex(operandStackIndex);
                        if (ByteArrayBitMap.isSet(_targetMethod.referenceMaps(), offset, _targetMethod.frameReferenceMapSize(), fpRelativeIndex)) {
                            Log.print(' ');
                            Log.print(operandStackIndex);
                            Log.print("[fp+");
                            Log.print(fpRelativeIndex * Word.size());
                            Log.print("]");
                        }
                    }
                    Log.println(" }");
                }
            }

            Log.print("Finalized JIT reference maps for ");
            Log.printMethodActor(classMethodActor(), true);
            Log.unlock(lockDisabledSafepoints);
        }

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + classMethodActor() + "]";
    }
}
