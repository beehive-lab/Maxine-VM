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
package com.sun.max.vm.verifier;

import java.util.*;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.stackmap.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.types.*;

/**
 * A frame represents the type state of the operand stack and local variables during the bytecode abstract
 * interpretation performed by the {@linkplain TypeCheckingVerifier type checking verifier}.
 *
 * @author David Liu
 * @author Doug Simon
 */
public class Frame implements FrameModel {

    final VerificationType[] locals;
    final MethodVerifier methodVerifier;
    final VerificationType[] stack;

    int stackSize;
    int activeLocals;

    /**
     * Creates the initial stack and local variables frame for a method.
     */
    public Frame(MethodActor classMethodActor, MethodVerifier methodVerifier) {
        this.methodVerifier = methodVerifier;

        final CodeAttribute codeAttribute = methodVerifier.codeAttribute();
        final int maxLocals = codeAttribute.maxLocals;
        final int maxStack = codeAttribute.maxStack;

        this.locals = new VerificationType[maxLocals];
        this.stack = new VerificationType[maxStack];

        initializeEntryFrame(classMethodActor);
    }

    /**
     * Initializes the state of the stack and locals from the signature of a given method.
     */
    protected void initializeEntryFrame(MethodActor classMethodActor) {
        final ClassActor classActor = classMethodActor.holder();
        // this reference
        if (classMethodActor.isStatic()) {
            if (classMethodActor.isInstanceInitializer()) {
                verifyError("Can't have a static <init> method");
            }
        } else {
            if (!classMethodActor.isInstanceInitializer()) {
                store(methodVerifier.getObjectType(classActor.typeDescriptor), 0);
            } else {
                if (classActor.equals(ClassRegistry.OBJECT)) {
                    store(VerificationType.OBJECT, 0);
                } else {
                    store(VerificationType.UNINITIALIZED_THIS, 0);
                }
            }
        }

        // parameters
        final SignatureDescriptor signature = classMethodActor.descriptor();
        for (int i = 0; i < signature.numberOfParameters(); i++) {
            final TypeDescriptor parameterType = signature.parameterDescriptorAt(i);
            final VerificationType type = methodVerifier.getVerificationType(parameterType);
            store(type, activeLocals);
        }

        for (int i = activeLocals; i < locals.length; i++) {
            locals[i] = VerificationType.TOP;
        }
    }

    Frame(Frame from) {
        methodVerifier = from.methodVerifier;
        locals = from.locals.clone();
        stack = from.stack.clone();
        activeLocals = from.activeLocals;
        stackSize = from.stackSize;
    }

    /**
     * Creates a frame that has the same state as this frame.
     */
    public Frame copy() {
        return new Frame(this);
    }

    public void verifyError(String errorMessage) {
        methodVerifier.verifyError(errorMessage);
    }

    public VerifyError fatalVerifyError(String errorMessage) {
        return methodVerifier.fatalVerifyError(errorMessage);
    }

    public void verifyIsAssignable(VerificationType fromType, VerificationType toType, String errorMessage) {
        methodVerifier.verifyIsAssignable(fromType, toType, errorMessage);
    }

