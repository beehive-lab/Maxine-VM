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
package com.sun.max.ins.debug;

import java.awt.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.LocalVariableTable.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.JavaStackFrameLayout.*;
import com.sun.max.vm.value.*;


final class JavaStackFramePanel extends StackFramePanel<JavaStackFrame> {

    /**
     * Specifies the display mode for the left column of the frame, where the
     * location of the frame slot is identified.
     *
     */
    public enum SlotNameDisplayMode {
        NAME ("Name", "The abstract name of the frame slot"),
        ADDRESS ("Address", "The absolute memory location of the frame slot"),
        OFFSET ("Offset", "The offset in bytes from the base of the frame"),
        BIASED_OFFSET ("Biased offset", "The offset in bytes from the base of the frame, biased in a platform-specific way");

        private final String label;
        private final String description;

        private SlotNameDisplayMode(String label, String description) {
            this.label = label;
            this.description = description;
        }

        String label() {
            return label;
        }

        String description() {
            return description;
        }

        @Override
        public String toString() {
            return label;
        }

        public static final IndexedSequence<SlotNameDisplayMode> VALUES = new ArraySequence<SlotNameDisplayMode>(values());
    }


    final WordValueLabel instructionPointerLabel;
    final Slots slots;
    final TextLabel[] slotLabels;
    final WordValueLabel[] slotValues;
    final TargetMethod targetMethod;
    final CodeAttribute codeAttribute;
    Pointer focusedInstructionPointer;

    JavaStackFramePanel(Inspection inspection, JavaStackFrame javaStackFrame) {
        super(inspection, javaStackFrame);
        final String frameClassName = javaStackFrame.getClass().getSimpleName();
        final Address slotBase = javaStackFrame.slotBase();
        targetMethod = javaStackFrame.targetMethod();
        final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
        codeAttribute = classMethodActor == null ? null : classMethodActor.codeAttribute();
        final int frameSize = javaStackFrame.layout.frameSize();

        final JPanel header = new InspectorPanel(inspection(), new SpringLayout());
        instructionPointerLabel = new WordValueLabel(inspection(), ValueMode.INTEGER_REGISTER, this) {
            @Override
            public Value fetchValue() {
                return WordValue.from(stackFrame.instructionPointer);
            }
        };

        header.add(new TextLabel(inspection(), "Frame size:", frameClassName));
        header.add(new DataLabel.IntAsDecimal(inspection(), frameSize));

        final TextLabel framePointerLabel = new TextLabel(inspection(), "Frame pointer:", frameClassName);
        final TextLabel stackPointerLabel = new TextLabel(inspection(), "Stack pointer:", frameClassName);
        final Pointer framePointer = javaStackFrame.framePointer;
        final Pointer stackPointer = javaStackFrame.stackPointer;
        final StackBias bias = javaStackFrame.bias();

        header.add(framePointerLabel);
        header.add(new DataLabel.BiasedStackAddressAsHex(inspection(), framePointer, bias));
        header.add(stackPointerLabel);
        header.add(new DataLabel.BiasedStackAddressAsHex(inspection(), stackPointer, bias));
        header.add(new TextLabel(inspection(), "Instruction pointer:", frameClassName));
        header.add(instructionPointerLabel);


        SpringUtilities.makeCompactGrid(header, 2);

        final JPanel slotsPanel = new InspectorPanel(inspection(), new SpringLayout());
        slotsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        slots = javaStackFrame.layout.slots();
        slotLabels = new TextLabel[slots.length()];
        slotValues = new WordValueLabel[slots.length()];
        int slotIndex = 0;
        for (Slot slot : slots) {
            final int offset = slot.offset;
            final TextLabel slotLabel = new TextLabel(inspection(), slot.name + ":");
            slotsPanel.add(slotLabel);
            final WordValueLabel slotValue = new WordValueLabel(inspection(), WordValueLabel.ValueMode.INTEGER_REGISTER, this) {
                @Override
                public Value fetchValue() {
                    return new WordValue(maxVM().readWord(slotBase, offset));
                }
            };
            slotsPanel.add(slotValue);

            slotLabels[slotIndex] = slotLabel;
            slotValues[slotIndex] = slotValue;
            ++slotIndex;
        }

        SpringUtilities.makeCompactGrid(slotsPanel, 2);
        refresh(true);

        add(header, BorderLayout.NORTH);
        final JScrollPane slotsScrollPane = new InspectorScrollPane(inspection(), slotsPanel);
        add(slotsScrollPane, BorderLayout.CENTER);
    }

    @Override
    public void instructionPointerFocusChanged(Pointer instructionPointer) {
        if (focusedInstructionPointer == null || !instructionPointer.equals(focusedInstructionPointer)) {
            if (targetMethod.contains(instructionPointer)) {
                focusedInstructionPointer = instructionPointer;
                refresh(true);
            } else {
                focusedInstructionPointer = null;
            }

        }
    }

