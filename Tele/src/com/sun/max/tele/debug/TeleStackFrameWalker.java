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
package com.sun.max.tele.debug;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Specialization of a StackFrameWalker for use with a {@link TeleVM}.
 *
 * @author Laurent Daynes
 * @author Doug Simon
 */
public final class TeleStackFrameWalker extends StackFrameWalker {

    private final TeleVM teleVM;
    private final TeleThreadLocalsArea teleEnabledVmThreadLocalValues;
    private final TeleNativeThread teleNativeThread;

    private final Pointer cpuInstructionPointer;
    private final Pointer cpuStackPointer;
    private final Pointer cpuFramePointer;

    public TeleStackFrameWalker(TeleVM teleVM, TeleNativeThread teleNativeThread) {
        super();
        this.teleVM = teleVM;
        this.teleNativeThread = teleNativeThread;
        this.teleEnabledVmThreadLocalValues = teleNativeThread.localsBlock().threadLocalsAreaFor(Safepoint.State.ENABLED);
        final TeleRegisterSet registers = teleNativeThread.registers();
        this.cpuInstructionPointer = registers.instructionPointer();
        this.cpuStackPointer = registers.stackPointer();
        this.cpuFramePointer = registers.framePointer();
    }

    /**
     * Denotes an error that occurred while walking the stack for the purpose of inspecting the
     * frames on the stack.
     */
    public class ErrorStackFrame extends StackFrame {
        private final String errorMessage;
        ErrorStackFrame(StackFrame callee, String errorMessage) {
            super(callee, Pointer.zero(), Pointer.zero(), Pointer.zero());
            this.errorMessage = errorMessage;
        }

        public String errorMessage() {
            return errorMessage;
        }

        @Override
        public boolean isSameFrame(StackFrame stackFrame) {
            if (stackFrame instanceof ErrorStackFrame) {
                final ErrorStackFrame other = (ErrorStackFrame) stackFrame;
                final StackFrame calleeFrame = calleeFrame();
                if (calleeFrame == null) {
                    return other.calleeFrame() == null;
                }
                return calleeFrame.isSameFrame(other.calleeFrame());
            }
            return false;
        }
        @Override
        public String toString() {
            return "<error: " + errorMessage + ">";
        }
    }

    public List<StackFrame> frames() {
        final VariableSequence<StackFrame> frames = new ArrayListSequence<StackFrame>();
        try {
            frames(frames, cpuInstructionPointer, cpuStackPointer, cpuFramePointer);
        } catch (Throwable e) {
            e.printStackTrace();
            final StackFrame parentFrame = frames.isEmpty() ? null : frames.last();
            frames.append(new ErrorStackFrame(parentFrame, e.toString()));
        }
        return new ArrayList<StackFrame>(frames.toCollection());
    }

    @Override
    public TargetMethod targetMethodFor(Pointer instructionPointer) {
        final TeleCompiledCode compiledCode = teleVM.codeCache().findCompiledCode(instructionPointer);
        if (compiledCode != null) {
            return compiledCode.teleTargetMethod().targetMethod();
        }
        return null;
    }

    @Override
    public byte readByte(Address address, int offset) {
        return teleVM.dataAccess().readByte(address, offset);
    }

    @Override
    public Word readWord(Address address, int offset) {
        return teleVM.dataAccess().readWord(address, offset);
    }

    @Override
    public int readInt(Address address, int offset) {
        return teleVM.dataAccess().readInt(address, offset);
    }

    @Override
    public Word readWord(VmThreadLocal local) {
        return teleEnabledVmThreadLocalValues == null ? Pointer.zero() : teleEnabledVmThreadLocalValues.getWord(local);
    }

    @Override
    public void useABI(TargetABI targetABI) {
        final TeleIntegerRegisters teleIntegerRegisters = teleNativeThread.registers().teleIntegerRegisters();
        final Pointer abiStackPointer = teleIntegerRegisters.get(VMRegister.Role.ABI_STACK_POINTER, targetABI);
        final Pointer abiFramePointer = teleIntegerRegisters.get(VMRegister.Role.ABI_FRAME_POINTER, targetABI);
        advance(cpuInstructionPointer, abiStackPointer, abiFramePointer);
    }
}
