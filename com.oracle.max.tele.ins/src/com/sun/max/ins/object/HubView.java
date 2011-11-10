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
package com.sun.max.ins.object;

import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;

/**
 * An object view specialized for displaying a low-level {@link Hybrid} object in the VM,
 * constructed using {@link HybridLayout}, representing a {@link Hub}.
 */
public final class HubView extends ObjectView<HubView> {

    private final TeleHub teleHub;

    // Instance preferences
    private boolean showFields;
    private boolean showVTables;
    private boolean showITables;
    private boolean showMTables;
    private boolean showRefMaps;

    private ObjectScrollPane fieldsPane;
    private ObjectScrollPane vTablePane;
    private ObjectScrollPane iTablePane;
    private ObjectScrollPane mTablePane;
    private ObjectScrollPane refMapPane;

    HubView(Inspection inspection, TeleObject teleObject) {
        super(inspection, teleObject);
        teleHub = (TeleHub) teleObject;

        // Initialize instance preferences from the global preferences
        final HubViewPreferences globalHubPreferences = HubViewPreferences.globalHubPreferences(inspection());
        showFields = globalHubPreferences.showFields;
        showVTables = globalHubPreferences.showVTables;
        showITables = globalHubPreferences.showITables;
        showMTables = globalHubPreferences.showMTables;
        showRefMaps = globalHubPreferences.showRefMaps;

        final InspectorFrame frame = createFrame(true);
        final InspectorMenu objectMenu = frame.makeMenu(MenuKind.OBJECT_MENU);
        final TeleClassMethodActor teleClassMethodActor = teleObject.getTeleClassMethodActorForObject();
        if (teleClassMethodActor != null) {
            // the object is, or is associated with a ClassMethodActor.
            final InspectorMenu debugMenu = frame.makeMenu(MenuKind.DEBUG_MENU);
            debugMenu.add(actions().setBytecodeBreakpointAtMethodEntry(teleClassMethodActor));
            debugMenu.add(actions().debugInvokeMethod(teleClassMethodActor));

            objectMenu.add(views().objects().makeViewAction(teleClassMethodActor, teleClassMethodActor.classActorForObjectType().simpleName()));
            final TeleClassActor teleClassActor = teleClassMethodActor.getTeleHolder();
            objectMenu.add(views().objects().makeViewAction(teleClassActor, teleClassActor.classActorForObjectType().simpleName()));
            objectMenu.add(actions().viewSubstitutionSourceClassActorAction(teleClassMethodActor));
            objectMenu.add(actions().viewMethodCompilationsMenu(teleClassMethodActor));

            final InspectorMenu codeMenu = frame.makeMenu(MenuKind.CODE_MENU);
            codeMenu.add(actions().viewMethodCompilationsCodeMenu(teleClassMethodActor));
            codeMenu.addSeparator();
            codeMenu.add(defaultMenuItems(MenuKind.CODE_MENU));
        }
        objectMenu.add(defaultMenuItems(MenuKind.OBJECT_MENU));
    }

    @Override
    protected void createViewContent() {
        super.createViewContent();

        final JPanel panel = new InspectorPanel(inspection());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        final InspectorStyle style = preference().style();

        // Display a tool bar with check boxes to control which panes are displayed.
        final JToolBar toolBar = new InspectorToolBar(inspection());
        final  InspectorCheckBox showFieldsCheckBox = new InspectorCheckBox(inspection(), "fields", "Display hub fields?", showFields);
        showFieldsCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                showFields = showFieldsCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showFieldsCheckBox);
        toolBar.add(Box.createHorizontalGlue());
        final InspectorCheckBox showVTableCheckBox = new InspectorCheckBox(inspection(), "vTable", "Display hub vTables?", showVTables);
        showVTableCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                showVTables = showVTableCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showVTableCheckBox);
        toolBar.add(Box.createHorizontalGlue());
        final InspectorCheckBox showITableCheckBox = new InspectorCheckBox(inspection(), "iTable", "Display hub iTables?", showITables);
        showITableCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                showITables = showITableCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showITableCheckBox);
        toolBar.add(Box.createHorizontalGlue());
        final InspectorCheckBox showMTableCheckBox = new InspectorCheckBox(inspection(), "mTable", "Display hub mTables?", showMTables);
        showMTableCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                showMTables = showMTableCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showMTableCheckBox);
        toolBar.add(Box.createHorizontalGlue());
        final InspectorCheckBox showRefMapCheckBox = new InspectorCheckBox(inspection(), "ref. map", "Display hub ref map?", showRefMaps);
        showRefMapCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                showRefMaps = showRefMapCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showRefMapCheckBox);

        panel.add(toolBar);

        fieldsPane = ObjectScrollPane.createFieldsPane(inspection(), teleHub, instanceViewPreferences);
        showFieldsCheckBox.setEnabled(true);
        if (showFieldsCheckBox.isSelected()) {
            fieldsPane.setBorder(style.defaultPaneTopBorder());
            panel.add(fieldsPane);
        }

        vTablePane = ObjectScrollPane.createVTablePane(inspection(), teleHub, instanceViewPreferences);
        showVTableCheckBox.setEnabled(vTablePane != null);
        if (vTablePane != null && showVTableCheckBox.isSelected()) {
            vTablePane.setBorder(style.defaultPaneTopBorder());
            panel.add(vTablePane);
        }

        iTablePane = ObjectScrollPane.createITablePane(inspection(), teleHub, instanceViewPreferences);
        showITableCheckBox.setEnabled(iTablePane != null);
        if (iTablePane != null && showITableCheckBox.isSelected()) {
            iTablePane.setBorder(style.defaultPaneTopBorder());
            panel.add(iTablePane);
        }

        mTablePane = ObjectScrollPane.createMTablePane(inspection(), teleHub, instanceViewPreferences);
        showMTableCheckBox.setEnabled(mTablePane != null);
        if (mTablePane != null && showMTableCheckBox.isSelected()) {
            mTablePane.setBorder(style.defaultPaneTopBorder());
            panel.add(mTablePane);
        }

        refMapPane = ObjectScrollPane.createRefMapPane(inspection(), teleHub, instanceViewPreferences);
        showRefMapCheckBox.setEnabled(refMapPane != null);
        if (refMapPane != null && showRefMapCheckBox.isSelected()) {
            refMapPane.setBorder(style.defaultPaneTopBorder());
            panel.add(refMapPane);
        }

        getContentPane().add(panel);
    }

    @Override
    protected void refreshState(boolean force) {
        fieldsPane.refresh(force);
        if (iTablePane != null) {
            iTablePane.refresh(force);
        }
        if (vTablePane != null) {
            vTablePane.refresh(force);
        }
        if (mTablePane != null) {
            mTablePane.refresh(force);
        }
        if (refMapPane != null) {
            refMapPane.refresh(force);
        }
        super.refreshState(force);
    }

}
