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
package com.sun.max.ins;

import java.awt.*;
import java.awt.event.*;

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
public class PreferenceDialog extends InspectorDialog {

    public PreferenceDialog(final Inspection inspection) {
        super(inspection, "Preferences", false);

        final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());

        final JPanel prefPanel = new InspectorPanel(inspection, new SpringLayout());

        final Border border = BorderFactory.createLineBorder(Color.black);

        final JPanel generalLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        generalLabelPanel.setBorder(border);
        generalLabelPanel.add(new TextLabel(inspection, "General"), BorderLayout.WEST);
        prefPanel.add(generalLabelPanel);

        final JPanel keyBindingsPanel = inspection().preferences().getPanel();
        keyBindingsPanel.setBorder(border);
        prefPanel.add(keyBindingsPanel);

        final JPanel methodLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        methodLabelPanel.setBorder(border);
        methodLabelPanel.add(new TextLabel(inspection, "Methods"), BorderLayout.WEST);
        prefPanel.add(methodLabelPanel);

        final JPanel methodInspectorPanel = MethodInspectorPreferences.globalPreferences(inspection).getPanel();
        methodInspectorPanel.setBorder(border);
        prefPanel.add(methodInspectorPanel);

        final JPanel targetCodeLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        targetCodeLabelPanel.setBorder(border);
        targetCodeLabelPanel.add(new TextLabel(inspection, "Target Code"), BorderLayout.WEST);
        prefPanel.add(targetCodeLabelPanel);

        final JPanel targetCodeInspectorPanel = JTableTargetCodeViewer.globalPreferences(inspection).getPanel();
        targetCodeInspectorPanel.setBorder(border);
        prefPanel.add(targetCodeInspectorPanel);

        final JPanel bytecodeLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        bytecodeLabelPanel.setBorder(border);
        bytecodeLabelPanel.add(new TextLabel(inspection, "Bytecode"), BorderLayout.WEST);
        prefPanel.add(bytecodeLabelPanel);

        final JPanel bytecodeInspectorPanel = JTableBytecodeViewer.globalPreferences(inspection).getPanel();
        bytecodeInspectorPanel.setBorder(border);
        prefPanel.add(bytecodeInspectorPanel);

        final JPanel objectLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        objectLabelPanel.setBorder(border);
        objectLabelPanel.add(new TextLabel(inspection, "Objects"), BorderLayout.WEST);
        prefPanel.add(objectLabelPanel);

        final JPanel objectInspectorPanel = ObjectInspectorPreferences.globalPreferences(inspection).getPanel();
        objectInspectorPanel.setBorder(border);
        prefPanel.add(objectInspectorPanel);

        final JPanel hubLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        hubLabelPanel.setBorder(border);
        hubLabelPanel.add(new TextLabel(inspection, "Hubs"), BorderLayout.WEST);
        prefPanel.add(hubLabelPanel);

        final JPanel hubInspectorPanel = HubInspector.globalHubPreferences(inspection).getPanel();
        hubInspectorPanel.setBorder(border);
        prefPanel.add(hubInspectorPanel);

        final JPanel threadsLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        threadsLabelPanel.setBorder(border);
        threadsLabelPanel.add(new TextLabel(inspection, "Threads"), BorderLayout.WEST);
        prefPanel.add(threadsLabelPanel);

        final JPanel threadsInspectorPanel = ThreadsViewPreferences.globalPreferences(inspection).getPanel();
        threadsInspectorPanel.setBorder(border);
        prefPanel.add(threadsInspectorPanel);

        final JPanel breakpointsLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        breakpointsLabelPanel.setBorder(border);
        breakpointsLabelPanel.add(new TextLabel(inspection, "Breakpoints"), BorderLayout.WEST);
        prefPanel.add(breakpointsLabelPanel);

        final JPanel breakpointsInspectorPanel = BreakpointsViewPreferences.globalPreferences(inspection).getPanel();
        breakpointsInspectorPanel.setBorder(border);
        prefPanel.add(breakpointsInspectorPanel);

        final JPanel memoryRegionsLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        memoryRegionsLabelPanel.setBorder(border);
        memoryRegionsLabelPanel.add(new TextLabel(inspection, "Memory regions"), BorderLayout.WEST);
        prefPanel.add(memoryRegionsLabelPanel);

        final JPanel memoryRegionsInspectorPanel = MemoryRegionsViewPreferences.globalPreferences(inspection).getPanel();
        memoryRegionsInspectorPanel.setBorder(border);
        prefPanel.add(memoryRegionsInspectorPanel);

        final JPanel registersLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        registersLabelPanel.setBorder(border);
        registersLabelPanel.add(new TextLabel(inspection, "Registers"), BorderLayout.WEST);
        prefPanel.add(registersLabelPanel);

        final JPanel registersInspectorPanel = RegistersViewPreferences.globalPreferences(inspection).getPanel();
        registersInspectorPanel.setBorder(border);
        prefPanel.add(registersInspectorPanel);

        final JPanel vmThreadLocalsLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        vmThreadLocalsLabelPanel.setBorder(border);
        vmThreadLocalsLabelPanel.add(new TextLabel(inspection, "VM Thread Locals"), BorderLayout.WEST);
        prefPanel.add(vmThreadLocalsLabelPanel);

        final JPanel vmThreadLocalsInspectorPanel = ThreadLocalsViewPreferences.globalPreferences(inspection).getPanel();
        vmThreadLocalsInspectorPanel.setBorder(border);
        prefPanel.add(vmThreadLocalsInspectorPanel);

        final JPanel bootImageLabelPanel = new InspectorPanel(inspection, new BorderLayout());
        bootImageLabelPanel.setBorder(border);
        bootImageLabelPanel.add(new TextLabel(inspection, "Boot Image"), BorderLayout.WEST);
        prefPanel.add(bootImageLabelPanel);

        final JPanel bootImageInspectorPanel = BootImageViewPreferences.globalPreferences(inspection).getPanel();
        bootImageInspectorPanel.setBorder(border);
        prefPanel.add(bootImageInspectorPanel);

        SpringUtilities.makeCompactGrid(prefPanel, 2);

        final JPanel buttonsPanel = new InspectorPanel(inspection);
        buttonsPanel.add(new InspectorButton(inspection, new AbstractAction("Close") {
            public void actionPerformed(ActionEvent e) {
                inspection.settings().save();
                dispose();
            }
        }));
        dialogPanel.add(prefPanel, BorderLayout.CENTER);
        dialogPanel.add(buttonsPanel, BorderLayout.SOUTH);
        setContentPane(dialogPanel);
        pack();
        inspection().moveToMiddle(this);
        setVisible(true);
    }

}
