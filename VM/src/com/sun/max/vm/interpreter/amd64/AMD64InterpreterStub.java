/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.interpreter.amd64;

import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.interpreter.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 *
 * @author Paul Caprioli
 */
public class AMD64InterpreterStub extends InterpreterStub {

    public AMD64InterpreterStub(ClassMethodActor classMethodActor, InterpreterStubCompiler compilerScheme, TargetABI abi) {
        super(classMethodActor, compilerScheme, abi);
//        final EirTargetEmitter<?> emitter = createEirTargetEmitter(eirMethod);
//        emitter.emitFrameAdapterPrologue();
//        eirMethod.emit(emitter);
//        emitter.emitFrameAdapterEpilogue();

        Label classMethodActorLiteral = new Label();
        Label callToInterpreter = new Label();

        Class<TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>> type = null;
        TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> amd64Abi = StaticLoophole.cast(type, abi);

        AMD64IndirectRegister64 latchRegister = AMD64Safepoint.LATCH_REGISTER.indirect();
        AMD64GeneralRegister64 scratchRegister = amd64Abi.scratchRegister();

        final AMD64Assembler asm = new AMD64Assembler(0L);

        // allocate the frame for this method
        AMD64GeneralRegister64 stackPointer = AMD64GeneralRegister64.RSP;
        int frameSize = computeFrameSize(classMethodActor);
        asm.subq(stackPointer, frameSize);

        // save all the general purpose registers
        int offset = 0;

        // bci
        offset += Word.size();

        // method
        asm.rip_mov(scratchRegister, classMethodActorLiteral);
        asm.mov(offset, stackPointer.indirect(), scratchRegister);
        offset += Word.size();

        // reference locals
        IndexedSequence<AMD64GeneralRegister64> gprParams = amd64Abi.integerIncomingParameterRegisters();
        IndexedSequence<AMD64XMMRegister> fprParams = amd64Abi.floatingPointParameterRegisters();
        SignatureDescriptor signature = classMethodActor.descriptor();
        adaptParameters(asm, signature, gprParams, fprParams);

        if (!classMethodActor.isStatic()) {
//            asm.mov(offset, stackPointer.indirect(), );
            offset += Word.size();
        }

        asm.rip_mov(scratchRegister, classMethodActorLiteral);
        asm.mov(VmThreadLocal.INTERPRETED_METHOD.offset, latchRegister, scratchRegister);
        int placeholderOffset = 0;
        asm.bindLabel(callToInterpreter);
        asm.jmp(placeholderOffset);

        final Object[] referenceLiterals = {classMethodActor};
        final int placeholderCodeLength = 0;
        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(0, referenceLiterals.length, placeholderCodeLength);

        asm.fixLabel(classMethodActorLiteral, targetBundleLayout.firstElementPointer(Address.zero(), ArrayField.referenceLiterals).toLong());
        final byte[] code;
        try {
            asm.setStartAddress(targetBundleLayout.firstElementPointer(Address.zero(), ArrayField.code).toLong());
            code = asm.toByteArray();
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected("assembling failed", assemblyException);
        }

        targetBundleLayout.update(ArrayField.code, code.length);
        Code.allocate(targetBundleLayout, this);

        setABI(abi);
        setStopPositions(new int[0], new Object[0], 0, 0);
        setFrameSize(0);
        setData(null, referenceLiterals, code);

        try {
            AMD64TargetMethod.patchJump32Site(this, callToInterpreter.position(), Interpreter.INTERPRETER_INTERPRET.address());
        } catch (AssemblyException e) {
            FatalError.unexpected("Error patching call-to-interpreter", e);
        }
    }

    public void adaptParameters(AMD64Assembler asm, SignatureDescriptor signature, IndexedSequence<AMD64GeneralRegister64> gprParams, IndexedSequence<AMD64XMMRegister> fprParams)  {
//        int iGeneral = 0;
//        int iXMM = 0;

        for (int i = 0; i < signature.numberOfParameters(); i++) {
            TypeDescriptor param = signature.parameterDescriptorAt(i);
            switch (param.toKind().asEnum) {
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHAR:
                case INT:
                case LONG:
                case WORD: {
                    break;
                }
                case REFERENCE: {
//                    if (iGeneral < generalParameterRegisters.length()) {
//                        asm.mov(offset, stackPointer.indirect(), param);
//                        result[i] = generalParameterRegisters.get(iGeneral);
//                        iGeneral++;
//                    }
                    break;
                }
                case FLOAT:
                case DOUBLE: {
//                    if (iXMM < xmmParameterRegisters.length()) {
//                        result[i] = xmmParameterRegisters.get(iXMM);
//                        iXMM++;
//                    }
                    break;
                }
                default: {
                    ProgramError.unknownCase();
                }
            }
        }
    }

    private static int computeFrameSize(ClassMethodActor classMethodActor) {
        int bciSlot = Word.size();
        int methodSlot = Word.size();
        int primitiveStack = classMethodActor.codeAttribute().maxStack() * Word.size();
        int referenceStack = primitiveStack;
        int primitiveLocals = classMethodActor.codeAttribute().maxLocals() * Word.size();
        int referenceLocals = primitiveLocals;
        return bciSlot + methodSlot + primitiveStack + referenceStack + primitiveLocals + referenceLocals;
    }

}

