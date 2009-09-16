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
import com.sun.max.vm.stack.*;


final class AdapterStackFramePanel extends StackFramePanel<AdapterStackFrame> {

    private final AppendableSequence<InspectorLabel> labels = new ArrayListSequence<InspectorLabel>(10);

    public AdapterStackFramePanel(Inspection inspection, AdapterStackFrame adapterStackFrame) {
        super(inspection, adapterStackFrame);
        final String frameClassName = adapterStackFrame.getClass().getSimpleName();
        final JPanel header = new InspectorPanel(inspection(), new SpringLayout());
        addLabel(header, new TextLabel(inspection(), "Frame size:", frameClassName));
        addLabel(header, new DataLabel.IntAsDecimal(inspection(), adapterStackFrame.layout.frameSize()));
        addLabel(header, new TextLabel(inspection(), "Frame pointer:", frameClassName));
        addLabel(header, new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, adapterStackFrame.framePointer, this));
        addLabel(header, new TextLabel(inspection(), "Stack pointer:", frameClassName));
        addLabel(header, new DataLabel.AddressAsHex(inspection(), adapterStackFrame.stackPointer));
        addLabel(header, new TextLabel(inspection(), "Instruction pointer:", frameClassName));
        addLabel(header, new WordValueLabel(inspection(), ValueMode.INTEGER_REGISTER, adapterStackFrame.instructionPointer, this));
        SpringUtilities.makeCompactGrid(header, 2);

        add(header, BorderLayout.NORTH);
        add(new InspectorPanel(inspection()), BorderLayout.CENTER);
    }

    private void addLabel(JPanel panel, InspectorLabel label) {
        panel.add(label);
        labels.append(label);
    }

    @Override
    public void refresh(boolean force) {
        for (InspectorLabel label : labels) {
            label.refresh(force);
        }
    }

    @Override
    public void redisplay() {
        for (InspectorLabel label : labels) {
            label.redisplay();
        }
    }
}
