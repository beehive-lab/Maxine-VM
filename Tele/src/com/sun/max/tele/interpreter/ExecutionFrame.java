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
package com.sun.max.tele.interpreter;

import java.util.*;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.value.*;

/**
 * Instances of this class represent individual execution frame entries on a given ExecutionThread's execution stack.
 *
 * @author Athul Acharya
 */
class ExecutionFrame {

    private final ClassMethodActor method;
    private int currentOpcodePosition;
    private int currentBytePosition;
    private final Value[] locals;
    private final Stack<Value> operands;
    private final ExecutionFrame callersFrame;
    private final byte[] code;
    private final int depth;

    public ExecutionFrame(ExecutionFrame callersFrame, ClassMethodActor method) {
        this.method = method;
        this.locals = new Value[method.codeAttribute().maxLocals];
        this.operands = new Stack<Value>();
        this.callersFrame = callersFrame;
        this.code = method.codeAttribute().code();
        this.depth = callersFrame == null ? 1 : callersFrame.depth + 1;
    }

    /**
     * Computes the number of frames on the call stack up to and including this frame.
     */
    public int depth() {
        return depth;
    }

    public ExecutionFrame callersFrame() {
        return callersFrame;
    }

    public void setLocal(int index, Value value) {
        locals[index] = value;
    }

    public int readOpcode() {
        currentOpcodePosition = currentBytePosition;
        return readByte() & 0xff;
    }

    public byte readByte() {
        try {
            return code[currentBytePosition++];
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            throw new VerifyError("Ran off end of code");
        }
    }

    public short readShort() {
        final int high = readByte();
        final int low = readByte() & 0xff;
        return (short) ((high << 8) | low);
    }

    public int readInt() {
        final int b3 = readByte() << 24;
        final int b2 = (readByte() & 0xff) << 16;
        final int b1 = (readByte() & 0xff) << 8;
        final int b0 = readByte() & 0xff;
        return b3 | b2 | b1 | b0;
    }

    public void skipBytes(int n) {
        currentBytePosition += n;
    }

    public void alignInstructionPosition() {
        final int remainder = currentBytePosition % 4;
        if (remainder != 0) {
            currentBytePosition += 4 - remainder;
        }
    }

    public void jump(int offset) {
        currentBytePosition = currentOpcodePosition + offset;
    }

    public int currentOpcodePosition() {
        return currentOpcodePosition;
    }

    public int currentBytePosition() {
        return currentBytePosition;
    }

    public void setBytecodePosition(int bcp) {
        currentBytePosition = bcp;
    }

    public byte[] code() {
        return code;
    }

    public Value getLocal(int index) {
        return locals[index];
    }

    public Stack<Value> stack() {
        return operands;
    }

    public ConstantPool constantPool() {
        return method.codeAttribute().constantPool;
    }

    public ClassMethodActor method() {
        return method;
    }

    @Override
    public String toString() {
        return method.format("%H.%n(%p) @ " + currentBytePosition);
    }

    /**
     * Handles an exception at the current execution point in this frame by updating the {@linkplain #setBytecodePosition(int)
     * instruction pointer} to the matching exception handler in this frame. If no matching exception handler is found
     * for the current execution point and the given exception type, then the instruction pointer in this frame is left
     * unmodified.
     * <p>
     * The current execution point is derived from the value of {@link ExecutionFrame#currentBytePosition()} which is now at the first
     * byte passed the instruction currently being executed. That is, an instruction is completely decoded before any
     * exceptions are thrown while executing it.
     *
     * @param throwableClassActor the type of the exception being thrown
     * @return {@code true} if an exception handler was found, {@code false} otherwise
     */
    public boolean handleException(ClassActor throwableClassActor) {
        final int bcp = currentOpcodePosition;
        final ExceptionHandlerEntry[] handlers = method().codeAttribute().exceptionHandlerTable();
        for (ExceptionHandlerEntry handler : handlers) {
            if (bcp >= handler.startPosition() && bcp < handler.endPosition()) {
                if (handler.catchTypeIndex() == 0) {
                    currentBytePosition = handler.handlerPosition();
                    return true;
                }
                final ClassActor catchType = constantPool().classAt(handler.catchTypeIndex()).resolve(constantPool(), handler.catchTypeIndex());
                if (catchType.isAssignableFrom(throwableClassActor)) {
                    currentBytePosition = handler.handlerPosition();
                    return true;
                }
            }
        }
        return false;
    }
}