    @Override
    public void refresh(boolean force) {
        instructionPointerLabel.refresh(force);
        final boolean isTopFrame = stackFrame.isTopFrame();
        int stopIndex = -1;
        if (focusedInstructionPointer == null) {
            final Pointer instructionPointer = stackFrame.instructionPointer;
            final Pointer instructionPointerForStopPosition = isTopFrame ?  instructionPointer.plus(1) :  instructionPointer;
            targetMethod.findClosestStopIndex(instructionPointerForStopPosition);
            if (stopIndex != -1 && isTopFrame) {
                final int stopPosition = targetMethod.stopPosition(stopIndex);
                final int targetCodePosition = targetMethod.targetCodePositionFor(instructionPointer);
                if (targetCodePosition != stopPosition) {
                    stopIndex = -1;
                }
            }
        } else {
            final int position = focusedInstructionPointer.minus(targetMethod.codeStart()).toInt();
            stopIndex = StopPositions.indexOf(targetMethod.stopPositions(), position);
        }

        // Update the color of the slot labels to denote if a reference map indicates they are holding object references:
        final ByteArrayBitMap referenceMap = stopIndex == -1 ? null : targetMethod.frameReferenceMapFor(stopIndex);
        for (int slotIndex = 0; slotIndex < slots.length(); ++slotIndex) {
            final Slot slot = slots.slot(slotIndex);
            final TextLabel slotLabel = slotLabels[slotIndex];
            updateSlotLabel(slot, slotLabel);
            slotValues[slotIndex].refresh(force);
            if (slot.referenceMapIndex != -1) {
                if (referenceMap != null && referenceMap.isSet(slot.referenceMapIndex)) {
                    slotLabel.setForeground(style().wordValidObjectReferenceDataColor());
                } else {
                    slotLabel.setForeground(style().textLabelColor());
                }
            }
        }
    }

    /**
     * Updates the text of a given slot's label based on the check box for specifying use of slot addresses. Also,
     * the tool tip is updated to show the slot's Java source variable name if such a variable name exists.
     *
     * @param slot the slot to update
     * @param slotLabel the label for {@code slot}
     */
    private void updateSlotLabel(Slot slot, TextLabel slotLabel) {
        final String sourceVariableName = sourceVariableName(slot);
        final int offset = slot.offset;
        final StackBias bias = stackFrame.bias();
        //final SlotNameDisplayMode locationDisplayMode = (SlotNameDisplayMode) locationDisplay.getSelectedItem();
        String name;
        switch (StackInspector.globalPreferences(inspection()).slotNameDisplayMode()) {
            case ADDRESS:
                name = stackFrame.slotBase().plus(offset).toHexString();
                break;
            case BIASED_OFFSET:

                name = "+" + stackFrame.biasedOffset(offset);
                break;
            case OFFSET:
                name = "+" + slot.offset;
                break;
            case NAME:
                name = slot.name;
                break;
            default:
                name = "";
                ProgramError.unknownCase();
        }
        slotLabel.setText(name + ":");
        String otherInfo = "";
        if (bias.isFramePointerBiased()) {
            final int biasedOffset = stackFrame.biasedOffset(offset);
            otherInfo = String.format("(%%fp %+d)", biasedOffset);
        }
        slotLabel.setToolTipText(String.format("%+d%s%s", offset, otherInfo, sourceVariableName == null ? "" : " [" + sourceVariableName + "]"));
    }

    /**
     * Gets the Java source variable name (if any) for a given slot.
     *
     * @param slot the slot for which the Java source variable name is being requested
     * @return the Java source name for {@code slot} or null if a name is not available
     */
    private String sourceVariableName(Slot slot) {
        if (targetMethod instanceof JitTargetMethod) {
            final JitTargetMethod jitTargetMethod = (JitTargetMethod) targetMethod;
            final JitStackFrameLayout jitLayout = (JitStackFrameLayout) stackFrame.layout;
            final int bytecodePosition = jitTargetMethod.bytecodePositionFor(stackFrame.instructionPointer);
            if (bytecodePosition != -1 && codeAttribute != null) {
                for (int localVariableIndex = 0; localVariableIndex < codeAttribute.maxLocals(); ++localVariableIndex) {
                    final int localVariableOffset = jitLayout.localVariableOffset(localVariableIndex);
                    if (slot.offset == localVariableOffset) {
                        final Entry entry = codeAttribute.localVariableTable().findLocalVariable(localVariableIndex, bytecodePosition);
                        if (entry != null) {
                            return entry.name(codeAttribute.constantPool()).string;
                        }
                    }
                }
            }
        }
        return null;
    }

}
