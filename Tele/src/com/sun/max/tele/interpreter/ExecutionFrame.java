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

import com.sun.max.collect.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.value.*;

/**
 * Instances of this class represent individual execution frame entries on a given ExecutionThread's execution stack.
 *
 * @author Athul Acharya
 */
class ExecutionFrame {

    private final ClassMethodActor _method;
    private int _currentOpcodePosition;
    private int _currentBytePosition;
    private final Value[] _locals;
    private final Stack<Value> _operands;
    private final ExecutionFrame _callersFrame;
    private final byte[] _code;
    private final int _depth;

    public ExecutionFrame(ExecutionFrame callersFrame, ClassMethodActor method) {
        _method = method;
        _locals = new Value[method.codeAttribute().maxLocals()];
        _operands = new Stack<Value>();
        _callersFrame = callersFrame;
        _code = method.codeAttribute().code();
        _depth = callersFrame == null ? 1 : callersFrame._depth + 1;
    }

    /**
     * Computes the number of frames on the call stack up to and including this frame.
     */
    public int depth() {
        return _depth;
    }

    public ExecutionFrame callersFrame() {
        return _callersFrame;
    }

    public void setLocal(int index, Value value) {
        _locals[index] = value;
    }

    public Bytecode readOpcode() {
        _currentOpcodePosition = _currentBytePosition;
        return Bytecode.from(readByte());
    }

    public byte readByte() {
        try {
            return _code[_currentBytePosition++];
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
        _currentBytePosition += n;
    }

    public void alignInstructionPosition() {
        final int remainder = _currentBytePosition % 4;
        if (remainder != 0) {
            _currentBytePosition += 4 - remainder;
        }
    }

    public void jump(int offset) {
        _currentBytePosition = _currentOpcodePosition + offset;
    }

    public int currentOpcodePosition() {
        return _currentOpcodePosition;
    }

    public int currentBytePosition() {
        return _currentBytePosition;
    }

    public void setBytecodePosition(int bcp) {
        _currentBytePosition = bcp;
    }

    public byte[] code() {
        return _code;
    }

    public Value getLocal(int index) {
        return _locals[index];
    }

    public Stack<Value> stack() {
        return _operands;
    }

    public ConstantPool constantPool() {
        return _method.codeAttribute().constantPool();
    }

    public ClassMethodActor method() {
        return _method;
    }

    @Override
    public String toString() {
        return _method.format("%H.%n(%p) @ " + _currentBytePosition);
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
        final int bcp = _currentOpcodePosition;
        final Sequence<ExceptionHandlerEntry> handlers = method().codeAttribute().exceptionHandlerTable();
        for (ExceptionHandlerEntry handler : handlers) {
            if (bcp >= handler.startPosition() && bcp < handler.endPosition()) {
                if (handler.catchTypeIndex() == 0) {
                    _currentBytePosition = handler.handlerPosition();
                    return true;
                }
                final ClassActor catchType = constantPool().classAt(handler.catchTypeIndex()).resolve(constantPool(), handler.catchTypeIndex());
                if (catchType.isAssignableFrom(throwableClassActor)) {
                    _currentBytePosition = handler.handlerPosition();
                    return true;
                }
            }
        }
        return false;
    }
}
