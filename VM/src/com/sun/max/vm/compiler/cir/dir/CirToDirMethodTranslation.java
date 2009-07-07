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

import static com.sun.max.collect.SequenceBag.MapType.*;

import java.io.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.Arrays;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.dir.DirTraceObserver.*;
import com.sun.max.vm.compiler.dir.transform.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.type.*;

/**
 * Translation of from CIR to DIR.
 *
 * The current implementation does not build and keep track of any variable environments.
 * This requires some structural constraints on CIR:
 * - there is always at most one normal continuation parameter and at most one exception continuation parameter in scope.
 *
 * TODO: implement a variant that represents continuations as branch arguments in DIR.
 * This should drastically reduce duplication in some larger methods.
 * Modern processors are sufficiently good at *indirect* branch prediction to keep runtime overhead in check.
 *
 * @author Bernd Mathiske
 * @author Hiroshi Yamauchi
 */
class CirToDirMethodTranslation {

    private Kind resultKind;
    private final CirClosure cirClosure;
    private final DirGenerator dirGenerator;
    private final DirMethod dirMethod;

    public CirToDirMethodTranslation(Kind resultKind, CirClosure cirClosure, DirMethod dirMethod, DirGenerator dirGenerator) {
        this.resultKind = resultKind;
        this.cirClosure = cirClosure;
        this.dirGenerator = dirGenerator;
        this.dirMethod = dirMethod;
    }

    /**
     * All generated blocks.
     */
    private final GrowableDeterministicSet<DirBlock> dirExceptionDispatchers = new LinkedIdentityHashSet<DirBlock>();

    final CirVariableFactory variableFactory = new CirVariableFactory();

    /**
     *  We regard every combination of a CIR closure and a continuation as a different
     *  code "path" that we have to translate separately.
     *
     *  Multiple such combinations exist for the same CIR closure
     *  when it is called at multiple call sites
     *  with different continuation arguments.

     *  One way to view this scenario is that we are specializing the closure for each actual continuation argument
     *  and then inlining the respective continuation into each copy of the block.
     *  Yes, this bloats direct style code compared to CPS,
     *  but we need to get to direct style somehow for the backend.
     */
    private class Translation {
        final IrBlock.Role role;
        final CirClosure closure;
        final Translation cc;
        final Translation ce;
        final DirBlock dirBlock;

        Translation(IrBlock.Role role, CirClosure closure, Translation cc, Translation ce) {
            this.role = role;
            this.closure = closure;
            this.cc = cc;
            this.ce = ce;
            if (role == IrBlock.Role.EXCEPTION_DISPATCHER) {
                this.dirBlock = new DirCatchBlock(cirToDirVariable(closure.parameters()[0]));
            } else {
                this.dirBlock = new DirBlock(role);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Translation) {
                final Translation translation = (Translation) other;
                return closure == translation.closure && cc == translation.cc && ce == translation.ce;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return closure.hashCode();
        }

        @Override
        public String toString() {
            final CharArrayWriter charArrayWriter = new CharArrayWriter();
            final IndentWriter writer = new IndentWriter(charArrayWriter);
            dirBlock.printTo(writer);
            return charArrayWriter.toString();
        }
    }

    private final Map<Translation, Translation> translations = new HashMap<Translation, Translation>();

    private Translation makeTranslation(IrBlock.Role role, CirClosure closure, Translation cc, Translation ce) {
        final Translation newTranslation = new Translation(role, closure, cc, ce);
        final Translation oldTranslation = translations.get(newTranslation);
        if (oldTranslation != null) {
            assert oldTranslation.role == role : "old = " + oldTranslation.role + " new = " + role;
            return oldTranslation;
        }
        translations.put(newTranslation, newTranslation);
        translationList.append(newTranslation);
        return newTranslation;
    }

    private CirClosure valueToClosure(CirValue closureValue) {
        if (closureValue instanceof CirBlock) {
            final CirBlock cirBlock = (CirBlock) closureValue;
            return cirBlock.closure();
        }
        return (CirClosure) closureValue;
    }

    private Translation makeNormalTranslation(CirValue continuation, Translation translation) {
        return makeTranslation(IrBlock.Role.NORMAL, valueToClosure(continuation), translation.cc, translation.ce);
    }

