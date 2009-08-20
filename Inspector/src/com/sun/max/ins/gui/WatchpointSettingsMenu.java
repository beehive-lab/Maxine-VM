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
package com.sun.max.ins.gui;

import java.awt.event.*;

import javax.swing.*;

import com.sun.max.tele.*;


/**
 * A menu that permits editing the settings of a {@linkplain MaxWatchpoint memory watchpoint?} in the VM.
 *
 * @author Michael Van De Vanter
 */
public class WatchpointSettingsMenu extends JMenu {


    public WatchpointSettingsMenu(final MaxWatchpoint watchpoint, String title) {
        super(title);
        if (watchpoint == null) {
            setEnabled(false);
        } else {
            final JCheckBoxMenuItem readItem = new JCheckBoxMenuItem("Trap on read", watchpoint.isRead());
            readItem.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    final JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getItem();
                    watchpoint.setRead(item.getState());
                }
            });
            add(readItem);
            final JCheckBoxMenuItem writeItem = new JCheckBoxMenuItem("Trap on write", watchpoint.isWrite());
            writeItem.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    final JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getItem();
                    watchpoint.setWrite(item.getState());
                }
            });
            add(writeItem);
            final JCheckBoxMenuItem execItem = new JCheckBoxMenuItem("Trap on exec", watchpoint.isExec());
            execItem.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    final JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getItem();
                    watchpoint.setExec(item.getState());
                }
            });
            add(execItem);
            final JCheckBoxMenuItem enabledDuringGCItem = new JCheckBoxMenuItem("Enabled during GC", watchpoint.isEnabledDuringGC());
            enabledDuringGCItem.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    final JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getItem();
                    watchpoint.setEnabledDuringGC(item.getState());
                }
            });
            add(enabledDuringGCItem);
        }
    }

    public WatchpointSettingsMenu(MaxWatchpoint watchpoint) {
        this(watchpoint, "Change memory watchpoint settings");
    }


}
