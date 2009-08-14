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

/**
 * An object inspector specialized for displaying a Maxine low-level character array in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class CharacterArrayInspector extends ObjectInspector {

    private JTabbedPane tabbedPane;
    private ObjectScrollPane elementsPane;
    private StringPane stringPane;

    // Should the alternate visualization be displayed?
    // Follows user's tab selection, but should persist when view reconstructed.
    private boolean alternateDisplay;

    CharacterArrayInspector(Inspection inspection, ObjectInspectorFactory factory, TeleObject teleObject) {
        super(inspection, factory, teleObject);
        // This is the default for a newly created inspector.
        // TODO (mlvdv) make this a global view option?
        alternateDisplay = true;
        createFrame(null);
    }

    @Override
    protected void createView() {
        super.createView();

        final TeleArrayObject teleArrayObject = (TeleArrayObject) teleObject();
        final String componentTypeName = teleArrayObject.classActorForType().componentClassActor().javaSignature(false);

        tabbedPane = new JTabbedPane();

        elementsPane = ObjectScrollPane.createArrayElementsPane(this, teleArrayObject);
        tabbedPane.add(componentTypeName + "[" + teleArrayObject.getLength() + "]", elementsPane);

        stringPane = StringPane.createStringPane(this, new StringSource() {
            public String fetchString() {
                final char[] chars = (char[]) teleArrayObject.shallowCopy();
                final int length = Math.min(chars.length, style().maxStringFromCharArrayDisplayLength());
                return new String(chars, 0, length);
            }
        });
        tabbedPane.add("string value", stringPane);

        tabbedPane.setSelectedComponent(alternateDisplay ? stringPane : elementsPane);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                final Prober prober = (Prober) tabbedPane.getSelectedComponent();
                // Remember which display is now selected
                alternateDisplay = prober == stringPane;
                // Refresh the display that is now visible.
                prober.refresh(true);
            }
        });
        frame().getContentPane().add(tabbedPane);
    }

    @Override
    protected boolean refreshView(boolean force) {
        // Only refresh the visible pane.
        final Prober prober = (Prober) tabbedPane.getSelectedComponent();
        prober.refresh(force);
        super.refreshView(force);
        return true;
    }

}