    private Translation makeExceptionDispatcherTranslation(CirValue continuation, Translation translation) {
        final Translation exceptionDispatcherTranslation = makeTranslation(IrBlock.Role.EXCEPTION_DISPATCHER, valueToClosure(continuation), translation.cc, translation.ce);
        dirExceptionDispatchers.add(exceptionDispatcherTranslation.dirBlock);
        return exceptionDispatcherTranslation;
    }

    private final Map<CirVariable, DirVariable> cirVariableToDirVariable = new IdentityHashMap<CirVariable, DirVariable>();

    DirVariable cirToDirVariable(CirVariable cirVariable) {
        DirVariable dirVariable = cirVariableToDirVariable.get(cirVariable);
        if (dirVariable == null) {
            dirVariable = new DirVariable(cirVariable.kind(), cirVariable.serial());
            cirVariableToDirVariable.put(cirVariable, dirVariable);
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
        if (cirValue == CirValue.UNDEFINED) {
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

    private final MapFunction<CirValue, DirValue> mapCirValueToDirValue = new MapFunction<CirValue, DirValue>() {
        public DirValue map(CirValue cirValue) {
            return cirToDirValue(cirValue);
        }
    };

    private void generateDirReturn(Translation translation, CirValue[] cirArguments) {
        if (cirArguments.length == 0) {
            translation.dirBlock.instructions().append(new DirReturn(DirConstant.VOID));
        } else {
            assert cirArguments.length == 1;
            translation.dirBlock.instructions().append(new DirReturn(cirToDirValue(cirArguments[0])));
        }
    }

    private void generateDirThrow(Translation translation, CirValue[] cirArguments) {
        assert cirArguments.length == 1;
        translation.dirBlock.instructions().append(new DirThrow(cirToDirValue(cirArguments[0])));
    }

    private void generateDirAssign(Translation translation, CirVariable cirVariable, DirValue dirValue) {
        final DirVariable dirVariable = cirToDirVariable(cirVariable);
        if (dirValue != dirVariable) {
            translation.dirBlock.instructions().append(new DirAssign(dirVariable, dirValue));
        }
    }

    private void generateDirAssign(Translation translation, CirVariable cirVariable, CirValue cirValue) {
        generateDirAssign(translation, cirVariable, cirToDirValue(cirValue));
    }

    private void link(DirBlock from, DirBlock to) {
        from.successors().add(to);
        to.predecessors().add(from);
    }

    private void generateDirGoto(Translation fromTranslation, Translation toTranslation) {
        fromTranslation.dirBlock.instructions().append(new DirGoto(toTranslation.dirBlock));
        link(fromTranslation.dirBlock, toTranslation.dirBlock);
    }

    private void generateDirGoto(Translation fromTranslation, Translation toTranslation, DirValue dirArgument) {
        final CirVariable[] parameters = toTranslation.closure.parameters();
        // TODO: temporary fix: I do know not how we come here with parameters of length 1 when cirArguments is empty.
        // I assume here that the combination is legal, but that the test is incomplete.
        if (parameters.length == 1 && dirArgument != null) {
            generateDirAssign(fromTranslation, parameters[0], dirArgument);
        } else {
            assert parameters.length == 0;
        }
        generateDirGoto(fromTranslation, toTranslation);
    }

    private void generateDirGoto(Translation fromTranslation, Translation toTranslation, CirValue[] cirArguments) {
        final DirValue dirArgument = cirArguments.length > 0 ? cirToDirValue(cirArguments[0]) : null;
        generateDirGoto(fromTranslation, toTranslation, dirArgument);
    }

    private DirBlock translateSwitchTarget(Translation translation, CirValue continuation) {
        if (!CirVariable.class.isInstance(continuation)) {
            final Translation targetTranslation = makeNormalTranslation(continuation, translation);
            link(translation.dirBlock, targetTranslation.dirBlock);
            return targetTranslation.dirBlock;
        }
        if (translation.cc != null) {
            link(translation.dirBlock, translation.cc.dirBlock);
            return translation.cc.dirBlock;
        }
        final DirBlock returnBlk = getReturnBlock();
        link(translation.dirBlock, returnBlk);
        return returnBlk;
    }

    private void generateDirSwitch(Translation translation, CirSwitch cirSwitch, CirValue[] cirArguments) {
        final CirValue[] cirMatches = Arrays.subArray(cirArguments, 1, cirSwitch.numberOfMatches());
        final DirValue[] dirMatches = Arrays.map(cirMatches, DirValue.class, mapCirValueToDirValue);
        final DirBlock[] targetBlocks = new DirBlock[cirSwitch.numberOfMatches()];
        for (int i = 0; i < targetBlocks.length; i++) {
            targetBlocks[i] = translateSwitchTarget(translation, cirArguments[1 + cirSwitch.numberOfMatches() + i]);
        }
        final DirBlock defaultTargetBlock = translateSwitchTarget(translation, cirArguments[cirArguments.length - 1]);
        translation.dirBlock.instructions().append(new DirSwitch(cirSwitch.comparisonKind(), cirSwitch.valueComparator(), cirToDirValue(cirArguments[0]), dirMatches, targetBlocks, defaultTargetBlock));
    }

    private final Map<CirJavaFrameDescriptor, DirJavaFrameDescriptor> cirToDirJavaFrameDescriptor =
        new HashMap<CirJavaFrameDescriptor, DirJavaFrameDescriptor>();

    private DirJavaFrameDescriptor cirToDirJavaFrameDescriptor(CirJavaFrameDescriptor cirJavaFrameDescriptor) {
        if (cirJavaFrameDescriptor == null) {
            return null;
        }
        DirJavaFrameDescriptor dirJavaFrameDescriptor = cirToDirJavaFrameDescriptor.get(cirJavaFrameDescriptor);
        if (dirJavaFrameDescriptor == null) {
            dirJavaFrameDescriptor = new DirJavaFrameDescriptor(cirToDirJavaFrameDescriptor(cirJavaFrameDescriptor.parent()),
                                                                       cirJavaFrameDescriptor.classMethodActor(),
                                                                       cirJavaFrameDescriptor.bytecodePosition(),
                                                                       cirToDirValues(cirJavaFrameDescriptor.locals),
                                                                       cirToDirValues(cirJavaFrameDescriptor.stackSlots));
            cirToDirJavaFrameDescriptor.put(cirJavaFrameDescriptor, dirJavaFrameDescriptor);
        }
        return dirJavaFrameDescriptor;
    }

    private DirVariable exceptionVariable;

    private DirVariable getExceptionVariable() {
        if (exceptionVariable == null) {
            exceptionVariable = new DirVariable(Kind.REFERENCE, -1);
        }
        return exceptionVariable;
    }

    private DirVariable returnVariable;

    private DirVariable getReturnVariable() {
        if (returnVariable == null && resultKind != Kind.VOID) {
            returnVariable = new DirVariable(resultKind, -1);
        }
        return returnVariable;
    }

    private DirVariable getResultVariable(Translation cc) {
        if (cc != null) {
            final CirVariable[] parameters = cc.closure.parameters();
            if (parameters.length == 1) {
                return cirToDirVariable(parameters[0]);
            }
            assert parameters.length == 0;
            return null;
        }
        return getReturnVariable();
    }

    private DirBlock returnBlock;

    private DirBlock getReturnBlock() {
        if (returnBlock == null) {
            returnBlock = new DirBlock(IrBlock.Role.NORMAL);
            returnBlock.instructions().append(new DirReturn(getReturnVariable()));
        }
        return returnBlock;
    }

    /**
     * Facility for generating the DIR code when translating a safepoint or a call to a builtin or method.
     */
    private abstract class CallGenerator<CirProcedure_Type extends CirValue> {

        /**
         * Generates the DIR instruction specific to the type of CIR call being processed by this call translation.
         *
         * @param result the variable to which the result of the call will be assigned
         * @param cirProcedure the procedure applied by the call
         * @param arguments the arguments of the call
         * @param catchBlock the DIR block where execution continues in case of an exception thrown during the call
         * @param isNativeCall specifies if this is a call to native code (i.e. one that crosses an ABI boundary)
         * @param javaFrameDescriptor the frame descriptor for the call (may be null)
         * @return the DIR instruction implementing the call
         */
        protected abstract DirInstruction createCallInstruction(DirVariable result, CirProcedure_Type cirProcedure, DirValue[] arguments, DirCatchBlock catchBlock, boolean isNativeCall, DirJavaFrameDescriptor javaFrameDescriptor);

        /**
         * Translates a CIR call.
         *
         * @param translation the translation to be processed
         * @param cirProcedure the procedure applied by the body of the {@linkplain Translation#closure closure} being translated
         * @param cirArguments the arguments of the procedure application
         * @param isNativeCall specifies if this is a call to native code (i.e. one that crosses an ABI boundary)
         * @param javaFrameDescriptor the frame descriptor for the call (may be null)
         */
        final void generateCall(Translation translation, CirProcedure_Type cirProcedure, CirValue[] cirArguments, boolean isNativeCall, DirJavaFrameDescriptor javaFrameDescriptor) {
            final CirValue normalContinuation = cirArguments[cirArguments.length - 2];
            Translation cc;
            DirVariable result;
            if (normalContinuation instanceof CirNormalContinuationParameter) {
                cc  = translation.cc;
                result = getResultVariable(cc);
            } else if (normalContinuation instanceof CirExceptionContinuationParameter) {
                cc = translation.ce;
                result = getExceptionVariable();
            } else if (normalContinuation == CirValue.UNDEFINED) {
                assert cirProcedure == CirSnippet.get(NonFoldableSnippet.RaiseThrowable.SNIPPET);
                cc = null;
                result = null;
            } else {
                cc = makeNormalTranslation(normalContinuation, translation);
                result = getResultVariable(cc);
            }

            final CirValue exceptionContinuation = cirArguments[cirArguments.length - 1];

            final DirCatchBlock catchBlock;
            if (exceptionContinuation instanceof CirExceptionContinuationParameter) {
                if (translation.ce == null) {
                    catchBlock = null;
                } else {
                    catchBlock = (DirCatchBlock) translation.ce.dirBlock;
                }
            } else if (exceptionContinuation instanceof CirNormalContinuationParameter) {
                if (translation.cc == null) {
                    catchBlock = null;
                } else {
                    catchBlock = (DirCatchBlock) translation.cc.dirBlock;
                }
            } else if (exceptionContinuation == CirValue.UNDEFINED) {
                catchBlock = null;
            } else {
                assert exceptionContinuation != null;
                catchBlock = (DirCatchBlock) makeExceptionDispatcherTranslation(exceptionContinuation, translation).dirBlock;
            }

            final CirValue[] a = Arrays.subArray(cirArguments, 0, cirArguments.length - 2); // clip the exception arguments
            final DirValue[] dirArguments = Arrays.map(a, DirValue.class, mapCirValueToDirValue);

            translation.dirBlock.instructions().append(createCallInstruction(result, cirProcedure, dirArguments, catchBlock, isNativeCall, javaFrameDescriptor));

            if (cc != null) {
                generateDirGoto(translation, cc, result);
            } else if (normalContinuation instanceof CirNormalContinuationParameter) {
                translation.dirBlock.instructions().append(new DirReturn(result));
            } else {
                if (normalContinuation == CirValue.UNDEFINED) {
                    assert cirProcedure == CirSnippet.get(NonFoldableSnippet.RaiseThrowable.SNIPPET);
                } else {
                    assert normalContinuation instanceof CirExceptionContinuationParameter;
                    translation.dirBlock.instructions().append(new DirThrow(result));
                }
            }
        }
    }

    private final CallGenerator<CirValue> methodCallGenerator = new CallGenerator<CirValue>() {
        @Override
        protected DirInstruction createCallInstruction(DirVariable result, CirValue cirMethod, DirValue[] arguments, DirCatchBlock catchBlock, boolean isNativeCall, DirJavaFrameDescriptor javaFrameDescriptor) {
            return new DirMethodCall(result, cirToDirValue(cirMethod), arguments, catchBlock, isNativeCall, javaFrameDescriptor);
        }
    };

    private final CallGenerator<CirBuiltin> builtinCallGenerator = new CallGenerator<CirBuiltin>() {
        @Override
        protected DirInstruction createCallInstruction(DirVariable result, CirBuiltin cirBuiltin, DirValue[] arguments, DirCatchBlock catchBlock, boolean isNativeCall, DirJavaFrameDescriptor javaFrameDescriptor) {
            return new DirBuiltinCall(result, cirBuiltin.builtin(), arguments, catchBlock, javaFrameDescriptor);
        }
    };

    private final CallGenerator<CirBuiltin> safepointGenerator = new CallGenerator<CirBuiltin>() {
        @Override
        protected DirInstruction createCallInstruction(DirVariable result, CirBuiltin cirBuiltin, DirValue[] arguments, DirCatchBlock catchBlock, boolean isNativeCall, DirJavaFrameDescriptor javaFrameDescriptor) {
            assert javaFrameDescriptor != null;
            return new DirSafepoint(javaFrameDescriptor);
        }
    };

    private void translateClosureCall(Translation translation, CirClosure closure, CirValue[] cirArguments) {
        final int temporaryVariableInsertionIndex = translation.dirBlock.instructions().length();
        final CirVariable[] cirParameters = closure.parameters();

        // Have to search for continuation variables among the parameters
        // because they are not necessarily present
        // and if they are present, then not necessarily at the end
        Translation cc = translation.cc;
        Translation ce = translation.ce;
        for (int i = 0; i < cirParameters.length; i++) {
            final CirVariable cirParameter = cirParameters[i];
            final CirValue cirArgument = cirArguments[i];
            if (cirParameter instanceof CirNormalContinuationParameter) {
                if (!(cirArgument instanceof CirVariable)) {
                    cc = makeNormalTranslation(cirArgument, translation);
                }
            } else if (cirParameter instanceof CirExceptionContinuationParameter) {
                if (!(cirArgument instanceof CirVariable)) {
                    if (cirArgument != CirValue.UNDEFINED) {
                        ce = makeExceptionDispatcherTranslation(cirArgument, translation);
                    }
                }
            } else if (cirArgument instanceof CirVariable && Arrays.contains(cirParameters, cirArgument)) {
                if (cirArgument != cirParameter) {
                    // Assignment ordering problem in recursive block calls: at least one variable is both parameter and argument.

                    final DirVariable dirArgument = cirToDirVariable((CirVariable) cirArgument);
                    final DirVariable dirTemporary = new DirVariable(cirArgument.kind(), -1);

                    // Prepend an assignment to a temp var to solve the ordering problem:
                    translation.dirBlock.instructions().insert(temporaryVariableInsertionIndex, new DirAssign(dirTemporary, dirArgument));

                    // Append an assignment from the temp var:
                    generateDirAssign(translation, cirParameter, dirTemporary);
                }
            } else {
                generateDirAssign(translation, cirParameter, cirArgument);
            }
        }
        final Translation t = makeTranslation(IrBlock.Role.NORMAL, closure, cc, ce);
        generateDirGoto(translation, t);
    }

    private void translate(Translation translation) {
        final CirCall cirCall = translation.closure.body();
        final CirValue cirProcedure = cirCall.procedure();
        final CirValue[] cirArguments = cirCall.arguments();

        if (cirProcedure instanceof CirVariable) {
            if (cirProcedure instanceof CirNormalContinuationParameter) {
                if (translation.cc == null) {
                    generateDirReturn(translation, cirArguments);
                } else {
                    generateDirGoto(translation, translation.cc, cirArguments);
                }
            } else if (cirProcedure instanceof CirExceptionContinuationParameter) {
                assert cirArguments.length == 1;
                if (translation.ce  == null) {
                    generateDirThrow(translation, cirArguments);
                } else {
                    generateDirGoto(translation, translation.ce, cirArguments);
                }
            } else {
                final DirJavaFrameDescriptor dirJavaFrameDescriptor = cirToDirJavaFrameDescriptor(cirCall.javaFrameDescriptor());
                methodCallGenerator.generateCall(translation, cirProcedure, cirArguments, cirCall.isNative(), dirJavaFrameDescriptor);
            }
        } else if (cirProcedure instanceof CirSwitch) {
            generateDirSwitch(translation, (CirSwitch) cirProcedure, cirArguments);
        } else if (cirProcedure instanceof CirBuiltin) {
            final CirBuiltin cirBuiltin = (CirBuiltin) cirProcedure;
            final DirJavaFrameDescriptor dirJavaFrameDescriptor = cirToDirJavaFrameDescriptor(cirCall.javaFrameDescriptor());
            if (cirBuiltin.builtin() == SafepointBuiltin.SoftSafepoint.BUILTIN || cirBuiltin.builtin() == SafepointBuiltin.HardSafepoint.BUILTIN) {
                safepointGenerator.generateCall(translation, cirBuiltin, cirArguments, false, dirJavaFrameDescriptor);
            } else {
                builtinCallGenerator.generateCall(translation, cirBuiltin, cirArguments, false, dirJavaFrameDescriptor);
            }
        } else if (cirProcedure instanceof CirMethod || cirProcedure instanceof CirConstant) {
            final DirJavaFrameDescriptor dirJavaFrameDescriptor = cirToDirJavaFrameDescriptor(cirCall.javaFrameDescriptor());
            methodCallGenerator.generateCall(translation, cirProcedure, cirArguments, cirCall.isNative(), dirJavaFrameDescriptor);
        } else if (cirProcedure instanceof CirBlock) {
            final CirBlock cirBlock = (CirBlock) cirProcedure;
            if (cirBlock.numberOfCalls() > 1) {
                // TODO: translateBlockCall(translation, cirBlock, cirArguments);
                translateClosureCall(translation, cirBlock.closure(), cirArguments);
            } else {
                translateClosureCall(translation, cirBlock.closure(), cirArguments);
            }
        } else {
            translateClosureCall(translation, (CirClosure) cirProcedure, cirArguments);
        }
    }

    private final VariableSequence<Translation> translationList = new ArrayListSequence<Translation>();

    private void translate() {
        while (!translationList.isEmpty()) {
            final Translation translation = translationList.removeFirst();
            translate(translation);
        }
    }

    /**
     * Merge the successor block into this block if it is the only successor
     * and this block is the only predecessor of its successor.
     * Repeat this until the above does not hold.
     * Then add this block to the predecessors of its successor.
     */
    private void assimilateSuccessors(DirBlock block) {
        DirBlock current = block;
        while (current.successors().length() == 1) {
            final DirBlock tail = current.successors().iterator().next();
            assert tail.predecessors().length() > 0;
            if (tail.predecessors().length() > 1) {
                tail.predecessors().add(block);
                return;
            }
            block.instructions().removeLast();
            AppendableSequence.Static.appendAll(block.instructions(), tail.instructions());
            block.setSuccessors(tail.successors());
            current = tail;
        }
    }

    private DirBlock getNonTrivialSuccessor(DirBlock trivialBlock) {
        DirBlock block = trivialBlock;
        do {
            final DirGoto dirGoto = (DirGoto) block.instructions().last();
            block = dirGoto.targetBlock();
        } while (block.isTrivial());
        return block;
    }

    private void forwardTrivialGotos(DirBlock block) {
        final DirInstruction lastInstruction = block.instructions().last();
        if (lastInstruction instanceof DirGoto) {
            final DirGoto dirGoto = (DirGoto) lastInstruction;
            if (dirGoto.targetBlock().isTrivial()) {
                dirGoto.setTargetBlock(getNonTrivialSuccessor(dirGoto.targetBlock()));
            }
        } else if (lastInstruction instanceof DirSwitch) {
            final DirSwitch dirSwitch = (DirSwitch) lastInstruction;
            final DirBlock[] targetBlocks = dirSwitch.targetBlocks();
            for (int i = 0; i < targetBlocks.length; i++) {
                if (targetBlocks[i].isTrivial()) {
                    targetBlocks[i] = getNonTrivialSuccessor(targetBlocks[i]);
                }
            }
            if (dirSwitch.defaultTargetBlock().isTrivial()) {
                dirSwitch.setDefaultTargetBlock(getNonTrivialSuccessor(dirSwitch.defaultTargetBlock()));
            }
        }
    }

    private int serial = 0;

    private void gatherMergedDirBlocks(DirBlock dirBlock, AppendableSequence<DirBlock> result) {
        final LinkedList<DirBlock> toDo = new LinkedList<DirBlock>();
        toDo.add(dirBlock);
        while (!toDo.isEmpty()) {
            final DirBlock block = toDo.removeFirst();
            if (block.serial() < 0) {
                if (!block.isTrivial()) {
                    result.append(block);
                    block.setSerial(serial);
                    serial++;
                    assimilateSuccessors(block);
                    forwardTrivialGotos(block);
                }
                for (DirBlock successor : block.successors()) {
                    toDo.add(successor);
                }
            }
        }
    }

    /**
     * Try to merge all blocks pairwise.
     * Two blocks can be merged by appending one's body to the other if one is the only successor of the other.
     * They can be merged by replacing all occurrences of one by the other if they are equivalent,
     * i.e. they can be replaced for each other without changing program semantics.
     *
     * @param blocks generated DIR blocks
     * @return the fix point of merging equivalent blocks
     */
    private IndexedSequence<DirBlock> unifyBlocks(IndexedSequence<DirBlock> blocks) {
        final Bag<Integer, DirBlock, Sequence<DirBlock>> hashBag = new SequenceBag<Integer, DirBlock>(HASHED);
        for (DirBlock block : blocks) {
            hashBag.add(block.hashCode(), block);
        }
        final Map<DirBlock, DirBlock> blockMap = new IdentityHashMap<DirBlock, DirBlock>();
        final DirBlockEquivalence dirBlockEquivalence = new DirBlockEquivalence();
        for (Sequence<DirBlock> sequence : hashBag.collections())  {
            switch (sequence.length()) {
                case 1: {
                    break;
                }
                case 2: {
                    if (dirBlockEquivalence.evaluate(sequence.first(), sequence.last())) {
                        blockMap.put(sequence.last(), sequence.first());
                    }
                    break;
                }
                default: {
                    final DirBlock[] array = Sequence.Static.toArray(sequence, DirBlock.class);
                    for (int i = 0; i < array.length; i++) {
                        final DirBlock a = array[i];
                        if (blockMap.get(a) == null) {
                            for (int j = i + 1; j < array.length; j++) {
                                final DirBlock b = array[j];
                                assert b != null;
                                if (dirBlockEquivalence.evaluate(a, b)) {
                                    blockMap.put(b, a);
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        final AppendableIndexedSequence<DirBlock> result = new ArrayListSequence<DirBlock>();
        for (DirBlock block : blocks) {
            final DirBlock unifiedBlock = blockMap.get(block);
            if (unifiedBlock == null || unifiedBlock == block) {
                result.append(block);
                for (DirInstruction instruction : block.instructions()) {
                    instruction.substituteBlocks(blockMap);
                }
            }
        }
        return result;
    }

    /**
     * @return DIR blocks generated from the CIR closure of the method
     */
    IndexedSequence<DirBlock> translateMethod() {
        final Translation translation = makeTranslation(IrBlock.Role.NORMAL, cirClosure, null, null);
        translate();

        final AppendableIndexedSequence<DirBlock> mergedBlocks = new ArrayListSequence<DirBlock>();
        // In principle, we could now add the exception dispatcher translations also,
        // but by gathering only the normal control flow first,
        // we end up with all the "normal" blocks at the beginning,
        // which is nicer later on for human consumption of DIR listings .
        gatherMergedDirBlocks(translation.dirBlock, mergedBlocks);

        for (DirBlock exceptionDispatcher : dirExceptionDispatchers) {
            assert exceptionDispatcher.role() == IrBlock.Role.EXCEPTION_DISPATCHER;
            gatherMergedDirBlocks(exceptionDispatcher, mergedBlocks);
        }

        dirGenerator.notifyBeforeTransformation(dirMethod, mergedBlocks, Transformation.BLOCK_UNIFICATION);
        final IndexedSequence<DirBlock> unifiedBlocks = unifyBlocks(mergedBlocks);
        dirGenerator.notifyAfterTransformation(dirMethod, mergedBlocks, Transformation.BLOCK_UNIFICATION);

        return unifiedBlocks;
    }
}
