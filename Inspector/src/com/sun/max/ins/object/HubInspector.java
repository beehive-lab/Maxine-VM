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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;

/**
 * An object inspector specialized for displaying a Maxine low-level {@link Hybrid} object in the {@link TeleVM},
 * constructed using {@link HybridLayout}, representing a {@link Hub}.
 *
 * @author Michael Van De Vanter
 */
public class HubInspector extends ObjectInspector {

    private static GlobalHubPreferences _globalHubPreferences;

    public static synchronized GlobalHubPreferences globalHubPreferences(Inspection inspection) {
        if (_globalHubPreferences == null) {
            _globalHubPreferences = new GlobalHubPreferences(inspection);
        }
        return _globalHubPreferences;
    }

    // Preferences

    private static final String SHOW_FIELDS_PREFERENCE = "showFields";
    private static final String SHOW_VTABLES_PREFERENCE = "showVTables";
    private static final String SHOW_ITABLES_PREFERENCE = "showITables";
    private static final String SHOW_MTABLES_PREFERENCE = "showMTables";
    private static final String SHOW_REFERENCE_MAPS_PREFERENCE = "showReferenceMaps";

    public static class GlobalHubPreferences {
        private final Inspection _inspection;
        boolean _showFields;
        boolean _showVTables;
        boolean _showITables;
        boolean _showMTables;
        boolean _showRefMaps;

        GlobalHubPreferences(Inspection inspection) {
            _inspection = inspection;
            final InspectionSettings settings = inspection.settings();
            final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener("hubInspectorPrefs", null) {
                public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                    saveSettingsEvent.save(SHOW_FIELDS_PREFERENCE, _showFields);
                    saveSettingsEvent.save(SHOW_VTABLES_PREFERENCE, _showVTables);
                    saveSettingsEvent.save(SHOW_ITABLES_PREFERENCE, _showITables);
                    saveSettingsEvent.save(SHOW_MTABLES_PREFERENCE, _showMTables);
                    saveSettingsEvent.save(SHOW_REFERENCE_MAPS_PREFERENCE,  _showRefMaps);
                }
            };
            settings.addSaveSettingsListener(saveSettingsListener);

