/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * An object inspector specialized for displaying a low-level heap object
 * in the VM that implements a {@link Enum}.
 *
 * @author Michael Van De Vanter
 */
public class EnumInspector extends ObjectInspector {

    private InspectorTabbedPane tabbedPane;
    private ObjectScrollPane fieldsPane;
    private StringPane stringPane;

    // Should the alternate visualization be displayed?
    // Follows user's tab selection, but should persist when view reconstructed.
    private boolean alternateDisplay;

    EnumInspector(Inspection inspection, ObjectInspectorFactory factory, TeleObject teleObject) {
        super(inspection, factory, teleObject);
        // This is the default for a newly created inspector.
        // TODO (mlvdv) make this a global view option?
        alternateDisplay = true;
        createFrame(true);
    }

    @Override
    protected void createView() {
        super.createView();
        final TeleEnum teleEnum = (TeleEnum) teleObject();
        final String name = teleEnum.classActorForObjectType().javaSignature(false);

        tabbedPane = new InspectorTabbedPane(inspection());

        fieldsPane = ObjectScrollPane.createFieldsPane(inspection(), teleEnum, instanceViewPreferences);
        tabbedPane.add(name, fieldsPane);

        stringPane = StringPane.createStringPane(this, new StringSource() {
            public String fetchString() {
                return teleEnum.toJava().name();
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
    protected void refreshView(boolean force) {
        // Only refresh the visible pane.
        final Prober pane = (Prober) tabbedPane.getSelectedComponent();
        pane.refresh(force);
        super.refreshView(force);
    }
}
