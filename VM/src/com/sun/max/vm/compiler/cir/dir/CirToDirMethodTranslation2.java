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
package com.sun.max.vm.compiler.cir.dir;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.Arrays;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;

/**
 * Experimental alternative translation from CIR to DIR.
 * Unfinished.
 * DIR->EIR lacks block edges from all indirect jump predecessors.
 *
 * @author Bernd Mathiske
 */
@Hypothetical
class CirToDirMethodTranslation2 {

    private Kind _resultKind;
    private final CirClosure _cirClosure;
    private final DirGenerator _dirGenerator;
    private final CirContinuationKindScout _cirContinuationKindScout;


    public CirToDirMethodTranslation2(Kind resultKind, CirClosure cirClosure, DirGenerator dirGenerator) {
        _resultKind = resultKind;
        _cirClosure = cirClosure;
        _dirGenerator = dirGenerator;
        _cirContinuationKindScout = new CirContinuationKindScout(resultKind, cirClosure);
    }

    private final Map<CirVariable, DirVariable> _cirVariableToDirVariable = new IdentityHashMap<CirVariable, DirVariable>();

    DirVariable cirToDirVariable(CirVariable cirVariable) {
        DirVariable dirVariable = _cirVariableToDirVariable.get(cirVariable);
        if (dirVariable == null) {
            dirVariable = new DirVariable(cirVariable.kind(), cirVariable.serial());
            _cirVariableToDirVariable.put(cirVariable, dirVariable);
        }
        return dirVariable;
    }

    private DirValue cirToDirValue(CirValue cirValue) {
        if (cirValue instanceof CirConstant) {
            return new DirConstant(cirValue.value());
        }
        if (cirValue instanceof CirVariable) {
            return cirToDirVariable((CirVariable) cirValue);
        }
        if (cirValue instanceof CirMethod) {
            final CirMethod cirCompiledMethod = (CirMethod) cirValue;
            return new DirMethodValue(cirCompiledMethod.classMethodActor());
        }
        if (cirValue instanceof CirClosure) {
            return makeDirJumpTargetBlock((CirClosure) cirValue);
        }
        if (cirValue instanceof CirValue.Undefined) {
            return DirValue.UNDEFINED;
        }
        throw ProgramError.unexpected("unexpected type of CirValue: " + cirValue);
    }

    private DirValue[] cirToDirValues(CirValue[] cirValues) {
        return com.sun.max.lang.Arrays.map(cirValues, DirValue.class, new MapFunction<CirValue, DirValue>() {
            public DirValue map(CirValue cirValue) {
                return cirToDirValue(cirValue);
            }
        });
    }

    private final MapFunction<CirValue, DirValue> _mapCirValueToDirValue = new MapFunction<CirValue, DirValue>() {
        public DirValue map(CirValue cirValue) {
            return cirToDirValue(cirValue);
        }
    };

    private void link(DirBlock from, DirBlock to) {
        from.successors().add(to);
        to.predecessors().add(from);
    }

    private final Map<CirJavaFrameDescriptor, DirJavaFrameDescriptor> _cirToDirJavaFrameDescriptor =
        new HashMap<CirJavaFrameDescriptor, DirJavaFrameDescriptor>();

    private DirJavaFrameDescriptor cirToDirJavaFrameDescriptor(CirJavaFrameDescriptor cirJavaFrameDescriptor) {
        if (cirJavaFrameDescriptor == null) {
            return null;
        }
        DirJavaFrameDescriptor dirJavaFrameDescriptor = _cirToDirJavaFrameDescriptor.get(cirJavaFrameDescriptor);
        if (dirJavaFrameDescriptor == null) {
            dirJavaFrameDescriptor = new DirJavaFrameDescriptor(cirToDirJavaFrameDescriptor(cirJavaFrameDescriptor.parent()),
                                                                       cirJavaFrameDescriptor.bytecodeLocation(),
                                                                       cirToDirValues(cirJavaFrameDescriptor.locals()),
                                                                       cirToDirValues(cirJavaFrameDescriptor.stackSlots()));
            _cirToDirJavaFrameDescriptor.put(cirJavaFrameDescriptor, dirJavaFrameDescriptor);
        }
        return dirJavaFrameDescriptor;
    }

