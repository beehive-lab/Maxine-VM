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
public class HubInspector extends ObjectInspector<HubInspector> {

    private static HubPreferences _globalHubPreferences;

    protected static void initializeStatic(Inspection inspection) {
        _globalHubPreferences = new HubPreferences(inspection);
    }

    public static synchronized HubPreferences globalHubPreferences(Inspection inspection) {
        return _globalHubPreferences;
    }

    // Preferences

    private static final String SHOW_FIELDS_PREFERENCE = "showFields";
    private static final String SHOW_VTABLES_PREFERENCE = "showVTables";
    private static final String SHOW_ITABLES_PREFERENCE = "showITables";
    private static final String SHOW_MTABLES_PREFERENCE = "showMTables";
    private static final String SHOW_REFERENCE_MAPS_PREFERENCE = "showReferenceMaps";

    public static class HubPreferences {
        private final Inspection _inspection;
        boolean _showFields;
        boolean _showVTables;
        boolean _showITables;
        boolean _showMTables;
        boolean _showRefMaps;

        HubPreferences(Inspection inspection) {
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
            final JCheckBox alwaysShowFieldsCheckBox = new JCheckBox("Fields");
            alwaysShowFieldsCheckBox.setOpaque(true);
            alwaysShowFieldsCheckBox.setBackground(_inspection.style().defaultBackgroundColor());
            alwaysShowFieldsCheckBox.setToolTipText("Should new Object Inspectors initially display the fields in a Hub?");
            alwaysShowFieldsCheckBox.setSelected(_showFields);

            final JCheckBox alwaysShowVTablesCheckBox = new JCheckBox("vTables");
            alwaysShowVTablesCheckBox.setOpaque(true);
            alwaysShowVTablesCheckBox.setBackground(_inspection.style().defaultBackgroundColor());
            alwaysShowVTablesCheckBox.setToolTipText("Should new Object Inspectors initially display the vTables in a Hub?");
            alwaysShowVTablesCheckBox.setSelected(_showVTables);

            final JCheckBox alwaysShowITablesCheckBox = new JCheckBox("iTables");
            alwaysShowITablesCheckBox.setOpaque(true);
            alwaysShowITablesCheckBox.setBackground(_inspection.style().defaultBackgroundColor());
            alwaysShowITablesCheckBox.setToolTipText("Should new Object Inspectors initially display the iTables in a Hub?");
            alwaysShowITablesCheckBox.setSelected(_showITables);

            final JCheckBox alwaysShowMTablesCheckBox = new JCheckBox("mTables");
            alwaysShowMTablesCheckBox.setOpaque(true);
            alwaysShowMTablesCheckBox.setBackground(_inspection.style().defaultBackgroundColor());
            alwaysShowMTablesCheckBox.setToolTipText("Should new Object Inspectors initially display the mTables in a Hub?");
            alwaysShowMTablesCheckBox.setSelected(_showMTables);

            final JCheckBox alwaysShowRefMapsCheckBox = new JCheckBox("Reference Maps");
            alwaysShowRefMapsCheckBox.setOpaque(true);
            alwaysShowRefMapsCheckBox.setBackground(_inspection.style().defaultBackgroundColor());
            alwaysShowRefMapsCheckBox.setToolTipText("Should new Object Inspectors initially display the reference maps in a Hub?");
            alwaysShowRefMapsCheckBox.setSelected(_showRefMaps);

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

            final JPanel contentPanel = new JPanel();
            contentPanel.add(new TextLabel(_inspection, "Show:  "));
            contentPanel.add(alwaysShowFieldsCheckBox);
            contentPanel.add(alwaysShowVTablesCheckBox);
            contentPanel.add(alwaysShowITablesCheckBox);
            contentPanel.add(alwaysShowMTablesCheckBox);
            contentPanel.add(alwaysShowRefMapsCheckBox);

            final JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(true);
            panel.setBackground(_inspection.style().defaultBackgroundColor());
            panel.add(contentPanel, BorderLayout.WEST);

            return panel;
        }

        void showDialog() {
            new Dialog(_inspection);
        }

        private final class Dialog extends InspectorDialog {

