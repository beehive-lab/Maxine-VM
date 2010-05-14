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
import java.util.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.jni.*;

/**
 * @author Bernd Mathiske
 */
public final class TargetJavaFrameDescriptorInspector extends Inspector {

    private static final int TRACE_VALUE = 2;

    private static final int MAX_BYTE_CODE_BITS = 20;

    private static Long makeKey(TargetJavaFrameDescriptor javaFrameDescriptor) {
        if (javaFrameDescriptor == null) {
            return -1L;
        }
        return (MethodID.fromMethodActor(javaFrameDescriptor.classMethodActor).asAddress().toLong() << MAX_BYTE_CODE_BITS) | javaFrameDescriptor.bytecodePosition;
    }

    private static Map<Long, TargetJavaFrameDescriptorInspector> inspectors =
        new Hashtable<Long, TargetJavaFrameDescriptorInspector>();

    /**
     * Display and highlight a target Java frame descriptor inspector for the frame..
     * @return The inspector, possibly newly created.
     */
    public static TargetJavaFrameDescriptorInspector make(Inspection inspection, TargetJavaFrameDescriptor javaFrameDescriptor, MaxCompiledMethod compiledMethod) {
        final Long key = makeKey(javaFrameDescriptor);
        TargetJavaFrameDescriptorInspector inspector = inspectors.get(key);
        if (inspector == null) {
            inspector = new TargetJavaFrameDescriptorInspector(inspection, javaFrameDescriptor, compiledMethod, key);
            inspectors.put(key, inspector);
        }
        return inspector;
    }

    private final MaxCompiledMethod compiledMethod;
    private final TargetJavaFrameDescriptor javaFrameDescriptor;
    private final Long key;

    private TargetJavaFrameDescriptorInspector(Inspection inspection, TargetJavaFrameDescriptor javaFrameDescriptor, MaxCompiledMethod compiledMethod, Long key) {
        super(inspection);
        this.javaFrameDescriptor = javaFrameDescriptor;
        this.compiledMethod = compiledMethod;
        this.key = key;
        final InspectorFrame frame = createFrame(true);
        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));
    }

    private String shortString(BytecodeLocation bytecodeLocation) {
        return bytecodeLocation.classMethodActor.name.toString() + " @ " + bytecodeLocation.bytecodePosition;
    }

    private JPanel createDescriptorPanel(TargetJavaFrameDescriptor descriptor) {
        final JPanel panel = new InspectorPanel(inspection());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        final BytecodeLocation bytecodeLocation = descriptor;
        final TextLabel bytecodeLocationLabel = new TextLabel(inspection(), shortString(bytecodeLocation));
        bytecodeLocationLabel.setToolTipText(bytecodeLocation.toString());
        panel.add(bytecodeLocationLabel);

        final String sourceFileName = bytecodeLocation.sourceFileName();
        final int lineNumber = bytecodeLocation.sourceLineNumber();
        if (sourceFileName != null || lineNumber >= 0) {
            String source = (sourceFileName == null) ? "?" : sourceFileName;
            if (lineNumber >= 0) {
                source += " : " + lineNumber;
            }
            final TextLabel sourceLocationLabel = new TextLabel(inspection(), source);
            sourceLocationLabel.setToolTipText(sourceFileName);
            sourceLocationLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    inspection().viewSourceExternally(bytecodeLocation);
                }
            });
            panel.add(sourceLocationLabel);
        }

        final CodeAttribute codeAttribute = bytecodeLocation.classMethodActor.codeAttribute();
        for (int i = 0; i < descriptor.locals.length; i++) {
            String local = "local #" + i;
            final LocalVariableTable.Entry entry = codeAttribute.localVariableTable().findLocalVariable(i, bytecodeLocation.bytecodePosition);
            if (entry != null) {
                local += ": " + entry.name(codeAttribute.constantPool);
            }
            local += " = " + compiledMethod.targetLocationToString(descriptor.locals[i]);
            panel.add(new TextLabel(inspection(), local));
        }
        for (int i = 0; i < descriptor.stackSlots.length; i++) {
            String stackSlot = "stack #" + i;
            stackSlot += " = " + compiledMethod.targetLocationToString(descriptor.stackSlots[i]);
            panel.add(new TextLabel(inspection(), stackSlot));
        }
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, style().defaultBorderColor()));
        return panel;
    }

    @Override
    public String getTextForTitle() {
        if (javaFrameDescriptor != null) {
            return shortString(javaFrameDescriptor);
        }
        return null;
    }

    @Override
    protected void createView() {
        if (javaFrameDescriptor != null) {
            final JPanel panel = new InspectorPanel(inspection());
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            TargetJavaFrameDescriptor descriptor = javaFrameDescriptor;
            do {
                panel.add(createDescriptorPanel(descriptor), 0);
                descriptor = descriptor.parent();
            } while (descriptor != null);
            setContentPane(panel);
        }
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing for " + getTitle());
        inspectors.remove(key);
        super.inspectorClosing();
    }

}