            _showFields = settings.get(saveSettingsListener, SHOW_FIELDS_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);
            _showVTables = settings.get(saveSettingsListener, SHOW_VTABLES_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
            _showITables = settings.get(saveSettingsListener, SHOW_ITABLES_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
            _showMTables = settings.get(saveSettingsListener, SHOW_MTABLES_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
            _showRefMaps = settings.get(saveSettingsListener, SHOW_REFERENCE_MAPS_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
        }

        /**
         * @return a GUI panel for setting preferences
         */
        public JPanel getPanel() {
            final InspectorCheckBox alwaysShowFieldsCheckBox =
                new InspectorCheckBox(_inspection, "Fields", "Should new Object Inspectors initially display the fields in a Hub?", _showFields);
            final InspectorCheckBox alwaysShowVTablesCheckBox =
                new InspectorCheckBox(_inspection, "vTables", "Should new Object Inspectors initially display the vTables in a Hub?", _showVTables);
            final InspectorCheckBox alwaysShowITablesCheckBox =
                new InspectorCheckBox(_inspection, "iTables", "Should new Object Inspectors initially display the iTables in a Hub?", _showITables);
            final InspectorCheckBox alwaysShowMTablesCheckBox =
                new InspectorCheckBox(_inspection, "mTables", "Should new Object Inspectors initially display the mTables in a Hub?", _showMTables);
            final InspectorCheckBox alwaysShowRefMapsCheckBox =
                new InspectorCheckBox(_inspection, "Reference Maps", "Should new Object Inspectors initially display the reference maps in a Hub?", _showRefMaps);

            final ItemListener itemListener = new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    final Object source = e.getItemSelectable();
                    if (source == alwaysShowFieldsCheckBox) {
                        _showFields = alwaysShowFieldsCheckBox.isSelected();
                    } else if (source == alwaysShowVTablesCheckBox) {
                        _showVTables = alwaysShowVTablesCheckBox.isSelected();
                    } else if (source == alwaysShowITablesCheckBox) {
                        _showITables = alwaysShowITablesCheckBox.isSelected();
                    } else if (source == alwaysShowMTablesCheckBox) {
                        _showMTables = alwaysShowMTablesCheckBox.isSelected();
                    } else if (source == alwaysShowRefMapsCheckBox) {
                        _showRefMaps = alwaysShowRefMapsCheckBox.isSelected();
                    }
                    _inspection.settings().save();
                }
            };
            alwaysShowFieldsCheckBox.addItemListener(itemListener);
            alwaysShowVTablesCheckBox.addItemListener(itemListener);
            alwaysShowITablesCheckBox.addItemListener(itemListener);
            alwaysShowMTablesCheckBox.addItemListener(itemListener);
            alwaysShowRefMapsCheckBox.addItemListener(itemListener);

            final JPanel contentPanel = new InspectorPanel(_inspection);
            contentPanel.add(new TextLabel(_inspection, "Show:  "));
            contentPanel.add(alwaysShowFieldsCheckBox);
            contentPanel.add(alwaysShowVTablesCheckBox);
            contentPanel.add(alwaysShowITablesCheckBox);
            contentPanel.add(alwaysShowMTablesCheckBox);
            contentPanel.add(alwaysShowRefMapsCheckBox);

            final JPanel panel = new InspectorPanel(_inspection, new BorderLayout());
            panel.add(contentPanel, BorderLayout.WEST);

            return panel;
        }

        void showDialog() {
            new Dialog(_inspection);
        }

        private final class Dialog extends InspectorDialog {

            Dialog(Inspection inspection) {
                super(inspection, "Hub Inspector Preferences", false);

                final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());
                final JPanel buttons = new InspectorPanel(inspection);
                buttons.add(new JButton(new InspectorAction(inspection(), "Close") {
                    @Override
                    protected void procedure() {
                        dispose();
                    }
                }));

                dialogPanel.add(getPanel(), BorderLayout.NORTH);
                dialogPanel.add(buttons, BorderLayout.SOUTH);

                setContentPane(dialogPanel);
                pack();
                inspection().moveToMiddle(this);
                setVisible(true);
            }
        }
    }

    private final TeleHub _teleHub;


    // Instance preferences
    private boolean _showFields;
    private boolean _showVTables;
    private boolean _showITables;
    private boolean _showMTables;
    private boolean _showRefMaps;

    private ObjectPane _fieldsPane;
    private ObjectPane _vTablePane;
    private ObjectPane _iTablePane;
    private ObjectPane _mTablePane;
    private ObjectPane _refMapPane;

    private final InspectorMenuItems _classMethodInspectorMenuItems;

    HubInspector(Inspection inspection, ObjectInspectorFactory factory, Residence residence, TeleObject teleObject) {
        super(inspection, factory, residence, teleObject);
        _teleHub = (TeleHub) teleObject;

        // Initialize instance preferences from the global preferences
        final GlobalHubPreferences globalHubPreferences = globalHubPreferences(inspection());
        _showFields = globalHubPreferences._showFields;
        _showVTables = globalHubPreferences._showVTables;
        _showITables = globalHubPreferences._showITables;
        _showMTables = globalHubPreferences._showMTables;
        _showRefMaps = globalHubPreferences._showRefMaps;

        createFrame(null);
        final TeleClassMethodActor teleClassMethodActor = teleObject.getTeleClassMethodActorForObject();
        if (teleClassMethodActor != null) {
            // the object is, or is associated with a ClassMethodActor.
            _classMethodInspectorMenuItems = new ClassMethodMenuItems(inspection(), teleClassMethodActor);
            frame().add(_classMethodInspectorMenuItems);
        } else {
            _classMethodInspectorMenuItems = null;
        }
    }