    private MutableSequence<AppendableIndexedSequence<DirVariable>> _dirParameterLists = new ArraySequence<AppendableIndexedSequence<DirVariable>>(Kind.ALL.length);

    private DirVariable makeDirParameter(Kind kind, int index) {
        final Kind stackKind = kind.toStackKind();
        final int listIndex = stackKind.asEnum().ordinal();
        AppendableIndexedSequence<DirVariable> dirParameterList = _dirParameterLists.get(listIndex);
        if (dirParameterList == null) {
            dirParameterList = new ArrayListSequence<DirVariable>();
            _dirParameterLists.set(listIndex, dirParameterList);
        }
        if (index >= dirParameterList.length()) {
            for (int i = dirParameterList.length(); i <= index; i++) {
                dirParameterList.append(new DirVariable(stackKind, -(stackKind.asEnum().ordinal() * 100 + i)));
            }
        }
        return dirParameterList.get(index);
    }

    private DirBlock _returnBlock;

    private DirBlock makeReturnBlock(Kind kind) {
        if (_returnBlock == null) {
            _returnBlock = new DirBlock(IrBlock.Role.NORMAL);
            final DirValue result = (kind == Kind.VOID) ? null : makeDirParameter(kind, 0);
            _returnBlock.appendInstruction(new DirReturn(result));
        }
        return _returnBlock;
    }

    private DirBlock _throwBlock;

    private DirBlock makeThrowBlock() {
        if (_throwBlock == null) {
            _throwBlock = new DirBlock(IrBlock.Role.NORMAL);
            _throwBlock.appendInstruction(new DirThrow(makeDirParameter(Kind.REFERENCE, 0)));
        }
        return _throwBlock;
    }

    private enum TranslationPrologue {
        CLOSURE, BLOCK, METHOD
    }

    private class Translation {
        final IrBlock.Role _role;
        final CirClosure _closure;
        final DirBlock _dirBlock;
        final TranslationPrologue _prologue;

        Translation(IrBlock.Role role, CirClosure closure, TranslationPrologue prologue) {
            _role = role;
            _closure = closure;
            if (role == IrBlock.Role.EXCEPTION_DISPATCHER) {
                _dirBlock = new DirCatchBlock(cirToDirVariable(_closure.parameters()[0]));
            } else {
                _dirBlock = new DirBlock(role);
            }
            appendDirBlock(_dirBlock);
            _prologue = prologue;
        }

        Translation(Translation translation, CirClosure closure) {
            _role = translation._role;
            _closure = closure;
            _dirBlock = translation._dirBlock;
            _prologue = TranslationPrologue.CLOSURE;
        }

        public void appendInstruction(DirInstruction dirInstruction) {
            _dirBlock.appendInstruction(dirInstruction);
        }
    }

    private final VariableSequence<Translation> _translationList = new ArrayListSequence<Translation>();

    private Translation createTranslation(IrBlock.Role role, CirClosure closure) {
        final Translation translation = new Translation(role, closure, TranslationPrologue.BLOCK);
        _translationList.append(translation);
        return translation;
    }

    private Translation appendTranslation(Translation translation, CirClosure closure) {
        final Translation newTranslation = new Translation(translation, closure);
        _translationList.append(newTranslation);
        return newTranslation;
    }

    private DirBlock createDirBlock(CirClosure closure) {
        return createTranslation(IrBlock.Role.NORMAL, closure)._dirBlock;
    }

    private DirCatchBlock createDirCatchBlock(CirClosure closure) {
        return (DirCatchBlock) createTranslation(IrBlock.Role.EXCEPTION_DISPATCHER, closure)._dirBlock;
    }

