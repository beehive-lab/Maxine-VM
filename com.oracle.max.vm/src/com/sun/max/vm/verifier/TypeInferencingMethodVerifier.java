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
package com.sun.max.vm.verifier;

import static com.sun.max.vm.verifier.types.VerificationType.*;

import java.io.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.stackmap.*;
import com.sun.max.vm.verifier.types.*;

/**
 * An implementation of a bytecode verifier that aims to have the same semantics as the original Sun bytecode verifier
 * (i.e. implemented in the check_code.c source file). That is, this verifier performs the iterative data-flow analysis
 * described in <a href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#9801">4.9.2</a> of
 * the The Java Virtual Machine Specification, Second Edition.
 * <p>
 * Implementation Note: The {@linkplain TypeCheckingMethodVerifier.Interpreter abstract interpreter} used by the
 * type-checking verifier is reused. This re-use comes at the cost of having to decode each instruction (and its
 * operands) each time it is 'executed' by the verifier. However, it also means that the semantics of each instruction
 * (for the purpose of verification) are described only once.
 */
public class TypeInferencingMethodVerifier extends TypeCheckingMethodVerifier {

    /**
     * A map from each BCI to the instruction at that BCI. A null entry
     * means that an instruction does not start at the corresponding BCI.
     */
    private final Instruction[] instructionMap;

    /**
     * The work-list of basic blocks/instructions still to be processed by the data-flow analyzer.
     */
    private final Queue<TypeState> targetQueue = new LinkedList<TypeState>();

    /**
     * The scanner used to decode each instruction as it is {@linkplain Instruction#interpret() interpreted}.
     */
    private final BytecodeScanner scanner;

    /**
     * The set of ASTORE instructions that store the return BCI of a subroutine.
     */
    private Set<Instruction> retBCIStores;

    public TypeInferencingMethodVerifier(ClassVerifier classVerifier, ClassMethodActor classMethodActor, CodeAttribute codeAttribute) {
        super(classVerifier, classMethodActor, codeAttribute);
        instructionMap = new Instruction[codeAttribute.code().length];
        scanner = new BytecodeScanner(interpreter);
    }

    @Override
    protected Frame[] initializeFrameMap(CodeAttribute codeAttribute, Frame initialFrame, ClassVerifier classVerifier) {
        final TypeState[] typeStateMap = new TypeState[codeAttribute.code().length];
        typeStateMap[0] = (TypeState) initialFrame;
        return typeStateMap;
    }

    @Override
    protected Frame createInitialFrame(MethodActor classMethodActor) {
        return new TypeState(classMethodActor, this);
    }

    protected TypeState[] typeStateMap() {
        return (TypeState[]) frameMap;
    }

    @Override
    public int currentOpcodeBCI() {
        return scanner == null ? -1 : scanner.currentOpcodeBCI();
    }

    public TypeState typeStateAt(int bci) {
        return typeStateMap()[bci];
    }

    public boolean isRetBCIStore(Instruction astore) {
        return retBCIStores != null && retBCIStores.contains(astore);
    }