            Dialog(Inspection inspection) {
                super(inspection, "Hub Inspector Preferences", false);

                final JPanel dialogPanel = new JPanel();
                dialogPanel.setLayout(new BorderLayout());
                dialogPanel.setOpaque(true);
                dialogPanel.setBackground(style().defaultBackgroundColor());

                final JPanel buttons = new JPanel();
                buttons.setOpaque(true);
                buttons.setBackground(style().defaultBackgroundColor());
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
    private final JToolBar _toolBar;
    private final JCheckBox _showFieldsCheckBox;
    private final JCheckBox  _showVTableCheckBox;
    private final JCheckBox  _showITableCheckBox;
    private final JCheckBox  _showMTableCheckBox;
    private final JCheckBox  _showRefMapCheckBox;

    private ObjectPane _fieldsPane;
    private ObjectPane _vTablePane;
    private ObjectPane _iTablePane;
    private ObjectPane _mTablePane;
    private ObjectPane _refMapPane;

    private final InspectorMenuItems _classMethodInspectorMenuItems;

    HubInspector(Inspection inspection, Residence residence, TeleObject teleObject) {
        super(inspection, residence, teleObject);
        _teleHub = (TeleHub) teleObject;
        _toolBar = new JToolBar();
        _toolBar.setFloatable(false);
        _toolBar.setBackground(style().defaultBackgroundColor());

        final HubPreferences hubPreferences = globalHubPreferences(inspection());

        _showFieldsCheckBox = new JCheckBox("fields", hubPreferences._showFields);
        _showFieldsCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                reconstructView();
            }
        });
        _toolBar.add(_showFieldsCheckBox);
        _toolBar.add(Box.createHorizontalGlue());

        _showVTableCheckBox = new JCheckBox("vTable", hubPreferences._showVTables);
        _showVTableCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                reconstructView();
            }
        });
        _toolBar.add(_showVTableCheckBox);
        _toolBar.add(Box.createHorizontalGlue());

        _showITableCheckBox = new JCheckBox("iTable", hubPreferences._showITables);
        _showITableCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                reconstructView();
            }
        });
        _toolBar.add(_showITableCheckBox);
        _toolBar.add(Box.createHorizontalGlue());

        _showMTableCheckBox = new JCheckBox("mTable", hubPreferences._showMTables);
        _showMTableCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                reconstructView();
            }
        });
        _toolBar.add(_showMTableCheckBox);
        _toolBar.add(Box.createHorizontalGlue());

        _showRefMapCheckBox = new JCheckBox("ref. map", hubPreferences._showRefMaps);
        _showRefMapCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                reconstructView();
            }
        });
        _toolBar.add(_showRefMapCheckBox);

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
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(style().defaultBackgroundColor());

        panel.add(_toolBar);

        _fieldsPane = ObjectPane.createFieldsPane(this, _teleHub);
        _showFieldsCheckBox.setEnabled(true);
        _showFieldsCheckBox.setBackground(style().defaultBackgroundColor());
        if (_showFieldsCheckBox.isSelected()) {
            _fieldsPane.setBorder(style().defaultPaneTopBorder());
            panel.add(_fieldsPane);
        }

        _vTablePane = ObjectPane.createVTablePane(this, _teleHub);
        _showVTableCheckBox.setEnabled(_vTablePane != null);
        if (_vTablePane != null && _showVTableCheckBox.isSelected()) {
            _vTablePane.setBorder(style().defaultPaneTopBorder());
            panel.add(_vTablePane);
        }

        _iTablePane = ObjectPane.createITablePane(this, _teleHub);
        _showITableCheckBox.setEnabled(_iTablePane != null);
        if (_iTablePane != null && _showITableCheckBox.isSelected()) {
            _iTablePane.setBorder(style().defaultPaneTopBorder());
            panel.add(_iTablePane);
        }

        _mTablePane = ObjectPane.createMTablePane(this, _teleHub);
        _showMTableCheckBox.setEnabled(_mTablePane != null);
        if (_mTablePane != null && _showMTableCheckBox.isSelected()) {
            _mTablePane.setBorder(style().defaultPaneTopBorder());
            panel.add(_mTablePane);
        }

        _refMapPane = ObjectPane.createRefMapPane(this, _teleHub);
        _showRefMapCheckBox.setEnabled(_refMapPane != null);
        if (_refMapPane != null && _showRefMapCheckBox.isSelected()) {
            _refMapPane.setBorder(style().defaultPaneTopBorder());
            panel.add(_refMapPane);
        }

        frame().getContentPane().add(panel);
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        super.refreshView(epoch, force);
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
    }


}
