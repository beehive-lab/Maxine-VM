/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
    private Instruction targetedInstruction;

    /**
     * The stack of subroutines.
     */
    private SubroutineFrame subroutineFrame = SubroutineFrame.TOP;

    private boolean visited;

    public TypeState(MethodActor classMethodActor, MethodVerifier methodVerifier) {
        super(classMethodActor, methodVerifier);
        visited = true;
    }

    public TypeState(TypeState from) {
        super(from);
        subroutineFrame = SubroutineFrame.TOP;
    }

    public boolean visited() {
        return visited;
    }

    public void setVisited() {
        visited = true;
    }

    public int position() {
        return targetedInstruction == null ? -1 : targetedInstruction.position();
    }

    @Override
    protected void initializeEntryFrame(MethodActor classMethodActor) {
        subroutineFrame = SubroutineFrame.TOP;
        visited = true;
        super.initializeEntryFrame(classMethodActor);
    }

    public void pushSubroutine(Subroutine subroutine) {
        if (subroutineFrame.contains(subroutine)) {
            verifyError("Recursive subroutine call");
        }
        subroutineFrame = new SubroutineFrame(subroutine, subroutineFrame);
    }

    /**
     * Sets any locals holding {@linkplain UninitializedType uninitialized} objects to be {@linkplain TopType undefined}.
     */
    public void killUninitializedObjects() {
        for (int i = 0; i < activeLocals; i++) {
            if (UNINITIALIZED.isAssignableFrom(locals[i])) {
                locals[i] = TOP;
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
        if (subroutineFrame == SubroutineFrame.TOP) {
            verifyError("Should be in a subroutine");
        }
        int numberOfSubroutineFramesPopped = 1;
        try {
            while (subroutineFrame.subroutine != subroutine) {
                ++numberOfSubroutineFramesPopped;
                subroutineFrame = subroutineFrame.parent();
            }
            subroutineFrame = subroutineFrame.parent();
        } catch (NullPointerException nullPointerException) {
            verifyError("Illegal return from subroutine");
        }
        return numberOfSubroutineFramesPopped;
    }

    public SubroutineFrame subroutineFrame() {
        return subroutineFrame;
    }

    @Override
    public void store(VerificationType type, int index) {
        if (SUBROUTINE.isAssignableFrom(type)) {
            try {
                final VerificationType value = locals[index];
                if (SUBROUTINE.isAssignableFrom(value)) {
                    if (value != type) {
                        verifyError(String.format("Two subroutines cannot merge to a single RET:%n" +
                            "  subroutine 1: %s%n  subroutine 2: %s", value, type));
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
        final boolean isCategory2 = locals[index].isCategory2();
        for (SubroutineFrame subroutineFrame = this.subroutineFrame; subroutineFrame != SubroutineFrame.TOP; subroutineFrame = subroutineFrame.parent()) {
            final Subroutine subroutine = subroutineFrame.subroutine;
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
     *
     * @param retIndex the index of the local variable holding the return address of the subroutine. Even though this
     *            variable is most likely accessed (i.e. written to) in a subroutine, its value should not be propagated
     *            outside the routine.
     */
    public void updateLocalsNotAccessedInSubroutine(TypeState typeStateAtJsr, Subroutine subroutine, int retIndex) {
        final int length = Math.max(this.activeLocals, typeStateAtJsr.activeLocals);
        int activeLocals = 0;
        for (int index = 0; index != length; ++index) {
            if (!subroutine.isVariableAccessed(index) || index == retIndex) {
                locals[index] = typeStateAtJsr.locals[index];
            }
            if (locals[index] != TOP) {
                activeLocals = index + 1;
            }
        }
        this.activeLocals = activeLocals;
    }

    @Override
    public void reset(Frame fromFrame) {
        final TypeState targetTypeState = (TypeState) fromFrame;
        super.reset(fromFrame);
        subroutineFrame = targetTypeState.subroutineFrame;
    }

    @Override
    public TypeState copy() {
        return new TypeState(this);
    }

    public Instruction targetedInstruction() {
        return targetedInstruction;
    }

    public void setTargetedInstruction(Instruction instruction) {
        assert instruction != null;
        targetedInstruction = instruction;
    }

    private TypeInferencingMethodVerifier verifier() {
        return (TypeInferencingMethodVerifier) methodVerifier;
    }

    public boolean mergeStackFrom(TypeState fromTypeState, int thisPosition) {
        boolean changed = false;
        if (stackSize != fromTypeState.stackSize) {
            verifyError("Inconsistent height for stacks being merged at bytecode position " + thisPosition);
        }

        for (int i = 0; i < stackSize; i++) {
            if (!stack[i].isAssignableFrom(fromTypeState.stack[i])) {
                final VerificationType mergedType = stack[i].mergeWith(fromTypeState.stack[i]);
                if (mergedType == TOP) {
                    verifyError("Incompatible types in slot " + i + " of stacks being merged at bytecode position " + thisPosition);
                }
                assert mergedType != stack[i];
                stack[i] = mergedType;
                changed = true;
            }
        }
        return changed;
    }

    public boolean mergeLocalsFrom(TypeState fromTypeState, int thisPosition) {
        boolean changed = false;
        int activeLocals = 0;
        for (int i = 0; i < this.activeLocals; i++) {
            if (!locals[i].isAssignableFrom(fromTypeState.locals[i])) {
                final VerificationType mergedType = locals[i].mergeWith(fromTypeState.locals[i]);
                assert mergedType != locals[i];
                locals[i] = mergedType;
                changed = true;
            }
            if (locals[i] != TOP) {
                activeLocals = i + 1;
            }
        }
        this.activeLocals = activeLocals;
        return changed;
    }

    public boolean mergeSubroutineFrames(TypeState fromTypeState) {
        final SubroutineFrame fromSubroutineFrame = fromTypeState.subroutineFrame;
        boolean changed = false;

        // The subroutine depth this frame may be greater than the incoming frame
        // if an exception handler covers a range of code that includes subroutines
        // at different depths.
        while (fromSubroutineFrame.depth < subroutineFrame.depth) {
            subroutineFrame = subroutineFrame.parent();
            changed = true;
        }
        if (fromSubroutineFrame.depth == subroutineFrame.depth) {
            final SubroutineFrame mergedSubroutineFrame = subroutineFrame.merge(fromSubroutineFrame);
            if (mergedSubroutineFrame != subroutineFrame) {
                subroutineFrame = mergedSubroutineFrame;
                assert subroutineFrame != null;
                return true;
            }
        }
        return changed;
    }

    @Override
    public void mergeFrom(Frame fromFrame, int thisPosition, int catchTypeIndex) {
        final TypeState fromTypeState = (TypeState) fromFrame;
        if (!visited) {
            reset(fromTypeState);
            if (catchTypeIndex != -1) {
                final VerificationType catchType;
                if (catchTypeIndex == 0) {
                    catchType = VerificationType.THROWABLE;
                } else {
                    final TypeDescriptor catchTypeDescriptor = methodVerifier.constantPool().classAt(catchTypeIndex).typeDescriptor();
                    catchType = methodVerifier.getObjectType(catchTypeDescriptor);
                }
                stack[0] = catchType;
                stackSize = 1;
            }
            visited = true;
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
        if (subroutineFrame != null && subroutineFrame != SubroutineFrame.TOP) {
            sb.append("in ").append(subroutineFrame).append("\n");
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

        final VerificationType[] locals = asStackMapTypes(this.locals, activeLocals);
        final VerificationType[] previousLocals = asStackMapTypes(previousTypeState.locals, previousTypeState.activeLocals);

        if (stackSize == 1) {
            if (locals.length == previousLocals.length && diff(previousLocals, locals) == 0) {
                if (positionDelta < StackMapTable.SAME_FRAME_BOUND) {
                    return new SameLocalsOneStack(positionDelta, stack[0]);
                }
                return new SameLocalsOneStackExtended(positionDelta, stack[0]);
            }
        } else if (stackSize == 0) {
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
        return new FullFrame(positionDelta, locals, asStackMapTypes(stack, stackSize));
    }
}
