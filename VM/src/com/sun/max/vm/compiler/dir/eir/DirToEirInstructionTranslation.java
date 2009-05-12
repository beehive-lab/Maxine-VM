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
package com.sun.max.vm.compiler.dir.eir;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.dir.transform.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Platform-independent aspects of translating any kind of DIR instruction to EIR.
 *
 * @author Bernd Mathiske
 */
public abstract class DirToEirInstructionTranslation implements DirVisitor {

    private final DirToEirMethodTranslation _methodTranslation;

    public final DirToEirMethodTranslation methodTranslation() {
        return _methodTranslation;
    }

    private EirBlock _eirBlock;

    public final EirBlock eirBlock() {
        return _eirBlock;
    }

    public final void setEirBlock(EirBlock eirBlock) {
        _eirBlock = eirBlock;

    }

    public final EirABI abi() {
        return _methodTranslation.eirMethod().abi();
    }

    public final void addInstruction(EirInstruction instruction) {
        _eirBlock.appendInstruction(instruction);
    }

    public final EirBlock createEirBlock(IrBlock.Role role) {
        return _methodTranslation.createEirBlock(role);
    }

    public final void setBlock(EirBlock eirBlock) {
        _eirBlock = eirBlock;
    }

    public final EirBlock dirToEirBlock(DirBlock dirBlock) {
        return _methodTranslation.dirToEirBlock(dirBlock);
    }

    public DirToEirInstructionTranslation(DirToEirMethodTranslation methodTranslation, EirBlock eirBlock) {
        _methodTranslation = methodTranslation;
        _eirBlock = eirBlock;
    }

    public final void addTry(DirCall dirCall) {
        final EirBlock eirCatchBlock = _methodTranslation.dirToEirBlock(dirCall.catchBlock());
        if (eirCatchBlock != null) {
            eirCatchBlock.addPredecessor(_eirBlock);
        }
        addInstruction(new EirTry(eirBlock(), eirCatchBlock));
    }

    public final void addJump(EirBlock toBlock) {
        _methodTranslation.addJump(_eirBlock, toBlock);
    }

    public final void splitBlock() {
        final EirBlock eirBlock = createEirBlock(IrBlock.Role.NORMAL);
        addJump(eirBlock);
        setEirBlock(eirBlock);
    }

    protected abstract DirToEirBuiltinTranslation createBuiltinTranslation(DirToEirInstructionTranslation instructionTranslation, DirJavaFrameDescriptor javaFrameDescriptor);

    public final void visitBuiltinCall(DirBuiltinCall dirBuiltinCall) {
        addTry(dirBuiltinCall);
        final DirToEirBuiltinTranslation builtinCallTranslation = createBuiltinTranslation(this, dirBuiltinCall.javaFrameDescriptor());
        dirBuiltinCall.builtin().acceptVisitor(builtinCallTranslation, dirBuiltinCall.result(), dirBuiltinCall.arguments());
        if (dirBuiltinCall.catchBlock() != null) {
            splitBlock();
        }
    }

    public final EirVariable createEirVariable(Kind kind) {
        return _methodTranslation.createEirVariable(kind);
    }

    public EirConstant createEirConstant(Value value) {
        return _methodTranslation.createEirConstant(value);
    }

    /**
     * Puts zero into a given variable.
     */
    public abstract void assignZero(Kind kind, EirValue variable);

    public EirValue dirToEirValue(DirValue dirValue) {
        if (dirValue != null && dirValue.isZeroConstant() && (dirValue.kind().width() == WordWidth.BITS_64 || dirValue.kind() == Kind.FLOAT)) {
            final EirVariable variable = createEirVariable(dirValue.kind());
            assignZero(dirValue.kind(), variable);
            return variable;
        }
        return _methodTranslation.dirToEirValue(dirValue);
    }

    public final EirConstant dirToEirConstant(DirConstant dirConstant) {
        return _methodTranslation.dirToEirConstant(dirConstant);
    }

    public final EirInstruction assign(Kind kind, EirValue destination, EirValue source) {
        final EirInstruction assignment = _methodTranslation.createAssignment(_eirBlock, kind, destination, source);
        _eirBlock.appendInstruction(assignment);
        return assignment;
    }

    public void visitReturn(DirReturn dirReturn) {
        final DirValue dirValue = dirReturn.returnValue();

        if (_methodTranslation.requiresEpilogue()) {
            addJump(_methodTranslation.makeEpilogue());
            if (dirValue.kind() != Kind.VOID) {
                _methodTranslation.addResultValue(_methodTranslation.dirToEirValue(dirValue));
            }
        } else {
            final EirEpilogue eirEpilogue = _methodTranslation.createEpilogueAndReturn(_eirBlock);
            if (dirValue.kind() != Kind.VOID) {
                final EirValue eirResultValue = _methodTranslation.dirToEirValue(dirValue);
                eirEpilogue.addResultValue(eirResultValue);
            }
        }
    }