    @Override
    protected synchronized void createView(long epoch) {
        super.createView(epoch);

        final JPanel panel = new InspectorPanel(inspection());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        final JToolBar toolBar = new InspectorToolBar(inspection());

        final  JCheckBox showFieldsCheckBox = new InspectorCheckBox(inspection(), "fields", "Display hub fields?", _showFields);
        showFieldsCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                _showFields = showFieldsCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showFieldsCheckBox);
        toolBar.add(Box.createHorizontalGlue());

        final JCheckBox showVTableCheckBox = new InspectorCheckBox(inspection(), "vTable", "Display hub vTables?", _showVTables);
        showVTableCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                _showVTables = showVTableCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showVTableCheckBox);
        toolBar.add(Box.createHorizontalGlue());

        final JCheckBox showITableCheckBox = new InspectorCheckBox(inspection(), "iTable", "Display hub iTables?", _showITables);
        showITableCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                _showITables = showITableCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showITableCheckBox);
        toolBar.add(Box.createHorizontalGlue());

        final JCheckBox showMTableCheckBox = new InspectorCheckBox(inspection(), "mTable", "Display hub mTables?", _showMTables);
        showMTableCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                _showMTables = showMTableCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showMTableCheckBox);
        toolBar.add(Box.createHorizontalGlue());

        final JCheckBox showRefMapCheckBox = new InspectorCheckBox(inspection(), "ref. map", "Display hub ref map?", _showRefMaps);
        showRefMapCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                _showRefMaps = showRefMapCheckBox.isSelected();
                reconstructView();
            }
        });
        toolBar.add(showRefMapCheckBox);

        panel.add(toolBar);

        _fieldsPane = ObjectPane.createFieldsPane(this, _teleHub);
        showFieldsCheckBox.setEnabled(true);
        if (showFieldsCheckBox.isSelected()) {
            _fieldsPane.setBorder(style().defaultPaneTopBorder());
            panel.add(_fieldsPane);
        }

        _vTablePane = ObjectPane.createVTablePane(this, _teleHub);
        showVTableCheckBox.setEnabled(_vTablePane != null);
        if (_vTablePane != null && showVTableCheckBox.isSelected()) {
            _vTablePane.setBorder(style().defaultPaneTopBorder());
            panel.add(_vTablePane);
        }

        _iTablePane = ObjectPane.createITablePane(this, _teleHub);
        showITableCheckBox.setEnabled(_iTablePane != null);
        if (_iTablePane != null && showITableCheckBox.isSelected()) {
            _iTablePane.setBorder(style().defaultPaneTopBorder());
            panel.add(_iTablePane);
        }

        _mTablePane = ObjectPane.createMTablePane(this, _teleHub);
        showMTableCheckBox.setEnabled(_mTablePane != null);
        if (_mTablePane != null && showMTableCheckBox.isSelected()) {
            _mTablePane.setBorder(style().defaultPaneTopBorder());
            panel.add(_mTablePane);
        }

        _refMapPane = ObjectPane.createRefMapPane(this, _teleHub);
        showRefMapCheckBox.setEnabled(_refMapPane != null);
        if (_refMapPane != null && showRefMapCheckBox.isSelected()) {
            _refMapPane.setBorder(style().defaultPaneTopBorder());
            panel.add(_refMapPane);
        }

        frame().getContentPane().add(panel);
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        _fieldsPane.refresh(epoch, force);
        if (_iTablePane != null) {
            _iTablePane.refresh(epoch, force);
        }
        if (_vTablePane != null) {
            _vTablePane.refresh(epoch, force);
        }
        if (_mTablePane != null) {
            _mTablePane.refresh(epoch, force);
        }
        if (_refMapPane != null) {
            _refMapPane.refresh(epoch, force);
        }
        if (_classMethodInspectorMenuItems != null) {
            _classMethodInspectorMenuItems.refresh(epoch, force);
        }
        super.refreshView(epoch, force);
    }


}
