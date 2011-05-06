/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.cri.ci.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxMachineCode.InstructionMap;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;


/**
 * A display showing detailed information about he currently selected location in compiled code,
 * starting with Java frame descriptors, if available.
 *
 * @author Michael Van De Vanter
 */
public final class CodeLocationInspector extends Inspector<CodeLocationInspector> {

    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.CODE_LOCATION;
    private static final String SHORT_NAME = "Code Location";
    private static final String LONG_NAME = "CodeLocation Inspector";
    private static final String GEOMETRY_SETTINGS_KEY = "codeLocationInspectorGeometry";


    public static final class CodeLocationViewManager extends AbstractSingletonViewManager<CodeLocationInspector> {

        protected CodeLocationViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        @Override
        protected CodeLocationInspector createView(Inspection inspection) {
            return new CodeLocationInspector(inspection);
        }

    }

    private static CodeLocationViewManager viewManager = null;

    public static CodeLocationViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new CodeLocationViewManager(inspection);
        }
        return viewManager;
    }

    private MaxCodeLocation codeLocation = null;
    private MaxCompiledCode compiledCode = null;
    private CiFrame frames = null;


    private final Rectangle originalFrameGeometry;
    private final InspectorPanel nullPanel;

    protected CodeLocationInspector(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        Trace.begin(1,  tracePrefix() + " initializing");

        nullPanel = new InspectorPanel(inspection);
        updateCodeLocation(focus().codeLocation());

        final InspectorFrame frame = createFrame(true);

        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        originalFrameGeometry = getGeometry();
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    protected Rectangle defaultGeometry() {
        return originalFrameGeometry;
    }

    @Override
    public String getTextForTitle() {
        final StringBuilder sb = new StringBuilder(viewManager.shortName() + ": ");
        if (codeLocation == null) {
            sb.append("<none>");
        } else {
            if (codeLocation.hasAddress()) {
                sb.append(codeLocation.address().to0xHexString());
                sb.append(" ");
            }
            if (compiledCode != null) {
                sb.append(inspection().nameDisplay().extremelyShortName(compiledCode));
                if (codeLocation.hasTeleClassMethodActor()) {
                    sb.append(" bci=").append(codeLocation.bytecodePosition());
                }
            } else if (codeLocation.hasTeleClassMethodActor()) {
                sb.append(inspection().nameDisplay().veryShortName(codeLocation.teleClassMethodActor()));
                sb.append(" bci=").append(codeLocation.bytecodePosition());
            }
        }
        return sb.toString();
    }

    @Override
    protected void createView() {
        if (frames != null) {
            final JPanel panel = new InspectorPanel(inspection());
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            CiFrame frame = frames;
            do {
                panel.add(createFramePanel(frame), 0);
                frame = frame.caller();
            } while (frame != null);
            setContentPane(panel);
        } else {
            setContentPane(nullPanel);
        }
        setTitle();
    }

    @Override
    protected void refreshState(boolean force) {
        updateCodeLocation(codeLocation);
        reconstructView();
    }

    @Override
    public void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative) {
        updateCodeLocation(codeLocation);
        reconstructView();
    }

    private void updateCodeLocation(MaxCodeLocation codeLocation) {
        this.codeLocation = codeLocation;
        compiledCode = null;
        frames = null;
        final Address instructionAddress = codeLocation.address();
        if (instructionAddress != null && !instructionAddress.isZero()) {
            compiledCode = vm().codeCache().findCompiledCode(instructionAddress);
            if (compiledCode != null) {
                final InstructionMap instructionMap = compiledCode.getInstructionMap();
                final int instructionIndex = instructionMap.findInstructionIndex(codeLocation.address());
                if (instructionIndex >= 0) {
                    this.frames = instructionMap.bytecodeFrames(instructionIndex);
                }
            }
        }
    }


    private String shortString(CiCodePos codePos) {
        return codePos.method.name() + " @ " + codePos.bci;
    }

    private JPanel createFramePanel(CiFrame frame) {
        final JPanel panel = new InspectorPanel(inspection());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        final CiCodePos codePos = frame;
        final TextLabel bytecodeLocationLabel = new TextLabel(inspection(), shortString(codePos));
        bytecodeLocationLabel.setToolTipText(codePos.toString());
        panel.add(bytecodeLocationLabel);

        ClassMethodActor method = (ClassMethodActor) codePos.method;
        final String sourceFileName = method.holder().sourceFileName;
        final int lineNumber = method.sourceLineNumber(codePos.bci);
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
                    inspection().viewSourceExternally(codePos);
                }
            });
            panel.add(sourceLocationLabel);
        }

        final CodeAttribute codeAttribute = method.codeAttribute();
        for (int i = 0; i < frame.numLocals; i++) {
            String local = "local #" + i;
            final LocalVariableTable.Entry entry = codeAttribute.localVariableTable().findLocalVariable(i, codePos.bci);
            if (entry != null) {
                local += ": " + entry.name(codeAttribute.cp);
            }
            local += " = " + frame.getLocalValue(i);
            panel.add(new TextLabel(inspection(), local));
        }
        for (int i = 0; i < frame.numStack; i++) {
            String stackSlot = "stack #" + i;
            stackSlot += " = " + frame.getStackValue(i);
            panel.add(new TextLabel(inspection(), stackSlot));
        }
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, style().defaultBorderColor()));
        return panel;
    }


}