    private void generateCall(DirJavaFrameDescriptor dirJavaFrameDescriptor, EirABI abi, Kind resultKind, EirValue result, EirValue function, Kind[] argumentKinds, EirValue... arguments) {
        final EirLocation[] argumentLocations = abi.getParameterLocations(EirStackSlot.Purpose.LOCAL, argumentKinds);
        final EirLocation resultLocation = (result == null) ? null : abi.getResultLocation(resultKind);
        final boolean isTemplate = _methodTranslation.isTemplate();
        final EirCall instruction = isTemplate ?
                        _methodTranslation.createRuntimeCall(_eirBlock, abi, result, resultLocation, function, arguments, argumentLocations) :
                        _methodTranslation.createCall(_eirBlock, abi, result, resultLocation, function, arguments, argumentLocations);
        addInstruction(instruction);
        if (!isTemplate) {
            instruction.setEirJavaFrameDescriptor(_methodTranslation.dirToEirJavaFrameDescriptor(dirJavaFrameDescriptor, instruction));
        }
    }

    private EirValue _raiseThrowableEirValue = null;

    private EirValue makeRaiseThrowableEirValue() {
        if (_raiseThrowableEirValue == null) {
            _raiseThrowableEirValue = _methodTranslation.makeEirMethodValue(NonFoldableSnippet.RaiseThrowable.SNIPPET.classMethodActor());
        }
        return _raiseThrowableEirValue;
    }

    public final void visitThrow(DirThrow dirThrow) {
        addInstruction(new EirTry(eirBlock(), null));
        final MethodActor classMethodActor = NonFoldableSnippet.RaiseThrowable.SNIPPET.classMethodActor();
        final EirValue eirThrowable = _methodTranslation.dirToEirValue(dirThrow.throwable());
        generateCall(null, _methodTranslation.eirGenerator().eirABIsScheme().javaABI(), null, null,
                     makeRaiseThrowableEirValue(), classMethodActor.getParameterKinds(), eirThrowable);
        // No need for a JavaFrameDescriptor here.
        // Throw.raise() disables safepoints until the exception has been delivered to its dispatcher.
    }

    public final void visitMethodCall(DirMethodCall dirMethodCall) {
        addTry(dirMethodCall);

        final DirValue dirResult = dirMethodCall.result();
        final Kind resultKind = (dirResult == null) ? null : dirResult.kind();
        final EirValue eirResult = dirToEirValue(dirResult);
        final EirValue methodEirValue = dirToEirValue(dirMethodCall.method());

        final int numberOfArguments = dirMethodCall.arguments().length;
        final Kind[] argumentKinds = new Kind[numberOfArguments];
        final EirValue[] eirArguments = new EirValue[numberOfArguments];
        for (int i = 0; i < numberOfArguments; i++) {
            final DirValue dirArgument = dirMethodCall.arguments()[i];
            argumentKinds[i] = dirArgument.kind();
            eirArguments[i] = dirToEirValue(dirArgument);
        }

        EirABI abi;
        if (dirMethodCall.method() instanceof DirMethodValue) {
            final DirMethodValue dirMethodValue = (DirMethodValue) dirMethodCall.method();
            abi = _methodTranslation.eirGenerator().eirABIsScheme().getABIFor(dirMethodValue.classMethodActor());
        } else if (dirMethodCall.isNative()) {
            abi = _methodTranslation.eirGenerator().eirABIsScheme().nativeABI();
        } else {
            abi = _methodTranslation.eirGenerator().eirABIsScheme().javaABI();
        }

        generateCall(dirMethodCall.javaFrameDescriptor(), abi, resultKind, eirResult, methodEirValue, argumentKinds, eirArguments);

        if (dirMethodCall.catchBlock() != null && methodEirValue != makeRaiseThrowableEirValue()) {
            splitBlock();
        }
    }

    public final void visitGoto(DirGoto dirGoto) {
        final EirBlock eirBlock = _methodTranslation.dirToEirBlock(dirGoto.targetBlock());
        addJump(eirBlock);
    }

    public final void visitAssign(DirAssign dirAssign) {
        final EirValue destination = _methodTranslation.dirToEirValue(dirAssign.destination());
        final EirValue source = _methodTranslation.dirToEirValue(dirAssign.source());
        assign(destination.kind(), destination, source);
    }

    public final void visitSafepoint(DirSafepoint dirSafepoint) {
        final EirSafepoint instruction = methodTranslation().createSafepoint(eirBlock());
        addInstruction(instruction);
        instruction.setEirJavaFrameDescriptor(methodTranslation().dirToEirJavaFrameDescriptor(dirSafepoint.javaFrameDescriptor(), instruction));
    }

    public final void visitGuardpoint(DirGuardpoint dirGuardpoint) {
        final EirGuardpoint instruction = methodTranslation().createGuardpoint(eirBlock());
        addInstruction(instruction);
        instruction.setEirJavaFrameDescriptor(methodTranslation().dirToEirJavaFrameDescriptor(dirGuardpoint.javaFrameDescriptor(), instruction));
    }

}
