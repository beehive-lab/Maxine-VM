/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
    public static TargetJavaFrameDescriptorInspector make(Inspection inspection, TargetJavaFrameDescriptor javaFrameDescriptor, MaxCompiledCode compiledCode) {
        final Long key = makeKey(javaFrameDescriptor);
        TargetJavaFrameDescriptorInspector inspector = inspectors.get(key);
        if (inspector == null) {
            inspector = new TargetJavaFrameDescriptorInspector(inspection, javaFrameDescriptor, compiledCode, key);
            inspectors.put(key, inspector);
        }
        return inspector;
    }

    private final MaxCompiledCode compiledCode;
    private final TargetJavaFrameDescriptor javaFrameDescriptor;
    private final Long key;

    private TargetJavaFrameDescriptorInspector(Inspection inspection, TargetJavaFrameDescriptor javaFrameDescriptor, MaxCompiledCode compiledCode, Long key) {
        super(inspection);
        this.javaFrameDescriptor = javaFrameDescriptor;
        this.compiledCode = compiledCode;
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
            local += " = " + compiledCode.machineCodeLocationToString(descriptor.locals[i]);
            panel.add(new TextLabel(inspection(), local));
        }
        for (int i = 0; i < descriptor.stackSlots.length; i++) {
            String stackSlot = "stack #" + i;
            stackSlot += " = " + compiledCode.machineCodeLocationToString(descriptor.stackSlots[i]);
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
