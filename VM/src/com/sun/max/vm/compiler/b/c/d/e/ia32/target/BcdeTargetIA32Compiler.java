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
package com.sun.max.vm.compiler.b.c.d.e.ia32.target;

import static com.sun.max.vm.compiler.CallEntryPoint.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.ia32.complete.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.b.c.d.e.ia32.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.eir.ia32.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.stack.ia32.*;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 */
public final class BcdeTargetIA32Compiler extends BcdeIA32Compiler implements TargetGeneratorScheme {

    private final IA32EirToTargetTranslator _eirToTargetTranslator;

    protected IA32EirToTargetTranslator createTargetTranslator() {
        return new IA32EirToTargetTranslator(this);
    }

    public BcdeTargetIA32Compiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        _eirToTargetTranslator = new IA32EirToTargetTranslator(this);
    }

    public TargetGenerator targetGenerator() {
        return _eirToTargetTranslator;
    }

    @Override
    public IrGenerator irGenerator() {
        return _eirToTargetTranslator;
    }

    @Override
    public Sequence<IrGenerator> irGenerators() {
        return Sequence.Static.appended(super.irGenerators(), _eirToTargetTranslator);
    }

    public TargetABI javaTargetABI() {
        return eirGenerator().eirABIsScheme().javaABI().targetABI();
    }

    private static final int RETURN_ADDRESS_OFFSET = 0;

    @INLINE
    private static void patchReturnInstructionPointer(Pointer instructionPointer) {
        final Pointer stackPointer = VMRegister.getCpuStackPointer();
        stackPointer.writeWord(RETURN_ADDRESS_OFFSET, instructionPointer);
    }

    static final int RIP_CALL_INSTRUCTION_SIZE = 5;
    static final int REGISTER_CALL_INSTRUCTION_SIZE = 1;

    @INLINE
    private static void patchRipCallSite(Pointer callSite, Address calleeEntryPoint) {
        final int calleeOffset = calleeEntryPoint.minus(callSite.plus(RIP_CALL_INSTRUCTION_SIZE)).toInt();
        callSite.writeInt(1, calleeOffset);
    }

    @INLINE
    private static Pointer getCurrentCallSite(int callInstructionSize) {
        Problem.unimplemented();
        return Pointer.zero();
    }

    /**
     * This is a snippet implementation and we want to access the snippet's caller via our stack pointer.
     * So we eliminate one call layer around the body of this method by making inlining into the snippet mandatory.
     *
     * @see StaticTrampoline
     */
    @INLINE
    @Override
    public void staticTrampoline() {
        final Pointer callSite = getCurrentCallSite(RIP_CALL_INSTRUCTION_SIZE); // somewhere in the snippet's caller, thanks to our being inlined
        final TargetMethod caller = Code.codePointerToTargetMethod(callSite);
        final ClassMethodActor callee = caller.callSiteToCallee(callSite);
        final Address calleeEntryPoint = CompilationScheme.Static.compile(callee, caller.abi().callEntryPoint(), CompilationDirective.DEFAULT);
        patchRipCallSite(callSite, calleeEntryPoint);

        // Make the snippet's caller re-execute the now modified CALL instruction after we return from this method:
        patchReturnInstructionPointer(callSite);
    }

    @Override
    public Word createInitialVTableEntry(int vTableIndex, VirtualMethodActor dynamicMethodActor) {
        return  vmConfiguration().trampolineScheme().makeVirtualCallEntryPoint(vTableIndex);
    }

    @Override
    public Word createInitialITableEntry(int iIndex, VirtualMethodActor dynamicMethodActor) {
        return  vmConfiguration().trampolineScheme().makeInterfaceCallEntryPoint(iIndex);
    }

    public void patchCallSite(TargetMethod targetMethod, int callOffset, Word callEntryPoint) {
        final Pointer callSite = targetMethod.codeStart().plus(callOffset).asPointer();
        final IA32Assembler assembler = new IA32Assembler(callSite.toInt());
        final Label label = new Label();
        assembler.fixLabel(label, callEntryPoint.asAddress().toInt());
        assembler.call(label);
        try {
            final byte[] code = assembler.toByteArray();
            Bytes.copy(code, 0, targetMethod.code(), callOffset, code.length);
        } catch (AssemblyException assemblyException) {
            ProgramError.unexpected("patching call site failed", assemblyException);
        }
    }

    private static final byte RET = (byte) 0xC3;

    @Override
    public boolean walkFrame(StackFrameWalker stackFrameWalker, boolean isTopFrame, TargetMethod targetMethod, Purpose purpose, Object context) {
        final Pointer instructionPointer = stackFrameWalker.instructionPointer();
        final Pointer stackPointer = stackFrameWalker.stackPointer();
        final Pointer framePointer = stackFrameWalker.framePointer();
        final int frameSize;
        final Pointer callEntryStackPointer;
        final Pointer callEntryPoint = OPTIMIZED_ENTRY_POINT.in(targetMethod);

        if (instructionPointer.equals(callEntryPoint) || stackFrameWalker.readByte(instructionPointer, 0) == RET) {
            // We are at the very first or the very last instruction to be executed.
            // In either case the stack pointer is unmodified wrt. the CALL that got us here.
            frameSize = 0;
            callEntryStackPointer = stackPointer;
        } else {
            // We are somewhere in the middle of this method.
            // The stack pointer has been bumped already to access locals and it has not been reset yet.
            frameSize = targetMethod.frameSize();
            callEntryStackPointer = stackPointer.plus(frameSize);
        }
        switch (purpose) {
            case REFERENCE_MAP_PREPARING: {
                targetMethod.prepareFrameReferenceMap((StackReferenceMapPreparer) context, instructionPointer, stackPointer, stackPointer); // frame pointer == stack pointer
                break;
            }
            case EXCEPTION_HANDLING: {
                final Address catchAddress = targetMethod.throwAddressToCatchAddress(instructionPointer);
                if (!catchAddress.isZero()) {
                    final Throwable throwable = UnsafeLoophole.cast(StackUnwindingContext.class, context)._throwable;
                    if (!(throwable instanceof StackOverflowError) || VmThread.current().hasSufficentStackToReprotectGuardPage(stackPointer)) {
                        // Reset the stack walker
                        stackFrameWalker.reset();

                        // Assign 'throwable' to RAX
                        IA32EirGenerator.assignExceptionToCatchParameterLocation(throwable);

                        // Unwind the stack
                        SpecialBuiltin.setIntegerRegister(VMRegister.Role.CPU_STACK_POINTER, stackPointer);

                        // Jump to the exception dispatcher
                        SpecialBuiltin.jump(catchAddress);
                    }
                }
                break;
            }
            case INSPECTING: {
                final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                if (!stackFrameVisitor.visitFrame(new IA32JavaStackFrame(stackFrameWalker.calleeStackFrame(), targetMethod, instructionPointer, stackPointer, stackPointer))) {
                    return false;
                }
                break;
            }
        }

        final Pointer callerInstructionPointer = stackFrameWalker.readWord(callEntryStackPointer, 0).asPointer();
        final Pointer callerStackPointer = callEntryStackPointer.plus(Word.size());
        stackFrameWalker.advance(callerInstructionPointer, callerStackPointer, framePointer);
        return true;
    }

    @Override
    public Pointer namedVariablesBasePointer(Pointer stackPointer, Pointer framePointer) {
        return stackPointer;
    }

    @Override
    public StackUnwindingContext makeStackUnwindingContext(Word stackPointer, Word framePointer, Throwable throwable) {
        return new StackUnwindingContext(throwable);
    }
}
