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
package com.sun.max.vm.verifier;

import static com.sun.max.vm.verifier.types.VerificationType.*;

import java.util.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.stackmap.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.TypeInferencingMethodVerifier.*;
import com.sun.max.vm.verifier.types.*;

/**
 * An extension of a {@link Frame} that encapsulates extra type state required for verifying a method via type
 * inferencing.
 *
 * @author Doug Simon
 */
public class TypeState extends Frame {

    /**
     * The instruction at this target or null if this type state is not fixed at an position.
     */
    private Instruction _targetedInstruction;

    /**
     * The stack of subroutines.
     */
    private SubroutineFrame _subroutineFrame = SubroutineFrame.TOP;

    private boolean _visited;

    public TypeState(MethodActor classMethodActor, MethodVerifier methodVerifier) {
        super(classMethodActor, methodVerifier);
        _visited = true;
    }

    public TypeState(TypeState from) {
        super(from);
        _subroutineFrame = SubroutineFrame.TOP;
    }

    public boolean visited() {
        return _visited;
    }

    public void setVisited() {
        _visited = true;
    }

    public int position() {
        return _targetedInstruction == null ? -1 : _targetedInstruction.position();
    }

    @Override
    protected void initializeEntryFrame(MethodActor classMethodActor) {
        _subroutineFrame = SubroutineFrame.TOP;
        _visited = true;
        super.initializeEntryFrame(classMethodActor);
    }

    public void pushSubroutine(Subroutine subroutine) {
        if (_subroutineFrame.contains(subroutine)) {
            throw verifyError("Recursive subroutine call");
        }
        _subroutineFrame = new SubroutineFrame(subroutine, _subroutineFrame);
    }

    /**
     * Sets any locals holding {@linkplain UninitializedType uninitialized} objects to be {@linkplain TopType undefined}.
     */
    public void killUninitializedObjects() {
        for (int i = 0; i < _activeLocals; i++) {
            if (UNINITIALIZED.isAssignableFrom(_locals[i])) {
                _locals[i] = TOP;
                access(i);
            }
        }
    }

    /**
     * Pops frames off the stack of subroutine frames until until the frame for a given subroutine is popped. That is,
     * the subroutine frame stack is unwound to the frame that calls {@code subroutine}.
     *
     * @return the number of frames popped
     */
    public int popSubroutine(Subroutine subroutine) {
        if (_subroutineFrame == SubroutineFrame.TOP) {
            throw verifyError("Should be in a subroutine");
        }
        int numberOfSubroutineFramesPopped = 1;
        try {
            while (_subroutineFrame.subroutine() != subroutine) {
                ++numberOfSubroutineFramesPopped;
                _subroutineFrame = _subroutineFrame.parent();
            }
            _subroutineFrame = _subroutineFrame.parent();
        } catch (NullPointerException nullPointerException) {
            throw verifyError("Illegal return from subroutine");
        }
        return numberOfSubroutineFramesPopped;
    }

    public SubroutineFrame subroutineFrame() {
        return _subroutineFrame;
    }

