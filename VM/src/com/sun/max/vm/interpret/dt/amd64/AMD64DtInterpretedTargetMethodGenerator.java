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
package com.sun.max.vm.interpret.dt.amd64;

import com.sun.max.asm.*;
import com.sun.max.asm.Assembler.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.b.c.d.e.amd64.target.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.interpret.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;

/**
 * Generates InterpretedTargetMethods for the AMD64DtInterpreter.
 *
 * The generated InterpretedTargetMethod contains stub code which performs the
 * method-specific set-up of an interpreter activation.
 *
 * @author Simon Wilkinson
 */
class AMD64DtInterpretedTargetMethodGenerator implements InterpretedTargetMethodGenerator {

    private static final TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> targetABI;

    private static final boolean needsAdapterFrame;

    static {
        final Class<TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>> type = null;
        targetABI = StaticLoophole.cast(type, VMConfiguration.target().targetABIsScheme().interpreterABI());
        needsAdapterFrame = VMConfiguration.target().compilerScheme() instanceof BcdeTargetAMD64Compiler;
    }

    private final AMD64Assembler asm = new AMD64Assembler();
    private final PrependableSequence<Object> referenceLiterals = new LinkSequence<Object>();
    private final AMD64DtInterpreter interpreter;

    protected AMD64DtInterpretedTargetMethodGenerator(AMD64DtInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    protected int computeReferenceLiteralOffset(int numReferenceLiteral) {
        return numReferenceLiteral * Word.size() + Layout.byteArrayLayout().getElementOffsetInCell(asm.currentPosition()).toInt();
    }

    private int createReferenceLiteral(Object literal) {
        int literalOffset = computeReferenceLiteralOffset(1 + referenceLiterals.length());
        referenceLiterals.prepend(literal);
        if (VMConfiguration.target().debugging()) {
            // Account for the DebugHeap tag in front of the code object:
            literalOffset += VMConfiguration.target().wordWidth().numberOfBytes;
        }
        return -literalOffset;
    }

    private Object[] packReferenceLiterals() {
        if (referenceLiterals.isEmpty()) {
            return null;
        }
        if (MaxineVM.isPrototyping()) {
            return Sequence.Static.toArray(referenceLiterals, Object.class);
        }
        // Must not cause checkcast here, since some reference literals may be static tuples.
        final Object[] result = new Object[referenceLiterals.length()];
        int i = 0;
        for (Object literal : referenceLiterals) {
            ArrayAccess.setObject(result, i, literal);
            i++;
        }
        return result;
    }

    private void buildExceptionHandlingInfo(ClassMethodActor classMethodActor) {
        final Sequence<ExceptionHandlerEntry> exceptionHandlers = classMethodActor.codeAttribute().exceptionHandlerTable();
        if (exceptionHandlers.isEmpty()) {
            return;
        }
        // Ignore exceptions for now
        return;
    }

    private static final int RIP_MOV_SIZE = 7;

    public void generate(InterpretedMethodState methodState) {

        final ClassMethodActor classMethodActor = methodState.classMethodActor();
        final InterpretedTargetMethod targetMethod = new AMD64DtInterpretedTargetMethod(classMethodActor, interpreter);
        final AMD64DtInterpreterStackFrameLayout stackFrameLayout = (AMD64DtInterpreterStackFrameLayout) targetMethod.stackFrameLayout();

        // Adapter frame setup
        if (needsAdapterFrame) {
            final EirGenerator eirGenerator = ((BcdeTargetAMD64Compiler) VMConfiguration.target().compilerScheme().vmConfiguration().compilerScheme()).eirGenerator();
            final EirABI optimizingCompilerAbi = eirGenerator.eirABIsScheme().getABIFor(classMethodActor);
            // Just use the opt-to-jit AdapterFrameGenerator as the interpreter shares the JIT's stack layout.
            final AMD64AdapterFrameGenerator adapterFrameGenerator = AMD64AdapterFrameGenerator.optimizingToJitCompilerAdapterFrameGenerator(classMethodActor, optimizingCompilerAbi);
            final Directives dir = asm.directives();
            final Label methodEntryPoint = new Label();
            asm.jmp(methodEntryPoint);
            asm.nop();
            asm.nop();
            asm.nop();
            dir.align(Kind.BYTE.width.numberOfBytes * 4);  // forcing alignment to the next 4-bytes will always provide an 8-bytes long prologue.
            final Label adapterCodeStart = new Label();
            asm.bindLabel(adapterCodeStart);
            adapterFrameGenerator.emitPrologue(asm);
            adapterFrameGenerator.emitEpilogue(asm);
            asm.bindLabel(methodEntryPoint);
        }

        // Stub code which performs the method-specific set-up of an interpreter activation.

        // Pass a pointer to the bytecode array we want to interpret
        asm.rip_mov(AMD64DtInterpreterABI.bytecodeArrayPointer(), createReferenceLiteral(classMethodActor.codeAttribute().code()) - RIP_MOV_SIZE);

        // Pass the size of the non-param locals needed by this method
        asm.mov(AMD64DtInterpreterABI.nonParameterlocalsSize(), stackFrameLayout.sizeOfNonParameterLocals());

        // Call the interpreter
        asm.mov(targetABI.scratchRegister(), interpreter.entryPoint().toLong());
        asm.call(targetABI.scratchRegister());
        asm.ret();

        // Produce target method
        final Object[] referenceLiterals = packReferenceLiterals();
        buildExceptionHandlingInfo(classMethodActor);
        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(0, 0, 0, 0, 0, (referenceLiterals == null) ? 0 : referenceLiterals.length, asm.currentPosition(), 0, 0);
        targetMethod.setSize(targetBundleLayout.bundleSize());
        Code.allocate(targetMethod);
        try {
            targetMethod.setGenerated(new TargetBundle(targetBundleLayout, targetMethod.start()), null, null, null, null, null, 0, 0, null, null, referenceLiterals, asm.toByteArray(), null, 0, 0, targetABI);
        } catch (AssemblyException e) {
            ProgramError.unexpected(e);
        }
        methodState.addInterprettedTargetMethod(targetMethod);
    }
}