    public boolean hasUnvisitedCode() {
        for (Instruction instruction = instructionMap[0]; instruction != null; instruction = instruction.next()) {
            if (!instruction.visited()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Derives a {@link StackMapTable} attribute from the type state inferred during verification. This method returns
     * null if there are no BCIs for which an explicit frame map needs to be recorded in a StackMapTable
     * attribute. That is, the only type state required to verify the method via
     * {@linkplain TypeCheckingMethodVerifier type checking} is the implicit entry frame that can be derived from the
     * method's signature.
     *
     * @param constantPoolEditor the constant pool editor
     * @return the StackMapTable attribute that enables this method to be verified via
     *         {@linkplain TypeCheckingMethodVerifier type checking}
     */
    public StackMapTable generateStackMapTable(ConstantPoolEditor constantPoolEditor) {
        final List<StackMapFrame> stackMapFrames = new ArrayList<StackMapFrame>();
        TypeState previousTypeState = typeStateMap()[0];
        for (TypeState typeState : typeStateMap()) {
            if (typeState != null && typeState.visited() && typeState.bci() != 0) {
                final StackMapFrame stackMapFrame = typeState.asStackMapFrame(previousTypeState);
                stackMapFrames.add(stackMapFrame);
                previousTypeState = typeState;
            }
        }

        if (stackMapFrames.isEmpty()) {
            return null;
        }
        return new StackMapTable(stackMapFrames.toArray(new StackMapFrame[stackMapFrames.size()]), constantPoolEditor);
    }

    @Override
    public void verify() {
        if (verbose || Verifier.TraceVerifierLevel >= Verifier.TRACE_METHOD) {
            Log.println(classMethodActor().format("[Verifying %H.%n(%p) via type-inferencing]"));
        }
        if (verbose) {
            Log.println("Input bytecode:");
            CodeAttributePrinter.print(Log.out, codeAttribute());
            Log.println();
            Log.println("Interpreting bytecode:");
        }

        final TypeState[] typeStateMap = typeStateMap();
        final TypeState initialTypeState = typeStateMap[0];
        initialTypeState.setVisited();

        parseInstructions();
        verifyExceptionHandlers();

        // Verify targets and bind them to their targeted instructions
        for (int bci = 0; bci != instructionMap.length; ++bci) {
            final TypeState typeState = typeStateMap[bci];
            if (typeState != null) {
                final Instruction targetedInstruction = instructionMap[bci];
                if (targetedInstruction == null) {
                    verifyError("Invalid branch target or exception handler entry BCI (" + bci + ")");
                }
                typeState.setTargetedInstruction(targetedInstruction);
            }
        }

        // Run dataflow analyzer.
        enqueChangedTypeState(initialTypeState);
        while (!targetQueue.isEmpty()) {
            final TypeState typeState = targetQueue.remove();
            assert typeState.visited();
            Instruction instruction = typeState.targetedInstruction();
            fallsThrough = false;

            while (true) {
                instruction.interpret();
                if (Bytecodes.isStop(instruction.opcode)) {
                    break;
                }
                instruction = instruction.next();
                if (instruction == null) {
                    verifyError("Execution falls off end of method");
                }
            }
        }
    }

    public void enqueChangedTypeState(final TypeState typeState) {
        assert typeState.targetedInstruction() != null;
        targetQueue.add(typeState);
    }

    @Override
    protected void verifyIsValidInstructionBCI(int bci, String bciDescription) {
        try {
            if (instructionMap[bci] != null) {
                return;
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            // fall through
        }
        verifyError("Invalid BCI " + bci + "(" + bciDescription + ")");
    }

    @Override
    protected void verifyExceptionHandler(ExceptionHandlerEntry info) {
        super.verifyExceptionHandler(info);
        makeTypeState(info.handlerBCI());
    }

    /**
     * Gets the current interpreter frame type state.
     * @return the type state
     */
    public TypeState typeState() {
        return (TypeState) frame;
    }

    TypeState makeTypeState(int bci) {
        try {
            final TypeState[] typeStateMap = typeStateMap();
            TypeState typeState = typeStateMap[bci];
            if (typeState == null) {
                typeState = new TypeState(typeState());
                typeState.clear();
                typeStateMap[bci] = typeState;
            }
            return typeState;
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            throw fatalVerifyError("Branch target outside of code range");
        }
    }

    @Override
    protected void performStore(VerificationType type, int index) {
        if ((type == REFERENCE || type == REFERENCE_OR_WORD) && SUBROUTINE.isAssignableFrom(typeState().top())) {
            final Subroutine subroutine = (Subroutine) typeState().pop(SUBROUTINE);
            typeState().store(subroutine, index);

            final Instruction astore = instructionMap[currentOpcodeBCI()];
            assert Bytecodes.nameOf(astore.opcode).startsWith("astore");
            if (retBCIStores == null) {
                retBCIStores = new HashSet<Instruction>();
            }
            retBCIStores.add(astore);

        } else {
            super.performStore(type, index);
        }
    }

    @Override
    protected void performJsr(int offset) {
        final int subroutineEntryBCI = currentOpcodeBCI() + offset;
        final Subroutine subroutine = classVerifier().getSubroutine(subroutineEntryBCI, codeAttribute().maxLocals);

        final int retBCI = scanner.currentBCI();
        final boolean firstVisit = !subroutine.containsRetTarget(retBCI);
        if (firstVisit) {
            subroutine.addRetTarget(retBCI);
        }
        typeState().push(subroutine);
        typeState().pushSubroutine(subroutine);

        // All uninitialized objects are set to bogus to prevent them from
        // propagating into a subroutine.
        typeState().killUninitializedObjects();

        performBranch(subroutineEntryBCI);

        if (firstVisit) {
            // Need to force a visit to the subroutine as a single object is used to represent
            // the subroutine state for all paths into the subroutine (i.e. the merge done in
            // the above call to performBranch() will not have detected a change in the type state).
            enqueChangedTypeState(typeStateAt(subroutineEntryBCI));

            // For the same reason as above, we also need to force a visit to all of the RET
            // instructions currently recorded for the subroutine. That is, forcing a visit
            // to the subroutine does not guarantee that the verifier will
            // continue along the control flow paths in the subroutine to all the RET
            // instructions in the routine.
            for (int retInstruction : subroutine.retInstructions()) {
                enqueChangedTypeState(typeStateAt(retInstruction));
            }
        }
    }

    @Override
    protected void performRet(int index) {
        final TypeState[] typeStateMap = typeStateMap();
        final TypeState typeState = typeState();
        final Subroutine subroutine = (Subroutine) typeState.load(SUBROUTINE, index);
        final int numberOfSubroutineFramesPopped = typeState.popSubroutine(subroutine);

        // All uninitialized objects are set to bogus to prevent them from
        // propagating out of a subroutine.
        typeState.killUninitializedObjects();

        // Record the BCI of this RET instruction
        final int currentOpcodeBCI = currentOpcodeBCI();
        subroutine.addRetInstruction(currentOpcodeBCI);

        // Create the type state at this BCI if it does not already exist.
        // This is required so that the data-flow analyzer can be forced to
        // (re)consider the control flow starting at each RET instruction in
        // a subroutine whenever a JSR to the subroutine is found.
        final Ret ret = (Ret) instructionMap[currentOpcodeBCI];
        if (typeStateMap[currentOpcodeBCI] == null) {
            typeStateMap[currentOpcodeBCI] = new TypeState(typeState);
            typeStateMap[currentOpcodeBCI].setTargetedInstruction(ret);
        }

        // Update the current frame based on the combination of the frame state upon entry to the subroutine
        // from the JSR immediately preceding the instruction(s) returned to and the frame state at
        // this RET instruction.
        for (int retTarget : subroutine.retTargets()) {

            final Instruction targetInstruction = instructionMap[retTarget];
            final Jsr jsr = (Jsr) targetInstruction.previous();

            jsr.verifyRet(ret);
            ret.setNumberOfFramesPopped(numberOfSubroutineFramesPopped);

            final TypeState jsrTypeState = typeStateMap[jsr.bci()];
            assert jsrTypeState != null;
            typeState.updateLocalsNotAccessedInSubroutine(jsrTypeState, subroutine, index);

            // Create the type state at the return BCI if it does not already exist
            TypeState retTypeState = typeStateMap[retTarget];
            if (retTypeState == null) {
                retTypeState = new TypeState(jsrTypeState);
                retTypeState.setTargetedInstruction(targetInstruction);
                typeStateMap[retTarget] = retTypeState;
            }

            performBranch(retTarget);
        }
    }

    private void parseInstructions() {
        // Scan the bytecode stream to find instruction boundaries
        class InstructionParser extends BytecodeAdapter {

            private Instruction previous;

            @Override
            public void instructionDecoded() {
                final int currentOpcodeBCI = currentOpcodeBCI();
                if (instructionMap[currentOpcodeBCI] == null) {
                    previous = new Instruction(currentOpcode(), currentOpcodeBCI, currentBCI(), previous);
                }
            }

            private TypeState makeTarget(int offset) {
                return makeTypeState(currentOpcodeBCI() + offset);
            }

            private void branch(int offset) {
                previous = new Branch(currentOpcode(), currentOpcodeBCI(), currentBCI(), makeTarget(offset), previous);
            }

            @Override
            protected void ifeq(int offset) {
                branch(offset);
            }

            @Override
            protected void ifne(int offset) {
                branch(offset);
            }

            @Override
            protected void iflt(int offset) {
                branch(offset);
            }

            @Override
            protected void ifge(int offset) {
                branch(offset);
            }

            @Override
            protected void ifgt(int offset) {
                branch(offset);
            }

            @Override
            protected void ifle(int offset) {
                branch(offset);
            }

            @Override
            protected void if_icmpeq(int offset) {
                branch(offset);
            }

            @Override
            protected void if_icmpne(int offset) {
                branch(offset);
            }

            @Override
            protected void if_icmplt(int offset) {
                branch(offset);
            }

            @Override
            protected void if_icmpge(int offset) {
                branch(offset);
            }

            @Override
            protected void if_icmpgt(int offset) {
                branch(offset);
            }

            @Override
            protected void if_icmple(int offset) {
                branch(offset);
            }

            @Override
            protected void if_acmpeq(int offset) {
                branch(offset);
            }

            @Override
            protected void if_acmpne(int offset) {
                branch(offset);
            }

            @Override
            protected void ifnull(int offset) {
                branch(offset);
            }

            @Override
            protected void ifnonnull(int offset) {
                branch(offset);
            }

            @Override
            protected void goto_(int offset) {
                branch(offset);
            }

            @Override
            protected void goto_w(int offset) {
                goto_(offset);
            }

            @Override
            protected void jsr(int offset) {
                previous = new Jsr(currentOpcode(), currentOpcodeBCI(), currentBCI(), makeTarget(offset), previous);

                // Create a target at the JSR to save the frame state before entering the subroutine. This
                // is used to restore the frame state upon leaving the subroutine via a RET.
                makeTarget(0);
            }

            @Override
            protected void jsr_w(int offset) {
                jsr(offset);
            }

            @Override
            protected void ret(int index) {
                previous = new Ret(currentOpcode(), currentOpcodeBCI(), currentBCI(), previous);
            }

            @Override
            protected void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
                final TypeState[] targets = new TypeState[numberOfCases];
                for (int i = 0; i != numberOfCases; ++i) {
                    targets[i] = makeTarget(bytecodeScanner().readSwitchOffset());
                }
                previous = new Tableswitch(currentOpcode(), currentOpcodeBCI(), currentBCI(), makeTarget(defaultOffset), lowMatch, highMatch, targets, previous);
            }

            @Override
            protected void lookupswitch(int defaultOffset, int numberOfCases) {
                final TypeState[] targets = new TypeState[numberOfCases];
                final int[] matches = new int[numberOfCases];
                for (int i = 0; i != numberOfCases; ++i) {
                    matches[i] = bytecodeScanner().readSwitchCase();
                    targets[i] = makeTarget(bytecodeScanner().readSwitchOffset());
                }
                previous = new Lookupswitch(currentOpcode(), currentOpcodeBCI(), currentBCI(), makeTarget(defaultOffset), matches, targets, previous);
            }
        }

        final InstructionParser parser = new InstructionParser();
        new BytecodeScanner(parser).scan(new BytecodeBlock(codeAttribute().code()));
    }

    interface TargetVisitor {
        void visit(TypeState successor);
    }

    public class Instruction {

        public final int opcode;
        private final BytecodeBlock block;
        private Instruction next;
        private boolean visited;

        public Instruction(int opcode, int bci, int endBCI, Instruction previous) {
            assert endBCI > bci;
            this.opcode = opcode;
            this.block = new BytecodeBlock(codeAttribute().code(), bci, endBCI - 1);
            instructionMap[bci] = this;
            if (previous != null) {
                previous.next = this;
            }
        }

        public int bci() {
            return block.start;
        }

        public int size() {
            return block.size();
        }

        public Instruction next() {
            return next;
        }

        public Instruction previous() {
            int bci = bci();
            while (bci > 0) {
                final Instruction instruction = instructionMap[--bci];
                if (instruction != null) {
                    return instruction;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return Bytecodes.nameOf(opcode);
        }

        public void writeTo(DataOutputStream outputStream) throws IOException {
            final byte[] code = block.code();
            for (int i = block.start; i <= block.end; ++i) {
                outputStream.write(code[i]);
            }
        }

        public void interpret() {
            visited = true;
            scanner.scanInstruction(block);
        }

        public boolean visited() {
            return visited;
        }
    }

    public class Branch extends Instruction {

        /**
         * The type state at the target of a branch.
         */
        final TypeState target;

        public Branch(int opcode, int bci, int endBCI, TypeState target, Instruction previous) {
            super(opcode, bci, endBCI, previous);
            this.target = target;
        }
    }

    public abstract class Select extends Instruction {

        final TypeState defaultTarget;
        final TypeState[] caseTargets;

        public Select(int opcode, int bci, int endBCI, TypeState defaultTarget, TypeState[] caseTargets, Instruction previous) {
            super(opcode, bci, endBCI, previous);
            this.defaultTarget = defaultTarget;
            this.caseTargets = caseTargets;
        }
    }

    public class Tableswitch extends Select {

        final int low;
        final int high;

        public Tableswitch(int opcode, int bci, int size, TypeState defaultTarget, int low, int high, TypeState[] caseTargets, Instruction previous) {
            super(opcode, bci, size, defaultTarget, caseTargets, previous);
            this.low = low;
            this.high = high;
        }
    }

    public class Lookupswitch extends Select {

        final int[] matches;

        public Lookupswitch(int opcode, int bci, int size, TypeState defaultTarget, int[] matches, TypeState[] caseTargets, Instruction previous) {
            super(opcode, bci, size, defaultTarget, caseTargets, previous);
            this.matches = matches;
        }
    }

    public class Jsr extends Branch {

        private Instruction ret;

        public Jsr(int opcode, int bci, int size, TypeState branchTarget, Instruction previous) {
            super(opcode, bci, size, branchTarget, previous);
        }

        public void verifyRet(Instruction ret) {
            assert ret.opcode == Bytecodes.RET;
            if (this.ret == null) {
                this.ret = ret;
            } else {
                if (this.ret != ret) {
                    verifyError("Multiple returns to single JSR");
                }
            }
        }

        public Instruction ret() {
            return ret;
        }
    }

    public class Ret extends Instruction {

        private int numberOfFramesPopped = -1;

        public Ret(int opcode, int bci, int endBCI, Instruction previous) {
            super(opcode, bci, endBCI, previous);
        }

        public int numberOfFramesPopped() {
            return numberOfFramesPopped;
        }

        public void setNumberOfFramesPopped(int count) {
            assert numberOfFramesPopped == -1 || numberOfFramesPopped == count;
            numberOfFramesPopped = count;
        }
    }
}
