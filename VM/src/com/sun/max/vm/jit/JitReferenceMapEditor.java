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

import java.util.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.stack.*;

public class JitReferenceMapEditor implements ReferenceMapInterpreterContext, ReferenceSlotVisitor {
    private final JitTargetMethod _targetMethod;
    private final JitStackFrameLayout _jitStackFrameLayout;
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
        _jitStackFrameLayout = jitStackFrameLayout;
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
            ByteArrayBitMap.set(_targetMethod.referenceMaps(), offset, _targetMethod.frameReferenceMapSize(), _jitStackFrameLayout.localVariableReferenceMapIndex(localVariableIndex));
        }
    }

    @Override
    public void visitReferenceOnOperandStack(int operandStackIndex, boolean parametersPopped) {
        for (int stopIndex = _bytecodeStopsIterator.nextStopIndex(true); stopIndex != -1; stopIndex = _bytecodeStopsIterator.nextStopIndex(false)) {
            if (parametersPopped == _bytecodeStopsIterator.isDirectRuntimeCall()) {
                final int offset = stopIndex * _targetMethod.frameReferenceMapSize();
                ByteArrayBitMap.set(_targetMethod.referenceMaps(), offset, _targetMethod.frameReferenceMapSize(), _jitStackFrameLayout.operandStackReferenceMapIndex(operandStackIndex));
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

    public void fillInMaps(int[] bytecodeToTargetCodePositionMap) {
        assert bytecodeToTargetCodePositionMap.length == _targetMethod.classMethodActor().rawCodeAttribute().code().length + 1;

        final ReferenceMapInterpreter interpreter = ReferenceMapInterpreter.from(_blockFrames);
        interpreter.finalizeFrames(this);
        interpreter.interpretReferenceSlots(this, this, _bytecodeStopsIterator);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + classMethodActor() + "]";
    }
}
