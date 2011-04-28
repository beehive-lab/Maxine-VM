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
import java.util.*;

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
import com.sun.max.vm.jni.*;

/**
 * Renders the Java frame descriptors available at a given location
 * in machine code.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class BytecodeFramesInspector extends Inspector {

    private static final int TRACE_VALUE = 2;
    private static final ViewKind VIEW_KIND = ViewKind.BYTECODE_FRAMES;
    private static final String SHORT_NAME = "Bytecode Frames";
    private static final String LONG_NAME = SHORT_NAME + " Inspector";

    private static final int MAX_BYTE_CODE_BITS = 20;

    private static BytecodeFrameViewManager viewManager;

    private static Long makeKey(CiFrame bytecodeFrames) {
        if (bytecodeFrames == null) {
            return -1L;
        }
        return (MethodID.fromMethodActor((MethodActor) bytecodeFrames.method).asAddress().toLong() << MAX_BYTE_CODE_BITS) | bytecodeFrames.bci;
    }

    public static BytecodeFrameViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new BytecodeFrameViewManager(inspection);
        }
        return viewManager;
    }

    public static final class BytecodeFrameViewManager extends AbstractMultiViewManager<BytecodeFramesInspector> implements BytecodeFramesViewFactory {

        private static Map<Long, BytecodeFramesInspector> inspectors = new Hashtable<Long, BytecodeFramesInspector>();

        private final InspectorAction makeViewAction;

        protected BytecodeFrameViewManager(final Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
            Trace.begin(TRACE_VALUE, tracePrefix() + "creating");
            makeViewAction = new InspectorAction(inspection, "Bytecode frames @ code locn.") {

                private CiFrame bytecodeFrames;

                @Override
                protected void procedure() {
                    assert bytecodeFrames != null;
                    if (focus().hasCodeLocation()) {
                        final Address instructionAddress = focus().codeLocation().address();
                        if (instructionAddress != null && !instructionAddress.isZero()) {
                            final MaxCompiledCode compiledCode = vm().codeCache().findCompiledCode(instructionAddress);
                            if (compiledCode != null) {
                                makeView(bytecodeFrames, compiledCode).highlight();
                            }
                        }
                    } else {
                        gui().errorMessage("Could not locate Java frame descriptor");
                    }
                }

                /**
                 * {@inheritDoc}.
                 * <p>
                 * Can only create a bytecode frames view if there is bytecode frame info at the focus machine code location
                 */
                @Override
                public void refresh(boolean force) {
                    if (focus().hasCodeLocation()) {
                        final Address instructionAddress = focus().codeLocation().address();
                        if (instructionAddress != null && !instructionAddress.isZero()) {
                            final MaxCompiledCode compiledCode = vm().codeCache().findCompiledCode(instructionAddress);
                            if (compiledCode != null) {
                                final InstructionMap instructionMap = compiledCode.getInstructionMap();
                                final int instructionIndex = instructionMap.findInstructionIndex(instructionAddress);
                                if (instructionIndex >= 0) {
                                    bytecodeFrames = instructionMap.bytecodeFrames(instructionIndex);
                                    if (bytecodeFrames == null) {
                                        setEnabled(false);
                                        return;
                                    }
                                }
                                setEnabled(true);
                                return;
                            }
                        }
                    }
                    bytecodeFrames = null;
                    setEnabled(false);
                }
            };
            makeViewAction.refresh(true);
            // Check each time the user's code selection changes to see if there are bytecode
            // frames known for the location.
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative) {
                    makeViewAction.refresh(true);
                }
            });
            Trace.end(TRACE_VALUE, tracePrefix() + "creating");
        }

        public boolean isSupported() {
            return true;
        }

        public boolean isEnabled() {
            return true;
        }

        @Override
        public void notifyViewClosing(Inspector inspector) {
            // TODO (mlvdv)  should be using generics here
            final BytecodeFramesInspector bytecodeFrameInspector = (BytecodeFramesInspector) inspector;
            assert inspectors.remove(bytecodeFrameInspector.key) != null;
            super.notifyViewClosing(bytecodeFrameInspector);
        }

        public InspectorAction makeViewAction() {
            return makeViewAction;
        }

        public BytecodeFramesInspector makeView(CiFrame bytecodeFrames, MaxCompiledCode compiledCode) {
            final Long key = makeKey(bytecodeFrames);
            BytecodeFramesInspector bytecodeFrameInspector = inspectors.get(key);
            if (bytecodeFrameInspector == null) {
                bytecodeFrameInspector = new BytecodeFramesInspector(inspection(), bytecodeFrames, compiledCode, key);
                inspectors.put(key, bytecodeFrameInspector);
                notifyAddingView(bytecodeFrameInspector);
            }
            return bytecodeFrameInspector;
        }
    }

    private final Rectangle originalFrameGeometry;
    private final MaxCompiledCode compiledCode;
    private final CiFrame frames;
    private final Long key;

    private BytecodeFramesInspector(Inspection inspection, CiFrame bytecodeFrames, MaxCompiledCode compiledCode, Long key) {
        super(inspection, VIEW_KIND, null);
        this.frames = bytecodeFrames;
        this.compiledCode = compiledCode;
        this.key = key;
        Trace.begin(TRACE_VALUE, tracePrefix() + " creating for " + getTextForTitle());

        final InspectorFrame frame = createFrame(true);
        // TODO (mlvdv) Need a better way to size these initially
        setSize(200, 200);

        final InspectorMenu defaultMenu = frame.makeMenu(MenuKind.DEFAULT_MENU);
        defaultMenu.add(defaultMenuItems(MenuKind.DEFAULT_MENU));
        defaultMenu.addSeparator();
        defaultMenu.add(views().deactivateOtherViewsAction(ViewKind.BYTECODE_FRAMES, this));
        defaultMenu.add(views().deactivateAllViewsAction(ViewKind.BYTECODE_FRAMES));

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));
        inspection.gui().setLocationRelativeToMouse(this, inspection().geometry().newFrameDiagonalOffset());
        originalFrameGeometry = getGeometry();

        Trace.end(TRACE_VALUE, tracePrefix() + " creating for " + getTextForTitle());
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

    @Override
    protected Rectangle defaultGeometry() {
        return originalFrameGeometry;
    }

    @Override
    public String getTextForTitle() {
        if (frames != null) {
            return shortString(frames);
        }
        return null;
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
        }
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        // don't try to recompute the title, just get the one that's been in use
        Trace.line(1, tracePrefix() + " closing for " + getTitle() + " - process terminated");
        super.inspectorClosing();
    }

    @Override
    protected void refreshState(boolean force) {
    }

}