    private DirBlock translateSwitchTarget(Translation translation, CirValue continuation) {
        DirBlock dirBlock;
        if (continuation instanceof CirVariable) {
            dirBlock = new DirBlock(IrBlock.Role.NORMAL);
            appendDirBlock(dirBlock);
            dirBlock.appendInstruction(new DirJump(cirToDirVariable((CirVariable) continuation)));
        } else {
            dirBlock = createDirBlock((CirClosure) continuation);
        }
        link(translation._dirBlock, dirBlock);
        return dirBlock;
    }

    private void generateDirSwitch(Translation translation, CirSwitch cirSwitch, CirValue[] cirArguments) {
        assert cirArguments.length >= 4;
        final CirValue[] cirMatches = Arrays.subArray(cirArguments, 1, cirSwitch.numberOfMatches());
        final DirValue[] dirMatches = Arrays.map(cirMatches, DirValue.class, _mapCirValueToDirValue);
        final DirBlock[] targetBlocks = new DirBlock[cirSwitch.numberOfMatches()];
        for (int i = 0; i < targetBlocks.length; i++) {
            targetBlocks[i] = translateSwitchTarget(translation, cirArguments[1 + cirSwitch.numberOfMatches() + i]);
        }
        final DirBlock defaultTargetBlock = translateSwitchTarget(translation, cirArguments[cirArguments.length - 1]);
        translation._dirBlock.appendInstruction(new DirSwitch(cirSwitch.comparisonKind(), cirSwitch.valueComparator(), cirToDirValue(cirArguments[0]), dirMatches, targetBlocks, defaultTargetBlock));
    }

    private void translateNormalContinuation(Translation translation, CirValue continuation) {
        if (continuation instanceof CirVariable) {
            translation.appendInstruction(new DirJump(cirToDirVariable((CirVariable) continuation)));
        } else {
            final DirBlock dirBlock = createDirBlock((CirClosure) continuation);
            translation.appendInstruction(new DirGoto(dirBlock));
            link(translation._dirBlock, dirBlock);
        }
    }

    private final GrowableMapping<DirVariable, DirCatchBlock> _dirVariableToCatchBlock = HashMapping.createIdentityMapping();

    private DirCatchBlock makeIndirectDirCatchBlock(CirVariable cirVariable) {
        final DirVariable dirVariable = cirToDirVariable(cirVariable);
        DirCatchBlock dirCatchBlock = _dirVariableToCatchBlock.get(dirVariable);
        if (dirCatchBlock == null) {
            dirCatchBlock = new DirCatchBlock(makeDirParameter(Kind.REFERENCE, 0));
            dirCatchBlock.appendInstruction(new DirJump(dirVariable));
            _dirVariableToCatchBlock.put(dirVariable, dirCatchBlock);
        }
        return dirCatchBlock;
    }

    private DirCatchBlock cirExceptionContinuationToDirCatchBlock(CirValue cirExceptionContinuation) {
        if (cirExceptionContinuation instanceof CirClosure) {
            return createDirCatchBlock((CirClosure) cirExceptionContinuation);
        }
        return makeIndirectDirCatchBlock((CirVariable) cirExceptionContinuation);
    }

    private void generateBuiltinCall(Translation translation, CirBuiltin cirBuiltin, CirValue[] cirArguments) {
        final DirValue[] dirArguments = Arrays.map(Arrays.subArray(cirArguments, 0, cirArguments.length - 2), DirValue.class, _mapCirValueToDirValue);
        final DirCatchBlock dirCatchBlock = cirExceptionContinuationToDirCatchBlock(cirArguments[cirArguments.length - 1]);

        final CirValue normalContinuation = cirArguments[cirArguments.length - 2];
        if (normalContinuation instanceof CirClosure) {
            final CirClosure cirClosure = (CirClosure) normalContinuation;
            if (cirClosure.parameters().length == 0) {
                translation.appendInstruction(new DirBuiltinCall(null, cirBuiltin.builtin(), dirArguments, dirCatchBlock));
            } else {
                translation.appendInstruction(new DirBuiltinCall(cirToDirVariable(cirClosure.parameters()[0]), cirBuiltin.builtin(), dirArguments, dirCatchBlock));
            }
            appendTranslation(translation, cirClosure);
        } else {
            translation.appendInstruction(new DirBuiltinCall(getResultVariable(cirBuiltin, normalContinuation), cirBuiltin.builtin(), dirArguments, dirCatchBlock));
            translateNormalContinuation(translation, normalContinuation);
        }
    }

