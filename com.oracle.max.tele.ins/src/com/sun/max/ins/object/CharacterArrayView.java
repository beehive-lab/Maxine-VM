/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.object;

import javax.swing.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.object.StringPane.StringSource;
import com.sun.max.tele.object.*;

/**
 * An object view specialized for displaying a low-level character array in the VM.
 */
public final class CharacterArrayView extends ObjectView<CharacterArrayView> {

    private InspectorTabbedPane tabbedPane;
    private ObjectScrollPane elementsPane;
    private StringPane stringPane;

    // Should the alternate visualization be displayed?
    // Follows user's tab selection, but should persist when view reconstructed.
    private boolean alternateDisplay;

    CharacterArrayView(Inspection inspection, TeleObject teleObject) {
        super(inspection, teleObject);
        // This is the default for a newly created view.
        // TODO (mlvdv) make this a global view option?
        alternateDisplay = true;
        final InspectorFrame frame = createFrame(true);
        final InspectorMenu objectMenu = frame.makeMenu(MenuKind.OBJECT_MENU);
        objectMenu.add(defaultMenuItems(MenuKind.OBJECT_MENU));
    }

    @Override
    protected void createViewContent() {
        super.createViewContent();

        final TeleArrayObject teleArrayObject = (TeleArrayObject) teleObject();
        final String componentTypeName = teleArrayObject.classActorForObjectType().componentClassActor().javaSignature(false);

        tabbedPane = new InspectorTabbedPane(inspection());

        elementsPane = ObjectScrollPane.createArrayElementsPane(inspection(), teleArrayObject, instanceViewPreferences);
        tabbedPane.add(componentTypeName + "[" + teleArrayObject.getLength() + "]", elementsPane);

        stringPane = StringPane.createStringPane(this, new StringSource() {
            public String fetchString() {
                final char[] chars = (char[]) teleArrayObject.shallowCopy();
                final int length = Math.min(chars.length, preference().style().maxStringFromCharArrayDisplayLength());
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
        getContentPane().add(tabbedPane);
    }

    @Override
    protected void refreshState(boolean force) {
        // Only refresh the visible pane.
        final Prober prober = (Prober) tabbedPane.getSelectedComponent();
        prober.refresh(force);
        super.refreshState(force);
    }

}
