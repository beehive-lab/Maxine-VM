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
/*VCSID=2e18e49f-749f-44a7-b4d6-1f5d3e77c7a9*/
package com.sun.max.ins.method;

import java.awt.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.Inspector.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.memory.MemoryInspector.*;
import com.sun.max.ins.memory.MemoryWordInspector.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;

/**
 * Base class for views of disassembled target code for a single method in the {@link TeleVM}.
 *
 * @author Mick Jordan
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class TargetCodeViewer extends CodeViewer implements MemoryInspectable, MemoryWordInspectable {

    @Override
    public  MethodInspector.CodeKind codeKind() {
        return MethodInspector.CodeKind.TARGET_CODE;
    }

    @Override
    public String codeViewerKindName() {
        return "Target Code";
    }

    private final TeleTargetRoutine _teleTargetRoutine;

    protected TeleTargetRoutine teleTargetRoutine() {
        return _teleTargetRoutine;
    }

    private final IndexedSequence<TargetCodeInstruction> _instructions;

    public IndexedSequence<TargetCodeInstruction> instructions() {
        return _instructions;
    }

    private final Color[] _rowToBackGroundColor;
    private final String[] _rowToTagText;
    private final BytecodeLocation[] _rowToBytecodeLocation;

    protected TargetCodeViewer(Inspection inspection, MethodInspector parent, TeleTargetRoutine teleTargetRoutine) {
        super(inspection, parent);

        _teleTargetRoutine = teleTargetRoutine;
        _instructions = teleTargetRoutine.getInstructions();
        _rowToStackFrameInfo = new StackFrameInfo[_instructions.length()];
        _rowToBackGroundColor = new Color[_instructions.length()];
        _rowToTagText = new String[_instructions.length()];
        _rowToBytecodeLocation = new BytecodeLocation[_instructions.length()];
        final Color backgroundColor = style().targetCodeBackgroundColor();
        final Color alternateBackgroundColor = style().targetCodeAlternateBackgroundColor();
        final Color stopBackgroundColor = style().targetCodeStopBackgroundColor();
        final BytecodeInfo[] bytecodeInfos = teleTargetRoutine.bytecodeInfos();
        final boolean[] stopPositionMap;
        final int[] stopIndexMap;
        final int[] stopPositions = teleTargetRoutine.getStopPositions();
        if (stopPositions != null) {
            stopPositionMap = new boolean[teleTargetRoutine.targetCodeRegion().size().toInt()];
            stopIndexMap = new int[teleTargetRoutine.targetCodeRegion().size().toInt()];
            for (int i = 0; i < stopPositions.length; ++i) {
                final int stopPosition = stopPositions[i];
                stopPositionMap[stopPosition] = true;
                stopIndexMap[stopPosition] = i + 1;
            }

        } else {
            stopPositionMap = null;
            stopIndexMap = null;
        }

        if (bytecodeInfos != null) { // JIT method
            final int[] bytecodeToTargetCodePositionMap = teleTargetRoutine.bytecodeToTargetCodePositionMap();
            boolean alternate = false;
            int bytecodePosition = 0; // position in the original bytecode stream.
            for (int row = 0; row < _instructions.length(); row++) {
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
                        final BytecodeLocation bytecodeLocation = new BytecodeLocation(teleTargetRoutine.getTeleClassMethodActor().classMethodActor(), bytecodePosition);
                        _rowToBytecodeLocation[row] = bytecodeLocation;
                    }
                    do {
                        ++bytecodePosition;
                    } while (bytecodePosition < bytecodeToTargetCodePositionMap.length && bytecodeToTargetCodePositionMap[bytecodePosition] == 0);
                }
                if (alternate) {
                    _rowToBackGroundColor[row] = alternateBackgroundColor;
                } else {
                    _rowToBackGroundColor[row] = backgroundColor;
                }
                if (stopPositionMap != null && stopPositionMap[instructionPosition]) {
                    _rowToBackGroundColor[row] = _rowToBackGroundColor[row].darker();
                }
            }
        } else {
            for (int row = 0; row < _instructions.length(); row++) {
                final TargetCodeInstruction targetCodeInstruction = _instructions.get(row);
                if (stopPositionMap != null && stopPositionMap[targetCodeInstruction.position()]) {
                    _rowToBackGroundColor[row] = stopBackgroundColor;
                } else {
                    _rowToBackGroundColor[row] = backgroundColor;
                }
                if (stopIndexMap != null && teleTargetRoutine instanceof TeleTargetMethod) {
                    final TeleTargetMethod teleTargetMethod = (TeleTargetMethod) teleTargetRoutine;
                    final int stopIndex = stopIndexMap[targetCodeInstruction.position()] - 1;
                    if (stopIndex != -1) {
                        final TargetJavaFrameDescriptor javaFrameDescriptor = teleTargetMethod.getJavaFrameDescriptor(stopIndex);
                        if (javaFrameDescriptor != null) {
                            _rowToBytecodeLocation[row] = javaFrameDescriptor.bytecodeLocation();
                        }
                    }
                }
            }
        }
        updateStackCache();
    }

    protected TargetCodeInstruction targetCodeInstructionAt(int row) {
        return _instructions.get(row);
    }

    /**
     * Return the row containing the instruction at an address.
     */
    protected int addressToRow(Address instructionPointer) {
        int row = 0;
        for (TargetCodeInstruction targetCodeInstruction : _instructions) {
            final Address address = targetCodeInstruction.address();
            if (address.equals(instructionPointer)) {
                return row;
            }
            row++;
        }
        return -1;
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
        return teleVM().teleProcess().targetBreakpointFactory().getNonTransientBreakpointAt(targetCodeInstructionAt(row).address());
    }

    protected final Color rowToBackgroundColor(int row) {
        return _rowToBackGroundColor[row];
    }

    protected final String rowToTagText(int row) {
        if (_rowToTagText[row] != null) {
            return _rowToTagText[row];
        }
        return "";
    }

    protected final BytecodeLocation rowToBytecodeLocation(int row) {
        if (_rowToBytecodeLocation != null) {
            return _rowToBytecodeLocation[row];
        }
        return null;
    }

    public void makeMemoryInspector() {
        final TargetCodeRegion targetCodeRegion = _teleTargetRoutine.targetCodeRegion();
        MemoryInspector.create(inspection(), Residence.INTERNAL, targetCodeRegion.start(), targetCodeRegion.size().toInt(), 1, 8);
    }

    public InspectorAction getMemoryInspectorAction() {
        return new InspectorAction(inspection(), "Inspect Memory") {
            @Override
            protected void procedure() {
                makeMemoryInspector();
            }
        };
    }

    public void makeMemoryWordInspector() {
        final TargetCodeRegion targetCodeRegion = _teleTargetRoutine.targetCodeRegion();
        MemoryWordInspector.create(inspection(), Residence.INTERNAL, targetCodeRegion.start(), targetCodeRegion.size().toInt());
    }

    public InspectorAction getMemoryWordInspectorAction() {
        return new InspectorAction(inspection(), "Inspect Memory Words") {
            @Override
            protected void procedure() {
                makeMemoryWordInspector();
            }
        };
    }

}