    @Override
    public void store(VerificationType type, int index) {
        if (SUBROUTINE.isAssignableFrom(type)) {
            try {
                final VerificationType value = _locals[index];
                if (SUBROUTINE.isAssignableFrom(value)) {
                    if (value != type) {
                        throw verifyError("Two subroutines cannot merge to a single RET");
                    }
                }
            } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
                // Super class call below will throw the appropriate error
            }
        }
        super.store(type, index);
        access(index);
    }

    @Override
    public VerificationType load(VerificationType expectedType, int index) {
        final VerificationType value = super.load(expectedType, index);
        access(index);
        return value;
    }

    public void access(int index) {
        final boolean isCategory2 = _locals[index].isCategory2();
        for (SubroutineFrame subroutineFrame = _subroutineFrame; subroutineFrame != SubroutineFrame.TOP; subroutineFrame = subroutineFrame.parent()) {
            final Subroutine subroutine = subroutineFrame.subroutine();
            subroutine.accessesVariable(index);
            if (isCategory2) {
                subroutine.accessesVariable(index + 1);
            }
        }
    }

    /**
     * For each variable not accessed by a given subroutine, the type of the local variable in this type state is
     * replaced with the type of the local variable in a given type state. The latter type state is from a JSR that
     * entered the subroutine.
     */
    public void updateLocalsNotAccessedInSubroutine(TypeState typeStateAtJsr, Subroutine subroutine) {
        final int length = Math.max(_activeLocals, typeStateAtJsr._activeLocals);
        int activeLocals = 0;
        for (int index = 0; index != length; ++index) {
            if (!subroutine.isVariableAccessed(index)) {
                _locals[index] = typeStateAtJsr._locals[index];
            }
            if (_locals[index] != TOP) {
                activeLocals = index + 1;
            }
        }
        _activeLocals = activeLocals;
    }

    @Override
    public void reset(Frame fromFrame) {
        final TypeState targetTypeState = (TypeState) fromFrame;
        super.reset(fromFrame);
        _subroutineFrame = targetTypeState._subroutineFrame;
    }

    @Override
    public TypeState copy() {
        return new TypeState(this);
    }

    public Instruction targetedInstruction() {
        return _targetedInstruction;
    }

    public void setTargetedInstruction(Instruction instruction) {
        assert instruction != null;
        _targetedInstruction = instruction;
    }

    private TypeInferencingMethodVerifier verifier() {
        return (TypeInferencingMethodVerifier) _methodVerifier;
    }

    public boolean mergeStackFrom(TypeState fromTypeState, int thisPosition) {
        boolean changed = false;
        if (_stackSize != fromTypeState._stackSize) {
            throw verifyError("Inconsistent height for stacks being merged at bytecode position " + thisPosition);
        }

        for (int i = 0; i < _stackSize; i++) {
            if (!_stack[i].isAssignableFrom(fromTypeState._stack[i])) {
                final VerificationType mergedType = _stack[i].mergeWith(fromTypeState._stack[i]);
                if (mergedType == TOP) {
                    verifyError("Incompatible types in slot " + i + " of stacks being merged at bytecode position " + thisPosition);
                }
                assert mergedType != _stack[i];
                _stack[i] = mergedType;
                changed = true;
            }
        }
        return changed;
    }

    public boolean mergeLocalsFrom(TypeState fromTypeState, int thisPosition) {
        boolean changed = false;
        int activeLocals = 0;
        for (int i = 0; i < _activeLocals; i++) {
            if (!_locals[i].isAssignableFrom(fromTypeState._locals[i])) {
                final VerificationType mergedType = _locals[i].mergeWith(fromTypeState._locals[i]);
                assert mergedType != _locals[i];
                _locals[i] = mergedType;
                changed = true;
            }
            if (_locals[i] != TOP) {
                activeLocals = i + 1;
            }
        }
        _activeLocals = activeLocals;
        return changed;
    }

    public boolean mergeSubroutineFrames(TypeState fromTypeState) {
        final SubroutineFrame fromSubroutineFrame = fromTypeState._subroutineFrame;
        if (fromSubroutineFrame.depth() != _subroutineFrame.depth()) {
            return false;
        }

        final SubroutineFrame mergedSubroutineFrame = _subroutineFrame.merge(fromSubroutineFrame);
        if (mergedSubroutineFrame != _subroutineFrame) {
            _subroutineFrame = mergedSubroutineFrame;
            assert _subroutineFrame != null;
            return true;
        }
        return false;
    }

    @Override
    public void mergeFrom(Frame fromFrame, int thisPosition, int catchTypeIndex) {
        final TypeState fromTypeState = (TypeState) fromFrame;
        if (!_visited) {
            reset(fromTypeState);
            if (catchTypeIndex != -1) {
                final ObjectType catchType;
                if (catchTypeIndex == 0) {
                    catchType = VerificationType.THROWABLE;
                } else {
                    final TypeDescriptor catchTypeDescriptor = _methodVerifier.constantPool().classAt(catchTypeIndex).typeDescriptor();
                    catchType = _methodVerifier.getObjectType(catchTypeDescriptor);
                }
                _stack[0] = catchType;
                _stackSize = 1;
            }
            _visited = true;
            verifier().enqueChangedTypeState(this);
        } else {
            boolean changed = mergeSubroutineFrames(fromTypeState);
            changed = mergeLocalsFrom(fromTypeState, thisPosition) || changed;
            if (catchTypeIndex == -1) {
                changed = mergeStackFrom(fromTypeState, thisPosition) || changed;
            }
            if (changed) {
                verifier().enqueChangedTypeState(this);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (_subroutineFrame != null && _subroutineFrame != SubroutineFrame.TOP) {
            sb.append("in ").append(_subroutineFrame).append("\n");
        }

        sb.append(super.toString());

        return sb.toString();
    }

    private static final int MAX_LOCAL_LENGTH_DIFF = 4;

    /**
     * Compares two sequences of types. Let {@code n} be the difference between the lengths of {@code types1} and
     * {@code types2}. If {@code n > MAX_LOCAL_LENGTH_DIFF} then {@link Integer#MAX_VALUE} is returned. Otherwise, if
     * {@code types1} is a prefix of {@code types2}, then {@code -n} is returned. Otherwise, if {@code types2} is a
     * prefix of {@code types1}, then {@code n} is returned. Otherwise, {@link Integer#MAX_VALUE} is returned.
     */
    private static int diff(VerificationType[] types1, VerificationType[] types2) {
        final int diffLength = types1.length - types2.length;
        if (diffLength > MAX_LOCAL_LENGTH_DIFF || diffLength < -MAX_LOCAL_LENGTH_DIFF) {
            return Integer.MAX_VALUE;
        }
        final int length = (diffLength > 0) ? types2.length : types1.length;
        for (int i = 0; i < length; ++i) {
            if (types1[i] != types2[i]) {
                if (!INTEGER.isAssignableFrom(types1[i]) || !INTEGER.isAssignableFrom(types2[i])) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        return diffLength;
    }

    /**
     * Converts a sequence of types into the sequence as it would be encoded in a frame of a
     * {@link StackMapTable} where {@linkplain Category2Type category 2} types are encoded in one unit.
     */
    private static VerificationType[] asStackMapTypes(VerificationType[] types, int length) {
        if (length == 0) {
            return VerificationType.NO_TYPES;
        }
        int stackMapTypesLength = 0;
        for (int i = 0; i != length; ++i) {
            if (types[i].classfileTag() != -1) {
                ++stackMapTypesLength;
            }
        }

        if (stackMapTypesLength == length) {
            // No category 2 types
            return Arrays.copyOf(types, stackMapTypesLength);
        }
        final VerificationType[] stackMapTypes = new VerificationType[stackMapTypesLength];
        stackMapTypesLength = 0;
        for (int i = 0; i != length; ++i) {
            if (types[i].classfileTag() != -1) {
                stackMapTypes[stackMapTypesLength++] = types[i];
            }
        }
        return stackMapTypes;
    }

    public StackMapFrame asStackMapFrame(TypeState previousTypeState) {
        final int previousPosition = previousTypeState.position();
        final int positionDelta = previousPosition == 0 ? position() : position() - previousPosition - 1;

        final VerificationType[] locals = asStackMapTypes(_locals, _activeLocals);
        final VerificationType[] previousLocals = asStackMapTypes(previousTypeState._locals, previousTypeState._activeLocals);

        if (_stackSize == 1) {
            if (locals.length == previousLocals.length && diff(previousLocals, locals) == 0) {
                if (positionDelta < StackMapTable.SAME_FRAME_BOUND) {
                    return new SameLocalsOneStack(positionDelta, _stack[0]);
                }
                return new SameLocalsOneStackExtended(positionDelta, _stack[0]);
            }
        } else if (_stackSize == 0) {
            final int diffLength = diff(previousLocals, locals);
            if (diffLength == 0) {
                if (positionDelta < StackMapTable.SAME_FRAME_BOUND) {
                    return new SameFrame(positionDelta);
                }
                return new SameFrameExtended(positionDelta);
            } else if (-MAX_LOCAL_LENGTH_DIFF < diffLength && diffLength < 0) {
                // APPEND
                final VerificationType[] localsDiff = new VerificationType[-diffLength];
                int j = 0;
                for (int i = previousLocals.length; i < locals.length; i++, j++) {
                    localsDiff[j] = locals[i];
                }
                return new AppendFrame(positionDelta, localsDiff);
            } else if (0 < diffLength && diffLength < MAX_LOCAL_LENGTH_DIFF) {
                // CHOP
                return new ChopFrame(positionDelta, diffLength);
            }
        }

        // FULL_FRAME
        return new FullFrame(positionDelta, locals, asStackMapTypes(_stack, _stackSize));
    }
}
