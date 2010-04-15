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
package com.sun.max.ins.method;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Base class for views of disassembled target code for a single method in the VM.
 *
 * @author Mick Jordan
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class TargetCodeViewer extends CodeViewer {

    @Override
    public  MethodCodeKind codeKind() {
        return MethodCodeKind.TARGET_CODE;
    }

    @Override
    public String codeViewerKindName() {
        return "Target Code";
    }

    private final TeleTargetRoutine teleTargetRoutine;

    /**
     * @return surrogate for the {@link TargetRoutine} in the VM for the method being viewed.
     */
    protected TeleTargetRoutine teleTargetRoutine() {
        return teleTargetRoutine;
    }

    private final IndexedSequence<TargetCodeInstruction> instructions;
    private IndexedSequence<MaxCodeLocation> instructionLocations;

    /**
     * @return disassembled target code instructions for the method being viewed.
     */
    public IndexedSequence<TargetCodeInstruction> instructions() {
        return instructions;
    }

    public IndexedSequence<MaxCodeLocation> instructionLocations() {
        return instructionLocations;
    }

    private final TeleConstantPool teleConstantPool;

    /**
     * @return surrogate for the {@link ConstantPool} in the VM for the method being viewed.
     */
    protected final TeleConstantPool teleConstantPool() {
        return teleConstantPool;
    }

    private final ConstantPool localConstantPool;

    /**
     * @return local {@link ConstantPool} for the class containing the method in the VM being viewed.
     */
    protected final ConstantPool localConstantPool() {
        return localConstantPool;
    }

    /**
     * local copy of the method bytecodes in the VM; null if a native method or otherwise unavailable.
     */
    private final byte[] bytecodes;

    /**
     * Is this row the start of a new group of instructions.
     */
    protected boolean[] isBoundaryRow;

    /**
     * Is this row a stop point in the target code.
     */
    protected boolean[] isStopRow;

    private final String[] tagTextForRow;

    /**
     * Map: index into the sequence of target code instructions -> bytecode that compiled into code starting at this instruction, if known; else null.
     * The bytecode location may be in a different method that was inlined.
     */
    private final BytecodeLocation[] rowToBytecodeLocation;

    /**
     * Map:  index into the sequence of target code instructions -> constant pool index of {@MethodRefConstant} if this is a call instruction; else -1.
     */
    private final int[] rowToCalleeIndex;

    protected TargetCodeViewer(Inspection inspection, MethodInspector parent, TeleTargetRoutine teleTargetRoutine) {
        super(inspection, parent);
        this.teleTargetRoutine = teleTargetRoutine;
        instructions = teleTargetRoutine.getInstructions();
        instructionLocations = new VectorSequence<MaxCodeLocation>(teleTargetRoutine.getInstructionLocations());
        final TeleClassMethodActor teleClassMethodActor = teleTargetRoutine.getTeleClassMethodActor();
        if (teleClassMethodActor != null) {
            final TeleCodeAttribute teleCodeAttribute = teleClassMethodActor.getTeleCodeAttribute();
            bytecodes = teleCodeAttribute.readBytecodes();
            teleConstantPool = teleCodeAttribute.getTeleConstantPool();
            ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
            localConstantPool = classMethodActor == null ? null : classMethodActor.codeAttribute().constantPool;
        } else {  // native method
            bytecodes = null;
            teleConstantPool = null;
            localConstantPool = null;
        }
        final int targetInstructionCount = instructions.length();
        rowToStackFrame = new MaxStackFrame[targetInstructionCount];
        tagTextForRow = new String[targetInstructionCount];
        rowToBytecodeLocation = new BytecodeLocation[targetInstructionCount];
        rowToCalleeIndex = new int[targetInstructionCount];
        Arrays.fill(rowToCalleeIndex, -1);
        isBoundaryRow = new boolean[targetInstructionCount];
        Arrays.fill(isBoundaryRow, false);
        isStopRow = new boolean[targetInstructionCount];
        Arrays.fill(isStopRow, false);

        final int targetCodeLength = teleTargetRoutine.targetCodeRegion().size().toInt();
        final int[] positionToStopIndex = new int[targetCodeLength];
        Arrays.fill(positionToStopIndex, -1);
        final StopPositions stopPositions = teleTargetRoutine.getStopPositions();
        if (stopPositions != null) {
            for (int stopPositionIndex = 0; stopPositionIndex < stopPositions.length(); ++stopPositionIndex) {
                final int stopPosition = stopPositions.get(stopPositionIndex);
                ProgramError.check(stopPosition >= 0 && stopPosition < positionToStopIndex.length);
                positionToStopIndex[stopPosition] = stopPositionIndex;
            }
        }

        if (teleTargetRoutine instanceof TeleJitTargetMethod) { // JIT method
            final int[] bytecodeToTargetCodePositionMap = ((TeleJitTargetMethod) teleTargetRoutine).bytecodeToTargetCodePositionMap();
            boolean alternate = false;
            int bytecodeIndex = 0; // position in the original bytecode stream.
            for (int row = 0; row < targetInstructionCount; row++) {
                final int bytecodePosition = bytecodeIndex;
                // To check if we're crossing a bytecode boundary in the JITed code, compare the offset of the instruction at the current row with the offset recorded by the JIT
                // for the start of bytecode template.
                final int instructionPosition = instructions.get(row).position;
                if (bytecodePosition < bytecodeToTargetCodePositionMap.length && instructionPosition == bytecodeToTargetCodePositionMap[bytecodePosition]) {
                    isBoundaryRow[row] = true;
                    alternate = !alternate;

                    int opcode = Bytes.beU1(bytecodes, bytecodeIndex);
                    if (opcode == Bytecodes.WIDE) {
                        opcode = Bytes.beU1(bytecodes, bytecodeIndex + 1);
                    }

                    tagTextForRow[row] = bytecodePosition + ": " + Bytecodes.nameOf(opcode);
                    final BytecodeLocation bytecodeLocation = new BytecodeLocation(teleClassMethodActor.classMethodActor(), bytecodePosition);
                    rowToBytecodeLocation[row] = bytecodeLocation;
                    do {
                        ++bytecodeIndex;
                    } while (bytecodeIndex < bytecodeToTargetCodePositionMap.length && bytecodeToTargetCodePositionMap[bytecodeIndex] == 0);
                }
                if (positionToStopIndex[instructionPosition] >= 0) {
                    isStopRow[row] = true;
                }
            }
        } else {
            for (int row = 0; row < targetInstructionCount; row++) {
                int stopIndex = -1;
                // byte offset of this machine code instruction from beginning
                final int machineInstructionPosition = instructions.get(row).position;
                if (machineInstructionPosition >= 0 && machineInstructionPosition < positionToStopIndex.length) {
                    // The disassembler sometimes seems to report wild positions
                    // when disassembling random binary; this can happen when
                    // viewing some unknown native code whose length we must guess.
                    stopIndex = positionToStopIndex[machineInstructionPosition];
                }
                if (stopIndex >= 0) {
                    // the row is at a stop point
                    isStopRow[row] = true;
                    if (teleTargetRoutine instanceof TeleTargetMethod) {
                        final TeleTargetMethod teleTargetMethod = (TeleTargetMethod) teleTargetRoutine;
                        final BytecodeLocation bytecodeLocation = teleTargetMethod.getBytecodeLocation(stopIndex);
                        rowToBytecodeLocation[row] = bytecodeLocation;
                        // TODO (mlvdv) only works for non-inlined calls
                        if (bytecodeLocation != null && bytecodeLocation.classMethodActor.equals(teleTargetMethod.classMethodActor())) {
                            rowToCalleeIndex[row] = findCalleeIndex(bytecodes, bytecodeLocation.bytecodePosition);
                        }
                    }
                }
            }
        }
        updateStackCache();
    }

    /**
     * Adapter for bytecode scanning that only knows the constant pool index argument of the last method invocation instruction scanned.
     */
    private static final class MethodRefIndexFinder extends BytecodeAdapter  {
        int methodRefIndex = -1;

        public MethodRefIndexFinder reset() {
            methodRefIndex = -1;
            return this;
        }

        @Override
        protected void invokestatic(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokespecial(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokevirtual(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokeinterface(int index, int count) {
            methodRefIndex = index;
        }

        public int methodRefIndex() {
            return methodRefIndex;
        }
    };

    private final MethodRefIndexFinder methodRefIndexFinder = new MethodRefIndexFinder();

    /**
     * @param bytecodes
     * @param bytecodePosition byte offset into bytecodes
     * @return if a call instruction, the index into the constant pool of the called {@link MethodRefConstant}; else -1.
     */
    private int findCalleeIndex(byte[] bytecodes, int bytecodePosition) {
        if (bytecodePosition >= bytecodes.length) {
            return -1;
        }
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(methodRefIndexFinder.reset());
        bytecodeScanner.scanInstruction(bytecodes, bytecodePosition);
        return methodRefIndexFinder.methodRefIndex();
    }

    /**
     * Rebuilds the cache of stack information if needed, based on the thread that is the current focus.
     * <br>
     * Identifies for each row (instruction) a stack frame (if any) that is related to the instruction.
     * In the case of the top frame, this would be the row (instruction) at the current IP.
     * In the case of other frames, this would be the row (instruction) that is the call return site.
     *
     */
    @Override
    protected void updateStackCache() {
        final MaxThread thread = focus().thread();
        if (thread == null) {
            return;
        }
        final Sequence<MaxStackFrame> frames = thread.stack().frames();

        Arrays.fill(rowToStackFrame, null);

        // For very deep stacks (e.g. when debugging a metacircular related infinite recursion issue),
        // it's faster to loop over the frames and then only loop over the instructions for each
        // frame related to the target code represented by this viewer.
        final TargetCodeRegion targetCodeRegion = teleTargetRoutine().targetCodeRegion();
        for (MaxStackFrame frame : frames) {
            final MaxCodeLocation frameCodeLocation = frame.codeLocation();
            if (frameCodeLocation != null) {
                final boolean isFrameForThisCode =
                    frame instanceof MaxStackFrame.Compiled ?
                                    targetCodeRegion.overlaps(frame.targetMethod()) :
                                        targetCodeRegion.contains(frameCodeLocation);
                if (isFrameForThisCode) {
                    for (int row = 0; row < instructions.length(); row++) {
                        if (instructions.get(row).address.equals(frameCodeLocation.address())) {
                            rowToStackFrame[row] = frame;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Does the instruction address have a target code breakpoint set in the VM.
     */
    protected MaxBreakpoint getTargetBreakpointAtRow(int row) {
        return vm().breakpointManager().findBreakpoint(instructionLocations.get(row));
    }

    protected final String rowToTagText(int row) {
        if (tagTextForRow[row] != null) {
            return tagTextForRow[row];
        }
        return "";
    }

    /**
     * @param row an index into the sequence of target code instructions
     * @return the location of the bytecode instruction (possibly inlined from a different method) from which the target code starting here was compiled; null if unavailable.
     */
    protected final BytecodeLocation rowToBytecodeLocation(int row) {
        return rowToBytecodeLocation[row];
    }

    /**
     * @param row an index into the sequence of target code instructions
     * @return if a call instruction, the index into the constant pool of the operand/callee; else -1.
     */
    protected final int rowToCalleeIndex(int row) {
        return rowToCalleeIndex[row];
    }

}
