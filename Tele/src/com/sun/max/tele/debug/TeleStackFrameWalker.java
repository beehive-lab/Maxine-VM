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

import com.sun.max.collect.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Specialization of a StackFrameWalker for use with a remote tele VM.
 *
 * @author Laurent Daynes
 * @author Doug Simon
 */
public final class TeleStackFrameWalker extends StackFrameWalker {

    private final TeleVM _teleVM;
    private final TeleVMThreadLocalValues _teleEnabledVmThreadLocalValues;
    private final TeleNativeThread _teleNativeThread;
    private final DataAccess _dataAccess;

    private final Pointer _cpuInstructionPointer;
    private final Pointer _cpuStackPointer;
    private final Pointer _cpuFramePointer;

    public TeleStackFrameWalker(TeleVM teleVM, TeleNativeThread teleNativeThread) {
        super(teleVM.compilerScheme());
        _teleVM = teleVM;
        _teleNativeThread = teleNativeThread;
        _teleEnabledVmThreadLocalValues = teleNativeThread.stack().enabledVmThreadLocalValues();
        _dataAccess = teleVM.teleProcess().dataAccess();
        _cpuInstructionPointer = teleNativeThread.instructionPointer();
        _cpuStackPointer = teleNativeThread.stackPointer();
        _cpuFramePointer = teleNativeThread.framePointer();
    }

    public class ErrorStackFrame extends StackFrame {
        private final String _errorMessage;
        ErrorStackFrame(StackFrame callee, StackFrameWalker walker, String errorMessage) {
            super(callee, walker.instructionPointer(), walker.stackPointer(), walker.framePointer());
            _errorMessage = errorMessage;
        }

        @Override
        public TargetMethod targetMethod() {
            return null;
        }

        @Override
        public boolean isJavaStackFrame() {
            return false;
        }

        public String errorMessage() {
            return _errorMessage;
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
    }

    public Sequence<StackFrame> frames() {
        final AppendableSequence<StackFrame> frames = new LinkSequence<StackFrame>();
        try {
            frames(frames, _cpuInstructionPointer, _cpuStackPointer, _cpuFramePointer);
        } catch (Throwable e) {
            final StackFrame parentFrame = frames.isEmpty() ? null : frames.last();
            frames.append(new ErrorStackFrame(parentFrame, this, e.toString()));
        }
        return frames;
    }

    @Override
    public boolean isThreadInNative() {
        return _teleEnabledVmThreadLocalValues.isValid() && !_teleEnabledVmThreadLocalValues.isInJavaCode();
    }

    @Override
    public TargetMethod targetMethodFor(Pointer instructionPointer) {
        final TeleTargetMethod teleTargetMethod = TeleTargetMethod.make(_teleVM, instructionPointer);
        if (teleTargetMethod != null) {
            return teleTargetMethod.reducedShallowCopy();
        }
        return null;
    }

    @Override
    protected RuntimeStub runtimeStubFor(Pointer instructionPointer) {
        final TeleRuntimeStub teleRuntimeStub = TeleRuntimeStub.make(_teleVM, instructionPointer);
        if (teleRuntimeStub != null) {
            return teleRuntimeStub.runtimeStub();
        }
        return null;
    }
    @Override
    public byte readByte(Address address, int offset) {
        return _dataAccess.readByte(address, offset);
    }

    @Override
    public Word readWord(Address address, int offset) {
        return _dataAccess.readWord(address, offset);
    }

    @Override
    public int readInt(Address address, int offset) {
        return _dataAccess.readInt(address, offset);
    }

    @Override
    public Word readWord(VmThreadLocal local) {
        return _teleEnabledVmThreadLocalValues.getWord(local);
    }

    @Override
    public boolean trapHandlerHasRecordedTrapFrame() {
        return _teleEnabledVmThreadLocalValues.isValid() && !_teleEnabledVmThreadLocalValues.getWord(VmThreadLocal.TRAP_HANDLER_HAS_RECORDED_TRAP_FRAME).isZero();
    }

    @Override
    public Word readFramelessCallAddressRegister(TargetABI targetABI) {
        return _teleNativeThread.integerRegisters().get(Role.FRAMELESS_CALL_INSTRUCTION_ADDRESS, targetABI);
    }

    @Override
    public void useABI(TargetABI targetABI) {
        final Pointer abiStackPointer = _teleNativeThread.integerRegisters().get(VMRegister.Role.ABI_STACK_POINTER, targetABI);
        final Pointer abiFramePointer = _teleNativeThread.integerRegisters().get(VMRegister.Role.ABI_FRAME_POINTER, targetABI);
        advance(_cpuInstructionPointer, abiStackPointer, abiFramePointer);
    }
}