    /**
     * Loads a value from a local variable.
     *
     * @param expectedType the type of the local variable at {@code index} must be assignable to this type
     * @param index an index into the local variables array
     * @return the type of the local variable
     * @throws VerifyError if {@code index} is out of the bounds of the local variable array, denotes an undefined local
     *             variable or denotes a local variable of a type not assignable to {@code expectedType}
     */
    public VerificationType load(VerificationType expectedType, int index) {
        try {
            if (index < activeLocals) {
                final VerificationType type = locals[index];
                verifyIsAssignable(type, expectedType, "Invalid load of local variable");
                if (type.isCategory2()) {
                    verifyIsAssignable(locals[index + 1], type.secondWordType(), "Invalid load of two word value");
                }
                return type;
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            // index < 0
        }
        throw fatalVerifyError("Load from invalid local variable index " + index);
    }

    /**
     * Stores a value to a local variable.
     *
     * @param type the type of the value being stored to the local variable at {@code index}
     * @param index an index into the local variables array
     * @throws VerifyError if {@code index} is out of the bounds of the local variable array
     */
    public void store(VerificationType type, int index) {
        try {
            locals[index] = type;
            if (type.isCategory2()) {
                locals[index + 1] = type.secondWordType();
                activeLocals = Math.max(activeLocals, index + 2);
            } else {
                activeLocals = Math.max(activeLocals, index + 1);
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            // index < 0 || index >= _locals.length
            verifyError("Store to invalid local variable index " + index);
        }
    }

    /**
     * Adjusts the number of defined locals down by a given amount.
     *
     * @param numberOfLocals the number local variables whose definitions are to be killed
     * @throws VerifyError if {@code numberOfLocals > activeLocals()}
     */
    public void chopLocals(int numberOfLocals) {
        try {
            for (int i = 0; i < numberOfLocals; i++) {
                final VerificationType local = locals[activeLocals - 1];
                if (local.isSecondWordType()) {
                    locals[--activeLocals] = VerificationType.TOP;
                }
                locals[--activeLocals] = VerificationType.TOP;
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            verifyError("Chopping more locals than currently active");
        }
    }

    /**
     * Gets the maximum number of locals that have live values. That is, {@code activeLocals() - 1} is the highest index
     * of a local variable in this frame whose value is not {@linkplain VerificationType#TOP}.
     */
    public int activeLocals() {
        return activeLocals;
    }

    /**
     * Pushes a value to the stack.
     *
     * @param type the type of the value being pushed
     * @throws VerifyError if the stack overflows
     */
    public void push(VerificationType type) {
        assert !type.isSecondWordType();
        try {
            stack[stackSize++] = type;
            if (type.isCategory2()) {
                stack[stackSize++] = type.secondWordType();
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            verifyError("Stack overflow");
        }
    }

    /**
     * Pops a value from the top of the stack.
     *
     * @param expectedType the type of the value on the top of the stack must be assignable to this type
     * @return the type of the value popped from the stack
     * @throws VerifyError if the stack underflows or if the value on the top of the stack is not assignable to {@code
     *             expectedType}
     */
    public VerificationType pop(VerificationType expectedType) {
        try {
            final VerificationType type;
            if (expectedType.isCategory2()) {
                type = stack[stackSize - 2];
                verifyIsAssignable(stack[stackSize - 1], type.secondWordType(), "Invalid pop of a two word value from the stack");
                stackSize -= 2;
            } else {
                type = stack[--stackSize];
            }
            verifyIsAssignable(type, expectedType, "Invalid pop of a value from the stack");
            return type;
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            throw fatalVerifyError("Stack underflow");
        }
    }

    /**
     * Gets the value on the top of the stack without adjusting the size of the stack.
     *
     * @throws VerifyError if the stack is emtpy
     */
    public VerificationType top() {
        try {
            final VerificationType top = stack[stackSize - 1];
            if (!top.isSecondWordType()) {
                return top;
            }
            assert stack[stackSize - 2].secondWordType() == top;
            return stack[stackSize - 2];
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            throw fatalVerifyError("Stack underflow");
        }

    }

    public boolean isTypeOnStack(VerificationType type) {
        for (int i = 0; i < stackSize; i++) {
            if (stack[i].equals(type)) {
                return true;
            }
        }
        return false;
    }

    public void clearLocals() {
        for (int i = 0; i < locals.length; i++) {
            locals[i] = VerificationType.TOP;
        }
        activeLocals = 0;
    }

    public void replaceLocals(VerificationType oldType, VerificationType newType) {
        for (int i = 0; i < activeLocals; i++) {
            if (locals[i].equals(oldType)) {
                locals[i] = newType;
            }
        }
    }

    public void replaceStack(VerificationType oldType, VerificationType newType) {
        for (int i = 0; i < stackSize; i++) {
            if (stack[i].equals(oldType)) {
                stack[i] = newType;
            }
        }
    }

    public void clearStack() {
        stackSize = 0;
    }

    public void clear() {
        clearLocals();
        clearStack();
    }

    /**
     * Merges the state of a given frame into this frame.
     *
     * @param fromFrame the frame to merge from
     * @param thisBCI the BCI of this frame
     * @param catchTypeIndex if -1, then this value is ignored and the operand stack state of {@code fromFrame} is
     *            merged into the operand stack state of this frame. Otherwise this value is a constant pool index of
     *            the exception type caught by the exception handler whose entry state is represented by this frame.
     */
    public void mergeFrom(Frame fromFrame, int thisBCI, int catchTypeIndex) {
        if (catchTypeIndex == -1) {
            verifyStackIsAssignableFrom(fromFrame, thisBCI);
        }
        verifyLocalsAreAssignableFrom(fromFrame, thisBCI);
    }

    static String inconsistentFramesMessageSuffix(Frame targetFrame, Frame derivedFrame) {
        final String nl = System.getProperty("line.separator", "\n");
        return nl + "Derived frame:" + nl + derivedFrame + nl + "Recorded frame:" + nl + targetFrame;
    }

    /**
     * Verifies that this frame's operand stack height is the same as a given frame's operand stack height and that each
     * slot in this frame's operand stack {@linkplain VerificationType#isAssignableFrom(VerificationType) is assignable
     * from} the same slot in the given frame.
     *
     * @param fromFrame a frame that must be assignable to this frame
     * @param thisBCI the BCI of this frame
     */
    public final void verifyStackIsAssignableFrom(Frame fromFrame, int thisBCI) {
        if (stackSize != fromFrame.stackSize) {
            verifyError("Inconsistent stackmap frame for BCI " + thisBCI + " (stack sizes differ)" +
                inconsistentFramesMessageSuffix(fromFrame, this));
        }

        for (int i = 0; i < stackSize; i++) {
            if (!stack[i].isAssignableFrom(fromFrame.stack[i])) {
                if (!VerificationType.isTypeIncompatibilityBetweenPointerAndAccessor(fromFrame.stack[i], stack[i])) {
                    verifyError("Stack slot " + i + " is incompatible with stackmap frame for BCI " + thisBCI +
                        inconsistentFramesMessageSuffix(this, fromFrame));
                }
            }
        }
    }

    /**
     * Verifies that each local variable in this frame {@linkplain VerificationType#isAssignableFrom(VerificationType)
     * is assignable from} the same variable in a given frame.
     *
     * @param fromFrame a frame that must be assignable to this frame
     * @param thisBCI the BCI of this frame
     */
    public final void verifyLocalsAreAssignableFrom(Frame fromFrame, int thisBCI) {
        if (fromFrame.activeLocals < activeLocals) {
            verifyError("Inconsistent stackmap frame for BCI " + thisBCI + " (less live locals than implied by stackmap frame)" +
                inconsistentFramesMessageSuffix(fromFrame, this));
        }

        for (int i = 0; i < fromFrame.activeLocals; i++) {
            if (!locals[i].isAssignableFrom(fromFrame.locals[i])) {
                if (!VerificationType.isTypeIncompatibilityBetweenPointerAndAccessor(fromFrame.locals[i], locals[i])) {
                    verifyError("Local variable " + i + " is incompatible with stackmap frame for BCI " + thisBCI +
                        inconsistentFramesMessageSuffix(this, fromFrame));
                }
            }
        }
    }

    public void reset(Frame fromFrame) {
        resetStack(fromFrame);
        resetLocals(fromFrame);
    }

    /**
     * Resets the stack state in this frame to be the same as a given frame.
     */
    public void resetStack(Frame fromFrame) {
        stackSize = fromFrame.stackSize;
        System.arraycopy(fromFrame.stack, 0, stack, 0, stackSize);
        for (int i = stackSize; i != stack.length; ++i) {
            stack[i] = VerificationType.TOP;
        }
    }

    /**
     * Resets the local variable state in this frame to be the same as a given frame.
     */
    public void resetLocals(Frame fromFrame) {
        activeLocals = fromFrame.activeLocals;
        System.arraycopy(fromFrame.locals, 0, locals, 0, activeLocals);
        for (int i = activeLocals; i != locals.length; ++i) {
            locals[i] = VerificationType.TOP;
        }
    }

    /**
     * Gets a copy of the stack state in this frame.
     *
     * @return an array of the types on the stack. The length of this array gives depth of the stack in this frame.
     */
    public VerificationType[] stack() {
        if (stackSize == 0) {
            return VerificationType.NO_TYPES;
        }
        return Arrays.copyOf(stack, stackSize);
    }

    /**
     * Gets a copy of the local variable state in this frame.
     *
     * @return an array of the types in the local variables. The length of this array gives {@linkplain #activeLocals()
     *         the maximum number of locals that have live values}.
     */
    public VerificationType[] locals() {
        if (activeLocals == 0) {
            return VerificationType.NO_TYPES;
        }
        return Arrays.copyOf(locals, activeLocals);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (stackSize > 0) {
            String prefix = "stack[";
            for (int i = 0; i != stackSize; ++i) {
                sb.append(prefix).append(i).append("] = ").append(stack[i]).append('\n');
                prefix = "     [";
            }
        }
        if (activeLocals > 0) {
            String prefix = "local[";
            for (int i = 0; i != activeLocals; ++i) {
                sb.append(prefix).append(i).append("] = ").append(locals[i]).append('\n');
                prefix = "     [";
            }
        }

        // Remove last newline
        final int length = sb.length();
        if (length != 0) {
            assert sb.charAt(length - 1) == '\n';
            sb.deleteCharAt(length - 1);
        }

        return sb.toString();
    }
}
