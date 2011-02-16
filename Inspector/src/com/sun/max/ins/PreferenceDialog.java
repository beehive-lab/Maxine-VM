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
package com.sun.max.ins;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.*;

import com.sun.max.gui.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.method.*;
import com.sun.max.ins.object.*;

/**
 * A rudimentary Preferences dialog; leveraging {@link JPanel}s provided by various {@link Inspector}s.
 *
 * @author Michael Van De Vanter
 */
public class PreferenceDialog extends SimpleDialog {

    private static final Border border = BorderFactory.createLineBorder(Color.black);

    public static PreferenceDialog create(Inspection inspection) {

        final JPanel prefPanel = new InspectorPanel(inspection, new SpringLayout());

        final JPanel generalLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        generalLabelPanel.setBorder(border);
        generalLabelPanel.add(new TextLabel(inspection, "General"), BorderLayout.WEST);
        prefPanel.add(generalLabelPanel);

        final JPanel keyBindingsPanel = inspection.globalPreferencesPanel();
        keyBindingsPanel.setBorder(border);
        prefPanel.add(keyBindingsPanel);

        final JPanel methodLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        methodLabelPanel.setBorder(border);
        methodLabelPanel.add(new TextLabel(inspection, "Methods"), BorderLayout.WEST);
        prefPanel.add(methodLabelPanel);

        final JPanel methodInspectorPanel = MethodInspectorPreferences.globalPreferencesPanel(inspection);
        methodInspectorPanel.setBorder(border);
        prefPanel.add(methodInspectorPanel);

        final JPanel machineCodeLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        machineCodeLabelPanel.setBorder(border);
        machineCodeLabelPanel.add(new TextLabel(inspection, "Machine Code"), BorderLayout.WEST);
        prefPanel.add(machineCodeLabelPanel);

        final JPanel machineCodeInspectorPanel = MachineCodeViewPreferences.globalPreferences(inspection).getPanel();
        machineCodeInspectorPanel.setBorder(border);
        prefPanel.add(machineCodeInspectorPanel);

        final JPanel bytecodeLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        bytecodeLabelPanel.setBorder(border);
        bytecodeLabelPanel.add(new TextLabel(inspection, "Bytecodes"), BorderLayout.WEST);
        prefPanel.add(bytecodeLabelPanel);

        final JPanel bytecodeInspectorPanel = BytecodeViewPreferences.globalPreferences(inspection).getPanel();
        bytecodeInspectorPanel.setBorder(border);
        prefPanel.add(bytecodeInspectorPanel);

        final JPanel objectLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        objectLabelPanel.setBorder(border);
        objectLabelPanel.add(new TextLabel(inspection, "Objects"), BorderLayout.WEST);
        prefPanel.add(objectLabelPanel);

        final JPanel objectInspectorPanel = ObjectViewPreferences.globalPreferencesPanel(inspection);
        objectInspectorPanel.setBorder(border);
        prefPanel.add(objectInspectorPanel);

        final JPanel hubLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        hubLabelPanel.setBorder(border);
        hubLabelPanel.add(new TextLabel(inspection, "Hubs"), BorderLayout.WEST);
        prefPanel.add(hubLabelPanel);

        final JPanel hubInspectorPanel = HubInspectorPreferences.globalPreferencesPanel(inspection);
        hubInspectorPanel.setBorder(border);
        prefPanel.add(hubInspectorPanel);

        final JPanel threadsLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        threadsLabelPanel.setBorder(border);
        threadsLabelPanel.add(new TextLabel(inspection, "Threads"), BorderLayout.WEST);
        prefPanel.add(threadsLabelPanel);

        final JPanel threadsInspectorPanel = ThreadsViewPreferences.globalPreferencesPanel(inspection);
        threadsInspectorPanel.setBorder(border);
        prefPanel.add(threadsInspectorPanel);

        final JPanel breakpointsLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        breakpointsLabelPanel.setBorder(border);
        breakpointsLabelPanel.add(new TextLabel(inspection, "Breakpoints"), BorderLayout.WEST);
        prefPanel.add(breakpointsLabelPanel);

        final JPanel breakpointsInspectorPanel = BreakpointsViewPreferences.globalPreferencesPanel(inspection);
        breakpointsInspectorPanel.setBorder(border);
        prefPanel.add(breakpointsInspectorPanel);

        final JPanel watchpointsLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        watchpointsLabelPanel.setBorder(border);
        watchpointsLabelPanel.add(new TextLabel(inspection, "Watchpoints"), BorderLayout.WEST);
        prefPanel.add(watchpointsLabelPanel);

        final JPanel watchpointsInspectorPanel = WatchpointsViewPreferences.globalPreferencesPanel(inspection);
        watchpointsInspectorPanel.setBorder(border);
        prefPanel.add(watchpointsInspectorPanel);

        final JPanel memoryRegionsLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        memoryRegionsLabelPanel.setBorder(border);
        memoryRegionsLabelPanel.add(new TextLabel(inspection, "Memory regions"), BorderLayout.WEST);
        prefPanel.add(memoryRegionsLabelPanel);

        final JPanel memoryRegionsInspectorPanel = MemoryRegionsViewPreferences.globalPreferencesPanel(inspection);
        memoryRegionsInspectorPanel.setBorder(border);
        prefPanel.add(memoryRegionsInspectorPanel);

        final JPanel memoryWordsLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        memoryWordsLabelPanel.setBorder(border);
        memoryWordsLabelPanel.add(new TextLabel(inspection, "Memory"), BorderLayout.WEST);
        prefPanel.add(memoryWordsLabelPanel);

        final JPanel memoryWordsInspectorPanel = MemoryWordsInspector.globalPreferencesPanel(inspection);
        memoryWordsInspectorPanel.setBorder(border);
        prefPanel.add(memoryWordsInspectorPanel);

        final JPanel registersLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        registersLabelPanel.setBorder(border);
        registersLabelPanel.add(new TextLabel(inspection, "Registers"), BorderLayout.WEST);
        prefPanel.add(registersLabelPanel);

        final JPanel registersInspectorPanel = RegistersViewPreferences.globalPreferencesPanel(inspection);
        registersInspectorPanel.setBorder(border);
        prefPanel.add(registersInspectorPanel);

        final JPanel javaStackFrameLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        javaStackFrameLabelPanel.setBorder(border);
        javaStackFrameLabelPanel.add(new TextLabel(inspection, "Java Stack Frames"), BorderLayout.WEST);
        prefPanel.add(javaStackFrameLabelPanel);

        final JPanel javaStackFramePanel = CompiledStackFrameViewPreferences.globalPreferencesPanel(inspection);
        javaStackFramePanel.setBorder(border);
        prefPanel.add(javaStackFramePanel);

        final JPanel tlaLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        tlaLabelPanel.setBorder(border);
        tlaLabelPanel.add(new TextLabel(inspection, "VM Thread Locals"), BorderLayout.WEST);
        prefPanel.add(tlaLabelPanel);

        final JPanel tlaInspectorPanel = ThreadLocalsViewPreferences.globalPreferencesPanel(inspection);
        tlaInspectorPanel.setBorder(border);
        prefPanel.add(tlaInspectorPanel);

        final JPanel bootImageLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        bootImageLabelPanel.setBorder(border);
        bootImageLabelPanel.add(new TextLabel(inspection, "Boot Image"), BorderLayout.WEST);
        prefPanel.add(bootImageLabelPanel);

        final JPanel bootImageInspectorPanel = BootImageViewPreferences.globalPreferencesPanel(inspection);
        bootImageInspectorPanel.setBorder(border);
        prefPanel.add(bootImageInspectorPanel);

        SpringUtilities.makeCompactGrid(prefPanel, 2);
        return new PreferenceDialog(inspection, prefPanel);
    }

    public PreferenceDialog(Inspection inspection, JPanel panel) {
        super(inspection, panel, "Inspector Preferences", false);
    }

}
