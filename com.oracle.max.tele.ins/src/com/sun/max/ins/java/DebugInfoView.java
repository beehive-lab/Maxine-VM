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

import static com.sun.max.platform.Platform.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import com.sun.cri.ci.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.stack.*;


/**
 * A view that displays {@linkplain CiDebugInfo debug information} (if available) about the currently selected location in compiled code.
 */
public final class DebugInfoView extends AbstractView<DebugInfoView> {

    private static final int TRACE_VALUE = 1;
    private static final ViewKind VIEW_KIND = ViewKind.DEBUG_INFO;
    private static final String SHORT_NAME = "Debug Info";
    private static final String LONG_NAME = "Debug Info View";
    private static final String GEOMETRY_SETTINGS_KEY = "debugInfoViewGeometry";


    public static final class DebugInfoViewManager extends AbstractSingletonViewManager<DebugInfoView> {

        protected DebugInfoViewManager(Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        }

        @Override
        protected DebugInfoView createView(Inspection inspection) {
            return new DebugInfoView(inspection);
        }

    }

    private static DebugInfoViewManager viewManager = null;

    public static DebugInfoViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new DebugInfoViewManager(inspection);
        }
        return viewManager;
    }

    private MaxCodeLocation codeLocation = null;
    private MaxCompilation compiledCode = null;
    private CiDebugInfo debugInfo = null;

    private final MatteBorder frameBorder;

    private final Rectangle originalFrameGeometry;
    private final InspectorPanel nullPanel;
    private final InspectorPanel simplePanel;
    private final PlainLabel simplePanelLabel;

    protected DebugInfoView(Inspection inspection) {
        super(inspection, VIEW_KIND, GEOMETRY_SETTINGS_KEY);
        Trace.begin(1,  tracePrefix() + " initializing");

        frameBorder = BorderFactory.createMatteBorder(1, 0, 1, 0, inspection.preference().style().defaultBorderColor());


        nullPanel = new InspectorPanel(inspection, new BorderLayout());
        nullPanel.add(new PlainLabel(inspection, inspection.nameDisplay().unavailableDataShortText()), BorderLayout.PAGE_START);

        simplePanel = new InspectorPanel(inspection, new BorderLayout());
        simplePanelLabel = new PlainLabel(inspection, "");
        simplePanel.add(simplePanelLabel, BorderLayout.PAGE_START);
        simplePanel.setBorder(frameBorder);

        updateCodeLocation(focus().codeLocation());

        final InspectorFrame frame = createFrame(true);

        frame.makeMenu(MenuKind.DEFAULT_MENU).add(defaultMenuItems(MenuKind.DEFAULT_MENU));

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        originalFrameGeometry = getGeometry();
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    public String getTextForTitle() {
        final StringBuilder sb = new StringBuilder(viewManager.shortName() + ": ");
        if (codeLocation == null) {
            sb.append("<none>");
        } else if (codeLocation.hasTeleClassMethodActor()) {
            final TeleClassMethodActor teleClassMethodActor = codeLocation.teleClassMethodActor();
            sb.append(teleClassMethodActor.classMethodActor().holder().simpleName()).append(".");
            sb.append(inspection().nameDisplay().veryShortName(teleClassMethodActor));
        } else if (codeLocation.hasAddress()) {
            MaxExternalCode externalCode = vm().codeCache().findExternalCode(codeLocation.address());
            if (externalCode == null) {
                sb.append("<native>");
            } else {
                sb.append(externalCode.entityName());
            }
        }
        return sb.toString();
    }

    @Override
    protected void createViewContent() {
        // TODO (mlvdv) convert this to some kind of tabular display with useful
        // functionality (inspection, navigation, etc.) over each cell.
        if (codeLocation == null) {
            setContentPane(nullPanel);
        } else if (debugInfo == null) {
            simplePanelLabel.setText(inspection().nameDisplay().shortName(codeLocation));
            setContentPane(simplePanel);
        } else {
            final JPanel panel = new InspectorPanel(inspection());
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            // TODO: add register and frame reference map information to panel
            // debugInfo.registerRefMap ...
            // debugInfo.frameRefMap ...

            CiFrame frame = debugInfo.frame();
            while (frame != null) {
                panel.add(createFramePanel(frame), 0);
                frame = frame.caller();
            }

            panel.add(createRefMapsPanel(debugInfo), 0);

            simplePanelLabel.setText(inspection().nameDisplay().shortName(codeLocation));
            panel.add(simplePanel, 0);
            setContentPane(new InspectorScrollPane(inspection(), panel));
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
        if (codeLocation != null) {
            compiledCode = codeLocation.compiledCode();
            debugInfo = codeLocation.debugInfo();
        }
    }

    private String shortString(CiCodePos codePos) {
        return codePos.method.name() + "() bci=" + codePos.bci;
    }

    private JPanel createRefMapsPanel(CiDebugInfo info) {
        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        final JPanel headerPanel = new InspectorPanel(inspection(), new FlowLayout(FlowLayout.LEADING));
        headerPanel.add(new TextLabel(inspection(), "Reference Map"));
        panel.add(headerPanel, BorderLayout.PAGE_START);

        final JPanel slotsPanel = new JPanel();
        slotsPanel.setLayout(new BoxLayout(slotsPanel, BoxLayout.PAGE_AXIS));

        if (info.hasRegisterRefMap()) {
            CiBitMap bm = info.registerRefMap;
            CiArchitecture arch = target().arch;
            for (int reg = bm.nextSetBit(0); reg >= 0; reg = bm.nextSetBit(reg + 1)) {
                slotsPanel.add(new TextLabel(inspection(), arch.registers[reg].asValue(CiKind.Object).toString()));
            }
        }

        if (info.hasStackRefMap()) {
            CiBitMap bm = info.frameRefMap;
            for (int i = bm.nextSetBit(0); i >= 0; i = bm.nextSetBit(i + 1)) {
                VMFrameLayout layout = compiledCode.frameLayout();
                CiRegister fp = layout.framePointerReg();
                int refMapOffset = i * VMFrameLayout.STACK_SLOT_SIZE;
                int fpOffset = refMapOffset + layout.frameReferenceMapOffset();
                String slot = new CiAddress(CiKind.Object, fp.asValue(), fpOffset).toString();
                slotsPanel.add(new TextLabel(inspection(), slot));
            }
        }

        if (slotsPanel.getComponentCount() != 0) {
            panel.add(slotsPanel, BorderLayout.LINE_START);
        }
        panel.setBorder(frameBorder);
        return panel;
    }

    private JPanel createFramePanel(CiFrame frame) {
        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        final JPanel headerPanel = new InspectorPanel(inspection(), new FlowLayout(FlowLayout.LEADING));

        final CiCodePos codePos = frame;
        final PlainLabel bytecodeLocationLabel = new PlainLabel(inspection(), shortString(codePos));
        final String methodToolTipText = CiUtil.appendLocation(new StringBuilder(10), codePos.method, codePos.bci).toString();
        bytecodeLocationLabel.setToolTipText(methodToolTipText);
        headerPanel.add(bytecodeLocationLabel);

        ClassMethodActor method = (ClassMethodActor) codePos.method;
        String sourceFileName = method.holder().sourceFileName;
        final int lineNumber = method.sourceLineNumber(codePos.bci);
        if (sourceFileName != null || lineNumber >= 0) {
            if (sourceFileName == null) {
                sourceFileName = inspection().nameDisplay().unavailableDataShortText();
            }
            final String labelText = lineNumber >= 0 ? String.valueOf(lineNumber) : inspection().nameDisplay().unavailableDataShortText();
            final PlainLabel sourceLocationLabel = new PlainLabel(inspection(), " line=" + labelText);
            sourceLocationLabel.setToolTipText(methodToolTipText);
            sourceLocationLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    inspection().viewSourceExternally(codePos);
                }
            });
            headerPanel.add(sourceLocationLabel);
        }
        panel.add(headerPanel, BorderLayout.PAGE_START);

        if (frame.values.length > 0) {
            final JPanel slotsPanel = new JPanel();
            slotsPanel.setLayout(new BoxLayout(slotsPanel, BoxLayout.PAGE_AXIS));
            final CodeAttribute codeAttribute = method.codeAttribute();
            for (int i = 0; i < frame.numLocals; i++) {
                String local = "local #" + i;
                final LocalVariableTable.Entry entry = codeAttribute.localVariableTable().findLocalVariable(i, codePos.bci);
                if (entry != null) {
                    local += ": " + entry.name(codeAttribute.cp);
                }
                local += " = " + frame.getLocalValue(i);
                slotsPanel.add(new TextLabel(inspection(), local));
            }
            for (int i = 0; i < frame.numStack; i++) {
                String stackSlot = "stack #" + i;
                stackSlot += " = " + frame.getStackValue(i);
                slotsPanel.add(new TextLabel(inspection(), stackSlot));
            }
            for (int i = 0; i < frame.numLocks; i++) {
                String lock = "lock #" + i;
                lock += " = " + frame.getLockValue(i);
                slotsPanel.add(new TextLabel(inspection(), lock));
            }
            panel.add(slotsPanel, BorderLayout.LINE_START);
        }
        panel.setBorder(frameBorder);
        return panel;
    }

}