    private void generateSafepoint(Translation translation, CirValue[] cirArguments, CirJavaFrameDescriptor cirJavaFrameDescriptor) {
        translation.appendInstruction(new DirSafepoint(cirToDirJavaFrameDescriptor(cirJavaFrameDescriptor)));
        final CirValue normalContinuation = cirArguments[0];
        if (normalContinuation instanceof CirClosure) {
            final CirClosure cirClosure = (CirClosure) normalContinuation;
            appendTranslation(translation, cirClosure);
        } else {
            translateNormalContinuation(translation, normalContinuation);
        }
    }

    private Kind getResultKind(CirValue cirProcedure, CirValue continuation) {
        if (cirProcedure instanceof CirMethod) {
            final CirMethod cirMethod = (CirMethod) cirProcedure;
            return cirMethod.resultKind();
        }
        if (continuation instanceof CirClosure) {
            return ((CirClosure) continuation).parameters()[0].kind();
        }
        if (continuation instanceof CirExceptionContinuationParameter) {
            return Kind.REFERENCE;
        }
        // We have an anonymous call without result kind information
        // and the continuation is a variable, which does not carry a parameter Kind.
        // The result Kind can now only be determined by search in a larger context:
        return _cirContinuationKindScout.getKind((CirNormalContinuationParameter) continuation);
    }

    private DirVariable getResultVariable(CirValue cirProcedure, CirValue normalContinuation) {
        final Kind kind = getResultKind(cirProcedure, normalContinuation);
        if (kind == Kind.VOID) {
            return null;
        }
        return makeDirParameter(getResultKind(cirProcedure, normalContinuation), 0);
    }

    private void generateMethodCall(Translation translation, CirValue cirProcedure, CirValue[] cirArguments, boolean isNative, CirJavaFrameDescriptor cirJavaFrameDescriptor) {
        final DirValue[] dirArguments = Arrays.map(Arrays.subArray(cirArguments, 0, cirArguments.length - 2), DirValue.class, _mapCirValueToDirValue);
        final DirCatchBlock dirCatchBlock = cirExceptionContinuationToDirCatchBlock(cirArguments[cirArguments.length - 1]);
        final CirValue normalContinuation = cirArguments[cirArguments.length - 2];

        if (normalContinuation instanceof CirClosure) {
            final CirClosure cirClosure = (CirClosure) normalContinuation;
            if (cirClosure.parameters().length == 0) {
                translation.appendInstruction(new DirMethodCall(null, cirToDirValue(cirProcedure), dirArguments, dirCatchBlock, isNative, cirToDirJavaFrameDescriptor(cirJavaFrameDescriptor)));
            } else {
                translation.appendInstruction(new DirMethodCall(cirToDirVariable(cirClosure.parameters()[0]), cirToDirValue(cirProcedure), dirArguments, dirCatchBlock, isNative, cirToDirJavaFrameDescriptor(cirJavaFrameDescriptor)));
            }
            appendTranslation(translation, cirClosure);
        } else {
            translation.appendInstruction(new DirMethodCall(getResultVariable(cirProcedure, normalContinuation), cirToDirValue(cirProcedure), dirArguments, dirCatchBlock, isNative, cirToDirJavaFrameDescriptor(cirJavaFrameDescriptor)));
            translateNormalContinuation(translation, normalContinuation);
        }
    }

