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
package com.sun.max.ins.object;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.object.StringPane.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.type.*;


/**
 * An object inspector specialized for displaying a Maxine low-level heap object in the VM that implements a {@link Descriptor}.
 *
 * @author Michael Van De Vanter
 */
public class DescriptorInspector extends ObjectInspector {

    private JTabbedPane tabbedPane;
    private ObjectScrollPane fieldsPane;
    private StringPane stringPane;

    // Should the alternate visualization be displayed?
    // Follows user's tab selection, but should persist when view reconstructed.
    private boolean alternateDisplay;

    DescriptorInspector(Inspection inspection, ObjectInspectorFactory factory, TeleObject teleObject) {
        super(inspection, factory, teleObject);
        // This is the default for a newly created inspector.
        // TODO (mlvdv) make this a global view option?
        alternateDisplay = true;
        createFrame();
    }

    @Override
    protected void createView() {
        super.createView();
        final TeleDescriptor teleDescriptor = (TeleDescriptor) teleObject();
        final String name = teleDescriptor.classActorForType().javaSignature(false);

        tabbedPane = new JTabbedPane();

        fieldsPane = ObjectScrollPane.createFieldsPane(inspection(), teleDescriptor, instanceViewPreferences);
        tabbedPane.add(name, fieldsPane);

        stringPane = StringPane.createStringPane(this, new StringSource() {
            public String fetchString() {
                return teleDescriptor.string();
            }
        });
        tabbedPane.add("string value", stringPane);

        tabbedPane.setSelectedComponent(alternateDisplay ? stringPane : fieldsPane);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                final Prober prober = (Prober) tabbedPane.getSelectedComponent();
                // Remember which display is now selected
                alternateDisplay = prober == stringPane;
                // Refresh the display that is now visible.
                prober.refresh(true);
            }
        });
        getContentPane().add(tabbedPane);
    }

    @Override
    protected boolean refreshView(boolean force) {
        // Only refresh the visible pane.
        final Prober pane = (Prober) tabbedPane.getSelectedComponent();
        pane.refresh(force);
        super.refreshView(force);
        return true;
    }
}
