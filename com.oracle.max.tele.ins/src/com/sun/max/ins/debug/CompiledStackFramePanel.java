/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.debug;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.sun.max.*;
import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.value.*;

/**
 * Display panel for a stack frame in the VM created by a compiled method, including
 * a header and a table describing the frame slots.
 * <p>
 * Creates a stack frame header at the top of the panel.
 * <p>
 * Concrete subclasses can be specialized for different kinds of frames, if necessary.
 */
abstract class CompiledStackFramePanel extends InspectorPanel {

    // TODO (mlvdv) create a suite of StackFramePanels to handle the different kinds of frames more usefully.

    private MaxStackFrame.Compiled stackFrame;
    private CompiledStackFrameHeaderPanel headerPanel;

    /**
     * A panel specialized for displaying a stack frame, uses {@link BorderLayout}, and
     * adds a generic frame description at the top.
     */
    public CompiledStackFramePanel(Inspection inspection, MaxStackFrame.Compiled stackFrame) {
        super(inspection, new BorderLayout());
        this.stackFrame = stackFrame;
        this.headerPanel = new CompiledStackFrameHeaderPanel(inspection, stackFrame);
        add(headerPanel, BorderLayout.NORTH);
    }

    public final MaxStackFrame.Compiled stackFrame() {
        return stackFrame;
    }

    public final void setStackFrame(MaxStackFrame stackFrame) {
        final Class<MaxStackFrame.Compiled> type = null;
        this.stackFrame = Utils.cast(type, stackFrame);
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
     * @return a string representing the currently displayed data in the panel,
     * not clipped by scrolling.
     */
    public abstract String getContentString();

    /**
     * A generic panel displaying summary information about a compiled stack frame.
     *
     */
    private final class CompiledStackFrameHeaderPanel extends InspectorPanel {

        // Labels that may need updating
        private final List<InspectorLabel> labels = new ArrayList<InspectorLabel>();

        public CompiledStackFrameHeaderPanel(Inspection inspection, MaxStackFrame.Compiled stackFrame) {
            super(inspection, new SpringLayout());

            final String frameClassName = stackFrame.getClass().getSimpleName();
            final StackBias bias = stackFrame.bias();

            addInspectorLabel(new TextLabel(inspection(), "Size:", "Frame size in bytes (" + frameClassName + ")"));
            final DataLabel.IntAsDecimal sizeLabel = new DataLabel.IntAsDecimal(inspection()) {

                @Override
                public void refresh(boolean force) {
                    final int frameSize = CompiledStackFramePanel.this.stackFrame.layout().frameSize();
                    setValue(frameSize);
                    setWrappedToolTipHtmlText(intToDecimalAndHex(frameSize));
                }
            };
            sizeLabel.setToolTipPrefix("Stack frame size = ");
            sizeLabel.setToolTipSuffix(" bytes");
            addInspectorLabel(sizeLabel);

            addInspectorLabel(new TextLabel(inspection(), "FP:", "Frame pointer (" + frameClassName + ")"));
            final DataLabel.BiasedStackAddressAsHex fpValueLabel = new DataLabel.BiasedStackAddressAsHex(inspection(), bias) {
                @Override
                public void refresh(boolean force) {
                    final Pointer fpValue = CompiledStackFramePanel.this.stackFrame.fp();
                    setValue(fpValue);
                    setWrappedToolTipHtmlText(fpValue.to0xHexString());
                }
            };
            fpValueLabel.setToolTipPrefix("Frame pointer for stack frame<br>address= ");
            addInspectorLabel(fpValueLabel);

            addInspectorLabel(new TextLabel(inspection(), "SP:", "Stack pointer (" + frameClassName + ")"));
            final DataLabel.BiasedStackAddressAsHex spValueLabel = new DataLabel.BiasedStackAddressAsHex(inspection(), bias) {
                @Override
                public void refresh(boolean force) {
                    final Pointer spValue = CompiledStackFramePanel.this.stackFrame.sp();
                    setValue(spValue);
                    setWrappedToolTipHtmlText(spValue.to0xHexString());
                }
            };
            spValueLabel.setToolTipPrefix("Stack pointer for stack frame<br>address= ");
            addInspectorLabel(spValueLabel);

            addInspectorLabel(new TextLabel(inspection(), "IP:", "Instruction pointer for (" + frameClassName + ")"));
            final WordValueLabel ipValueLabel = new WordValueLabel(inspection, ValueMode.WORD, this) {
                @Override
                public Value fetchValue() {
                    return WordValue.from(CompiledStackFramePanel.this.stackFrame.ip());
                }
            };
            ipValueLabel.setToolTipPrefix("Instruction pointer for stack frame<br>address= ");
            addInspectorLabel(ipValueLabel);

            SpringUtilities.makeCompactGrid(this, 2);
        }

        private void addInspectorLabel(InspectorLabel label) {
            add(label);
            labels.add(label);
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