    private GrowableMapping<CirClosure, DirBlock> _cirClosureToDirBlock = HashMapping.createIdentityMapping();

    private DirBlock makeDirBlock(CirClosure closure) {
        DirBlock dirBlock = _cirClosureToDirBlock.get(closure);
        if (dirBlock == null) {
            dirBlock = createDirBlock(closure);
            _cirClosureToDirBlock.put(closure, dirBlock);
        }
        return dirBlock;
    }

    private GrowableMapping<CirClosure, DirBlock> _cirClosureToDirJumpTargetBlock = HashMapping.createIdentityMapping();

    private DirBlock makeDirJumpTargetBlock(CirClosure cirClosure) {
        DirBlock dirJumpTargetBlock = _cirClosureToDirJumpTargetBlock.get(cirClosure);
        if (dirJumpTargetBlock == null) {
            dirJumpTargetBlock = new DirBlock(IrBlock.Role.NORMAL);
            appendDirBlock(dirJumpTargetBlock);
            for (int i = 0; i < cirClosure.parameters().length; i++) {
                final DirVariable dirParameter = cirToDirVariable(cirClosure.parameters()[i]);
                dirJumpTargetBlock.appendInstruction(new DirAssign(dirParameter, makeDirParameter(dirParameter.kind(), i)));
            }
            final DirBlock dirBlock = makeDirBlock(cirClosure);
            dirJumpTargetBlock.appendInstruction(new DirGoto(dirBlock));
            _cirClosureToDirJumpTargetBlock.put(cirClosure, dirJumpTargetBlock);
            link(dirJumpTargetBlock, dirBlock);
        }
        return dirJumpTargetBlock;
    }

    private void generateDirAssign(Translation translation, DirVariable dirVariable, DirValue dirValue) {
        if (dirValue != dirVariable) {
            translation.appendInstruction(new DirAssign(dirVariable, dirValue));
        }
    }

    private void generateDirAssign(Translation translation, CirVariable cirVariable, DirValue dirValue) {
        generateDirAssign(translation, cirToDirVariable(cirVariable), dirValue);
    }

    private void generateDirAssign(Translation translation, CirVariable cirVariable, CirValue cirValue) {
        generateDirAssign(translation, cirVariable, cirToDirValue(cirValue));
    }

    private void translateClosureCall(Translation translation, CirClosure cirClosure, CirValue[] cirArguments) {
        final int temporaryVariableInsertionIndex = translation._dirBlock.instructions().length();
        final CirVariable[] cirParameters = cirClosure.parameters();
        for (int i = 0; i < cirArguments.length; i++) {
            final CirVariable cirParameter = cirParameters[i];
            final CirValue cirArgument = cirArguments[i];
            if (cirArgument instanceof CirVariable && Arrays.contains(cirParameters, cirArgument)) {
                if (cirArgument != cirParameter) {
                    // Assignment ordering problem in recursive block calls: at least one variable is both parameter and argument.

                    final DirVariable dirArgument = cirToDirVariable((CirVariable) cirArgument);
                    final DirVariable dirTemporary = new DirVariable(cirArgument.kind(), -1);

                    // Prepend an assignment to a temp var to solve the ordering problem:
                    translation._dirBlock.instructions().insert(temporaryVariableInsertionIndex, new DirAssign(dirTemporary, dirArgument));

                    // Append an assignment from the temp var:
                    generateDirAssign(translation, cirParameter, dirTemporary);
                }
            } else {
                generateDirAssign(translation, cirParameter, cirArgument);
            }
        }
        translation.appendInstruction(new DirGoto(makeDirBlock(cirClosure)));
    }

    private final AppendableIndexedSequence<DirBlock> _dirBlocks = new ArrayListSequence<DirBlock>();

