/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.cir.dir;

import static com.sun.max.vm.cps.collect.ListBag.MapType.*;

import java.io.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.dir.DirTraceObserver.Transformation;
import com.sun.max.vm.cps.dir.transform.*;
import com.sun.max.vm.cps.ir.*;
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
    private final LinkedIdentityHashSet<DirBlock> dirExceptionDispatchers = new LinkedIdentityHashSet<DirBlock>();

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
        translationList.add(newTranslation);
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
        final DirValue[] to = new DirValue[cirValues.length];
        for (int i = 0; i < cirValues.length; i++) {
            to[i] = cirToDirValue(cirValues[i]);
        }
        return to;
    }

    private void generateDirReturn(Translation translation, CirValue[] cirArguments) {
        if (cirArguments.length == 0) {
            translation.dirBlock.instructions().add(new DirReturn(DirConstant.VOID));
        } else {
            assert cirArguments.length == 1;
            translation.dirBlock.instructions().add(new DirReturn(cirToDirValue(cirArguments[0])));
        }
    }

    private void generateDirThrow(Translation translation, CirValue[] cirArguments) {
        assert cirArguments.length == 1;
        translation.dirBlock.instructions().add(new DirThrow(cirToDirValue(cirArguments[0])));
    }

    private void generateDirAssign(Translation translation, CirVariable cirVariable, DirValue dirValue) {
        final DirVariable dirVariable = cirToDirVariable(cirVariable);
        if (dirValue != dirVariable) {
            translation.dirBlock.instructions().add(new DirAssign(dirVariable, dirValue));
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
        fromTranslation.dirBlock.instructions().add(new DirGoto(toTranslation.dirBlock));
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
        final DirValue[] dirMatches = new DirValue[cirSwitch.numberOfMatches()];
        for (int i = 0; i < dirMatches.length; i++) {
            dirMatches[i] = cirToDirValue(cirArguments[i + 1]);
        }
        final DirBlock[] targetBlocks = new DirBlock[cirSwitch.numberOfMatches()];
        for (int i = 0; i < targetBlocks.length; i++) {
            targetBlocks[i] = translateSwitchTarget(translation, cirArguments[1 + cirSwitch.numberOfMatches() + i]);
        }
        final DirBlock defaultTargetBlock = translateSwitchTarget(translation, cirArguments[cirArguments.length - 1]);
        translation.dirBlock.instructions().add(new DirSwitch(cirSwitch.comparisonKind(), cirSwitch.valueComparator(), cirToDirValue(cirArguments[0]), dirMatches, targetBlocks, defaultTargetBlock));
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
                                                                cirJavaFrameDescriptor.classMethodActor,
                                                                cirJavaFrameDescriptor.bci,
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
            returnBlock.instructions().add(new DirReturn(getReturnVariable()));
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
                assert cirProcedure == CirSnippet.get(Snippet.RaiseThrowable.SNIPPET);
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

            // clip the exception arguments
            final DirValue[] dirArguments = new DirValue[cirArguments.length - 2];
            for (int i = 0; i < dirArguments.length; i++) {
                dirArguments[i] = cirToDirValue(cirArguments[i]);
            }

            translation.dirBlock.instructions().add(createCallInstruction(result, cirProcedure, dirArguments, catchBlock, isNativeCall, javaFrameDescriptor));

            if (cc != null) {
                generateDirGoto(translation, cc, result);
            } else if (normalContinuation instanceof CirNormalContinuationParameter) {
                translation.dirBlock.instructions().add(new DirReturn(result));
            } else {
                if (normalContinuation == CirValue.UNDEFINED) {
                    assert cirProcedure == CirSnippet.get(Snippet.RaiseThrowable.SNIPPET);
                } else {
                    assert normalContinuation instanceof CirExceptionContinuationParameter;
                    translation.dirBlock.instructions().add(new DirThrow(result));
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
            return new DirBuiltinCall(result, cirBuiltin.builtin, arguments, catchBlock, javaFrameDescriptor);
        }
    };

    private final CallGenerator<CirBuiltin> infopointGenerator = new CallGenerator<CirBuiltin>() {
        @Override
        protected DirInstruction createCallInstruction(DirVariable result, CirBuiltin cirBuiltin, DirValue[] arguments, DirCatchBlock catchBlock, boolean isNativeCall, DirJavaFrameDescriptor javaFrameDescriptor) {
            assert javaFrameDescriptor != null;
            int opcode = arguments[0].value().asInt();
            return new DirInfopoint(result, javaFrameDescriptor, opcode);
        }
    };

    /**
     * Records if the method being translated uses the {@link MakeStackVariable} builtin. This is
     * required so that the rest of the backend knows that the liveness of any pinned stack variable
     * extends to the end of the method.
     */
    boolean usesMakeStackVariable;

    private void translateClosureCall(Translation translation, CirClosure closure, CirValue[] cirArguments) {
        final int temporaryVariableInsertionIndex = translation.dirBlock.instructions().size();
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
            } else if (cirArgument instanceof CirVariable && (Utils.indexOfIdentical(cirParameters, cirArgument) >= 0)) {
                if (cirArgument != cirParameter) {
                    // Assignment ordering problem in recursive block calls: at least one variable is both parameter and argument.

                    final DirVariable dirArgument = cirToDirVariable((CirVariable) cirArgument);
                    final DirVariable dirTemporary = new DirVariable(cirArgument.kind(), -1);

                    // Prepend an assignment to a temp var to solve the ordering problem:
                    translation.dirBlock.instructions().add(temporaryVariableInsertionIndex, new DirAssign(dirTemporary, dirArgument));

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
            if (cirBuiltin.builtin == InfopointBuiltin.BUILTIN) {
                infopointGenerator.generateCall(translation, cirBuiltin, cirArguments, false, dirJavaFrameDescriptor);
            } else {
                if (cirBuiltin.builtin == MakeStackVariable.BUILTIN) {
                    usesMakeStackVariable = true;
                }
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

    private final List<Translation> translationList = new ArrayList<Translation>();

    private void translate() {
        while (!translationList.isEmpty()) {
            final Translation translation = translationList.remove(0);
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
        while (current.successors().size() == 1) {
            final DirBlock tail = current.successors().iterator().next();
            assert tail.predecessors().size() > 0;
            if (tail.predecessors().size() > 1) {
                tail.predecessors().add(block);
                return;
            }
            block.instructions().remove(block.instructions().size() - 1);
            block.instructions().addAll(tail.instructions());
            block.setSuccessors(tail.successors());
            current = tail;
        }
    }

    private DirBlock getNonTrivialSuccessor(DirBlock trivialBlock) {
        DirBlock block = trivialBlock;
        do {
            ArrayList<DirInstruction> s = block.instructions();
            final DirGoto dirGoto = (DirGoto) s.get(s.size() - 1);
            block = dirGoto.targetBlock();
        } while (block.isTrivial());
        return block;
    }

    private void forwardTrivialGotos(DirBlock block) {
        ArrayList<DirInstruction> s = block.instructions();
        final DirInstruction lastInstruction = s.get(s.size() - 1);
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

    private void gatherMergedDirBlocks(DirBlock dirBlock, List<DirBlock> result) {
        final LinkedList<DirBlock> toDo = new LinkedList<DirBlock>();
        toDo.add(dirBlock);
        while (!toDo.isEmpty()) {
            final DirBlock block = toDo.removeFirst();
            if (block.serial() < 0) {
                if (!block.isTrivial()) {
                    result.add(block);
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
    private List<DirBlock> unifyBlocks(List<DirBlock> blocks) {
        final ListBag<Integer, DirBlock> hashBag = new ListBag<Integer, DirBlock>(HASHED);
        for (DirBlock block : blocks) {
            hashBag.add(block.hashCode(), block);
        }
        final Map<DirBlock, DirBlock> blockMap = new IdentityHashMap<DirBlock, DirBlock>();
        final DirBlockEquivalence dirBlockEquivalence = new DirBlockEquivalence();
        for (List<DirBlock> sequence : hashBag.collections())  {
            switch (sequence.size()) {
                case 1: {
                    break;
                }
                case 2: {
                    if (dirBlockEquivalence.evaluate(Utils.first(sequence), Utils.last(sequence))) {
                        blockMap.put(Utils.last(sequence), Utils.first(sequence));
                    }
                    break;
                }
                default: {
                    final DirBlock[] array = sequence.toArray(new DirBlock[sequence.size()]);
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
        final List<DirBlock> result = new ArrayList<DirBlock>();
        for (DirBlock block : blocks) {
            final DirBlock unifiedBlock = blockMap.get(block);
            if (unifiedBlock == null || unifiedBlock == block) {
                result.add(block);
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
    List<DirBlock> translateMethod() {
        final Translation translation = makeTranslation(IrBlock.Role.NORMAL, cirClosure, null, null);
        translate();

        final List<DirBlock> mergedBlocks = new ArrayList<DirBlock>();
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
        final List<DirBlock> unifiedBlocks = unifyBlocks(mergedBlocks);
        dirGenerator.notifyAfterTransformation(dirMethod, mergedBlocks, Transformation.BLOCK_UNIFICATION);

        return unifiedBlocks;
    }
}
