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
package com.sun.max.vm.jit.amd64;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.amd64.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64Deoptimization extends Deoptimization {

    AMD64Deoptimization() {
        super();
    }

    private final PrependableSequence<JitStackFrameInfo> jitStackFrameInfos = new ArrayListSequence<JitStackFrameInfo>();

    private int currentCallSaveAreaPosition;

    @Override
    protected void createJitFrame(TargetJavaFrameDescriptor javaFrameDescriptor, Deoptimizer.Situation situation) {
        final ClassMethodActor classMethodActor = javaFrameDescriptor.classMethodActor();
        final JitTargetMethod jitTargetMethod = (JitTargetMethod) VMConfiguration.target().jitScheme().compile(classMethodActor, CompilationDirective.DEFAULT);
        final AMD64JitStackFrameLayout layout = (AMD64JitStackFrameLayout) jitTargetMethod.stackFrameLayout();

        int numberOfStackSlots = javaFrameDescriptor.stackSlots().length;
        int numberOfDescribedStackSlots = numberOfStackSlots;
        Kind resultKind = Kind.VOID;
        if (situation != Deoptimizer.Situation.SAFEPOINT) {
            // We are working on the top frame just after a return from a call.
            // Not all of the Java state described by the frame descriptor applies.
            assert currentCallSaveAreaPosition == 0;

            // First, subtract the call's arguments (which JIT code would have popped from the stack by now):
            final MethodActor callee = javaFrameDescriptor.getCalleeMethodActor();
            if (!callee.isStatic()) {
                // Remove receiver argument slot:
                numberOfStackSlots--;
            }
            for (Kind parameterKind : callee.getParameterKinds()) {
                numberOfStackSlots -= parameterKind.isCategory1() ? 1 : 2;
            }
            numberOfDescribedStackSlots = numberOfStackSlots;

            // Then add as many stack slots as needed for the return value:
            resultKind = callee.resultKind();
            if (resultKind != Kind.VOID) {
                numberOfStackSlots += resultKind.isCategory1() ? 1 : 2;
            }
        }

        final int stackBottomPosition = currentCallSaveAreaPosition + numberOfStackSlots * JitStackFrameLayout.JIT_SLOT_SIZE;
        final int framePointerPosition = stackBottomPosition + layout.numberOfNonParameterSlots() * JitStackFrameLayout.JIT_SLOT_SIZE;
        currentCallSaveAreaPosition = stackBottomPosition + layout.frameSize();
        final int incomingParametersPosition = currentCallSaveAreaPosition + layout.numberOfParameterSlots() * JitStackFrameLayout.JIT_SLOT_SIZE;
        buffer().extend(incomingParametersPosition);

        if (javaFrameDescriptor.stackSlots() != null) {
            for (int i = 1; i <= numberOfDescribedStackSlots; i++) {
                buffer().setPosition(stackBottomPosition - (i * JitStackFrameLayout.JIT_SLOT_SIZE));
                javaFrameDescriptor.stackSlots()[i - 1].acceptVisitor(this);
            }
        }

        if (resultKind != Kind.VOID) {
            // Fill the return slot, which is the first slot in the whole buffer:
            buffer().setPosition(0);
            if (resultKind == Kind.FLOAT || resultKind == Kind.DOUBLE) {
                AMD64Deoptimizer.FLOATING_POINT_RETURN_REGISTER.acceptVisitor(this);
            } else {
                AMD64Deoptimizer.INTEGER_RETURN_REGISTER.acceptVisitor(this);
            }
        }

        if (javaFrameDescriptor.locals() != null) {
            for (int i = 1; i <= javaFrameDescriptor.locals().length; i++) {
                if (i < layout.numberOfParameterSlots()) {
                    buffer().setPosition(incomingParametersPosition - (i * JitStackFrameLayout.JIT_SLOT_SIZE));
                } else {
                    buffer().setPosition(framePointerPosition - ((i - incomingParametersPosition) * JitStackFrameLayout.JIT_SLOT_SIZE));
                }
                javaFrameDescriptor.locals()[i - 1].acceptVisitor(this);
            }
        }

        final Pointer instructionPointer = jitTargetMethod.codeStart().plus(jitTargetMethod.targetCodePositionFor(javaFrameDescriptor.bytecodePosition()));
        jitStackFrameInfos.prepend(new JitStackFrameInfo(currentCallSaveAreaPosition, framePointerPosition, instructionPointer));
    }

    public void visit(TargetLocation.Block block) {
        buffer().writeWord(sourceFrame().targetMethod().codeStart().plus(block.position()));
    }

    public void visit(TargetLocation.Immediate immediate) {
        buffer().writeWord(immediate.value().toWord());
    }

    private double getFloatingPointRegisterValue(int index) {
        throw FatalError.unimplemented();
    }

    public void visit(TargetLocation.FloatingPointRegister floatingPointRegister) {
        buffer().writeWord(Address.fromLong(UnsafeLoophole.doubleToLong(getFloatingPointRegisterValue(floatingPointRegister.index()))));
    }

    private Word getIntegerRegisterValue(int index) {
        throw FatalError.unimplemented();
    }

    public void visit(TargetLocation.IntegerRegister integerRegister) {
        buffer().writeWord(getIntegerRegisterValue(integerRegister.index()));
    }

    public void visit(TargetLocation.LocalStackSlot localStackSlot) {
        buffer().writeWord(sourceFrame().slotBase().getWord(localStackSlot.index()));
    }

    public void visit(TargetLocation.ParameterStackSlot parameterStackSlot) {
        buffer().writeWord(parentFrame().slotBase().getWord(parameterStackSlot.index()));
    }

    public void visit(TargetLocation.ReferenceLiteral referenceLiteral) {
        buffer().writeWord(UnsafeLoophole.objectToWord(sourceFrame().targetMethod().referenceLiterals()[referenceLiteral.index()]));
    }

    public void visit(TargetLocation.ScalarLiteral scalarLiteral) {
        final Pointer p = ArrayAccess.elementPointer(sourceFrame().targetMethod().scalarLiteralBytes(), scalarLiteral.index());
        buffer().writeWord(p.getWord());
    }

    public void visit(TargetLocation.Method method) {
        ProgramError.unexpected("found method as target location during deopt");
    }

    public void visit(TargetLocation.Undefined undefined) {
    }

    @Override
    protected void createAdapterFrame(TargetJavaFrameDescriptor javaFrameDescriptor) {
        buffer().extend(buffer().size() + JavaStackFrameLayout.STACK_SLOT_SIZE);
        final int callSaveAreaPosition = buffer().size();

        final ClassMethodActor classMethodActor = javaFrameDescriptor.classMethodActor();
        final JitTargetMethod jitTargetMethod = (JitTargetMethod) VMConfiguration.target().jitScheme().compile(classMethodActor, CompilationDirective.DEFAULT);
        assert jitTargetMethod.adapterReturnPosition() > 0;
        final Pointer instructionPointer = jitTargetMethod.codeStart().plus(jitTargetMethod.adapterReturnPosition());

        jitStackFrameInfos.prepend(new JitStackFrameInfo(callSaveAreaPosition, instructionPointer));
    }

    private Pointer stackPointer;
    private Pointer instructionPointer;
    private Pointer _framePointer;

    @Override
    protected void fixCallChain() {
        stackPointer = parentFrame().stackPointer().minus(buffer().size());
        instructionPointer = parentFrame().instructionPointer();
        _framePointer = parentFrame().framePointer();
        for (JitStackFrameInfo info : jitStackFrameInfos) {
            buffer().setPosition(info.callSaveAreaPosition() - JavaStackFrameLayout.STACK_SLOT_SIZE);
            buffer().writeWord(instructionPointer);
            instructionPointer = info.instructionPointer();

            if (!info.isAdapterFrame()) {
                buffer().setPosition(info.callSaveAreaPosition() - (2 * JavaStackFrameLayout.STACK_SLOT_SIZE));
                buffer().writeWord(_framePointer);
                _framePointer = stackPointer.plus(info.framePointerPosition());
            }
        }
    }

    private static final int EXTRA_SPACE = 256;

    @Override
    protected void patchExecutionContext() {
        if (VMRegister.getAbiStackPointer().greaterThan(stackPointer.plus(EXTRA_SPACE))) {
            // Recurse until there is sufficient space on the stack to patch the new JIT frames in:
            patchExecutionContext();
        }
        buffer().copyToMemory(stackPointer);
        Safepoint.enable();
        AMD64JitCompiler.unwind(instructionPointer, stackPointer, _framePointer);
    }
}