    private void translate(Translation translation) {
        final CirClosure cirClosure = translation._closure;
        final CirCall cirCall = cirClosure.body();
        final CirValue cirProcedure = cirCall.procedure();
        final CirValue[] cirArguments = cirCall.arguments();

        if (cirProcedure instanceof CirVariable) {
            if (cirProcedure instanceof CirNormalContinuationParameter) {
                final CirNormalContinuationParameter continuationParameter = (CirNormalContinuationParameter) cirProcedure;
                if (cirArguments.length > 0) {
                    assert cirArguments.length == 1;
                    final DirValue dirArgument = cirToDirValue(cirArguments[0]);
                    generateDirAssign(translation, makeDirParameter(_cirContinuationKindScout.getKind(continuationParameter), 0), dirArgument);
                }
                translation.appendInstruction(new DirJump(cirToDirVariable(continuationParameter)));
            } else if (cirProcedure instanceof CirExceptionContinuationParameter) {
                final CirExceptionContinuationParameter continuationParameter = (CirExceptionContinuationParameter) cirProcedure;
                assert cirArguments.length == 1;
                final DirValue dirArgument = cirToDirValue(cirArguments[0]);
                generateDirAssign(translation, makeDirParameter(Kind.REFERENCE, 0), dirArgument);
                translation.appendInstruction(new DirJump(cirToDirVariable(continuationParameter)));
            } else {
                generateMethodCall(translation, cirProcedure, cirArguments, cirCall.isNative(), cirCall.javaFrameDescriptor());
            }
        } else if (cirProcedure instanceof CirSwitch) {
            generateDirSwitch(translation, (CirSwitch) cirProcedure, cirArguments);
        } else if (cirProcedure instanceof CirBuiltin) {
            final CirBuiltin cirBuiltin = (CirBuiltin) cirProcedure;
            if (cirBuiltin.builtin() == SafepointBuiltin.SoftSafepoint.BUILTIN || cirBuiltin.builtin() == SafepointBuiltin.HardSafepoint.BUILTIN) {
                generateSafepoint(translation, cirArguments, cirCall.javaFrameDescriptor());
            } else {
                generateBuiltinCall(translation, cirBuiltin, cirArguments);
            }
        } else if (cirProcedure instanceof CirMethod || cirProcedure instanceof CirConstant) {
            generateMethodCall(translation, cirProcedure, cirArguments, cirCall.isNative(), cirCall.javaFrameDescriptor());
        } else if (cirProcedure instanceof CirBlock) {
            final CirBlock cirBlock = (CirBlock) cirProcedure;
            translateClosureCall(translation, cirBlock.closure(), cirArguments);
        } else {
            translateClosureCall(translation, (CirClosure) cirProcedure, cirArguments);
        }
    }

    private int _serial = 0;

    private void appendDirBlock(DirBlock dirBlock) {
        if (dirBlock != null) {
            dirBlock.setSerial(_serial);
            _serial++;
            _dirBlocks.append(dirBlock);
        }
    }

    /**
     * @return DIR blocks generated from the CIR closure of the method
     */
    IndexedSequence<DirBlock> translateMethod() {
        Translation translation = new Translation(IrBlock.Role.NORMAL, _cirClosure, TranslationPrologue.METHOD);

        final CirVariable normalContinuationParameter = _cirClosure.parameters()[_cirClosure.parameters().length - 2];
        generateDirAssign(translation, normalContinuationParameter, makeReturnBlock(_resultKind));

        final CirVariable exceptionContinuationParameter = _cirClosure.parameters()[_cirClosure.parameters().length - 1];
        generateDirAssign(translation, exceptionContinuationParameter, makeThrowBlock());

        while (true) {
            translate(translation);
            if (_translationList.isEmpty()) {
                appendDirBlock(_returnBlock);
                appendDirBlock(_throwBlock);
                for (DirBlock catchBlock : _dirVariableToCatchBlock.values()) {
                    appendDirBlock(catchBlock);
                }
                return _dirBlocks;
            }
            translation = _translationList.removeFirst();
        }
    }
}
