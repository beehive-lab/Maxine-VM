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

import java.awt.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;

/**
 * Base class for views of disassembled target code for a single method in the {@link TeleVM}.
 *
 * @author Mick Jordan
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class TargetCodeViewer extends CodeViewer {

    @Override
    public  MethodInspector.CodeKind codeKind() {
        return MethodInspector.CodeKind.TARGET_CODE;
    }

    @Override
    public String codeViewerKindName() {
        return "Target Code";
    }

    private final TeleTargetRoutine _teleTargetRoutine;

    /**
     * @return surrogate for the {@link TargetRoutine} in the {@link TeleVM} for the method being viewed.
     */
    protected TeleTargetRoutine teleTargetRoutine() {
        return _teleTargetRoutine;
    }

    private final IndexedSequence<TargetCodeInstruction> _instructions;

    /**
     * @return disassembled target code instructions for the method being viewed.
     */
    public IndexedSequence<TargetCodeInstruction> instructions() {
        return _instructions;
    }

    private final TeleConstantPool _teleConstantPool;

    /**
     * @return surrogate for the {@link ConstantPool} in the {@link TeleVM} for the method being viewed.
     */
    protected final TeleConstantPool teleConstantPool() {
        return _teleConstantPool;
    }

    private final ConstantPool _localConstantPool;

    /**
     * @return local {@link ConstantPool} for the class containing the method in the {@TeleVM} being viewed.
     */
    protected final ConstantPool localConstantPool() {
        return _localConstantPool;
    }

    /**
     * local copy of the method bytecodes in the {@link TeleVM}; null if a native method or otherwise unavailable.
     */
    private final byte[] _bytecodes;

    /**
     * Color to use for background of a row during normal display; modulated by safepoints, etc.
     * May be overridden by other states.
     */
    private final Color[] _rowToBackGroundColor;

    private final String[] _rowToTagText;

    /**
     * Map:  index into the sequence of target code instructions -> bytecode that compiled into code starting at this instruction, if known; else null.
     * The bytecode location may be in a different method that was inlined.
     */
    private final BytecodeLocation[] _rowToBytecodeLocation;

    /**
     * Map:  index into the sequence of target code instructions -> constant pool index of {@MethodRefConstant} if this is a call instruction; else -1.
     */
    private final int[] _rowToCalleeIndex;

    protected TargetCodeViewer(Inspection inspection, MethodInspector parent, TeleTargetRoutine teleTargetRoutine) {
        super(inspection, parent);

        _teleTargetRoutine = teleTargetRoutine;
        _instructions = teleTargetRoutine.getInstructions();
        final TeleClassMethodActor teleClassMethodActor = teleTargetRoutine.getTeleClassMethodActor();
        if (teleClassMethodActor != null) {
            final TeleCodeAttribute teleCodeAttribute = teleClassMethodActor.getTeleCodeAttribute();
            _bytecodes = teleCodeAttribute.readBytecodes();
            _teleConstantPool = teleCodeAttribute.getTeleConstantPool();
            _localConstantPool = teleClassMethodActor.classMethodActor().codeAttribute().constantPool();
        } else {  // native method
            _bytecodes = null;
            _teleConstantPool = null;
            _localConstantPool = null;
        }
        final int targetInstructionCount = _instructions.length();
        _rowToStackFrameInfo = new StackFrameInfo[targetInstructionCount];
        _rowToBackGroundColor = new Color[targetInstructionCount];
        _rowToTagText = new String[targetInstructionCount];
        _rowToBytecodeLocation = new BytecodeLocation[targetInstructionCount];
        _rowToCalleeIndex = new int[targetInstructionCount];
        Arrays.fill(_rowToCalleeIndex, -1);
        final Color backgroundColor = style().targetCodeBackgroundColor();
        final Color alternateBackgroundColor = style().targetCodeAlternateBackgroundColor();
        final Color stopBackgroundColor = style().targetCodeStopBackgroundColor();
        final BytecodeInfo[] bytecodeInfos = teleTargetRoutine.bytecodeInfos();
        final int targetCodeLength = teleTargetRoutine.targetCodeRegion().size().toInt();
        final int[] positionToStopIndex = new int[targetCodeLength];
        Arrays.fill(positionToStopIndex, -1);
        final int[] stopPositions = teleTargetRoutine.getStopPositions();
        if (stopPositions != null) {
            for (int i = 0; i < stopPositions.length; ++i) {
                positionToStopIndex[stopPositions[i]] = i;
            }
        }

        if (bytecodeInfos != null) { // JIT method
            final int[] bytecodeToTargetCodePositionMap = teleTargetRoutine.bytecodeToTargetCodePositionMap();
            boolean alternate = false;
            int bytecodeIndex = 0; // position in the original bytecode stream.
            for (int row = 0; row < targetInstructionCount; row++) {
                final int bytecodePosition = bytecodeIndex;
                // To check if we're crossing a bytecode boundary in the JITed code, compare the offset of the instruction at the current row with the offset recorded by the JIT
                // for the start of bytecode template.
                final int instructionPosition = _instructions.get(row).position();
                if (bytecodePosition < bytecodeToTargetCodePositionMap.length && instructionPosition == bytecodeToTargetCodePositionMap[bytecodePosition]) {
                    alternate = !alternate;
                    final BytecodeInfo bytecodeInfo = bytecodeInfos[bytecodePosition];
                    if (bytecodeInfo == null) {
                        _rowToTagText[row] = ""; // presumably in the prolog
                    } else {
                        _rowToTagText[row] = bytecodePosition + ": " + bytecodeInfo.bytecode().name();
                        final BytecodeLocation bytecodeLocation = new BytecodeLocation(teleClassMethodActor.classMethodActor(), bytecodePosition);
                        _rowToBytecodeLocation[row] = bytecodeLocation;
                    }
                    do {
                        ++bytecodeIndex;
                    } while (bytecodeIndex < bytecodeToTargetCodePositionMap.length && bytecodeToTargetCodePositionMap[bytecodeIndex] == 0);
                }
                if (alternate) {
                    _rowToBackGroundColor[row] = alternateBackgroundColor;
                } else {
                    _rowToBackGroundColor[row] = backgroundColor;
                }
                if (positionToStopIndex[instructionPosition] >= 0) {
                    // the row is at a stop point
                    _rowToBackGroundColor[row] = _rowToBackGroundColor[row].darker();
                }
            }
        } else {
            for (int row = 0; row < targetInstructionCount; row++) {
                final int stopIndex = positionToStopIndex[_instructions.get(row).position()];
                if (stopIndex >= 0) {
                    // the row is at a stop point
                    _rowToBackGroundColor[row] = stopBackgroundColor;
                    if (teleTargetRoutine instanceof TeleTargetMethod) {
                        final TeleTargetMethod teleTargetMethod = (TeleTargetMethod) teleTargetRoutine;
                        final TargetJavaFrameDescriptor javaFrameDescriptor = teleTargetMethod.getJavaFrameDescriptor(stopIndex);
                        if (javaFrameDescriptor != null) {
                            final BytecodeLocation bytecodeLocation = javaFrameDescriptor;
                            _rowToBytecodeLocation[row] = bytecodeLocation;
                            // TODO (mlvdv) only works for non-inlined calls
                            if (bytecodeLocation.classMethodActor().equals(teleTargetMethod.classMethodActor())) {
                                _rowToCalleeIndex[row] = findCalleeIndex(_bytecodes, bytecodeLocation.bytecodePosition());
                            }
                        }
                    }
                } else {
                    _rowToBackGroundColor[row] = backgroundColor;
                }
            }
        }
        updateStackCache();
    }

    /**
     * Adapter for bytecode scanning that only knows the constant pool index argument of the last method invocation instruction scanned.
     */
    private static final class MethodRefIndexFinder extends BytecodeAdapter  {
        int _methodRefIndex = -1;

        public MethodRefIndexFinder reset() {
            _methodRefIndex = -1;
            return this;
        }

        @Override
        protected void invokestatic(int index) {
            _methodRefIndex = index;
        }

        @Override
        protected void invokespecial(int index) {
            _methodRefIndex = index;
        }

        @Override
        protected void invokevirtual(int index) {
            _methodRefIndex = index;
        }

        @Override
        protected void invokeinterface(int index, int count) {
            _methodRefIndex = index;
        }

        public int methodRefIndex() {
            return _methodRefIndex;
        }
    };

    private final MethodRefIndexFinder _methodRefIndexFinder = new MethodRefIndexFinder();

    /**
     * @param bytecodePosition
     * @return if a call instruction, the index into the constant pool of the called {@link MethodRefConstant}; else -1.
     */
    private int findCalleeIndex(byte[] bytecodes, int bytecodePosition) {
        if (bytecodePosition >= bytecodes.length) {
            return -1;
        }
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(_methodRefIndexFinder.reset());
        bytecodeScanner.scanInstruction(bytecodes, bytecodePosition);
        return _methodRefIndexFinder.methodRefIndex();
    }

    /**
     * Rebuilds the cache of stack information if needed, based on the thread that is the current focus.
     * Identifies for each instruction in the method a stack frame (if any) whose instruction pointer is at the address of the instruction.
     */
    @Override
    protected void updateStackCache() {
        final TeleNativeThread teleNativeThread = inspection().focus().thread();
        if (teleNativeThread == null) {
            return;
        }
        final Sequence<StackFrame> frames = teleNativeThread.frames();

        Arrays.fill(_rowToStackFrameInfo, null);

        // For very deep stacks (e.g. when debugging a metacircular related infinite recursion issue),
        // it's faster to loop over the frames and then only loop over the instructions for each
        // frame related to the target code represented by this viewer.
        int stackPosition = 0;
        for (StackFrame frame : frames) {
            final TargetCodeRegion targetCodeRegion = teleTargetRoutine().targetCodeRegion();
            final boolean isFrameForThisCode = frame.isJavaStackFrame() ?
                            targetCodeRegion.overlaps(frame.targetMethod()) :
                            targetCodeRegion.contains(frame.instructionPointer());
            if (isFrameForThisCode) {
                int row = 0;
                for (TargetCodeInstruction targetCodeInstruction : _instructions) {
                    final Address address = targetCodeInstruction.address();
                    if (address.equals(frame.instructionPointer())) {
                        _rowToStackFrameInfo[row] = new StackFrameInfo(frame, teleNativeThread, stackPosition);
                        break;
                    }
                    row++;
                }
            }
            stackPosition++;
        }
    }

    /**
     * Does the instruction address have a target code breakpoint set in the {@link TeleVM}.
     */
    protected TeleTargetBreakpoint getTargetBreakpointAtRow(int row) {
        return teleVM().getTargetBreakpoint(_instructions.get(row).address());
    }

    protected final Color rowToBackgroundColor(int row) {
        final IndexedSequence<Integer> searchMatchingRows = getSearchMatchingRows();
        if (searchMatchingRows != null) {
            for (int matchingRow : searchMatchingRows) {
                if (row == matchingRow) {
                    return style().searchMatchedBackground();
                }
            }
        }
        return _rowToBackGroundColor[row];
    }

    protected final String rowToTagText(int row) {
        if (_rowToTagText[row] != null) {
            return _rowToTagText[row];
        }
        return "";
    }

    /**
     * @param row an index into the sequence of target code instructions
     * @return the location of the bytecode instruction (possibly inlined from a different method) from which the target code starting here was compiled; null if unavailable.
     */
    protected final BytecodeLocation rowToBytecodeLocation(int row) {
        return _rowToBytecodeLocation[row];
    }

    /**
     * @param row an index into the sequence of target code instructions
     * @return if a call instruction, the index into the constant pool of the operand/callee; else -1.
     */
    protected final int rowToCalleeIndex(int row) {
        return _rowToCalleeIndex[row];
    }

}
