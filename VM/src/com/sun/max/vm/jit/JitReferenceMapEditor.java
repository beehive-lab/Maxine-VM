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

    int _currentStopIndex;

    /**
     * The sorted list of basic block starting positions.
     * <p>
     * Using a char array (as opposed to a short array) means that {@linkplain Arrays#binarySearch(char[], char) binary search} can be used to
     * find the basic block enclosing a given bytecode position.
     */
    private final char[] _blockStartBytecodePositions;

    public JitReferenceMapEditor(JitTargetMethod targetMethod, int numberOfBlocks, boolean[] blockStarts, JitStackFrameLayout jitStackFrameLayout) {
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
        setBit(_jitStackFrameLayout.localVariableReferenceMapIndex(localVariableIndex));
    }

    @Override
    public void visitReferenceOnOperandStack(int operandStackIndex) {
        setBit(_jitStackFrameLayout.operandStackReferenceMapIndex(operandStackIndex));
    }

    public void setBit(int bitIndex) {
        final int offset = _currentStopIndex * _targetMethod.frameReferenceMapSize();
        ByteArrayBitMap.set(_targetMethod.referenceMaps(), offset, _targetMethod.frameReferenceMapSize(), bitIndex);
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

        int startBytecodePosition = 0;
        int startTargetCodePosition = bytecodeToTargetCodePositionMap[0];
        assert startTargetCodePosition != 0;
        for (int bytecodePosition = 1; bytecodePosition < bytecodeToTargetCodePositionMap.length; bytecodePosition++) {
            final int targetCodePosition = bytecodeToTargetCodePositionMap[bytecodePosition];
            if (targetCodePosition != 0) {
                // Iterate over all the stops in the target code for the bytecode at 'startBytecodePosition':
                for (int stopIndex = 0; stopIndex < _targetMethod.numberOfStopPositions(); ++stopIndex) {
                    final int stopPosition = _targetMethod.stopPosition(stopIndex);
                    if (stopPosition < startTargetCodePosition) {
                        // this is a stop in the prologue of the method (e.g. the invocation count call)
                        _currentStopIndex = stopIndex;
                        interpreter.interpretReferenceSlotsAt(this, this, 0, true);
                    } else if (stopPosition < targetCodePosition) {
                        // this is a stop in the machine code generated for bytecodes
                        _currentStopIndex = stopIndex;
                        final boolean isDirectCallToRuntime = _targetMethod.isDirectCallToRuntime(stopIndex);
                        interpreter.interpretReferenceSlotsAt(this, this, startBytecodePosition, isDirectCallToRuntime);
                    }
                }
                startTargetCodePosition = targetCodePosition;
                startBytecodePosition = bytecodePosition;
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + classMethodActor() + "]";
    }
}
