/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.ins.object.StringPane.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.layout.*;

/**
 * An object view specialized for displaying a low-level heap object in the VM constructed using {@link TupleLayout}. If
 * a textual visualization for the value of the object is available, then the view is created as a tabbed view, with one
 * tab displaying the standard field-oriented representation and the other tab displaying the textual visualization.
 */
public class TupleView extends ObjectView<TupleView> {

    private ObjectScrollPane fieldsPane;
    private InspectorTabbedPane tabbedPane = null;
    private StringPane stringPane = null;

    /**
     * Is the alternate textual visualization, if present, selected? Persists when view reconstructed.
     */
    private boolean alternateDisplay;

    TupleView(Inspection inspection, MaxObject object) {
        super(inspection, object);
        alternateDisplay = object.hasTextualVisualization();
        createFrame(true);
    }

    @Override
    protected void createViewContent() {
        super.createViewContent();
        fieldsPane = ObjectScrollPane.createTupleFieldsPane(inspection(), this);

        if (object().hasTextualVisualization()) {
            final String tabName = object().classActorForObjectType().javaSignature(false);

            tabbedPane = new InspectorTabbedPane(inspection());
            tabbedPane.setBackground(viewBackgroundColor());

            tabbedPane.add(tabName, fieldsPane);

            stringPane = StringPane.createStringPane(this, new StringSource() {
                public String fetchString() {
                    return object().textualVisualization();
                }
            });
            tabbedPane.add("as text", stringPane);

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
        } else {
            getContentPane().add(fieldsPane);
        }

        // View-specific menus
        final MaxCompilation compilation = vm().machineCode().findCompilation(object().origin());
        if (compilation != null) {
            makeMenu(MenuKind.DEBUG_MENU).add(actions().setMachineCodeBreakpointAtEntry(compilation));
        }

        final TeleClassMethodActor teleClassMethodActor = object().getTeleClassMethodActorForObject();
        final InspectorMenu objectMenu = makeMenu(MenuKind.OBJECT_MENU);
        if (teleClassMethodActor != null) {
            // This object is associated with a class method
            objectMenu.add(views().objects().makeViewAction(teleClassMethodActor, "Method: " + teleClassMethodActor.classActorForObjectType().simpleName()));
            final TeleClassActor teleClassActor = teleClassMethodActor.getTeleHolder();
            objectMenu.add(views().objects().makeViewAction(teleClassActor, "Holder: " + teleClassActor.classActorForObjectType().simpleName()));
            objectMenu.add(actions().viewSubstitutionSourceClassActorAction(teleClassMethodActor));
            objectMenu.add(actions().inspectMethodCompilationsMenu(teleClassMethodActor, "Method compilations"));

            final InspectorMenu codeMenu = makeMenu(MenuKind.CODE_MENU);
            codeMenu.add(actions().viewJavaSource(teleClassMethodActor));
            codeMenu.add(actions().viewMethodBytecode(teleClassMethodActor));
            codeMenu.add(actions().viewMethodCompilationsCodeMenu(teleClassMethodActor));
            codeMenu.add(defaultMenuItems(MenuKind.CODE_MENU));

            final InspectorMenu debugMenu = makeMenu(MenuKind.DEBUG_MENU);
            final InspectorMenu breakOnEntryMenu = new InspectorMenu("Break at method entry");
            breakOnEntryMenu.add(actions().setBytecodeBreakpointAtMethodEntry(teleClassMethodActor, "Bytecodes"));
            debugMenu.add(breakOnEntryMenu);
            debugMenu.add(actions().debugInvokeMethod(teleClassMethodActor));
            debugMenu.add(defaultMenuItems(MenuKind.DEBUG_MENU));
        }
        objectMenu.add(defaultMenuItems(MenuKind.OBJECT_MENU));
    }

    @Override
    protected void refreshState(boolean force) {
        if (getJComponent().isShowing() || force) {
            super.refreshState(force);
            if (tabbedPane == null) {
                fieldsPane.refresh(force);
            } else {
                tabbedPane.setBackground(viewBackgroundColor());
                // Only refresh the visible pane.
                final Prober prober = (Prober) tabbedPane.getSelectedComponent();
                prober.refresh(force);
            }
        }
    }

}
