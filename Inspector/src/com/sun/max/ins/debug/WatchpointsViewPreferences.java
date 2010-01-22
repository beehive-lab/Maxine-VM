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
package com.sun.max.ins.debug;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.MaxWatchpoint.*;

/**
 * Persistent preferences for managing and viewing watchpoints in the VM.
 *
 * @author Michael Van De Vanter
  */
public final class WatchpointsViewPreferences extends TableColumnVisibilityPreferences<WatchpointsColumnKind> {

    private static WatchpointsViewPreferences globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing a table of watchpoints.
     */
    public static WatchpointsViewPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new WatchpointsViewPreferences(inspection);
        }
        return globalPreferences;
    }

    // Prefix for all persistent column preferences in view
    private static final String WATCHPOINT_COLUMN_PREFERENCE = "watchpointsViewColumn";

    // Prefix for all other preferences in view
    private static final String WATCHPOINT_INSPECTOR_PREFERENCE = "watchpointInspectorPrefs";

    // Names of other preferences in view
    private static final String WATCHPOINT_READ_PREFERENCE = "trapOnRead";
    private static final String WATCHPOINT_WRITE_PREFERENCE = "trapOnWrite";
    private static final String WATCHPOINT_EXEC_PREFERENCE = "trapOnExec";
    private static final String WATCHPOINT_GC_PREFERENCE = "enableDuringGC";

    /**
     * @return a GUI panel suitable for setting global preferences for this kind of view.
     */
    public static JPanel globalPreferencesPanel(Inspection inspection) {
        return globalPreferences(inspection).getPanel();
    }

    private boolean trapOnRead = true;
    private boolean trapOnWrite = true;
    private boolean trapOnExec = false;
    private boolean enableDuringGC = false;

    /**
    * Creates a set of preferences specified for use by singleton instances, where local and
    * persistent global choices are identical.
    */
    private WatchpointsViewPreferences(Inspection inspection) {
        super(inspection, WATCHPOINT_COLUMN_PREFERENCE, WatchpointsColumnKind.VALUES);
        final InspectionSettings settings = inspection.settings();
        final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener(WATCHPOINT_INSPECTOR_PREFERENCE) {
            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                saveSettingsEvent.save(WATCHPOINT_READ_PREFERENCE, trapOnRead);
                saveSettingsEvent.save(WATCHPOINT_WRITE_PREFERENCE, trapOnWrite);
                saveSettingsEvent.save(WATCHPOINT_EXEC_PREFERENCE, trapOnExec);
                saveSettingsEvent.save(WATCHPOINT_GC_PREFERENCE,  enableDuringGC);
            }
        };
        settings.addSaveSettingsListener(saveSettingsListener);

        trapOnRead = settings.get(saveSettingsListener, WATCHPOINT_READ_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);
        trapOnWrite = settings.get(saveSettingsListener, WATCHPOINT_WRITE_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);
        trapOnExec = settings.get(saveSettingsListener, WATCHPOINT_EXEC_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
        enableDuringGC = settings.get(saveSettingsListener, WATCHPOINT_GC_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);

    }

    public WatchpointSettings settings() {
        return new WatchpointSettings(trapOnRead, trapOnWrite, trapOnExec, enableDuringGC);
    }

    private static class WatchpointsPreferencesPanel extends InspectorPanel {

        public WatchpointsPreferencesPanel(Inspection inspection) {
            super(inspection);
        }
    }

    @Override
    public JPanel getPanel() {

        final JCheckBox readCheckBox = new InspectorCheckBox(inspection(), "Read", "New watchpoints by default trap on trapOnRead", trapOnRead);
        readCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBox checkBox = (JCheckBox) e.getSource();
                trapOnRead = checkBox.isSelected();
            }
        });
        final JCheckBox writeCheckBox = new InspectorCheckBox(inspection(), "Write", "New watchpoints by default trap on trapOnWrite", trapOnWrite);
        writeCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBox checkBox = (JCheckBox) e.getSource();
                trapOnWrite = checkBox.isSelected();
            }
        });
        final JCheckBox execCheckBox = new InspectorCheckBox(inspection(), "Exec", "New watchpoints by default trap on trapOnExec", trapOnExec);
        execCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBox checkBox = (JCheckBox) e.getSource();
                trapOnExec = checkBox.isSelected();
            }
        });
        final JCheckBox gcCheckBox = new InspectorCheckBox(inspection(), "Enable During GC", "New watchpoints by default area enabled during GC", enableDuringGC);
        gcCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final JCheckBox checkBox = (JCheckBox) e.getSource();
                enableDuringGC = checkBox.isSelected();
            }
        });

        final JPanel panel2 = new InspectorPanel(inspection(), new BorderLayout());

        final JPanel watchpointPanel = new InspectorPanel(inspection());
        watchpointPanel.add(new TextLabel(inspection(), "Watchpoint defaults:  "));
        watchpointPanel.add(readCheckBox);
        watchpointPanel.add(writeCheckBox);
        watchpointPanel.add(execCheckBox);
        watchpointPanel.add(gcCheckBox);
        panel2.add(watchpointPanel, BorderLayout.WEST);

        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        panel.add(super.getPanel(), BorderLayout.NORTH);
        panel.add(panel2, BorderLayout.SOUTH);
        return panel;

    }
}
