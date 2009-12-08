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
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.value.*;

/**
 * Display panels for VM Java stack frames.
 * <br>
 * Creates a generic stack frame header at the top of the panel, leaving it to subclasses to add a view of the
 * stack frame contents.
 *
 * @author Michael Van De Vanter
 */
abstract class JavaStackFramePanel<StackFrame_Type extends JavaStackFrame> extends InspectorPanel {

    // TODO (mlvdv) create a suite of StackFramePanels to handle the different kinds of frames more usefully.

    private StackFrame_Type stackFrame;
    private JavaStackFrameHeaderPanel headerPanel;

    /**
     * A panel specialized for displaying a stack frame, uses {@link BorderLayout}, and
     * adds a generic frame description at the top.
     */
    public JavaStackFramePanel(Inspection inspection, StackFrame_Type stackFrame) {
        super(inspection, new BorderLayout());
        this.stackFrame = stackFrame;
        this.headerPanel = new JavaStackFrameHeaderPanel(inspection, stackFrame);
        add(headerPanel, BorderLayout.NORTH);
    }

    public final StackFrame_Type stackFrame() {
        return stackFrame;
    }

    public final void setStackFrame(StackFrame stackFrame) {
        final Class<StackFrame_Type> type = null;
        this.stackFrame = StaticLoophole.cast(type, stackFrame);
        refresh(true);
    }

    public void instructionPointerFocusChanged(Pointer instructionPointer) {
    }

    @Override
    public void refresh(boolean force) {
        headerPanel.refresh(force);
    }

    @Override
    public void redisplay() {
        headerPanel.redisplay();
    }

    /**
     * A generic panel displaying summary information about a Java stack frame.
     *
     */
    private final class JavaStackFrameHeaderPanel extends InspectorPanel {

        // Labels that may need updating
        private final AppendableSequence<InspectorLabel> labels = new LinkSequence<InspectorLabel>();

        public JavaStackFrameHeaderPanel(Inspection inspection, StackFrame_Type stackFrame) {
            super(inspection, new SpringLayout());

            final String frameClassName = stackFrame.getClass().getSimpleName();
            final StackBias bias = stackFrame.bias();

            addInspectorLabel(new TextLabel(inspection(), "Size:", "Frame size in bytes (" + frameClassName + ")"));
            addInspectorLabel(new DataLabel.IntAsDecimal(inspection()) {

                @Override
                public void refresh(boolean force) {
                    setValue(JavaStackFramePanel.this.stackFrame.layout.frameSize());
                }
            });

            addInspectorLabel(new TextLabel(inspection(), "FP:", "Frame pointer (" + frameClassName + ")"));
            addInspectorLabel(new DataLabel.BiasedStackAddressAsHex(inspection(), bias) {
                @Override
                public void refresh(boolean force) {
                    setValue(JavaStackFramePanel.this.stackFrame.framePointer);
                }
            });

            addInspectorLabel(new TextLabel(inspection(), "SP:", "Stack pointer (" + frameClassName + ")"));
            addInspectorLabel(new DataLabel.BiasedStackAddressAsHex(inspection(), bias) {
                @Override
                public void refresh(boolean force) {
                    setValue(JavaStackFramePanel.this.stackFrame.framePointer);
                }
            });

            addInspectorLabel(new TextLabel(inspection(), "IP:", "Instruction pointer (" + frameClassName + ")"));
            addInspectorLabel(new WordValueLabel(inspection, ValueMode.WORD, this) {
                @Override
                public Value fetchValue() {
                    return WordValue.from(JavaStackFramePanel.this.stackFrame.instructionPointer);
                }
            });

            SpringUtilities.makeCompactGrid(this, 2);
        }

        private void addInspectorLabel(InspectorLabel label) {
            add(label);
            labels.append(label);
        }

        @Override
        public void redisplay() {
            for (InspectorLabel label : labels) {
                label.redisplay();
            }
        }

        @Override
        public void refresh(boolean force) {
            for (InspectorLabel label : labels) {
                label.refresh(force);
            }
        }
    }
}
