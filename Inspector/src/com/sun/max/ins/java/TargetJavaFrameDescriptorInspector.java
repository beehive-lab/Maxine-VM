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
package com.sun.max.ins.java;

import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public final class TargetJavaFrameDescriptorInspector extends UniqueInspector<TargetJavaFrameDescriptorInspector> {

    private final TargetJavaFrameDescriptor _javaFrameDescriptor;
    private final String _framePointer;
    private final int _wordSize;

    private static final int MAX_BYTE_CODE_BITS = 20;

    private static long subject(TargetJavaFrameDescriptor javaFrameDescriptor) {
        if (javaFrameDescriptor == null) {
            return -1L;
        }
        final BytecodeLocation bytecodeLocation = javaFrameDescriptor.bytecodeLocation();
        return (MethodID.fromMethodActor(bytecodeLocation.classMethodActor()).asAddress().toLong() << MAX_BYTE_CODE_BITS) | bytecodeLocation.position();
    }

    private TargetJavaFrameDescriptorInspector(Inspection inspection, Residence residence, TargetJavaFrameDescriptor javaFrameDescriptor, TargetABI abi) {
        super(inspection, residence, LongValue.from(subject(javaFrameDescriptor)));
        _javaFrameDescriptor = javaFrameDescriptor;
        _framePointer = TeleIntegerRegisters.symbolizer(teleVM().vmConfiguration()).fromValue(abi.framePointer().value()).toString();
        _wordSize = teleVM().vmConfiguration().platform().processorKind().dataModel().wordWidth().numberOfBytes();
        createFrame(null);
    }

    /**
     * Display and highlight a target Java frame descriptor inspector for the frame..
     * @return The inspector, possibly newly created.
     */
    public static TargetJavaFrameDescriptorInspector make(Inspection inspection, TargetJavaFrameDescriptor javaFrameDescriptor, TargetABI abi) {
        final UniqueInspector.Key<TargetJavaFrameDescriptorInspector> key = UniqueInspector.Key.create(TargetJavaFrameDescriptorInspector.class, subject(javaFrameDescriptor));
        TargetJavaFrameDescriptorInspector inspector = UniqueInspector.find(inspection, key);
        if (inspector == null) {
            inspector = new TargetJavaFrameDescriptorInspector(inspection, Residence.INTERNAL, javaFrameDescriptor, abi);
        }
        inspector.highlight();
        return inspector;
    }

    private String shortString(BytecodeLocation bytecodeLocation) {
        return bytecodeLocation.classMethodActor().name().toString() + " @ " + bytecodeLocation.position();
    }

    private String targetLocationToString(TargetLocation targetLocation) {
        switch (targetLocation.tag()) {
            case INTEGER_REGISTER: {
                final TargetLocation.IntegerRegister integerRegister = (TargetLocation.IntegerRegister) targetLocation;
                return TeleIntegerRegisters.symbolizer(teleVM().vmConfiguration()).fromValue(integerRegister.index()).toString();
            }
            case FLOATING_POINT_REGISTER: {
                final TargetLocation.FloatingPointRegister floatingPointRegister = (TargetLocation.FloatingPointRegister) targetLocation;
                return TeleFloatingPointRegisters.symbolizer(teleVM().vmConfiguration()).fromValue(floatingPointRegister.index()).toString();
            }
            case LOCAL_STACK_SLOT: {
                final TargetLocation.LocalStackSlot localStackSlot = (TargetLocation.LocalStackSlot) targetLocation;
                return _framePointer + "[" + (localStackSlot.index() * _wordSize) + "]";
            }
            default: {
                return targetLocation.toString();
            }
        }
    }

    private JPanel createDescriptorPanel(TargetJavaFrameDescriptor descriptor) {
        final JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(style().defaultBackgroundColor());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        final BytecodeLocation bytecodeLocation = descriptor.bytecodeLocation();
        final JLabel bytecodeLocationLabel = new JLabel(shortString(bytecodeLocation));
        bytecodeLocationLabel.setToolTipText(bytecodeLocation.toString());
        panel.add(bytecodeLocationLabel);

        final String sourceFileName = bytecodeLocation.sourceFileName();
        final int lineNumber = bytecodeLocation.sourceLineNumber();
        if (sourceFileName != null || lineNumber >= 0) {
            String source = (sourceFileName == null) ? "?" : sourceFileName;
            if (lineNumber >= 0) {
                source += " : " + lineNumber;
            }
            final JLabel sourceLocationLabel = new JLabel(source);
            sourceLocationLabel.setToolTipText(sourceFileName);
            sourceLocationLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    inspection().viewSourceExternally(bytecodeLocation);
                }
            });
            panel.add(sourceLocationLabel);
        }

        final CodeAttribute codeAttribute = bytecodeLocation.classMethodActor().codeAttribute();
        for (int i = 0; i < descriptor.locals().length; i++) {
            String local = "local #" + i;
            final LocalVariableTable.Entry entry = codeAttribute.localVariableTable().findLocalVariable(i, bytecodeLocation.position());
            if (entry != null) {
                local += ": " + entry.name(codeAttribute.constantPool());
            }
            local += " = " + targetLocationToString(descriptor.locals()[i]);
            panel.add(new JLabel(local));
        }
        for (int i = 0; i < descriptor.stackSlots().length; i++) {
            String stackSlot = "stack #" + i;
            stackSlot += " = " + targetLocationToString(descriptor.stackSlots()[i]);
            panel.add(new JLabel(stackSlot));
        }
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, style().defaultBorderColor()));
        return panel;
    }

    @Override
    public String getTextForTitle() {
        if (_javaFrameDescriptor != null) {
            return shortString(_javaFrameDescriptor.bytecodeLocation());
        }
        return null;
    }

    @Override
    protected synchronized void createView(long epoch) {
        if (_javaFrameDescriptor != null) {
            final JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            TargetJavaFrameDescriptor descriptor = _javaFrameDescriptor;
            do {
                panel.add(createDescriptorPanel(descriptor), 0);
                descriptor = descriptor.parent();
            } while (descriptor != null);
            frame().setContentPane(panel);
        }
    }

    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }

}
