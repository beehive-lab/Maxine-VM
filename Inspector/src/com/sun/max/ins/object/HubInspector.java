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

import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;

/**
 * An object inspector specialized for displaying a Maxine low-level {@link Hybrid} object in the VM,
 * constructed using {@link HybridLayout}, representing a {@link Hub}.
 *
 * @author Michael Van De Vanter
 */
public class HubInspector extends ObjectInspector {



    private final TeleHub teleHub;


    // Instance preferences
    private boolean showFields;
    private boolean showVTables;
    private boolean showITables;
    private boolean showMTables;
    private boolean showRefMaps;

    private ObjectPane fieldsPane;
    private ObjectPane vTablePane;
    private ObjectPane iTablePane;
    private ObjectPane mTablePane;
    private ObjectPane refMapPane;

    private final InspectorMenuItems classMethodInspectorMenuItems;

    HubInspector(Inspection inspection, ObjectInspectorFactory factory, TeleObject teleObject) {
        super(inspection, factory, teleObject);
        teleHub = (TeleHub) teleObject;

        // Initialize instance preferences from the global preferences
        final HubInspectorPreferences globalHubPreferences = HubInspectorPreferences.globalHubPreferences(inspection());
        showFields = globalHubPreferences.showFields;
        showVTables = globalHubPreferences.showVTables;
        showITables = globalHubPreferences.showITables;
        showMTables = globalHubPreferences.showMTables;
        showRefMaps = globalHubPreferences.showRefMaps;

        createFrame(null);
        final TeleClassMethodActor teleClassMethodActor = teleObject.getTeleClassMethodActorForObject();
        if (teleClassMethodActor != null) {
            // the object is, or is associated with a ClassMethodActor.
            classMethodInspectorMenuItems = new ClassMethodMenuItems(inspection(), teleClassMethodActor);
            frame().add(classMethodInspectorMenuItems);
        } else {
            classMethodInspectorMenuItems = null;
        }
    }

    @Override
    protected void createView() {
        super.createView();

        final JPanel panel = new InspectorPanel(inspection());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        final JToolBar toolBar = new InspectorToolBar(inspection());

        final  JCheckBox showFieldsCheckBox = new InspectorCheckBox(inspection(), "fields", "Display hub fields?", showFields);
        showFieldsCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                showFields = showFieldsCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showFieldsCheckBox);
        toolBar.add(Box.createHorizontalGlue());

        final JCheckBox showVTableCheckBox = new InspectorCheckBox(inspection(), "vTable", "Display hub vTables?", showVTables);
        showVTableCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                showVTables = showVTableCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showVTableCheckBox);
        toolBar.add(Box.createHorizontalGlue());

        final JCheckBox showITableCheckBox = new InspectorCheckBox(inspection(), "iTable", "Display hub iTables?", showITables);
        showITableCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                showITables = showITableCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showITableCheckBox);
        toolBar.add(Box.createHorizontalGlue());

        final JCheckBox showMTableCheckBox = new InspectorCheckBox(inspection(), "mTable", "Display hub mTables?", showMTables);
        showMTableCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                showMTables = showMTableCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showMTableCheckBox);
        toolBar.add(Box.createHorizontalGlue());

        final JCheckBox showRefMapCheckBox = new InspectorCheckBox(inspection(), "ref. map", "Display hub ref map?", showRefMaps);
        showRefMapCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                showRefMaps = showRefMapCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showRefMapCheckBox);

        panel.add(toolBar);

        fieldsPane = ObjectPane.createFieldsPane(this, teleHub);
        showFieldsCheckBox.setEnabled(true);
        if (showFieldsCheckBox.isSelected()) {
            fieldsPane.setBorder(style().defaultPaneTopBorder());
            panel.add(fieldsPane);
        }

        vTablePane = ObjectPane.createVTablePane(this, teleHub);
        showVTableCheckBox.setEnabled(vTablePane != null);
        if (vTablePane != null && showVTableCheckBox.isSelected()) {
            vTablePane.setBorder(style().defaultPaneTopBorder());
            panel.add(vTablePane);
        }

        iTablePane = ObjectPane.createITablePane(this, teleHub);
        showITableCheckBox.setEnabled(iTablePane != null);
        if (iTablePane != null && showITableCheckBox.isSelected()) {
            iTablePane.setBorder(style().defaultPaneTopBorder());
            panel.add(iTablePane);
        }

        mTablePane = ObjectPane.createMTablePane(this, teleHub);
        showMTableCheckBox.setEnabled(mTablePane != null);
        if (mTablePane != null && showMTableCheckBox.isSelected()) {
            mTablePane.setBorder(style().defaultPaneTopBorder());
            panel.add(mTablePane);
        }

        refMapPane = ObjectPane.createRefMapPane(this, teleHub);
        showRefMapCheckBox.setEnabled(refMapPane != null);
        if (refMapPane != null && showRefMapCheckBox.isSelected()) {
            refMapPane.setBorder(style().defaultPaneTopBorder());
            panel.add(refMapPane);
        }

        frame().getContentPane().add(panel);
    }

    @Override
    protected void refreshView(boolean force) {
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
        if (classMethodInspectorMenuItems != null) {
            classMethodInspectorMenuItems.refresh(force);
        }
        super.refreshView(force);
    }


}
